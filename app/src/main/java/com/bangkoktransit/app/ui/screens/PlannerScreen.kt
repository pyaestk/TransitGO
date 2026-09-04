package com.bangkoktransit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.Station
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitCoral
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.theme.TransitLine
import com.bangkoktransit.app.ui.viewmodel.TransitUiState

private const val PlannerMotionFastMillis = 160
private const val PlannerMotionMediumMillis = 280

@Composable
fun PlannerScreen(
    state: TransitUiState,
    onChooseStart: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwapStations: () -> Unit,
    onPlanRoute: () -> Unit,
    onSelectRoute: (RoutePath) -> Unit,
    onToggleSaved: () -> Unit,
    onOpenMap: () -> Unit,
    onClearRoute: () -> Unit,
    onRefreshStations: () -> Unit,
) {
    val canPlan = state.selectedStart != null &&
        state.selectedTarget != null &&
        !state.isPlanningRoute &&
        !state.isLoadingStations
    val hasRouteOutput = state.activeRoute != null ||
        state.routeOptions.isNotEmpty() ||
        state.routeError != null
    val routeSubtitle = when {
        state.selectedStart == null && state.selectedTarget == null -> "Choose two stations"
        state.selectedStart != null && state.selectedTarget != null -> {
            "${state.selectedStart.displayCode} to ${state.selectedTarget.displayCode}"
        }
        state.selectedStart != null -> "From ${state.selectedStart.displayCode}"
        else -> "To ${state.selectedTarget?.displayCode.orEmpty()}"
    }
    var swapMotionStep by remember { mutableIntStateOf(0) }
    val swapRotation by animateFloatAsState(
        targetValue = swapMotionStep * 180f,
        animationSpec = tween(
            durationMillis = PlannerMotionMediumMillis,
            easing = FastOutSlowInEasing,
        ),
        label = "swap station rotation",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "Plan trip",
            subtitle = routeSubtitle,
            action = if (hasRouteOutput) {
                {
                    QuietActionButton(
                        text = "Clear result",
                        icon = Icons.Filled.Clear,
                        onClick = onClearRoute,
                    )
                }
            } else {
                null
            },
        )

        if (state.stationError != null && state.stations.isEmpty()) {
            EmptyStateCard(
                title = "Station data is unavailable",
                body = state.stationError,
                actionText = "Try again",
                onAction = onRefreshStations,
            )
        }

        TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitBlue) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StationSelectCard(
                    label = "From",
                    station = state.selectedStart,
                    placeholder = "Choose start station",
                    tint = TransitBlue,
                    onClick = onChooseStart,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconActionButton(
                        icon = Icons.Filled.SwapVert,
                        contentDescription = "Swap stations",
                        modifier = Modifier.graphicsLayer(rotationZ = swapRotation),
                        enabled = state.selectedStart != null || state.selectedTarget != null,
                        onClick = {
                            swapMotionStep += 1
                            onSwapStations()
                        },
                    )
                }

                StationSelectCard(
                    label = "To",
                    station = state.selectedTarget,
                    placeholder = "Choose destination",
                    tint = TransitCoral,
                    onClick = onChooseTarget,
                )

                PrimaryActionButton(
                    text = if (state.isPlanningRoute) "Finding routes" else "Find routes",
                    enabled = canPlan,
                    icon = Icons.Filled.Route,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onPlanRoute,
                )

                if (state.routeError != null) {
                    Text(
                        text = state.routeError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.isPlanningRoute,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(
                    durationMillis = PlannerMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(animationSpec = tween(durationMillis = PlannerMotionFastMillis)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(
                    durationMillis = PlannerMotionFastMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(animationSpec = tween(durationMillis = PlannerMotionFastMillis)),
        ) {
            LoadingRouteCard()
        }

        val activeRoute = state.activeRoute
        AnimatedVisibility(
            visible = activeRoute != null,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(
                    durationMillis = PlannerMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(animationSpec = tween(durationMillis = PlannerMotionFastMillis)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(
                    durationMillis = PlannerMotionFastMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(animationSpec = tween(durationMillis = PlannerMotionFastMillis)),
        ) {
            state.activeRoute?.let { currentRoute ->
                RouteSummaryCarousel(
                    routes = state.routeOptions.ifEmpty { listOf(currentRoute) },
                    selectedRoute = currentRoute,
                    stations = state.stations,
                    isSaved = state.isActiveRouteSaved,
                    onSelectRoute = onSelectRoute,
                    onToggleSaved = onToggleSaved,
                    onOpenMap = onOpenMap,
                )
            }
        }

    }
}

@Composable
private fun RouteSummaryCarousel(
    routes: List<RoutePath>,
    selectedRoute: RoutePath?,
    stations: List<Station>,
    isSaved: Boolean,
    onSelectRoute: (RoutePath) -> Unit,
    onToggleSaved: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val selectedRouteKey = selectedRoute?.routeKey
    val selectedIndex = routes.indexOfFirst { it.routeKey == selectedRouteKey }
        .takeIf { it >= 0 }
        ?: 0
    val pagerState = rememberPagerState(
        initialPage = selectedIndex,
        pageCount = { routes.size },
    )

    LaunchedEffect(selectedRouteKey, routes) {
        val nextIndex = routes.indexOfFirst { it.routeKey == selectedRouteKey }
        if (nextIndex >= 0 && nextIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(nextIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, routes) {
        val route = routes.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (route.routeKey != selectedRouteKey) {
            onSelectRoute(route)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
        ) { page ->
            val route = routes[page]
            val isSelectedRoute = route.routeKey == selectedRouteKey
            RouteSummaryPanel(
                route = route,
                stations = stations,
                isSaved = isSelectedRoute && isSaved,
                routePosition = if (routes.size > 1) "${page + 1}/${routes.size}" else null,
                selected = isSelectedRoute,
                onToggleSaved = {
                    if (!isSelectedRoute) onSelectRoute(route)
                    onToggleSaved()
                },
                onOpenMap = {
                    if (!isSelectedRoute) onSelectRoute(route)
                    onOpenMap()
                },
            )
        }

        if (routes.size > 1) {
            RoutePageIndicator(
                pageCount = routes.size,
                currentPage = pagerState.currentPage,
            )
        }
    }
}

@Composable
private fun RoutePageIndicator(
    pageCount: Int,
    currentPage: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val dotSize by animateDpAsState(
                targetValue = if (index == currentPage) 8.dp else 6.dp,
                animationSpec = tween(durationMillis = PlannerMotionFastMillis),
                label = "route page dot size",
            )
            Surface(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(dotSize),
                color = if (index == currentPage) TransitBlue else TransitLine,
                shape = RoundedCornerShape(8.dp),
            ) {}
        }
    }
}

@Composable
private fun StationSelectCard(
    label: String,
    station: Station?,
    placeholder: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val stationScale = remember { Animatable(0.98f) }
    LaunchedEffect(station?.stationCode) {
        stationScale.snapTo(0.98f)
        stationScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = stationScale.value
                scaleY = stationScale.value
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = tint.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationCodeBadge(
                code = station?.displayCode ?: label.take(1),
                color = tint,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
                Text(
                    text = station?.nameEn ?: placeholder,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (station != null) {
                    Text(
                        text = station.line?.nameEn ?: "Transit line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LoadingRouteCard() {
    TransitCard(modifier = Modifier.fillMaxWidth(), tint = TransitGreen) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Finding route options",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RouteSummaryPanel(
    route: RoutePath,
    stations: List<Station>,
    isSaved: Boolean,
    routePosition: String? = null,
    selected: Boolean = true,
    modifier: Modifier = Modifier,
    onToggleSaved: () -> Unit,
    onOpenMap: () -> Unit,
) {
    val routeStats = "${route.stats.totalStations} stations / ${route.stats.totalTransfers} transfers / ${route.stats.totalLines} lines"
    val routeSubtitle = routePosition?.let { "Route $it / $routeStats" } ?: routeStats
    val routeScale = remember { Animatable(0.98f) }

    LaunchedEffect(route.routeKey) {
        routeScale.snapTo(0.98f)
        routeScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    TransitCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = routeScale.value
                scaleY = routeScale.value
            },
        tint = if (selected) TransitGreen else TransitLine,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = routeSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = route.fareTotal.formatFare(),
                    style = MaterialTheme.typography.titleLarge,
                    color = TransitGreen,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryActionButton(
                    text = "View map",
                    icon = Icons.Filled.Map,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMap,
                )
                IconActionButton(
                    icon = Icons.Filled.Bookmark,
                    contentDescription = if (isSaved) "Remove saved route" else "Save route",
                    selected = isSaved,
                    onClick = onToggleSaved,
                )
            }

            RouteStepsTimeline(
                route = route,
                stations = stations,
            )
        }
    }
}
