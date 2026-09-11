package com.callbackdev.chiaro.sync

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.data.ServiceLocator
import com.callbackdev.chiaro.data.local.ChiaroDatabase
import com.callbackdev.chiaro.data.local.WarningRecordKind
import com.callbackdev.chiaro.data.warnings.OfficialWarningStore
import com.callbackdev.chiaro.data.warnings.WarningFetchFailure
import com.callbackdev.chiaro.data.warnings.WarningFetchResult
import com.callbackdev.chiaro.data.warnings.WarningFetchState
import com.callbackdev.chiaro.data.warnings.WarningSource
import com.callbackdev.chiaro.domain.Alert
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.model.WeatherReport
import com.callbackdev.chiaro.domain.rules.RuleTrigger
import com.callbackdev.chiaro.domain.settings.NotificationSettings
import com.callbackdev.chiaro.domain.settings.UnitSettings
import com.callbackdev.chiaro.domain.warnings.WarningBulletin
import com.callbackdev.chiaro.domain.warnings.WarningHazard
import com.callbackdev.chiaro.domain.warnings.WarningLevel
import com.callbackdev.chiaro.domain.warnings.WarningNotification
import com.callbackdev.chiaro.domain.warnings.WarningZoneIndex
import com.callbackdev.chiaro.domain.warnings.ZoneWarning
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * The warnings leg of the job with a scripted source and a recording notifier: when
 * it asks the network, what it keeps, what it writes in the Journal, when it speaks.
 * The zone index is the real asset, read off the data module's tree.
 */
