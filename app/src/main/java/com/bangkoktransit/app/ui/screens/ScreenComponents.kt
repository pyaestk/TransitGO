package com.bangkoktransit.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.domain.model.RoutePath
import com.bangkoktransit.app.domain.model.RouteStep
import com.bangkoktransit.app.domain.model.SavedTrip
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitCoral
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.transitgo.app.R
import java.util.Locale

private val CardShape = RoundedCornerShape(16.dp)
private val ControlShape = RoundedCornerShape(12.dp)

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun TransitCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
//        border = BorderStroke(
//            1.dp,
//            tint?.copy(alpha = 0.32f) ?: MaterialTheme.colorScheme.outline,
//        ),
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        shape = CardShape,
        content = content,
    )
}

@Composable
fun EmptyStateCard(
    title: String,
    body: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitCoral) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionText != null && onAction != null) {
                PrimaryActionButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = TransitBlue,
) {
    TransitCard(modifier = modifier, tint = tint) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(tint),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun StationRow(
    station: Station,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .defaultMinSize(minHeight = 72.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
//        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = CardShape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val lineColor = lineColorForName(station.line?.nameEn)
                TransitLineLogo(
                    lineName = station.line?.nameEn,
                    size = 40.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = station.nameEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = station.line?.nameEn ?: "Transit line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                CompactMetric(
                    text = station.displayCode,
                    tint = lineColor,
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailing()
            }
        }
    }
}

@Composable
fun StationCodeBadge(
    code: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = code.take(6),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TransitLineLogo(
    lineName: String?,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val lineColor = lineColorForName(lineName)
    val logoResource = lineLogoResourceForName(lineName)

    Surface(
        modifier = modifier.size(size),
        color = Color.White,
        contentColor = lineColor,
        shape = RoundedCornerShape(size * 0.24f),
        border = BorderStroke(1.dp, lineColor.copy(alpha = 0.3f)),
    ) {
        if (logoResource != null) {
            Image(
                painter = painterResource(logoResource),
                contentDescription = null,
                modifier = Modifier.padding(size * 0.12f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Train,
                    contentDescription = null,
                    modifier = Modifier.size(size * 0.58f),
                )
            }
        }
    }
}

@Composable
fun RouteOptionRow(
    route: RoutePath,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .defaultMinSize(minHeight = 104.dp)
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(1.dp, borderColor),
        shape = CardShape,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = route.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = route.fareTotal.formatFare(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TransitGreen,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CompactMetric("${route.stats.totalStations} stations")
                CompactMetric("${route.stats.totalTransfers} transfers")
                CompactMetric("${route.stats.totalLines} lines")
            }
        }
    }
}

@Composable
fun TripRow(
    trip: SavedTrip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
//        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = CardShape,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    CompactMetric(trip.routeType.displayPathType(), tint = TransitBlue)
                    Text(
                        text = trip.fareTotal.formatFare(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TransitGreen,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Text(
                    text = "${trip.fromName} to ${trip.toName}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${trip.totalStations} stations  /  ${trip.totalTransfers} transfers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(10.dp))
                trailing()
            }
        }
    }
}

