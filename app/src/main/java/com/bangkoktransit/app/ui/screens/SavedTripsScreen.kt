package com.bangkoktransit.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.bangkoktransit.app.data.model.SavedTrip
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.theme.TransitLine
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(TripTab.entries) { tab ->
                TripTabChip(
                    tab = tab,
                    count = when (tab) {
                        TripTab.Saved -> state.savedTrips.size
                        TripTab.Recent -> state.recentTrips.size
                    },
                    selected = selectedTab == tab,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
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
    onClick: () -> Unit,
) {
    val tint = if (tab == TripTab.Saved) TransitBlue else TransitGreen
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) tint else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) tint else TransitLine),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
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
