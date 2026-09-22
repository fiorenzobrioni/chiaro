package com.callbackdev.chiaro.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * The locale a composable formats in, read from the configuration so that a change of
 * language recomposes what depends on it. `Locale.getDefault()` gave the same answer but
 * read it past Compose, and Compose 1.10's lint refuses it (`NonObservableLocale`, met
 * with the Navigation 3 upgrade of 22 set 2026; Saldo met it the same way).
 */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
