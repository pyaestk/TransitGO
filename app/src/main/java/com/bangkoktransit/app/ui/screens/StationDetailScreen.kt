package com.bangkoktransit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.data.model.PlaceGroup
import com.bangkoktransit.app.data.model.Station
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.theme.TransitSoftGreen
import com.bangkoktransit.app.ui.viewmodel.TransitUiState

@Composable
fun StationDetailScreen(
    state: TransitUiState,
    station: Station?,
    onBack: () -> Unit,
    onSetStart: () -> Unit,
    onSetTarget: () -> Unit,
    onShowMap: () -> Unit,
) {
    val placeGroup = remember(station, state.places) {
        station?.let { current ->
            state.places.firstOrNull { place ->
                place.stations.any { it.stationCode == current.stationCode }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = station?.displayCode ?: "Station",
            subtitle = station?.nameEn ?: "Station details",
            action = {
                QuietActionButton(
                    text = "Back",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = onBack,
                )
            },
        )

        if (station == null) {
            EmptyStateCard(
                title = "Station not found",
                body = "The station list may still be loading.",
                actionText = "Back",
                onAction = onBack,
            )
            return@Column
        }

        StationSummaryCard(station = station, placeGroup = placeGroup)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryActionButton(
                text = "Set as From",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Place,
                onClick = onSetStart,
            )
            QuietActionButton(
                text = "Set as To",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Flag,
                onClick = onSetTarget,
            )
            IconActionButton(
                icon = Icons.Filled.Map,
                contentDescription = "Show on map",
                onClick = onShowMap,
            )
        }

        if (placeGroup != null) {
            PlaceGroupCard(placeGroup = placeGroup, currentStation = station)
        }
    }
}

@Composable
private fun StationSummaryCard(
    station: Station,
    placeGroup: PlaceGroup?,
) {
    val lineColor = lineColorForName(station.line?.nameEn)
    val placeName = station.place?.nameEn ?: placeGroup?.nameEn ?: "Bangkok"

    TransitCard(modifier = Modifier.fillMaxWidth(), tint = lineColor) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationCodeBadge(
                code = station.displayCode,
                color = lineColor,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = station.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = station.line?.nameEn ?: "Transit line",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactMetric(station.stationCode, tint = lineColor)
                    CompactMetric(
                        text = placeName,
                        modifier = Modifier.weight(1f),
                        tint = TransitGreen,
                    )
                }
                if (!station.nameThai.isNullOrBlank()) {
                    Text(
                        text = station.nameThai,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceGroupCard(
    placeGroup: PlaceGroup,
    currentStation: Station,
) {
    val relatedStations = placeGroup.stations.filter { it.stationCode != currentStation.stationCode }
    if (relatedStations.isEmpty()) return

    TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitGreen) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(text = "Transfers")
            relatedStations.forEach { station ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TransitSoftGreen)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TransitGreen),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.nameEn,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = station.stationCode,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        text = "Transfer",
                        style = MaterialTheme.typography.labelMedium,
                        color = TransitBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
