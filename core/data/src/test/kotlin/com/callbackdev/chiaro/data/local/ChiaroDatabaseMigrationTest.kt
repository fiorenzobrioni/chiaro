package com.callbackdev.chiaro.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Migration 1→2 must carry the committente's real history across the update: a
 * database is hand-built with the exact v1 schema (this test pins it — if it
 * drifts from what Room generated, opening fails validation), then opened with
 * Room v2 + the migration.
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

    @Test
    fun `v1 rows survive the migration with a null forecast`() {
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v1 ->
            v1.execSQL(
                "CREATE TABLE IF NOT EXISTS `weather_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`city_key` TEXT NOT NULL, `city_label` TEXT NOT NULL, " +
                    "`hash` TEXT NOT NULL, `author` TEXT NOT NULL, " +
                    "`timestamp_epoch_s` INTEGER NOT NULL, `snapshot_json` TEXT NOT NULL)"
            )
            v1.execSQL(
                "INSERT INTO weather_history " +
                    "(city_key, city_label, hash, author, timestamp_epoch_s, snapshot_json) " +
                    "VALUES ('milan', 'Milan, Lombardy', 'a1b2c3d', 'sys@chiaro.app', " +
                    "1755000000, '{\"current.temp_c\":\"31.0\"}')"
            )
            v1.version = 1
        }

        val db = Room.databaseBuilder(context, ChiaroDatabase::class.java, dbName)
            .addMigrations(*ChiaroDatabase.MIGRATIONS)
            .build()
            .also { database = it }

        val migrated = runBlocking { db.weatherHistoryDao().historyFor("milan", 10) }
        val old = migrated.single()
        assertEquals("a1b2c3d", old.hash)
        assertEquals("{\"current.temp_c\":\"31.0\"}", old.snapshotJson)
        assertNull(old.forecastJson)
        assertNull("a v1 row has no sky runs either", old.skyRunsJson)

        // And the new column round-trips on fresh inserts
        runBlocking {
            db.weatherHistoryDao().insert(
                WeatherHistoryEntry(
                    cityKey = "milan",
                    cityLabel = "Milan, Lombardy",
                    hash = "9f8e7d6",
                    author = "sys@chiaro.app",
                    timestampEpochSeconds = 1755003600,
                    snapshotJson = "{}",
                    forecastJson = "{\"2026-08-18.high_c\":\"27.0\"}"
                )
            )
        }
        val newest = runBlocking { db.weatherHistoryDao().historyFor("milan", 10) }.first()
        assertEquals("{\"2026-08-18.high_c\":\"27.0\"}", newest.forecastJson)
    }

    @Test
    fun `v2 rows survive the migration with null fired rules`() {
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v2 ->
            // The exact v2 schema (v1 + Fase 9h's forecast_json), pinned like above
            v2.execSQL(
                "CREATE TABLE IF NOT EXISTS `weather_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`city_key` TEXT NOT NULL, `city_label` TEXT NOT NULL, " +
                    "`hash` TEXT NOT NULL, `author` TEXT NOT NULL, " +
                    "`timestamp_epoch_s` INTEGER NOT NULL, `snapshot_json` TEXT NOT NULL, " +
                    "`forecast_json` TEXT)"
            )
            v2.execSQL(
                "INSERT INTO weather_history " +
                    "(city_key, city_label, hash, author, timestamp_epoch_s, snapshot_json) " +
                    "VALUES ('milan', 'Milan, Lombardy', 'a1b2c3d', 'sys@chiaro.app', " +
                    "1755000000, '{}')"
            )
            v2.version = 2
        }

        val db = Room.databaseBuilder(context, ChiaroDatabase::class.java, dbName)
            .addMigrations(*ChiaroDatabase.MIGRATIONS)
            .build()
            .also { database = it }

        val migrated = runBlocking { db.weatherHistoryDao().historyFor("milan", 10) }.single()
        assertNull(migrated.firedRulesJson)

        // And the fired-rules UPDATE lands on the city's NEWEST commit only
        runBlocking {
            db.weatherHistoryDao().insert(
                WeatherHistoryEntry(
                    cityKey = "milan",
                    cityLabel = "Milan, Lombardy",
                    hash = "9f8e7d6",
                    author = "sys@chiaro.app",
                    timestampEpochSeconds = 1755003600,
                    snapshotJson = "{}"
                )
            )
            db.weatherHistoryDao().setFiredRulesOnLatest("milan", "[\"umbrella\"]")
        }
        val entries = runBlocking { db.weatherHistoryDao().historyFor("milan", 10) }
        assertEquals("[\"umbrella\"]", entries.first().firedRulesJson)
        assertNull(entries.last().firedRulesJson)
    }

    /** v4 → v5 (Fase 11) adds a table and touches nothing else: the history rows are
     * exactly where they were, and the new table takes and gives back a row. */
    @Test
    fun `v4 rows survive the migration and the warning records table is born beside them`() {
        val file = context.getDatabasePath(dbName).also { it.parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { v4 ->
            // The exact v4 schema: v1 + forecast_json + fired_rules + sky_runs.
            v4.execSQL(
                "CREATE TABLE IF NOT EXISTS `weather_history` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`city_key` TEXT NOT NULL, `city_label` TEXT NOT NULL, " +
                    "`hash` TEXT NOT NULL, `author` TEXT NOT NULL, " +
                    "`timestamp_epoch_s` INTEGER NOT NULL, `snapshot_json` TEXT NOT NULL, " +
                    "`forecast_json` TEXT, `fired_rules` TEXT, `sky_runs` TEXT)"
            )
            v4.execSQL(
                "INSERT INTO weather_history " +
                    "(city_key, city_label, hash, author, timestamp_epoch_s, snapshot_json, sky_runs) " +
                    "VALUES ('milan', 'Milan, Lombardy', 'a1b2c3d', 'sys@chiaro.app', " +
                    "1755000000, '{}', '[]')"
            )
            v4.version = 4
        }

        val db = Room.databaseBuilder(context, ChiaroDatabase::class.java, dbName)
            .addMigrations(*ChiaroDatabase.MIGRATIONS)
            .build()
            .also { database = it }

        val old = runBlocking { db.weatherHistoryDao().historyFor("milan", 10) }.single()
        assertEquals("a1b2c3d", old.hash)
        assertEquals("[]", old.skyRunsJson)

        runBlocking {
            db.warningRecordDao().insert(
                WarningRecordEntity(
                    cityKey = "milan",
                    recordedEpochSeconds = 1_757_430_000,
                    kind = WarningRecordKind.LEVEL_CHANGE.name,
                    bulletinId = "DPC_BULLETIN_2026_09_09_6506",
                    issuedAt = "2026-09-09T15:46",
                    zoneName = "Nodo Idraulico di Milano",
                    day = "2026-09-09",
                    hazard = "THUNDERSTORM",
                    fromLevel = "NONE",
                    toLevel = "ORANGE"
                )
            )
        }
        val record = runBlocking { db.warningRecordDao().recordsFor("milan", 10) }.single()
        assertEquals("ORANGE", record.toLevel)
        assertEquals("Nodo Idraulico di Milano", record.zoneName)
        assertEquals(emptyList<WarningRecordEntity>(), runBlocking { db.warningRecordDao().recordsFor("rome", 10) })
    }
}
