package com.callbackdev.chiaro.ui.alerts

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.callbackdev.chiaro.R
import com.callbackdev.chiaro.domain.model.City
import com.callbackdev.chiaro.domain.model.Coordinates
import com.callbackdev.chiaro.domain.rules.NotificationRule
import com.callbackdev.chiaro.domain.rules.RuleCondition
import com.callbackdev.chiaro.domain.rules.RuleOp
import com.callbackdev.chiaro.domain.rules.RulePlace
import com.callbackdev.chiaro.readme.MilanRecording
import com.callbackdev.chiaro.ui.format.LocalClock
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import java.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * A rule bound to a saved place says where on its card (15.12, 29 Oct 2026): «A Torino», with
 * the age of that place's forecast once it is past the stale bar, or «nessuna previsione
 * ancora»; a rule that follows the active place has no such line. A rule whose place is gone
 * says so, draws its switch off, and its switch opens «Dove vale» instead of turning anything
 * on; a place picked there is handed to [AlertsActions.choosePlace].
 *
 * In `testDebug` for the reason the Sky's sheet tests give: the Compose rule's activity comes
 * from a debug-only manifest.
 */
@RunWith(RobolectricTestRunner::class)
class AlertsPlaceTest {

    @get:Rule
    val compose = createComposeRule()

    private val app: Context = ApplicationProvider.getApplicationContext()
    private val zone = MilanRecording.alerts(app).zone

    private val turin = City(
        id = 3165524, name = "Torino", region = "Piemonte", country = "Italia",
        coordinates = Coordinates(45.0703, 7.6869), timezone = "Europe/Rome"
    )

    private fun rule(id: Long, name: String, place: RulePlace? = null, enabled: Boolean = true) = NotificationRule(
        id = id,
        name = name,
        enabled = enabled,
        conditions = listOf(RuleCondition("current.temp_c", RuleOp.GT, 15.0)),
        message = "Si va",
        place = place
    )

    private val chosen = mutableListOf<Pair<Long, Long?>>()

    /** Milan's page with [cards]: each rule with the place its card says. */
    private fun show(vararg cards: Pair<NotificationRule, RuleCardPlace?>) {
        val base = MilanRecording.alerts(app, rules = cards.map { it.first })
        val state = base.copy(
            rules = base.rules.zip(cards) { card, (_, place) -> card.copy(place = place) },
            savedPlaces = listOf(MilanRecording.city, turin)
        )
        compose.setContent {
            CompositionLocalProvider(LocalClock provides Clock.fixed(MilanRecording.now, zone)) {
                ChiaroTheme(dynamicColor = false) {
                    AlertsScreen(
                        state = state,
                        actions = AlertsActions(
                            {}, {}, {}, {}, {}, {}, {}, { _, _ -> },
                            choosePlace = { rule, place -> chosen += rule.id to place?.id }
                        ),
                        onEdit = {},
                        onOpenPlaces = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
    }

    private fun scrollTo(text: String) {
        // The page is a lazy list: a card is composed only once the list reaches it.
        compose.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasText(text))
    }

    @Test
    fun `a bound rule says where, and one that follows the active place does not`() {
        show(
            rule(1, "Qui") to null,
            rule(2, "Laggiù", RulePlace(turin.id, turin.name)) to RuleCardPlace.Saved("Torino")
        )
        val line = RuleText.placeAt(app.resources, "Torino")

        scrollTo(line)
        compose.onNodeWithText(line).assertExists()
        // One line of place on the page: the rule that follows the active place has none.
        compose.onAllNodesWithText(RuleText.placeAt(app.resources, MilanRecording.city.name)).assertCountEquals(0)
    }

    @Test
    fun `a bound place's old forecast, or none, is said on the place line`() {
        show(
            rule(1, "Vecchia", RulePlace(turin.id, turin.name)) to
                RuleCardPlace.Saved("Torino", staleSince = MilanRecording.now.minusSeconds(3 * 3_600)),
            rule(2, "Mai vista", RulePlace(3176219, "Genova")) to RuleCardPlace.Saved("Genova", noForecast = true)
        )
        val age = app.resources.getQuantityString(R.plurals.freshness_hours_ago, 3, 3)
            .replaceFirstChar { it.lowercase() }
        val old = RuleText.placeAt(app.resources, "Torino") + " · " + age
        val none = RuleText.placeAt(app.resources, "Genova") + " · " + app.getString(R.string.rule_place_no_forecast)

        scrollTo(none)
        compose.onNodeWithText(old).assertExists()
        compose.onNodeWithText(none).assertExists()
    }

    @Test
    fun `a rule whose place is gone says so, and its switch asks where`() {
        show(rule(7, "Neve", RulePlace(3181554, "Bormio")) to RuleCardPlace.Gone("Bormio"))
        val gone = app.getString(R.string.rule_place_gone, "Bormio")

        scrollTo(gone)
        compose.onNodeWithText(gone).assertExists()
        // The last switch of the page is the rule's: drawn off, though the rule is still on
        // until the job's next run, since on nowhere it cannot ring.
        val switch = compose.onAllNodes(isToggleable()).onLast()
        switch.assertIsOff()
        switch.performClick()

        compose.onNodeWithText(app.getString(R.string.rule_place_pick_title)).assertExists()
        compose.onNodeWithText(app.getString(R.string.rule_place_active)).assertExists()
        compose.onNodeWithText("Torino").performClick()
        compose.waitForIdle()
        assertEquals(listOf(7L to turin.id), chosen)
    }
}
