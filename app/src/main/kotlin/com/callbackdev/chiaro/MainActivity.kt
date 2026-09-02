package com.callbackdev.chiaro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.callbackdev.chiaro.ui.theme.ChiaroTheme
import com.callbackdev.chiaro.ui.today.TodayRoute

/**
 * One destination so far: Today. The bottom navigation of VISION §5.1 arrives with the
 * second destination (Sky, Fase 5) — a bar with three dead tabs would be the screen
 * lying about what the app can do.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChiaroTheme {
                TodayRoute()
            }
        }
    }
}