@RunWith(RobolectricTestRunner::class)
class OfficialWarningsStepTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val rome: ZoneId = ZoneId.of("Europe/Rome")
    private lateinit var database: ChiaroDatabase
    private lateinit var store: OfficialWarningStore
    private val source = ScriptedSource()
    private val notifiers = RecordingNotifiers()

    private val milano = City(
        3_173_435, "Milano", "Lombardia", "Italia", Coordinates(45.4643, 9.1895), "Europe/Rome",
        countryCode = "IT", admin3 = "Comune di Milano"
    )
    private val lugano = City(
        2_659_836, "Lugano", "Ticino", "Svizzera", Coordinates(46.0037, 8.9511), "Europe/Zurich",
        countryCode = "CH", admin3 = "Lugano"
    )

    private val sept9: LocalDate = LocalDate.of(2026, 9, 9)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, ChiaroDatabase::class.java)
            .allowMainThreadQueries().build()
        store = OfficialWarningStore(
            PreferenceDataStoreFactory.create(scope = scope) {
                tmp.newFile("warnings-${System.nanoTime()}.preferences_pb")
            },
            Json { ignoreUnknownKeys = true }
        )
        val index = WarningZoneIndex.decode(File("../data/src/main/assets/warning_zones_it.json").readText())
        ServiceLocator.overrideForTests(
            warningStore = store,
            warningZoneIndex = index,
            warningSource = source,
            warningRecordDao = database.warningRecordDao()
        )
    }

    @After
    fun tearDown() {
        ServiceLocator.overrideForTests()
        database.close()
        scope.cancel()
    }

    private fun row(day: LocalDate, hazard: WarningHazard, level: WarningLevel) =
        ZoneWarning("Lomb-09", day, hazard, level)

    private fun bulletin(issued: LocalDateTime, id: String, vararg rows: ZoneWarning) = WarningBulletin(
        id = id, issuedAt = issued, days = listOf(issued.toLocalDate(), issued.toLocalDate().plusDays(1)),
        note = null, warnings = rows.toList()
    )

    private fun fresh(bulletin: WarningBulletin, stamp: String = "20260909_1546") =
        WarningFetchResult.Fresh(bulletin, stamp, feedTag = "\"tag\"")

    private fun run(
        city: City = milano,
        at: LocalDateTime,
        settings: NotificationSettings = NotificationSettings(),
        force: Boolean = false,
        silent: Boolean = false
    ) = runBlocking {
        OfficialWarningsStep(context).run(
            active = city,
            cityKey = city.id.toString(),
            others = listOf(city),
            settings = settings,
            notifiers = if (silent) null else notifiers,
            force = force,
            now = at.atZone(rome).toInstant()
        )
    }

    private fun records() = runBlocking { database.warningRecordDao().recordsFor(milano.cacheKey, 20) }

    @Test
    fun `a place outside every zone never touches the network`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)))
        run(city = lugano, at = sept9.atTime(16, 0))
        assertEquals(emptyList<WarningFetchState>(), source.calls)
        assertEquals(0, notifiers.warnings.size)
    }

    @Test
    fun `a fresh bulletin is kept, and an orange one is announced once`() {
        val orange = bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE))
        source.next = fresh(orange)
        run(at = sept9.atTime(16, 0))

        val state = runBlocking { store.state("dpc").first() }
        assertEquals("20260909_1546", state.current?.stamp)
        assertEquals(orange, state.current?.bulletin)
        assertEquals("\"tag\"", state.feedTag)
        assertEquals(1, notifiers.warnings.size)
        assertEquals("3173435:warn:B1:ORANGE", notifiers.warnings.single().fingerprint)
        assertEquals(setOf("3173435:warn:B1:ORANGE"), runBlocking { store.notified.first() })
        // The first bulletin a place sees is its state, not news for the Journal.
        assertEquals(0, records().size)
        assertEquals(WarningFetchState(stamp = null, feedTag = null), source.calls.single())
    }

    @Test
    fun `today's bulletin in hand - the cadence waits an hour, and nothing is said twice`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)))
        run(at = sept9.atTime(16, 0))
        run(at = sept9.atTime(16, 20))
        assertEquals(1, source.calls.size)

        source.next = WarningFetchResult.Unchanged("\"tag\"")
        run(at = sept9.atTime(17, 5))
        assertEquals(2, source.calls.size)
        assertEquals(WarningFetchState(stamp = "20260909_1546", feedTag = "\"tag\""), source.calls.last())
        assertEquals(1, notifiers.warnings.size)
    }

    @Test
    fun `an update that raises the level is announced again and written in the Journal`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)))
        run(at = sept9.atTime(16, 0))

        source.next = fresh(
            bulletin(
                sept9.atTime(17, 30), "B1",
                row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE),
                row(sept9, WarningHazard.HYDROGEOLOGICAL, WarningLevel.RED)
            ),
            stamp = "20260909_1730"
        )
        run(at = sept9.atTime(17, 40))

        assertEquals(listOf("3173435:warn:B1:ORANGE", "3173435:warn:B1:RED"), notifiers.warnings.map { it.fingerprint })
        val change = records().single()
        assertEquals(WarningRecordKind.LEVEL_CHANGE.name, change.kind)
        assertEquals("HYDROGEOLOGICAL", change.hazard)
        assertEquals("NONE", change.fromLevel)
        assertEquals("RED", change.toLevel)
        assertEquals("2026-09-09", change.day)
        assertEquals("Nodo Idraulico di Milano", change.zoneName)
    }

    @Test
    fun `a failure after the hour writes one missed row a day, before the hour none`() {
        source.next = WarningFetchResult.Failed(WarningFetchFailure.OFFLINE)
        run(at = sept9.atTime(16, 0))
        run(at = sept9.atTime(16, 30))
        val missed = records()
        assertEquals(1, missed.size)
        assertEquals(WarningRecordKind.BULLETIN_MISSED.name, missed.single().kind)
        assertNull(missed.single().bulletinId)

        run(at = sept9.plusDays(1).atTime(10, 0))
        assertEquals(1, records().size)
        run(at = sept9.plusDays(1).atTime(16, 0))
        assertEquals(2, records().size)
    }

    @Test
    fun `a failure keeps the bulletin in hand`() {
        val held = bulletin(sept9.minusDays(1).atTime(15, 19), "B0", row(sept9, WarningHazard.HYDRAULIC, WarningLevel.YELLOW))
        source.next = fresh(held, stamp = "20260908_1519")
        run(at = sept9.atTime(9, 0))
        source.next = WarningFetchResult.Failed(WarningFetchFailure.SERVICE)
        run(at = sept9.atTime(16, 0))
        assertEquals("20260908_1519", runBlocking { store.state("dpc").first() }.current?.stamp)
        assertEquals(1, records().count { it.kind == WarningRecordKind.BULLETIN_MISSED.name })
    }

    // --- the screen's own run (11 set 2026): fetches, never speaks -------------------

    @Test
    fun `a run with no notifiers keeps the bulletin and says nothing`() {
        val orange = bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE))
        source.next = fresh(orange)
        run(at = sept9.atTime(16, 0), silent = true)

        // The content landed: this is the whole point of the screen being able to ask.
        assertEquals("20260909_1546", runBlocking { store.state("dpc").first() }.current?.stamp)
        assertEquals(0, notifiers.warnings.size)
    }

    @Test
    fun `a silent run leaves the fingerprint unburnt, so the job can still speak`() {
        val orange = bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE))
        source.next = fresh(orange)
        run(at = sept9.atTime(16, 0), silent = true)
        assertEquals(0, notifiers.warnings.size)

        // The reader walked away; the job runs next and the warning is still news.
        source.next = WarningFetchResult.Unchanged("\"tag\"")
        run(at = sept9.atTime(17, 5))
        assertEquals(1, notifiers.warnings.size)
    }

    @Test
    fun `a silent run still writes the Journal, because a level moved either way`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.YELLOW)))
        run(at = sept9.atTime(16, 0), silent = true)
        val first = records().size

        source.next = fresh(
            bulletin(sept9.atTime(17, 0), "B2", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)),
            stamp = "20260909_1700"
        )
        run(at = sept9.atTime(17, 30), force = true, silent = true)
        assertTrue(
            "the diary is content, not speech",
            records().count { it.kind == WarningRecordKind.LEVEL_CHANGE.name } > first
        )
    }

    @Test
    fun `with the switch off the bulletin is kept and nothing is said`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)))
        run(at = sept9.atTime(16, 0), settings = NotificationSettings(officialWarnings = false))
        assertEquals("20260909_1546", runBlocking { store.state("dpc").first() }.current?.stamp)
        assertEquals(0, notifiers.warnings.size)
    }

    @Test
    fun `below the reader's threshold nothing is said, and green never is`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.HYDRAULIC, WarningLevel.YELLOW)))
        run(at = sept9.atTime(16, 0), settings = NotificationSettings(officialWarningsFrom = WarningLevel.ORANGE))
        assertEquals(0, notifiers.warnings.size)
        run(at = sept9.atTime(16, 1), settings = NotificationSettings(officialWarningsFrom = WarningLevel.YELLOW))
        assertEquals(1, notifiers.warnings.size)

        source.next = fresh(bulletin(sept9.atTime(17, 0), "B2"), stamp = "20260909_1700")
        run(at = sept9.atTime(17, 30), force = true)
        assertEquals(1, notifiers.warnings.size)
    }

    @Test
    fun `an unposted notification keeps its chance`() {
        source.next = fresh(bulletin(sept9.atTime(15, 46), "B1", row(sept9, WarningHazard.THUNDERSTORM, WarningLevel.ORANGE)))
        notifiers.posts = false
        run(at = sept9.atTime(16, 0))
        assertEquals(1, notifiers.warnings.size)
        assertEquals(emptySet<String>(), runBlocking { store.notified.first() })

        notifiers.posts = true
        source.next = WarningFetchResult.Unchanged("\"tag\"")
        run(at = sept9.atTime(16, 10))
        assertEquals(2, notifiers.warnings.size)
        assertEquals(setOf("3173435:warn:B1:ORANGE"), runBlocking { store.notified.first() })
    }

    private class ScriptedSource : WarningSource {
        override val id = "dpc"
        override val zone: ZoneId = ZoneId.of("Europe/Rome")
        var next: WarningFetchResult = WarningFetchResult.Unchanged(null)
        val calls = mutableListOf<WarningFetchState>()
        override suspend fun fetch(known: WarningFetchState, allowLargeDownload: Boolean): WarningFetchResult {
            calls += known
            return next
        }
    }

    private class RecordingNotifiers : SyncNotifiers {
        var posts = true
        val warnings = mutableListOf<WarningNotification>()
        override fun notificationsEnabled() = true
        override fun notifyAlert(alert: Alert, report: WeatherReport, units: UnitSettings) = true
        override fun notifyRule(
            trigger: RuleTrigger, cityLabel: String, report: WeatherReport, now: LocalDateTime, units: UnitSettings
        ) = true
        override suspend fun rearmSkyReminders() = Unit
        override fun notifyOfficialWarning(notification: WarningNotification, city: City, today: LocalDate): Boolean {
            warnings += notification
            return posts
        }
    }
}
