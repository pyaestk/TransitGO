package com.bangkoktransit.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.domain.model.Station
import com.bangkoktransit.app.ui.StationPickerMode
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.viewmodel.TransitUiState

@Composable
fun StationSearchScreen(
    state: TransitUiState,
    mode: StationPickerMode,
    onBack: () -> Unit,
    onStationSelected: (Station) -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedLine by rememberSaveable { mutableStateOf("All") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val lines = remember(state.stations) {
        state.stations.mapNotNull { it.line?.nameEn }.distinct().sorted()
    }
    val filteredStations = remember(query, selectedLine, state.stations) {
        val normalized = query.trim()
        state.stations
            .filter { station -> selectedLine == "All" || station.line?.nameEn == selectedLine }
            .filter { station ->
                normalized.isBlank() ||
                    station.nameEn.contains(normalized, ignoreCase = true) ||
                    station.stationCode.contains(normalized, ignoreCase = true) ||
                    station.stationShortName.contains(normalized, ignoreCase = true) ||
                    station.place?.nameEn?.contains(normalized, ignoreCase = true) == true
            }
    }
    val title = when (mode) {
        StationPickerMode.Browse -> "Search stations"
        StationPickerMode.Start -> "Choose start"
        StationPickerMode.Target -> "Choose destination"
    }
    val actionLabel = when (mode) {
        StationPickerMode.Browse -> "Open"
        StationPickerMode.Start -> "From"
        StationPickerMode.Target -> "To"
    }
//    val stationCountText = if (selectedLine == "All") {
//        "${filteredStations.size} stations"
//    } else {
//        "$selectedLine / ${filteredStations.size} stations"
//    }

    val stationCountText = "${filteredStations.size} stations"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(
            title = title,
            subtitle = "Search by name or code",
            action = {
                IconActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )
            },
        )

        TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitBlue) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    placeholder = { Text("Siam, Mo Chit, BL22") },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stationCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (lines.isNotEmpty()) {
                        QuietActionButton(
                            text = if (showFilters) "Hide filters" else "Filters",
                            icon = Icons.Filled.Tune,
                            onClick = { showFilters = !showFilters },
                        )
                    }
                }

                if (showFilters) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 1.dp),
                    ) {
                        items(listOf("All") + lines) { line ->
                            LineFilterChip(
                                label = line,
                                selected = selectedLine == line,
                                onClick = { selectedLine = line },
                            )
                        }
                    }
                }
            }
        }

        when {
            state.isLoadingStations && state.stations.isEmpty() -> {
                LoadingStations()
            }

            state.stationError != null && state.stations.isEmpty() -> {
                EmptyStateCard(
                    title = "Station data is unavailable",
                    body = state.stationError,
                    actionText = "Try again",
                    onAction = onRetry,
                )
            }

            filteredStations.isEmpty() -> {
                EmptyStateCard(
                    title = "No station found",
                    body = "Try another station name, code, or line.",
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    if (mode != StationPickerMode.Browse && query.isBlank() && selectedLine == "All") {
                        item(key = "nearby") {
                            NearbyStationsSection(state.stations, onStationSelected, onRetry)
                        }
                        item(key = "all-stations") {
                            Text("All stations", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    items(
                        items = filteredStations,
                        key = { station -> station.stationCode },
                    ) { station ->
                        StationRow(
                            station = station,
                            onClick = { onStationSelected(station) },
                            trailing = {
                                StationSelectionPill(
                                    text = actionLabel,
                                    tint = lineColorForName(station.line?.nameEn),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingStations() {
    TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitGreen) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = "Loading stations",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LineFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (label == "All") TransitBlue else lineColorForName(label)
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) tint.copy(alpha = 0.72f) else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransitLineLogo(
                lineName = label.takeUnless { it == "All" },
                size = 26.dp,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StationSelectionPill(
    text: String,
    tint: Color,
) {
    val readableTint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) {
        lerp(tint, Color.White, 0.36f)
    } else {
        tint
    }
    Surface(
        color = tint.copy(alpha = 0.08f),
        contentColor = readableTint,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, readableTint.copy(alpha = 0.42f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}
