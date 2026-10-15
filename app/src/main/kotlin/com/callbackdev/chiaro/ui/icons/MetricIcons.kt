package com.callbackdev.chiaro.ui.icons

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The details grid's eyebrows (DESIGN §8.6, second review of Today, 15 ott 2026): one glyph per
 * quantity, from one monochrome family, tinted with the label beside them.
 *
 * Not Meteocons. The weather's own drawings keep their colours everywhere a state is drawn
 * (the hours, the week, the agenda) and they were never meant to be tinted: a flat tint
 * collapses a Meteocon into its silhouette (device, 4 set 2026, «due sembrano uguali»). But in
 * the grid, ten coloured drawings of ten styles beside one another — a blue drop, a gauge with
 * a red needle, a green plant with a badge — were the loudest and least consistent thing on
 * the page, louder than the values they introduce. Material Symbols are drawn to be read as
 * silhouettes, at the size of a label, in one ink: the eyebrow names the quantity and the
 * value does the talking, as on the other tabs' rows.
 *
 * Copied, not depended on: these are ten glyphs of the outlined Material Icons theme, the path
 * data unchanged from `androidx.compose.material:material-icons-extended` 1.7.8, so the build
 * does not compile and dex the library's thousands of others for ten. Apache 2.0
 * (`licenses/MaterialIcons-Apache-2.0.txt`).
 */
object MetricIcons {

