package com.callbackdev.chiaro.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Each migration must carry the reader's history across the update: a database is
 * hand-built with an old schema exactly as Room generated it (the test pins it — if it
 * drifts, opening fails Room's validation), then opened with today's Room and the
 * migrations, all of them in a row.
 */
@RunWith(RobolectricTestRunner::class)
class ChiaroDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"
    private var database: ChiaroDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(dbName)
    }

    private fun open(): ChiaroDatabase =
        Room.databaseBuilder(context, ChiaroDatabase::class.java, dbName)
            .addMigrations(*ChiaroDatabase.MIGRATIONS)
            .build()
            .also { database = it }

    private fun build(version: Int, vararg sql: String) {
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            sql.forEach(db::execSQL)
            db.version = version
        }
    }

    /** The exact v5 schema: v1 + forecast_json + fired_rules + sky_runs, and Fase 11's table. */
    private val v5 = arrayOf(
        "CREATE TABLE IF NOT EXISTS `weather_history` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`city_key` TEXT NOT NULL, `city_label` TEXT NOT NULL, " +
            "`hash` TEXT NOT NULL, `author` TEXT NOT NULL, " +
            "`timestamp_epoch_s` INTEGER NOT NULL, `snapshot_json` TEXT NOT NULL, " +
            "`forecast_json` TEXT, `fired_rules` TEXT, `sky_runs` TEXT)",
        "CREATE TABLE IF NOT EXISTS `warning_records` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`city_key` TEXT NOT NULL, `recorded_epoch_s` INTEGER NOT NULL, " +
            "`kind` TEXT NOT NULL, `bulletin_id` TEXT, `issued_at` TEXT, " +
            "`zone_name` TEXT, `day` TEXT, `hazard` TEXT, " +
            "`from_level` TEXT, `to_level` TEXT)"
    )

    /**
     * v5 → v6 (14 ott 2026): the Journal left, and what only it read leaves with it. The
     * forecasts and the fired rules stay, row by row and id by id; a row with no forecast
     * carries nothing anyone reads and is not copied; `warning_records` is gone.
     */
    @Test
    fun `v5 keeps its forecasts and fired rules and drops what only the Journal read`() {
        build(
            5, *v5,
            "INSERT INTO weather_history (id, city_key, city_label, hash, author, " +
                "timestamp_epoch_s, snapshot_json, forecast_json, fired_rules, sky_runs) VALUES " +
                "(7, '4546:919', 'Milano, Lombardia', 'a1b2c3d', 'sys@chiaro.app', 1755000000, " +
                "'{\"current.temp_c\":\"31.0\"}', '{\"2026-08-18.high_c\":\"27.0\"}', " +
                "'[\"umbrella\"]', '[]')",
            "INSERT INTO weather_history (id, city_key, city_label, hash, author, " +
                "timestamp_epoch_s, snapshot_json) VALUES " +
                "(3, '4546:919', 'Milano, Lombardia', '0f0f0f0', 'sys@chiaro.app', 1754990000, '{}')",
            "INSERT INTO warning_records (city_key, recorded_epoch_s, kind) " +
                "VALUES ('4546:919', 1755000000, 'BULLETIN_MISSED')"
        )

        val db = open()
        val rows = runBlocking { db.weatherHistoryDao().historyFor("4546:919", 10) }
        val kept = rows.single()
        assertEquals(7L, kept.id)
        assertEquals(1755000000L, kept.timestampEpochSeconds)
        assertEquals("{\"2026-08-18.high_c\":\"27.0\"}", kept.forecastJson)
        assertEquals("[\"umbrella\"]", kept.firedRulesJson)

        db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'warning_records'"
        ).use { assertFalse("the Journal's table is gone", it.moveToFirst()) }
        db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' " +
                "AND name = 'index_weather_history_city_key_timestamp_epoch_s'"
        ).use { assertTrue("the table has its index", it.moveToFirst()) }
    }

    /** The oldest schema still opens through the whole chain, and the table works. */
    @Test
    fun `a v1 database walks every migration and takes new commits`() {
        build(
            1,
            "CREATE TABLE IF NOT EXISTS `weather_history` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`city_key` TEXT NOT NULL, `city_label` TEXT NOT NULL, " +
                "`hash` TEXT NOT NULL, `author` TEXT NOT NULL, " +
                "`timestamp_epoch_s` INTEGER NOT NULL, `snapshot_json` TEXT NOT NULL)",
            "INSERT INTO weather_history " +
                "(city_key, city_label, hash, author, timestamp_epoch_s, snapshot_json) " +
                "VALUES ('milan', 'Milan, Lombardy', 'a1b2c3d', 'sys@chiaro.app', " +
                "1755000000, '{\"current.temp_c\":\"31.0\"}')"
        )

        val db = open()
        val dao = db.weatherHistoryDao()
        // A v1 row had no forecast: nothing a reader of v6 can use.
        assertEquals(emptyList<WeatherHistoryEntry>(), runBlocking { dao.historyFor("milan", 10) })

        runBlocking {
            dao.insert(WeatherHistoryEntry(cityKey = "milan", timestampEpochSeconds = 1755003600, forecastJson = "{}"))
        }
        val newest = runBlocking { dao.latestFor("milan") }!!
        assertEquals(1755003600L, newest.timestampEpochSeconds)
        assertNull(newest.firedRulesJson)
        assertTrue(runBlocking { dao.any() })
    }
}
