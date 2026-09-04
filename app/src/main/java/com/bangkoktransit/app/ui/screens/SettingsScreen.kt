package com.bangkoktransit.app.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitCoral
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.theme.TransitLine

@Composable
fun SettingsScreen() {
    val versionName = rememberAppVersionName()
    var selectedDetail by rememberSaveable { mutableStateOf<SettingsDetail?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Transit Go information",
        )

        SettingsGroup(title = "About") {
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "About App",
                value = "Transit Go",
                tint = TransitBlue,
                onClick = { selectedDetail = SettingsDetail.AboutApp },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Version",
                value = versionName,
                tint = TransitBlue,
                onClick = { selectedDetail = SettingsDetail.Version },
            )
        }

        SettingsGroup(title = "Information") {
            SettingsRow(
                icon = Icons.Filled.Train,
                title = "Data Credits",
                value = "Sources",
                tint = TransitGreen,
                onClick = { selectedDetail = SettingsDetail.DataCredits },
            )
            SettingsDivider()
            SettingsRow(
                icon = Icons.Filled.AttachMoney,
                title = "Fare Estimates",
                value = "Approximate",
                tint = TransitCoral,
                onClick = { selectedDetail = SettingsDetail.FareEstimates },
            )
        }

        SettingsGroup(title = "Project") {
            SettingsRow(
                icon = Icons.Filled.Groups,
                title = "Project Team",
                value = "ICT Mahidol",
                tint = TransitBlue,
                onClick = { selectedDetail = SettingsDetail.ProjectTeam },
            )
        }

        Text(
            text = "This project is for academic and visualization purposes only.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }

    selectedDetail?.let { detail ->
        SettingsDetailDialog(
            detail = detail,
            versionName = versionName,
            onDismiss = { selectedDetail = null },
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = TransitBlue,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(26.dp),
            color = tint.copy(alpha = 0.12f),
            contentColor = tint,
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 54.dp, end = 14.dp)
            .height(1.dp)
            .background(TransitLine),
    )
}

@Composable
private fun SettingsDetailDialog(
    detail: SettingsDetail,
    versionName: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = detail.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (detail) {
                    SettingsDetail.AboutApp -> {
                        DetailParagraph(
                            text = "A unified, intelligent transit platform that solves Bangkok's long-standing navigation challenges by combining BTS, MRT, ARL, and SRT systems into one interactive experience.",
                        )
                        DetailParagraph(
                            text = "It supports both the current 2025 network and projected 2035 expansions, helping commuters, tourists, and planners explore real and future city connectivity.",
                        )
                    }
                    SettingsDetail.Version -> {
                        DetailKeyValue(label = "App", value = "Transit Go")
                        DetailKeyValue(label = "Version", value = versionName)
                        DetailParagraph(text = "Academic visualization project")
                    }
                    SettingsDetail.DataCredits -> {
                        DetailParagraph(text = "Transit information is referenced from publicly available data provided by:")
                        DetailBulletList(
                            items = listOf(
                                "BTS Skytrain (Bangkok Mass Transit System)",
                                "MRT Subway (Mass Rapid Transit Authority of Thailand - MRTA)",
                                "Airport Rail Link (ARL)",
                                "State Railway of Thailand (SRT)",
                                "Bangkok 2035 expansion proposals and publicly published planning documents",
                            ),
                        )
                        DetailParagraph(
                            text = "These sources are used solely for academic and visualization purposes. This project is not officially affiliated with any transit operator.",
                        )
                    }
                    SettingsDetail.FareEstimates -> {
                        DetailParagraph(
                            text = "Fares are estimates based on publicly available fare structures. Actual fares may vary depending on operators and policy updates.",
                        )
                        DetailBulletList(
                            items = listOf(
                                "Different systems use different pricing models",
                                "Temporary promotions or caps may apply",
                                "Interchange rules between operators can change",
                                "2035 network data is projected",
                            ),
                        )
                    }
                    SettingsDetail.ProjectTeam -> {
                        DetailParagraph(text = "This project was collaboratively developed by:")
                        DetailBulletList(
                            items = listOf(
                                "Thi Han Nyunt",
                                "Kaung Htet Tha",
                                "Pyae Htut Khaing",
                                "Pyae Sone Thant Kyaw",
                                "Xaphone Saechao",
                            ),
                        )
                        DetailParagraph(text = "Mahidol University - Faculty of ICT (ICT Mahidol)")
                        DetailParagraph(text = "Master of Science in Information Technology and Computer Science (ITCS)")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done")
            }
        },
    )
}

@Composable
private fun DetailParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DetailKeyValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun DetailBulletList(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(5.dp),
                    color = TransitBlue,
                    shape = CircleShape,
                ) {}
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = item,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class SettingsDetail(val title: String) {
    AboutApp("About App"),
    Version("Version"),
    DataCredits("Data Credits"),
    FareEstimates("Why are fares approximate?"),
    ProjectTeam("Project Team"),
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