    /** `Outlined.Umbrella`. */
    val rain: ImageVector by lazy {
        materialIcon(name = "Metric.rain") {
            materialPath {
                moveTo(14.5f, 6.92f)
                lineTo(13.0f, 5.77f)
                verticalLineTo(3.88f)
                verticalLineTo(3.4f)
                curveToRelative(0.0f, -0.26f, 0.22f, -0.48f, 0.5f, -0.48f)
                curveToRelative(0.28f, 0.0f, 0.5f, 0.21f, 0.5f, 0.48f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(3.4f)
                curveTo(16.0f, 2.07f, 14.88f, 1.0f, 13.5f, 1.0f)
                curveTo(12.12f, 1.0f, 11.0f, 2.07f, 11.0f, 3.4f)
                verticalLineToRelative(0.48f)
                verticalLineToRelative(1.89f)
                lineTo(9.5f, 6.92f)
                lineTo(6.0f, 6.07f)
                lineToRelative(5.05f, 15.25f)
                curveTo(11.2f, 21.77f, 11.6f, 22.0f, 12.0f, 22.0f)
                reflectiveCurveToRelative(0.8f, -0.23f, 0.95f, -0.69f)
                lineTo(18.0f, 6.07f)
                lineTo(14.5f, 6.92f)
                close()
                moveTo(13.28f, 8.5f)
                lineToRelative(0.76f, 0.58f)
                lineToRelative(0.92f, -0.23f)
                lineTo(13.0f, 14.8f)
                verticalLineTo(8.29f)
                lineTo(13.28f, 8.5f)
                close()
                moveTo(9.96f, 9.09f)
                lineToRelative(0.76f, -0.58f)
                lineTo(11.0f, 8.29f)
                verticalLineToRelative(6.51f)
                lineTo(9.03f, 8.86f)
                lineTo(9.96f, 9.09f)
                close()
            }
        }
    }

    /** `Outlined.AcUnit`. */
    val snow: ImageVector by lazy {
        materialIcon(name = "Metric.snow") {
            materialPath {
                moveTo(22.0f, 11.0f)
                horizontalLineToRelative(-4.17f)
                lineToRelative(3.24f, -3.24f)
                lineToRelative(-1.41f, -1.42f)
                lineTo(15.0f, 11.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineTo(9.0f)
                lineToRelative(4.66f, -4.66f)
                lineToRelative(-1.42f, -1.41f)
                lineTo(13.0f, 6.17f)
                verticalLineTo(2.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(4.17f)
                lineTo(7.76f, 2.93f)
                lineTo(6.34f, 4.34f)
                lineTo(11.0f, 9.0f)
                verticalLineToRelative(2.0f)
                horizontalLineTo(9.0f)
                lineTo(4.34f, 6.34f)
                lineTo(2.93f, 7.76f)
                lineTo(6.17f, 11.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(4.17f)
                lineToRelative(-3.24f, 3.24f)
                lineToRelative(1.41f, 1.42f)
                lineTo(9.0f, 13.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.0f)
                lineToRelative(-4.66f, 4.66f)
                lineToRelative(1.42f, 1.41f)
                lineTo(11.0f, 17.83f)
                verticalLineTo(22.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(-4.17f)
                lineToRelative(3.24f, 3.24f)
                lineToRelative(1.42f, -1.41f)
                lineTo(13.0f, 15.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(2.0f)
                lineToRelative(4.66f, 4.66f)
                lineToRelative(1.41f, -1.42f)
                lineTo(17.83f, 13.0f)
                horizontalLineTo(22.0f)
                verticalLineToRelative(-2.0f)
                close()
            }
        }
    }

    /** `Outlined.WbSunny`. */
    val uv: ImageVector by lazy {
        materialIcon(name = "Metric.uv") {
            materialPath {
                moveTo(6.76f, 4.84f)
                lineToRelative(-1.8f, -1.79f)
                lineToRelative(-1.41f, 1.41f)
                lineToRelative(1.79f, 1.79f)
                close()
                moveTo(1.0f, 10.5f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(2.0f)
                lineTo(1.0f, 12.5f)
                close()
                moveTo(11.0f, 0.55f)
                horizontalLineToRelative(2.0f)
                lineTo(13.0f, 3.5f)
                horizontalLineToRelative(-2.0f)
                close()
                moveTo(19.04f, 3.045f)
                lineToRelative(1.408f, 1.407f)
                lineToRelative(-1.79f, 1.79f)
                lineToRelative(-1.407f, -1.408f)
                close()
                moveTo(17.24f, 18.16f)
                lineToRelative(1.79f, 1.8f)
                lineToRelative(1.41f, -1.41f)
                lineToRelative(-1.8f, -1.79f)
                close()
                moveTo(20.0f, 10.5f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(-3.0f)
                close()
                moveTo(12.0f, 5.5f)
                curveToRelative(-3.31f, 0.0f, -6.0f, 2.69f, -6.0f, 6.0f)
                reflectiveCurveToRelative(2.69f, 6.0f, 6.0f, 6.0f)
                reflectiveCurveToRelative(6.0f, -2.69f, 6.0f, -6.0f)
                reflectiveCurveToRelative(-2.69f, -6.0f, -6.0f, -6.0f)
                close()
                moveTo(12.0f, 15.5f)
                curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
                reflectiveCurveToRelative(1.79f, -4.0f, 4.0f, -4.0f)
                reflectiveCurveToRelative(4.0f, 1.79f, 4.0f, 4.0f)
                reflectiveCurveToRelative(-1.79f, 4.0f, -4.0f, 4.0f)
                close()
                moveTo(11.0f, 19.5f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(2.95f)
                horizontalLineToRelative(-2.0f)
                close()
                moveTo(3.55f, 18.54f)
                lineToRelative(1.41f, 1.41f)
                lineToRelative(1.79f, -1.8f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }
    }

    /** `Outlined.Air`. */
    val wind: ImageVector by lazy {
        materialIcon(name = "Metric.wind") {
            materialPath {
                moveTo(14.5f, 17.0f)
                curveToRelative(0.0f, 1.65f, -1.35f, 3.0f, -3.0f, 3.0f)
                reflectiveCurveToRelative(-3.0f, -1.35f, -3.0f, -3.0f)
                horizontalLineToRelative(2.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineToRelative(9.5f)
                curveTo(13.15f, 14.0f, 14.5f, 15.35f, 14.5f, 17.0f)
                close()
                moveTo(19.0f, 6.5f)
                curveTo(19.0f, 4.57f, 17.43f, 3.0f, 15.5f, 3.0f)
                reflectiveCurveTo(12.0f, 4.57f, 12.0f, 6.5f)
                horizontalLineToRelative(2.0f)
                curveTo(14.0f, 5.67f, 14.67f, 5.0f, 15.5f, 5.0f)
                reflectiveCurveTo(17.0f, 5.67f, 17.0f, 6.5f)
                reflectiveCurveTo(16.33f, 8.0f, 15.5f, 8.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(13.5f)
                curveTo(17.43f, 10.0f, 19.0f, 8.43f, 19.0f, 6.5f)
                close()
                moveTo(18.5f, 11.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.5f)
                curveToRelative(0.83f, 0.0f, 1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(19.33f, 16.0f, 18.5f, 16.0f)
                verticalLineToRelative(2.0f)
                curveToRelative(1.93f, 0.0f, 3.5f, -1.57f, 3.5f, -3.5f)
                reflectiveCurveTo(20.43f, 11.0f, 18.5f, 11.0f)
                close()
            }
        }
    }

    /** `Outlined.Cloud`. */
    val cloud: ImageVector by lazy {
        materialIcon(name = "Metric.cloud") {
            materialPath {
                moveTo(12.0f, 6.0f)
                curveToRelative(2.62f, 0.0f, 4.88f, 1.86f, 5.39f, 4.43f)
                lineToRelative(0.3f, 1.5f)
                lineToRelative(1.53f, 0.11f)
                curveToRelative(1.56f, 0.1f, 2.78f, 1.41f, 2.78f, 2.96f)
                curveToRelative(0.0f, 1.65f, -1.35f, 3.0f, -3.0f, 3.0f)
                horizontalLineTo(6.0f)
                curveToRelative(-2.21f, 0.0f, -4.0f, -1.79f, -4.0f, -4.0f)
                curveToRelative(0.0f, -2.05f, 1.53f, -3.76f, 3.56f, -3.97f)
                lineToRelative(1.07f, -0.11f)
                lineToRelative(0.5f, -0.95f)
                curveTo(8.08f, 7.14f, 9.94f, 6.0f, 12.0f, 6.0f)
                moveToRelative(0.0f, -2.0f)
                curveTo(9.11f, 4.0f, 6.6f, 5.64f, 5.35f, 8.04f)
                curveTo(2.34f, 8.36f, 0.0f, 10.91f, 0.0f, 14.0f)
                curveToRelative(0.0f, 3.31f, 2.69f, 6.0f, 6.0f, 6.0f)
                horizontalLineToRelative(13.0f)
                curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
                curveToRelative(0.0f, -2.64f, -2.05f, -4.78f, -4.65f, -4.96f)
                curveTo(18.67f, 6.59f, 15.64f, 4.0f, 12.0f, 4.0f)
                close()
            }
        }
    }

    /** `Outlined.WaterDrop`. */
    val humidity: ImageVector by lazy {
        materialIcon(name = "Metric.humidity") {
            materialPath {
                moveTo(12.0f, 2.0f)
                curveToRelative(-5.33f, 4.55f, -8.0f, 8.48f, -8.0f, 11.8f)
                curveToRelative(0.0f, 4.98f, 3.8f, 8.2f, 8.0f, 8.2f)
                reflectiveCurveToRelative(8.0f, -3.22f, 8.0f, -8.2f)
                curveTo(20.0f, 10.48f, 17.33f, 6.55f, 12.0f, 2.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveToRelative(-3.35f, 0.0f, -6.0f, -2.57f, -6.0f, -6.2f)
                curveToRelative(0.0f, -2.34f, 1.95f, -5.44f, 6.0f, -9.14f)
                curveToRelative(4.05f, 3.7f, 6.0f, 6.79f, 6.0f, 9.14f)
                curveTo(18.0f, 17.43f, 15.35f, 20.0f, 12.0f, 20.0f)
                close()
                moveTo(7.83f, 14.0f)
                curveToRelative(0.37f, 0.0f, 0.67f, 0.26f, 0.74f, 0.62f)
                curveToRelative(0.41f, 2.22f, 2.28f, 2.98f, 3.64f, 2.87f)
                curveToRelative(0.43f, -0.02f, 0.79f, 0.32f, 0.79f, 0.75f)
                curveToRelative(0.0f, 0.4f, -0.32f, 0.73f, -0.72f, 0.75f)
                curveToRelative(-2.13f, 0.13f, -4.62f, -1.09f, -5.19f, -4.12f)
                curveTo(7.01f, 14.42f, 7.37f, 14.0f, 7.83f, 14.0f)
                close()
            }
        }
    }

    /** `Outlined.Speed`. */
    val pressure: ImageVector by lazy {
        materialIcon(name = "Metric.pressure") {
            materialPath {
                moveTo(20.38f, 8.57f)
                lineToRelative(-1.23f, 1.85f)
                arcToRelative(8.0f, 8.0f, 0.0f, false, true, -0.22f, 7.58f)
                horizontalLineTo(5.07f)
                arcTo(8.0f, 8.0f, 0.0f, false, true, 15.58f, 6.85f)
                lineToRelative(1.85f, -1.23f)
                arcTo(10.0f, 10.0f, 0.0f, false, false, 3.35f, 19.0f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.72f, 1.0f)
                horizontalLineToRelative(13.85f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 1.74f, -1.0f)
                arcToRelative(10.0f, 10.0f, 0.0f, false, false, -0.27f, -10.44f)
                close()
            }
            materialPath {
                moveTo(10.59f, 15.41f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 2.83f, 0.0f)
                lineToRelative(5.66f, -8.49f)
                lineToRelative(-8.49f, 5.66f)
                arcToRelative(2.0f, 2.0f, 0.0f, false, false, 0.0f, 2.83f)
                close()
            }
        }
    }

    /** `Outlined.Visibility`. */
    val visibility: ImageVector by lazy {
        materialIcon(name = "Metric.visibility") {
            materialPath {
                moveTo(12.0f, 6.0f)
                curveToRelative(3.79f, 0.0f, 7.17f, 2.13f, 8.82f, 5.5f)
                curveTo(19.17f, 14.87f, 15.79f, 17.0f, 12.0f, 17.0f)
                reflectiveCurveToRelative(-7.17f, -2.13f, -8.82f, -5.5f)
                curveTo(4.83f, 8.13f, 8.21f, 6.0f, 12.0f, 6.0f)
                moveToRelative(0.0f, -2.0f)
                curveTo(7.0f, 4.0f, 2.73f, 7.11f, 1.0f, 11.5f)
                curveTo(2.73f, 15.89f, 7.0f, 19.0f, 12.0f, 19.0f)
                reflectiveCurveToRelative(9.27f, -3.11f, 11.0f, -7.5f)
                curveTo(21.27f, 7.11f, 17.0f, 4.0f, 12.0f, 4.0f)
                close()
                moveTo(12.0f, 9.0f)
                curveToRelative(1.38f, 0.0f, 2.5f, 1.12f, 2.5f, 2.5f)
                reflectiveCurveTo(13.38f, 14.0f, 12.0f, 14.0f)
                reflectiveCurveToRelative(-2.5f, -1.12f, -2.5f, -2.5f)
                reflectiveCurveTo(10.62f, 9.0f, 12.0f, 9.0f)
                moveToRelative(0.0f, -2.0f)
                curveToRelative(-2.48f, 0.0f, -4.5f, 2.02f, -4.5f, 4.5f)
                reflectiveCurveTo(9.52f, 16.0f, 12.0f, 16.0f)
                reflectiveCurveToRelative(4.5f, -2.02f, 4.5f, -4.5f)
                reflectiveCurveTo(14.48f, 7.0f, 12.0f, 7.0f)
                close()
            }
        }
    }

    /** `Outlined.Grain`. */
    val air: ImageVector by lazy {
        materialIcon(name = "Metric.air") {
            materialPath {
                moveTo(10.0f, 12.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(6.0f, 8.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(6.0f, 16.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(18.0f, 8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                close()
                moveTo(14.0f, 16.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(18.0f, 12.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(14.0f, 8.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
                moveTo(10.0f, 4.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f)
                reflectiveCurveToRelative(2.0f, -0.9f, 2.0f, -2.0f)
                reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                close()
            }
        }
    }

    /** `Outlined.LocalFlorist`. */
    val pollen: ImageVector by lazy {
        materialIcon(name = "Metric.pollen") {
            materialPath {
                moveTo(8.66f, 13.07f)
                curveToRelative(0.15f, 0.0f, 0.29f, -0.01f, 0.43f, -0.03f)
                curveTo(9.56f, 14.19f, 10.69f, 15.0f, 12.0f, 15.0f)
                reflectiveCurveToRelative(2.44f, -0.81f, 2.91f, -1.96f)
                curveToRelative(0.14f, 0.02f, 0.29f, 0.03f, 0.43f, 0.03f)
                curveToRelative(1.73f, 0.0f, 3.14f, -1.41f, 3.14f, -3.14f)
                curveToRelative(0.0f, -0.71f, -0.25f, -1.39f, -0.67f, -1.93f)
                curveToRelative(0.43f, -0.54f, 0.67f, -1.22f, 0.67f, -1.93f)
                curveToRelative(0.0f, -1.73f, -1.41f, -3.14f, -3.14f, -3.14f)
                curveToRelative(-0.15f, 0.0f, -0.29f, 0.01f, -0.43f, 0.03f)
                curveTo(14.44f, 1.81f, 13.31f, 1.0f, 12.0f, 1.0f)
                reflectiveCurveToRelative(-2.44f, 0.81f, -2.91f, 1.96f)
                curveToRelative(-0.14f, -0.02f, -0.29f, -0.03f, -0.43f, -0.03f)
                curveToRelative(-1.73f, 0.0f, -3.14f, 1.41f, -3.14f, 3.14f)
                curveToRelative(0.0f, 0.71f, 0.25f, 1.39f, 0.67f, 1.93f)
                curveToRelative(-0.43f, 0.54f, -0.68f, 1.22f, -0.68f, 1.93f)
                curveToRelative(0.0f, 1.73f, 1.41f, 3.14f, 3.15f, 3.14f)
                close()
                moveTo(12.0f, 13.0f)
                curveToRelative(-0.62f, 0.0f, -1.12f, -0.49f, -1.14f, -1.1f)
                lineToRelative(0.12f, -1.09f)
                curveToRelative(0.32f, 0.12f, 0.66f, 0.19f, 1.02f, 0.19f)
                reflectiveCurveToRelative(0.71f, -0.07f, 1.03f, -0.19f)
                lineToRelative(0.11f, 1.09f)
                curveToRelative(-0.02f, 0.61f, -0.52f, 1.1f, -1.14f, 1.1f)
                close()
                moveTo(15.34f, 11.07f)
                curveToRelative(-0.24f, 0.0f, -0.46f, -0.07f, -0.64f, -0.2f)
                lineToRelative(-0.81f, -0.57f)
                curveToRelative(0.55f, -0.45f, 0.94f, -1.09f, 1.06f, -1.83f)
                lineToRelative(0.88f, 0.42f)
                curveToRelative(0.4f, 0.19f, 0.66f, 0.59f, 0.66f, 1.03f)
                curveToRelative(0.0f, 0.64f, -0.52f, 1.15f, -1.15f, 1.15f)
                close()
                moveTo(14.69f, 5.13f)
                curveToRelative(0.2f, -0.13f, 0.42f, -0.2f, 0.65f, -0.2f)
                curveToRelative(0.63f, 0.0f, 1.14f, 0.51f, 1.14f, 1.14f)
                curveToRelative(0.0f, 0.44f, -0.25f, 0.83f, -0.66f, 1.03f)
                lineToRelative(-0.88f, 0.42f)
                curveToRelative(-0.12f, -0.74f, -0.51f, -1.38f, -1.07f, -1.83f)
                lineToRelative(0.82f, -0.56f)
                close()
                moveTo(12.0f, 3.0f)
                curveToRelative(0.62f, 0.0f, 1.12f, 0.49f, 1.14f, 1.1f)
                lineToRelative(-0.11f, 1.09f)
                curveTo(12.71f, 5.07f, 12.36f, 5.0f, 12.0f, 5.0f)
                reflectiveCurveToRelative(-0.7f, 0.07f, -1.02f, 0.19f)
                lineToRelative(-0.12f, -1.09f)
                curveToRelative(0.02f, -0.61f, 0.52f, -1.1f, 1.14f, -1.1f)
                close()
                moveTo(8.66f, 4.93f)
                curveToRelative(0.24f, 0.0f, 0.46f, 0.07f, 0.64f, 0.2f)
                lineToRelative(0.81f, 0.56f)
                curveToRelative(-0.55f, 0.45f, -0.94f, 1.09f, -1.06f, 1.83f)
                lineToRelative(-0.88f, -0.42f)
                curveToRelative(-0.4f, -0.2f, -0.66f, -0.59f, -0.66f, -1.03f)
                curveToRelative(0.0f, -0.63f, 0.52f, -1.14f, 1.15f, -1.14f)
                close()
                moveTo(8.17f, 8.9f)
                lineToRelative(0.88f, -0.42f)
                curveToRelative(0.12f, 0.74f, 0.51f, 1.38f, 1.07f, 1.83f)
                lineToRelative(-0.81f, 0.55f)
                curveToRelative(-0.2f, 0.13f, -0.42f, 0.2f, -0.65f, 0.2f)
                curveToRelative(-0.63f, 0.0f, -1.14f, -0.51f, -1.14f, -1.14f)
                curveToRelative(-0.01f, -0.43f, 0.25f, -0.82f, 0.65f, -1.02f)
                close()
                moveTo(12.0f, 22.0f)
                curveToRelative(4.97f, 0.0f, 9.0f, -4.03f, 9.0f, -9.0f)
                curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f)
                close()
                moveTo(14.44f, 19.56f)
                curveToRelative(0.71f, -1.9f, 2.22f, -3.42f, 4.12f, -4.12f)
                curveToRelative(-0.71f, 1.9f, -2.22f, 3.41f, -4.12f, 4.12f)
                close()
                moveTo(3.0f, 13.0f)
                curveToRelative(0.0f, 4.97f, 4.03f, 9.0f, 9.0f, 9.0f)
                curveToRelative(0.0f, -4.97f, -4.03f, -9.0f, -9.0f, -9.0f)
                close()
                moveTo(5.44f, 15.44f)
                curveToRelative(1.9f, 0.71f, 3.42f, 2.22f, 4.12f, 4.12f)
                curveToRelative(-1.9f, -0.71f, -3.41f, -2.22f, -4.12f, -4.12f)
                close()
            }
        }
    }

}
