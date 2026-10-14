package com.callbackdev.chiaro.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * One weather fetch, kept for what the app reads back off it: the week it forecast
 * ([forecastJson], the [WeatherSnapshots.flattenForecast] map as JSON) and the reader's
 * rules that fired on its data ([firedRulesJson], a JSON array of names, null when none
 * did — the overwhelmingly common case).
 *
 * Three readers, all small (14 ott 2026, when the Journal that read the rest left the
 * app): Today's "what changed" diffs the newest forecasts, Today's quiet sentence takes
 * yesterday's high, and Alerts says when each rule last fired. The current-conditions
 * snapshot, the sky runs, the git-style hash and author and the place's label were the
 * Journal's alone and went with it ([ChiaroDatabase.MIGRATION_5_6]).
 */
@Entity(
    tableName = "weather_history",
    indices = [Index(value = ["city_key", "timestamp_epoch_s"])]
)
data class WeatherHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "city_key") val cityKey: String,
    @ColumnInfo(name = "timestamp_epoch_s") val timestampEpochSeconds: Long,
    @ColumnInfo(name = "forecast_json") val forecastJson: String,
    @ColumnInfo(name = "fired_rules") val firedRulesJson: String? = null
)

@Dao
interface WeatherHistoryDao {

    @Insert
    suspend fun insert(entry: WeatherHistoryEntry): Long

    /** Rewrites a commit in place: the fetch that lands inside the previous one's
     * window replaces it rather than standing beside it (see `WeatherRepository`). */
    @Update
    suspend fun update(entry: WeatherHistoryEntry)

    @Query(
        "SELECT * FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT :limit"
    )
    suspend fun historyFor(cityKey: String, limit: Int): List<WeatherHistoryEntry>

    @Query(
        "SELECT * FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT 1"
    )
    suspend fun latestFor(cityKey: String): WeatherHistoryEntry?

    /** Has this install ever written a commit, for any place. */
    @Query("SELECT EXISTS(SELECT 1 FROM weather_history)")
    suspend fun any(): Boolean

    /**
     * The retention that matters: **per place, by time**. A place keeps the commits of
     * the last [cutoffEpochSeconds]-worth of days, and never fewer than its newest
     * [keepAtLeast], so a phone left in a drawer comes back with a baseline to compare
     * the first new forecast against.
     *
     * By time and not by count since 14 ott 2026. The count it replaced (a hundred
     * commits) made the depth depend on how often the app fetched: four days at the
     * hourly cadence, a day for somebody who keeps the app open, less for somebody who
     * pulls to refresh. The readers ask about days (yesterday's high), not about rows.
     */
    @Query(
        "DELETE FROM weather_history WHERE city_key = :cityKey " +
            "AND timestamp_epoch_s < :cutoffEpochSeconds AND id NOT IN " +
            "(SELECT id FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT :keepAtLeast)"
    )
    suspend fun pruneCity(cityKey: String, cutoffEpochSeconds: Long, keepAtLeast: Int)

    /**
     * The backstop, and only a backstop: it evicts the globally oldest rows past a
     * ceiling sized so it can never be what truncates a place somebody still follows.
     */
    @Query(
        "DELETE FROM weather_history WHERE id NOT IN " +
            "(SELECT id FROM weather_history ORDER BY timestamp_epoch_s DESC LIMIT :keep)"
    )
    suspend fun prune(keep: Int)

    /**
     * Every commit of a place the app no longer follows, once that place has been gone
     * long enough to be gone for real ([StoredDataSweep]). All or nothing per key: a key
     * is dropped only when its NEWEST row predates [cutoffEpochSeconds], so a place
     * removed by a swipe keeps its history for as long as the undo, and the re-add, can
     * still want it.
     */
    @Query(
        "DELETE FROM weather_history WHERE city_key NOT IN (:liveKeys) AND city_key IN " +
            "(SELECT city_key FROM weather_history GROUP BY city_key " +
            "HAVING MAX(timestamp_epoch_s) < :cutoffEpochSeconds)"
    )
    suspend fun pruneForeign(liveKeys: List<String>, cutoffEpochSeconds: Long)

    /** The rules that fired on commit [id]'s data, as the repository merged them. */
    @Query("UPDATE weather_history SET fired_rules = :firedRulesJson WHERE id = :id")
    suspend fun setFiredRules(id: Long, firedRulesJson: String)
}

@Database(
    entities = [WeatherHistoryEntry::class],
    version = 6,
    exportSchema = false
)
abstract class ChiaroDatabase : RoomDatabase() {
    abstract fun weatherHistoryDao(): WeatherHistoryDao

    companion object {
        /** Every migration, in order: the one list the builder and the tests share. */
        val MIGRATIONS: Array<Migration>
            get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

        /** v2 (Fase 9h): forecast snapshot alongside the current-conditions one. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_history ADD COLUMN forecast_json TEXT")
            }
        }

        /** v3 (Fase 11): user rules fired on this commit's data. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_history ADD COLUMN fired_rules TEXT")
            }
        }

        /** v4 (Fase 16e): the sky jobs a fetch observed as run, for the Journal. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_history ADD COLUMN sky_runs TEXT")
            }
        }

        /** v5 (Fase 11, 9 set 2026): the official warnings' Journal rows. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `warning_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`city_key` TEXT NOT NULL, `recorded_epoch_s` INTEGER NOT NULL, " +
                        "`kind` TEXT NOT NULL, `bulletin_id` TEXT, `issued_at` TEXT, " +
                        "`zone_name` TEXT, `day` TEXT, `hazard` TEXT, " +
                        "`from_level` TEXT, `to_level` TEXT)"
                )
            }
        }

        /**
         * v6 (14 ott 2026): the Journal left the app, and what only it read leaves the
         * database. `warning_records` is dropped whole; `weather_history` keeps the
         * forecast and the fired rules and loses the snapshot, the sky runs, the hash,
         * the author and the label. A row with no forecast (written before v2) carries
         * nothing any reader wants and is not copied.
         *
         * A rebuild, not `ALTER TABLE … DROP COLUMN`: SQLite has that only from 3.35,
         * and minSdk 33 ships 3.32. It also gives the table the index its every query
         * has always wanted, `(city_key, timestamp_epoch_s)`, named the way Room names it
         * so the schema check after the migration finds exactly what the entity declares.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE `weather_history_new` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`city_key` TEXT NOT NULL, `timestamp_epoch_s` INTEGER NOT NULL, " +
                        "`forecast_json` TEXT NOT NULL, `fired_rules` TEXT)"
                )
                db.execSQL(
                    "INSERT INTO `weather_history_new` " +
                        "(`id`, `city_key`, `timestamp_epoch_s`, `forecast_json`, `fired_rules`) " +
                        "SELECT `id`, `city_key`, `timestamp_epoch_s`, `forecast_json`, `fired_rules` " +
                        "FROM `weather_history` WHERE `forecast_json` IS NOT NULL"
                )
                db.execSQL("DROP TABLE `weather_history`")
                db.execSQL("ALTER TABLE `weather_history_new` RENAME TO `weather_history`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_weather_history_city_key_timestamp_epoch_s` " +
                        "ON `weather_history` (`city_key`, `timestamp_epoch_s`)"
                )
                db.execSQL("DROP TABLE IF EXISTS `warning_records`")
            }
        }
    }
}
