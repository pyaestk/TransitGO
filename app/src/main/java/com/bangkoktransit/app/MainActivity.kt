package com.bangkoktransit.app

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bangkoktransit.app.data.local.AppearancePreferences
import com.bangkoktransit.app.ui.TransitGoApp
import com.bangkoktransit.app.ui.theme.BangkoktransitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appearancePreferences = AppearancePreferences(applicationContext)
        val systemUsesDarkTheme = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        setContent {
            var darkModeEnabled by rememberSaveable {
                mutableStateOf(
                    appearancePreferences.isDarkModeEnabled(
                        defaultValue = systemUsesDarkTheme,
                    ),
                )
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = if (!darkModeEnabled) {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkModeEnabled) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }

            BangkoktransitTheme(darkTheme = darkModeEnabled) {
                TransitGoApp(
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeChanged = { enabled ->
                        darkModeEnabled = enabled
                        appearancePreferences.setDarkModeEnabled(enabled)
                    },
                )
            }
        }
    }
}
