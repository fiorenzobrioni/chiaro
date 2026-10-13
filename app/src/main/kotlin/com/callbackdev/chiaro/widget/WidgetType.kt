package com.callbackdev.chiaro.widget

/**
 * **The widgets' type scale** (widget review, 13 ott 2026): the sizes every home-screen
 * card prints in, by role. Until this file each card had grown its own — twelve fixed
 * sizes between 11 and 34 sp, with the same role at a different size on each card (a
 * place at 16 on one and 13 on the next, a moment's name at 15 and 13, a strip's hour at
 * 12 over a week's day at 11) — so two cards side by side on one home screen read as two
 * families. A card's constants now name a step of this scale rather than a number, and
 * `WidgetTypeTest` fails on any widget size that is not one of them.
 *
 * A card is drawn by `RemoteViews` at a size the launcher grants, so these are not
 * Material's roles (DESIGN §5): the app's own scale cannot reach a widget, which is drawn
 * in the phone's sans and never sees the bundled face.
 *
 * Six steps, each a role:
 * - [Caption] — what is read last: the stale marker, a plot's hour ticks, a strip's rain
 *   and a day's low.
 * - [Label] — a row of a list and a cell of a strip: an agenda line, an hour's label and
 *   its figure, a chip's word, the line under a hero sentence.
 * - [Body] — the second thing read: the day's sentence, the place, a moment's name.
 * - [Title] — the figure of a card whose hero is something else: the arc's number on a
 *   one-row or one-cell card, where the drawing is what the card is for.
 * - [Display] — a hero that is wider than a temperature: a clock («7:55 PM» is twice the
 *   glyphs of «−12°», and does not fit a four-cell card's words at 34), and the arc's
 *   number where it heads a panel beside a sentence.
 * - [Hero] — the current temperature on the weather cards; where a card scales its hero
 *   to the grant (the tall Now card, the text card) this is where it starts from, and
 *   the grant decides the rest.
 *
 * One size stays off the scale, and on purpose: the text card's sentence at 18 sp
 * ([TextSentenceSp]), the committente's on the device (19 set 2026), because on a card
 * with no drawing the sentence is the second thing read and «17 over 16 is not a rank».
 */
internal object WidgetType {
    const val Caption = 11f
    const val Label = 13f
    const val Body = 16f
    const val Title = 22f
    const val Display = 30f
    const val Hero = 34f

    /** Every step, for the test that holds the cards to them. */
    val steps: Set<Float> = setOf(Caption, Label, Body, Title, Display, Hero)
}