@Composable
fun CompactMetric(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val usesDefaultTint = tint == MaterialTheme.colorScheme.surfaceVariant
    val readableTint = if (!usesDefaultTint && MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        lerp(tint, Color.White, 0.36f)
    } else {
        tint
    }
    Surface(
        modifier = modifier,
        color = tint.copy(alpha = if (usesDefaultTint) 0.72f else 0.12f),
        contentColor = if (usesDefaultTint) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            readableTint
        },
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun RouteStepsTimeline(
    route: RoutePath,
    stations: List<Station>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val stationLookup = remember(stations) { buildStationLookup(stations) }

    if (route.routeSteps.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            shape = CardShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Text(
                text = "No step details for this route yet.",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        route.routeSteps.forEachIndexed { index, step ->
            val matchingStation = stationLookup.findStation(step.station?.code)
            val lineName = step.line.presentOrNull() ?: matchingStation?.line?.nameEn.presentOrNull()
            val stationName = step.station?.name.presentOrNull() ?: matchingStation?.nameEn.presentOrNull()
            val stationCode = step.station?.code.presentOrNull() ?: matchingStation?.displayCode.presentOrNull()
            val isLast = index == route.routeSteps.lastIndex
            val tint = routeStepTint(
                lineName = lineName,
                isFirst = index == 0,
                isLast = isLast,
            )

            RouteTimelineStep(
                step = step,
                stepNumber = index + 1,
                stationName = stationName,
                stationCode = stationCode,
                lineName = lineName,
                tint = tint,
                icon = routeStepIcon(step.action, isLast),
                isLast = isLast,
                compact = compact,
            )
        }
    }
}

@Composable
private fun RouteTimelineStep(
    step: RouteStep,
    stepNumber: Int,
    stationName: String?,
    stationCode: String?,
    lineName: String?,
    tint: Color,
    icon: ImageVector,
    isLast: Boolean,
    compact: Boolean,
) {
    val nodeSize = if (compact) 28.dp else 32.dp
    val titleText = stationName ?: step.action.presentOrNull() ?: "Continue"
    val actionText = step.action.presentOrNull()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(nodeSize),
                color = tint.copy(alpha = 0.14f),
                contentColor = tint,
                shape = CircleShape,
                border = BorderStroke(2.dp, tint.copy(alpha = 0.7f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 15.dp else 17.dp),
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(tint.copy(alpha = 0.35f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
            shape = CardShape,
            border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = if (compact) 6.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = titleText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (compact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (stationCode != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TimelineCodeBadge(
                            code = stationCode,
                            color = tint,
                            compact = compact,
                        )
                    } else {
                        Spacer(modifier = Modifier.width(8.dp))
                        CompactMetric(
                            text = stepNumber.toString(),
                            tint = tint,
                        )
                    }
                }

                if (actionText != null && stationName != null) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (lineName != null) {
                    RouteLinePill(
                        lineName = lineName,
                        color = tint,
                        compact = compact,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineCodeBadge(
    code: String,
    color: Color,
    compact: Boolean,
) {
    Surface(
        modifier = Modifier.defaultMinSize(
            minWidth = if (compact) 30.dp else 34.dp,
            minHeight = if (compact) 24.dp else 26.dp,
        ),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.28f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = if (compact) 3.dp else 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = code.take(6),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RouteLinePill(
    lineName: String,
    color: Color,
    compact: Boolean = false,
) {
    Surface(
        color = color.copy(alpha = 0.13f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.26f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = if (compact) 3.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TransitLineLogo(
                lineName = lineName,
                size = if (compact) 18.dp else 20.dp,
            )
            Text(
                text = lineName,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shape = ControlShape,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun QuietActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        shape = ControlShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(text = text)
    }
}

@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier
            .size(48.dp)
            .clip(ControlShape)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
        shape = ControlShape,
        border = BorderStroke(
            1.dp,
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
            } else {
                MaterialTheme.colorScheme.outline
            },
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

fun lineColorForName(name: String?): Color {
    return when {
        name == null -> TransitBlue
        name.contains("Sukhumvit", ignoreCase = true) -> Color(0xFF6CC24A)
        name.contains("Silom", ignoreCase = true) -> Color(0xFF097976)
        name.contains("Gold", ignoreCase = true) -> Color(0xFFC5B358)
        name.contains("Blue", ignoreCase = true) -> Color(0xFF1E4491)
        name.contains("Purple", ignoreCase = true) -> Color(0xFF7D3CB5)
        name.contains("Yellow", ignoreCase = true) -> Color(0xFFFFD800)
        name.contains("Pink", ignoreCase = true) -> Color(0xFFF599B0)
        name.contains("Orange", ignoreCase = true) -> Color(0xFFF58220)
        name.contains("Airport", ignoreCase = true) -> Color(0xFF961A4B)
        name.contains("Light Red", ignoreCase = true) -> Color(0xFFF0493E)
        name.contains("Dark Red", ignoreCase = true) -> Color(0xFFC1272D)
        name.contains("Red", ignoreCase = true) -> Color(0xFFC1272D)
        else -> TransitCoral
    }
}

@DrawableRes
fun lineLogoResourceForName(name: String?): Int? {
    return when {
        name == null -> null
        name.contains("Sukhumvit", ignoreCase = true) -> R.drawable.line_bts_sukhumvit
        name.contains("Silom", ignoreCase = true) -> R.drawable.line_bts_silom
        name.contains("Gold", ignoreCase = true) -> R.drawable.line_bts_gold
        name.contains("Blue", ignoreCase = true) -> R.drawable.line_mrt_blue
        name.contains("Purple", ignoreCase = true) -> R.drawable.line_mrt_purple
        name.contains("Yellow", ignoreCase = true) -> R.drawable.line_mrt_yellow
        name.contains("Pink", ignoreCase = true) -> R.drawable.line_mrt_pink
        name.contains("Airport", ignoreCase = true) -> R.drawable.line_airport_rail_link
        name.contains("Light Red", ignoreCase = true) -> R.drawable.line_srt_light_red
        name.contains("Dark Red", ignoreCase = true) -> R.drawable.line_srt_dark_red
        else -> null
    }
}

private fun buildStationLookup(stations: List<Station>): Map<String, Station> {
    val lookup = mutableMapOf<String, Station>()
    stations.forEach { station ->
        listOf(
            station.stationCode,
            station.stationShortName,
            station.displayCode,
        ).forEach { code ->
            code.presentOrNull()?.let { lookup[it.lookupKey()] = station }
        }
    }
    return lookup
}

private fun Map<String, Station>.findStation(code: String?): Station? {
    return code.presentOrNull()?.let { this[it.lookupKey()] }
}

private fun routeStepTint(
    lineName: String?,
    isFirst: Boolean,
    isLast: Boolean,
): Color {
    return when {
        lineName != null -> lineColorForName(lineName)
        isFirst -> TransitBlue
        isLast -> TransitCoral
        else -> TransitGreen
    }
}

private fun routeStepIcon(action: String, isLast: Boolean): ImageVector {
    val normalized = action.lowercase(Locale.getDefault())
    return when {
        normalized.contains("walk") -> Icons.AutoMirrored.Filled.DirectionsWalk
        normalized.contains("transfer") || normalized.contains("change") -> Icons.Filled.SwapHoriz
        isLast || normalized.contains("arrive") -> Icons.Filled.Place
        else -> Icons.Filled.Train
    }
}

private fun String?.presentOrNull(): String? {
    return this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
}

private fun String.lookupKey(): String = uppercase(Locale.ROOT)

fun RoutePath.displayName(): String = pathType.displayPathType()

fun String.displayPathType(): String {
    return split("-", "_", " ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }
}

fun Double?.formatFare(): String {
    return this?.let { value ->
        if (value % 1.0 == 0.0) {
            "${value.toInt()} THB"
        } else {
            String.format(Locale.getDefault(), "%.1f THB", value)
        }
    } ?: "-- THB"
}
