package com.callbackdev.chiaro.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One weather fetch persisted as a git-style "commit" for the Logs screen
 * (`history.diff`): short hash, fixed author, timestamp and a flattened
 * key→value snapshot (JSON) that Fase 8 diffs against the previous entry.
 * [forecastJson] (Fase 9h) is the same-shaped flatten of the daily forecast for
 * the next two target dates, diffed per-date in `forecast.diff`; null on
 * rows written before the column existed. [firedRulesJson] (Fase 11) is the JSON
 * array of user-rule names that fired on this data — the commit's check lines;
 * null when nothing fired (the overwhelmingly common case). [skyRunsJson] (Fase 16e)
 * is the same idea for the sky module: the jobs whose instant this fetch was the
 * first to observe as past, with the verdict the data carried at that moment.
 */
@Entity(tableName = "weather_history")
data class WeatherHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "city_key") val cityKey: String,
    @ColumnInfo(name = "city_label") val cityLabel: String,
    val hash: String,
    val author: String,
    @ColumnInfo(name = "timestamp_epoch_s") val timestampEpochSeconds: Long,
    @ColumnInfo(name = "snapshot_json") val snapshotJson: String,
    @ColumnInfo(name = "forecast_json") val forecastJson: String? = null,
    @ColumnInfo(name = "fired_rules") val firedRulesJson: String? = null,
    @ColumnInfo(name = "sky_runs") val skyRunsJson: String? = null
)

@Dao
interface WeatherHistoryDao {

    @Insert
    suspend fun insert(entry: WeatherHistoryEntry): Long

    @Query(
        "SELECT * FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT :limit"
    )
    suspend fun historyFor(cityKey: String, limit: Int): List<WeatherHistoryEntry>

    @Query("SELECT * FROM weather_history ORDER BY timestamp_epoch_s DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<WeatherHistoryEntry>>

    /**
     * [historyFor] as a Flow: the Journal follows one city's commits instead of
     * re-reading the table on a timer. Room re-emits on write and only on write, so
     * a screen that is open sees a fetch land and an idle screen costs nothing.
     */
    @Query(
        "SELECT * FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT :limit"
    )
    fun observeFor(cityKey: String, limit: Int): Flow<List<WeatherHistoryEntry>>

    /**
     * The retention that matters: **per city**. The global [prune] below had been the
     * only one, so four saved places shared one hundred rows and each city's Journal
     * got twenty-five — a diary whose depth depended on how many other places you
     * follow, and a drift strip eating half of it.
     */
    @Query(
        "DELETE FROM weather_history WHERE city_key = :cityKey AND id NOT IN " +
            "(SELECT id FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT :keep)"
    )
    suspend fun pruneCity(cityKey: String, keep: Int)

    /**
     * The backstop, now only a backstop: it bounds the table when places come and go
     * (removing a place does not delete its commits) and evicts the globally oldest
     * rows, which for an abandoned place is exactly the right ones. Sized so it can
     * never be what truncates an active city's history.
     */
    @Query(
        "DELETE FROM weather_history WHERE id NOT IN " +
            "(SELECT id FROM weather_history ORDER BY timestamp_epoch_s DESC LIMIT :keep)"
    )
    suspend fun prune(keep: Int)

    /**
     * The third retention, and the only one that asks *whose* rows these are: every
     * commit of a place the app no longer follows, once that place has been gone long
     * enough to be gone for real.
     *
     * The two prunes above bound the table; neither empties it. A removed place and,
     * far more often, every ~1.1 km cell the GPS pseudo-city has ever minted keep a
     * hundred commits each until the global backstop happens to reach them — and it
     * reaches them by global age, so an abandoned cell's rows are evicted at the same
     * rate as a followed city's. That is the wrong question: those rows are not old,
     * they are ORPHANED, and no future fetch will ever read them again.
     *
     * All or nothing per key, never row by row: a key is dropped only when its NEWEST
     * row predates [cutoffEpochSeconds], so a place removed by a swipe keeps its whole
     * Journal for as long as the undo — and the re-add — can still want it. Half a
     * Journal is a diary with pages torn out, which is the one thing this table must
     * not be.
     */
    @Query(
        "DELETE FROM weather_history WHERE city_key NOT IN (:liveKeys) AND city_key IN " +
            "(SELECT city_key FROM weather_history GROUP BY city_key " +
            "HAVING MAX(timestamp_epoch_s) < :cutoffEpochSeconds)"
    )
    suspend fun pruneForeign(liveKeys: List<String>, cutoffEpochSeconds: Long)

    /**
     * Attaches fired user rules to the city's newest commit (Fase 11). An UPDATE
     * after the fact, not an insert-time field: the worker evaluates rules after
     * the fetch has committed — and on a cache HIT the data the rules ran on IS
     * that latest commit.
     */
    @Query(
        "UPDATE weather_history SET fired_rules = :firedRulesJson WHERE id = " +
            "(SELECT id FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT 1)"
    )
    suspend fun setFiredRulesOnLatest(cityKey: String, firedRulesJson: String)

    /**
     * Attaches the sky jobs this fetch observed as run (Fase 16e), by the same
     * after-the-fact UPDATE `fired_rules` uses and for the same reason: the worker
     * evaluates once the fetch has already committed, and on a cache HIT the data
     * the jobs were judged against IS that latest commit.
     */
    @Query(
        "UPDATE weather_history SET sky_runs = :skyRunsJson WHERE id = " +
            "(SELECT id FROM weather_history WHERE city_key = :cityKey " +
            "ORDER BY timestamp_epoch_s DESC LIMIT 1)"
    )
    suspend fun setSkyRunsOnLatest(cityKey: String, skyRunsJson: String)
}

@Database(
    entities = [WeatherHistoryEntry::class, WarningRecordEntity::class],
    version = 5,
    exportSchema = false
)
abstract class ChiaroDatabase : RoomDatabase() {
    abstract fun weatherHistoryDao(): WeatherHistoryDao
    abstract fun warningRecordDao(): WarningRecordDao

    companion object {
        /** Every migration, in order: the one list the builder and the tests share. */
        val MIGRATIONS: Array<Migration>
            get() = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

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

        /**
         * v4 (Fase 16e): the sky jobs this fetch observed as run.
         *
         * One nullable column rather than the table, DAO, recorder and second
         * pruning policy `VISION_SKY.md` first drafted — see §8.1. A sky run is not
         * an independent event, it is something a FETCH noticed, and attaching it to
         * the commit that noticed says so structurally: `obs`, the distance between
         * the event and its observation, stops being a number the recorder computes
         * and becomes one the schema implies.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE weather_history ADD COLUMN sky_runs TEXT")
            }
        }

        /**
         * v5 (Fase 11, 9 set 2026): the official warnings' Journal rows. Additive — a
         * new table, `weather_history` untouched — for the reason [WarningRecordEntity]
         * gives: a bulletin is a document with a validity window, not something a fetch
         * observed, and its rows must exist on days no fetch succeeded.
         */
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
    }
}
