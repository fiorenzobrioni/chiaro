package com.callbackdev.chiaro.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The backstop `warning_records` did not have (12 set 2026). [WarningRecordDao.pruneCity]
 * bounds the place it is handed and no other, so this table was the only store in the
 * app with no ceiling at all: every key that stopped being handed to it kept its rows
 * for the life of the install.
 */
@RunWith(RobolectricTestRunner::class)
class WarningRecordDaoTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var database: ChiaroDatabase
    private val dao get() = database.warningRecordDao()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ChiaroDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = database.close()

    private fun insert(cityKey: String, atEpochSeconds: Long) = runBlocking {
        dao.insert(
            WarningRecordEntity(
                cityKey = cityKey,
                recordedEpochSeconds = atEpochSeconds,
                kind = WarningRecordKind.BULLETIN_MISSED.name
            )
        )
    }

    @Test
    fun `the global prune bounds the table across places, newest kept`() = runBlocking {
        repeat(4) { insert("a", 100L + it) }
        repeat(4) { insert("b", 200L + it) }

        dao.prune(keep = 5)

        // Five rows survive in all, and they are the five newest wherever they live:
        // b's four, plus a's last.
        assertEquals(1, dao.recordsFor("a", 100).size)
        assertEquals(103L, dao.recordsFor("a", 100).single().recordedEpochSeconds)
        assertEquals(4, dao.recordsFor("b", 100).size)
    }

    @Test
    fun `the per-place prune leaves the other place alone`() = runBlocking {
        repeat(3) { insert("a", 100L + it) }
        repeat(3) { insert("b", 200L + it) }

        dao.pruneCity("a", keep = 1)

        assertEquals(1, dao.recordsFor("a", 100).size)
        assertEquals(3, dao.recordsFor("b", 100).size)
    }
}
