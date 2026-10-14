package com.bangkoktransit.app.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.transitgo.shared.ui.screens.SettingsScreen as SharedSettingsScreen

@Composable
fun SettingsScreen(
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
) {
    SharedSettingsScreen(
        darkModeEnabled = darkModeEnabled,
        onDarkModeChanged = onDarkModeChanged,
        versionName = rememberAppVersionName(),
    )
}

@Composable
private fun rememberAppVersionName(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0"
        }.getOrDefault("1.0")
    }
}
