package com.callbackdev.chiaro.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * What the Journal says about the official warnings (Fase 11): one row when, for a
 * place, a level appeared, rose or fell between two consecutive bulletins, and one row
 * — a day at most — when the day's bulletin could not be reached. Its own table and
 * not a column on `weather_history`: a bulletin is a document with a validity, not
 * something a fetch observed (the `sky_runs` argument does not apply), and the row
 * has to exist on days no fetch succeeded.
 *
 * Levels, hazards and dates are stored as their names and ISO strings: the Journal
 * renders them in the reader's language at read time, and a row written by a future
 * build with a hazard this one does not know is a row it can still list.
 */
@Entity(tableName = "warning_records")
data class WarningRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** `City.cacheKey`, like `weather_history`, so the Journal joins on one key. */
    @ColumnInfo(name = "city_key") val cityKey: String,
    @ColumnInfo(name = "recorded_epoch_s") val recordedEpochSeconds: Long,
    /** A [WarningRecordKind] name. */
    val kind: String,
    @ColumnInfo(name = "bulletin_id") val bulletinId: String? = null,
    /** The bulletin's issue time in the issuer's zone, ISO local date-time. */
    @ColumnInfo(name = "issued_at") val issuedAt: String? = null,
    @ColumnInfo(name = "zone_name") val zoneName: String? = null,
    /** ISO date of the day the level concerns. */
    val day: String? = null,
    /** A `WarningHazard` name. */
    val hazard: String? = null,
    /** `WarningLevel` names. */
    @ColumnInfo(name = "from_level") val fromLevel: String? = null,
    @ColumnInfo(name = "to_level") val toLevel: String? = null
)

enum class WarningRecordKind {
    /** A level moved for one day and hazard; `from_level` → `to_level`. */
    LEVEL_CHANGE,
    /** The day's bulletin was looked for and not reached; one per day. */
    BULLETIN_MISSED
}

@Dao
interface WarningRecordDao {

    @Insert
    suspend fun insert(record: WarningRecordEntity): Long

    @Query(
        "SELECT * FROM warning_records WHERE city_key = :cityKey " +
            "ORDER BY recorded_epoch_s DESC, id DESC LIMIT :limit"
    )
    suspend fun recordsFor(cityKey: String, limit: Int): List<WarningRecordEntity>

    /** The Journal follows one place's rows, as it does the history commits. */
    @Query(
        "SELECT * FROM warning_records WHERE city_key = :cityKey " +
            "ORDER BY recorded_epoch_s DESC, id DESC LIMIT :limit"
    )
    fun observeFor(cityKey: String, limit: Int): Flow<List<WarningRecordEntity>>

    /** Per place, like `weather_history`'s: a busy autumn must not shorten a quiet town's diary. */
    @Query(
        "DELETE FROM warning_records WHERE city_key = :cityKey AND id NOT IN " +
            "(SELECT id FROM warning_records WHERE city_key = :cityKey " +
            "ORDER BY recorded_epoch_s DESC, id DESC LIMIT :keep)"
    )
    suspend fun pruneCity(cityKey: String, keep: Int)

    /**
     * The backstop this table did not have, and `weather_history` did (12 set 2026).
     * [pruneCity] bounds the place it is handed and no other, so every key that stops
     * being handed to it — a removed place, and every ~1.1 km cell the GPS pseudo-city
     * has ever minted — kept its rows for the life of the install: the only table in
     * the app with no ceiling at all. Same shape and same size as the history's, for
     * the same reason: it must never be what truncates a place somebody still follows.
     */
    @Query(
        "DELETE FROM warning_records WHERE id NOT IN " +
            "(SELECT id FROM warning_records ORDER BY recorded_epoch_s DESC, id DESC LIMIT :keep)"
    )
    suspend fun prune(keep: Int)

    /**
     * Rows of places the app no longer follows, dropped once the place has been gone
     * long enough to be gone for real — the twin of `WeatherHistoryDao.pruneForeign`,
     * all or nothing per key for the reason given there.
     */
    @Query(
        "DELETE FROM warning_records WHERE city_key NOT IN (:liveKeys) AND city_key IN " +
            "(SELECT city_key FROM warning_records GROUP BY city_key " +
            "HAVING MAX(recorded_epoch_s) < :cutoffEpochSeconds)"
    )
    suspend fun pruneForeign(liveKeys: List<String>, cutoffEpochSeconds: Long)
}
