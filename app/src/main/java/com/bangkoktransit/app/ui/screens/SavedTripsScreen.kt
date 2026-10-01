package com.bangkoktransit.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.domain.model.SavedTrip
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.viewmodel.TransitUiState

private enum class TripTab(
    val label: String,
    val icon: ImageVector,
) {
    Saved("Saved", Icons.Filled.Bookmark),
    Recent("Recent", Icons.Filled.History),
}

@Composable
fun SavedTripsScreen(
    state: TransitUiState,
    onRestoreTrip: (SavedTrip) -> Unit,
    onOpenPlanner: () -> Unit,
    onRemoveSavedTrip: (SavedTrip) -> Unit,
    onRemoveRecentTrip: (SavedTrip) -> Unit,
    onClearSavedTrips: () -> Unit,
    onClearRecentTrips: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TripTab.Saved) }
    var clearTab by rememberSaveable { mutableStateOf<TripTab?>(null) }
    val trips = when (selectedTab) {
        TripTab.Saved -> state.savedTrips
        TripTab.Recent -> state.recentTrips
    }
    val removeTrip = when (selectedTab) {
        TripTab.Saved -> onRemoveSavedTrip
        TripTab.Recent -> onRemoveRecentTrip
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(maxWidth.coerceAtMost(960.dp))
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        ScreenHeader(
            title = "Trips",
            subtitle = "${state.savedTrips.size} saved / ${state.recentTrips.size} recent",
            action = if (trips.isNotEmpty()) {
                {
                    QuietActionButton(
                        text = "Delete all",
                        icon = Icons.Filled.Delete,
                        onClick = { clearTab = selectedTab },
                    )
                }
            } else {
                null
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TripTab.entries.forEach { tab ->
                TripTabChip(
                    tab = tab,
                    count = when (tab) {
                        TripTab.Saved -> state.savedTrips.size
                        TripTab.Recent -> state.recentTrips.size
                    },
                    selected = selectedTab == tab,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = tab },
                )
            }
        }

        if (trips.isEmpty()) {
            EmptyStateCard(
                title = if (selectedTab == TripTab.Saved) "No saved trips" else "No recent trips",
                body = if (selectedTab == TripTab.Saved) {
                    "Save a planned route and it will stay here."
                } else {
                    "Plan a route to create your recent trip list."
                },
                actionText = "Open planner",
                onAction = onOpenPlanner,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 360.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = trips,
                    key = { trip -> "${trip.key}:${trip.savedAt}" },
                ) { trip ->
                    TripRow(
                        trip = trip,
                        onClick = { onRestoreTrip(trip) },
                        trailing = {
                            IconActionButton(
                                icon = Icons.Filled.Delete,
                                contentDescription = "Delete trip",
                                onClick = { removeTrip(trip) },
                            )
                        },
                    )
                }
            }
        }
        }
    }

    clearTab?.let { tab ->
        val count = when (tab) {
            TripTab.Saved -> state.savedTrips.size
            TripTab.Recent -> state.recentTrips.size
        }
        AlertDialog(
            onDismissRequest = { clearTab = null },
            title = {
                Text(text = "Delete all ${tab.label.lowercase()} trips?")
            },
            text = {
                Text(text = "This removes $count ${tab.label.lowercase()} trips from this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (tab) {
                            TripTab.Saved -> onClearSavedTrips()
                            TripTab.Recent -> onClearRecentTrips()
                        }
                        clearTab = null
                    },
                ) {
                    Text(text = "Delete all")
                }
            },
            dismissButton = {
                TextButton(onClick = { clearTab = null }) {
                    Text(text = "Cancel")
                }
            },
        )
    }
}

@Composable
private fun TripTabChip(
    tab: TripTab,
    count: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (tab == TripTab.Saved) TransitBlue else TransitGreen
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (selected) tint else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) tint else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(imageVector = tab.icon, contentDescription = null)
            Text(
                text = "${tab.label} $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
