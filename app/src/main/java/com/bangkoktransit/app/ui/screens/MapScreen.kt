package com.bangkoktransit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.transitgo.app.R
import com.bangkoktransit.app.data.model.RoutePath
import com.bangkoktransit.app.data.model.RouteStation
import com.bangkoktransit.app.data.model.Station
import com.bangkoktransit.app.ui.theme.TransitBlue
import com.bangkoktransit.app.ui.theme.TransitCoral
import com.bangkoktransit.app.ui.theme.TransitGreen
import com.bangkoktransit.app.ui.theme.TransitInk
import com.bangkoktransit.app.ui.theme.TransitLine
import com.bangkoktransit.app.ui.viewmodel.TransitUiState

private const val MapMotionFastMillis = 160
private const val MapMotionMediumMillis = 260

@Composable
fun MapScreen(
    state: TransitUiState,
    onOpenPlanner: () -> Unit,
    onChooseStart: () -> Unit,
    onChooseTarget: () -> Unit,
    onSetStart: (Station) -> Unit,
    onSetTarget: (Station) -> Unit,
    onPlanRoute: () -> Unit,
    onSelectRoute: (RoutePath) -> Unit,
    onClearRoute: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var stationForDialog by remember { mutableStateOf<Station?>(null) }
    var showRouteSteps by rememberSaveable { mutableStateOf(false) }
    var showRouteSheet by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(
        state.activeRoute?.routeKey,
        state.selectedStart?.stationCode,
        state.selectedTarget?.stationCode,
    ) {
        showRouteSheet = true
    }

    fun clampedOffset(proposedOffset: Offset, targetScale: Float): Offset {
        val viewportWidth = viewportSize.width.toFloat()
        val viewportHeight = viewportSize.height.toFloat()
        if (viewportWidth <= 0f || viewportHeight <= 0f) return proposedOffset

        val mapGeometry = fittedMapGeometry(viewportWidth, viewportHeight)
        val drawnWidth = mapGeometry.widthPx * targetScale.coerceAtLeast(1f)
        val drawnHeight = mapGeometry.heightPx * targetScale.coerceAtLeast(1f)

        fun clampAxis(value: Float, viewport: Float, base: Float, drawnSize: Float): Float {
            val min = viewport - base - drawnSize
            val max = -base
            return value.coerceIn(minOf(min, max), maxOf(min, max))
        }

        return Offset(
            x = clampAxis(proposedOffset.x, viewportWidth, mapGeometry.baseX, drawnWidth),
            y = clampAxis(proposedOffset.y, viewportHeight, mapGeometry.baseY, drawnHeight),
        )
    }

    fun resetMap() {
        scale = 1f
        offset = Offset.Zero
    }

    fun zoomIn() {
        val nextScale = (scale * 1.22f).coerceAtMost(5f)
        scale = nextScale
        offset = clampedOffset(offset, nextScale)
    }

    fun zoomOut() {
        val nextScale = (scale / 1.22f).coerceAtLeast(1f)
        scale = nextScale
        offset = clampedOffset(offset, nextScale)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .background(Color.White),
    ) {
        RouteMapCanvas(
            stations = state.stations,
            route = state.activeRoute,
            startStation = state.selectedStart,
            targetStation = state.selectedTarget,
            scale = scale,
            offset = offset,
            onGesture = { pan, zoomChange ->
                val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                scale = nextScale
                offset = clampedOffset(offset + pan, nextScale)
            },
            onStationTap = { stationForDialog = it },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapControlButton(icon = Icons.Filled.Add, onClick = ::zoomIn)
            MapControlButton(icon = Icons.Filled.Remove, onClick = ::zoomOut)
            MapControlButton(icon = Icons.Filled.CenterFocusStrong, onClick = ::resetMap)
        }

        AnimatedVisibility(
            visible = showRouteSheet,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = tween(
                    durationMillis = MapMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(animationSpec = tween(durationMillis = MapMotionFastMillis)),
            exit = slideOutVertically(
                targetOffsetY = { it / 3 },
                animationSpec = tween(
                    durationMillis = MapMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(animationSpec = tween(durationMillis = MapMotionFastMillis)),
        ) {
            RouteMapSheet(
                state = state,
                onOpenPlanner = onOpenPlanner,
                onChooseStart = onChooseStart,
                onChooseTarget = onChooseTarget,
                onPlanRoute = onPlanRoute,
                onSelectRoute = onSelectRoute,
                onClearRoute = {
                    showRouteSteps = false
                    onClearRoute()
                },
                onHideSheet = { showRouteSheet = false },
                showRouteSteps = showRouteSteps,
                onToggleRouteSteps = { showRouteSteps = !showRouteSteps },
                modifier = Modifier,
            )
        }

        AnimatedVisibility(
            visible = !showRouteSheet,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(
                    durationMillis = MapMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeIn(animationSpec = tween(durationMillis = MapMotionFastMillis)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(
                    durationMillis = MapMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            ) + fadeOut(animationSpec = tween(durationMillis = MapMotionFastMillis)),
        ) {
            RouteMapSheetChip(
                state = state,
                onClick = { showRouteSheet = true },
                modifier = Modifier,
            )
        }

        stationForDialog?.let { station ->
            MapStationDialog(
                station = station,
                isStart = station.stationCode == state.selectedStart?.stationCode,
                isTarget = station.stationCode == state.selectedTarget?.stationCode,
                onSetStart = {
                    onSetStart(station)
                    stationForDialog = null
                },
                onSetTarget = {
                    onSetTarget(station)
                    stationForDialog = null
                },
                onDismiss = { stationForDialog = null },
            )
        }
    }
}

@Composable
private fun RouteMapSheet(
    state: TransitUiState,
    onOpenPlanner: () -> Unit,
    onChooseStart: () -> Unit,
    onChooseTarget: () -> Unit,
    onPlanRoute: () -> Unit,
    onSelectRoute: (RoutePath) -> Unit,
    onClearRoute: () -> Unit,
    onHideSheet: () -> Unit,
    showRouteSteps: Boolean,
    onToggleRouteSteps: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeRoute = state.activeRoute
    val canClear = activeRoute != null ||
        state.selectedStart != null ||
        state.selectedTarget != null ||
        state.routeOptions.isNotEmpty() ||
        state.routeError != null
    val canPlan = state.selectedStart != null &&
        state.selectedTarget != null &&
        !state.isPlanningRoute &&
        !state.isLoadingStations
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TransitLine),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activeRoute?.displayName() ?: "Route map",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (activeRoute != null) {
                    Text(
                        text = activeRoute.fareTotal.formatFare(),
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = TransitGreen,
                        maxLines = 1,
                    )
                }
                IconActionButton(
                    icon = Icons.Filled.ExpandMore,
                    contentDescription = "Hide route details",
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = onHideSheet,
                )
                if (canClear) {
                    IconActionButton(
                        icon = Icons.Filled.Clear,
                        contentDescription = "Clear result",
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = onClearRoute,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RouteEndpointChip(
                    label = "From",
                    station = state.selectedStart,
                    tint = TransitBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onChooseStart,
                )
                RouteEndpointChip(
                    label = "To",
                    station = state.selectedTarget,
                    tint = TransitCoral,
                    modifier = Modifier.weight(1f),
                    onClick = onChooseTarget,
                )
            }

            if (activeRoute == null) {
                PrimaryActionButton(
                    text = if (state.isPlanningRoute) "Planning route" else "Plan route",
                    enabled = canPlan,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Route,
                    onClick = onPlanRoute,
                )
            } else {
                if (state.routeOptions.size > 1) {
                    MapRouteOptionsStrip(
                        routes = state.routeOptions,
                        selectedRoute = activeRoute,
                        onSelectRoute = onSelectRoute,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${activeRoute.stats.totalStations} stations / ${activeRoute.stats.totalTransfers} transfers",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    QuietActionButton(
                        text = "Edit trip",
                        icon = Icons.Filled.Route,
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = onOpenPlanner,
                    )
                }

                RouteStepsDropdown(
                    route = activeRoute,
                    stations = state.stations,
                    showSteps = showRouteSteps,
                    onToggleSteps = onToggleRouteSteps,
                )
            }

            if (state.routeError != null) {
                Text(
                    text = state.routeError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MapRouteOptionsStrip(
    routes: List<RoutePath>,
    selectedRoute: RoutePath,
    onSelectRoute: (RoutePath) -> Unit,
) {
    val selectedRouteKey = selectedRoute.routeKey
    val selectedIndex = routes.indexOfFirst { it.routeKey == selectedRouteKey }
        .takeIf { it >= 0 }
        ?: 0
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(selectedRouteKey, routes) {
        if (selectedIndex < routes.size) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Route options",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = routes,
                key = { route -> route.routeKey },
            ) { route ->
                MapRouteOptionCard(
                    route = route,
                    selected = route.routeKey == selectedRoute.routeKey,
                    onClick = { onSelectRoute(route) },
                )
            }
        }
    }
}

@Composable
private fun MapRouteOptionCard(
    route: RoutePath,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) TransitBlue else TransitLine
    val cardScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.97f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "map route option scale",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = MapMotionFastMillis),
        label = "map route option border",
    )
    Surface(
        modifier = Modifier
            .width(164.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) TransitBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = route.displayName(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = route.fareTotal.formatFare(),
                    modifier = Modifier.padding(start = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = TransitGreen,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            Text(
                text = "${route.stats.totalStations} stations / ${route.stats.totalTransfers} transfers",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RouteMapSheetChip(
    state: TransitUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeRoute = state.activeRoute
    val title = activeRoute?.displayName() ?: "Route details"
    val subtitle = when {
        activeRoute != null -> {
            "${activeRoute.stats.totalStations} stations / ${activeRoute.stats.totalTransfers} transfers"
        }
        state.selectedStart != null && state.selectedTarget != null -> {
            "${state.selectedStart.displayCode} to ${state.selectedTarget.displayCode}"
        }
        state.selectedStart != null -> "From ${state.selectedStart.displayCode}"
        state.selectedTarget != null -> "To ${state.selectedTarget.displayCode}"
        else -> "Choose stations"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TransitLine),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ExpandLess,
                contentDescription = null,
                tint = TransitInk,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (activeRoute != null) {
                Text(
                    text = activeRoute.fareTotal.formatFare(),
                    style = MaterialTheme.typography.titleSmall,
                    color = TransitGreen,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RouteStepsDropdown(
    route: RoutePath,
    stations: List<Station>,
    showSteps: Boolean,
    onToggleSteps: () -> Unit,
) {
    QuietActionButton(
        text = if (showSteps) "Hide steps" else "View steps",
        icon = if (showSteps) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleSteps,
    )

    AnimatedVisibility(
        visible = showSteps,
        enter = expandVertically(
            animationSpec = tween(
                durationMillis = MapMotionMediumMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + fadeIn(animationSpec = tween(durationMillis = MapMotionFastMillis)),
        exit = shrinkVertically(
            animationSpec = tween(
                durationMillis = MapMotionFastMillis,
                easing = FastOutSlowInEasing,
            ),
        ) + fadeOut(animationSpec = tween(durationMillis = MapMotionFastMillis)),
    ) {
        RouteStepsTimeline(
            route = route,
            stations = stations,
            modifier = Modifier
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState()),
            compact = true,
        )
    }
}

@Composable
private fun RouteEndpointChip(
    label: String,
    station: Station?,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = tint.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StationCodeBadge(
                code = station?.displayCode ?: label.take(1),
                color = tint,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    maxLines = 1,
                )
                Text(
                    text = station?.nameEn ?: "Choose station",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MapStationDialog(
    station: Station,
    isStart: Boolean,
    isTarget: Boolean,
    onSetStart: () -> Unit,
    onSetTarget: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, TransitLine),
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StationCodeBadge(
                        code = station.displayCode,
                        color = lineColorForName(station.line?.nameEn),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = station.line?.nameEn ?: station.stationCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StationDialogAction(
                        text = if (isStart) "From selected" else "Set From",
                        tint = TransitBlue,
                        selected = isStart,
                        modifier = Modifier.weight(1f),
                        onClick = onSetStart,
                    )
                    StationDialogAction(
                        text = if (isTarget) "To selected" else "Set To",
                        tint = TransitCoral,
                        selected = isTarget,
                        modifier = Modifier.weight(1f),
                        onClick = onSetTarget,
                    )
                }

                QuietActionButton(
                    text = "Cancel",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun StationDialogAction(
    text: String,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = tint.copy(alpha = if (selected) 0.18f else 0.1f),
        contentColor = tint,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = if (selected) 0.42f else 0.22f)),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RouteMapCanvas(
    stations: List<Station>,
    route: RoutePath?,
    startStation: Station?,
    targetStation: Station?,
    scale: Float,
    offset: Offset,
    onGesture: (pan: Offset, zoomChange: Float) -> Unit,
    onStationTap: (Station) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnGesture by rememberUpdatedState(onGesture)
    val currentOnStationTap by rememberUpdatedState(onStationTap)

    BoxWithConstraints(
        modifier = modifier
            .background(Color.White)
            .clipToBounds()
            .pointerInput(stations, scale, offset) {
                detectTapGestures { tapOffset ->
                    nearestStationAt(
                        tapOffset = tapOffset,
                        stations = stations,
                        viewportWidth = size.width.toFloat(),
                        viewportHeight = size.height.toFloat(),
                        scale = scale,
                        offset = offset,
                        maxDistancePx = 30.dp.toPx(),
                    )?.let(currentOnStationTap)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    currentOnGesture(pan, zoomChange)
                }
            },
    ) {
        val mapSize = fittedMapSize(maxWidth, maxHeight)
        val mapOffsetX = (maxWidth - mapSize.width) / 2f
        val mapOffsetY = (maxHeight - mapSize.height) / 2f

        Box(
            modifier = Modifier
                .size(width = mapSize.width, height = mapSize.height)
                .offset(x = mapOffsetX, y = mapOffsetY)
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            Image(
                painter = painterResource(id = R.drawable.bangkok_transit_map),
                contentDescription = "Bangkok transit map",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            if (route != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = ROUTE_BACKGROUND_DIM_ALPHA)),
                )
            }
            RouteOverlay(
                route = route,
                startStation = startStation,
                targetStation = targetStation,
                scale = scale,
            )
            RouteMarkers(
                maxWidth = mapSize.width,
                maxHeight = mapSize.height,
                route = route,
                startStation = startStation,
                targetStation = targetStation,
                scale = scale,
            )
        }
    }
}

@Composable
private fun RouteOverlay(
    route: RoutePath?,
    startStation: Station?,
    targetStation: Station?,
    scale: Float,
) {
    val routeProgress = remember { Animatable(if (route == null) 0f else 1f) }

    LaunchedEffect(route?.routeKey) {
        if (route == null) {
            routeProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = MapMotionFastMillis),
            )
        } else {
            routeProgress.snapTo(0f)
            routeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MapMotionMediumMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val adjustedScale = scale.coerceAtLeast(1f)
        val progress = routeProgress.value.coerceIn(0f, 1f)
        val routePoints = route?.stations.orEmpty()
            .filter { it.x > 0.0 || it.y > 0.0 }
            .map { routeStation ->
                Offset(
                    x = (routeStation.x / MAP_WIDTH * size.width).toFloat(),
                    y = (routeStation.y / MAP_HEIGHT * size.height).toFloat(),
                )
            }

        if (progress > 0f) {
            routePoints.zipWithNext().forEach { (from, to) ->
                drawLine(
                    color = TransitGreen.copy(alpha = 0.82f * progress),
                    start = from,
                    end = to,
                    strokeWidth = (6f + 2f * progress) / adjustedScale,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.82f * progress),
                    start = from,
                    end = to,
                    strokeWidth = (2f + progress) / adjustedScale,
                    cap = StrokeCap.Round,
                )
            }
        }

        listOfNotNull(startStation, targetStation)
            .filter { it.x > 0.0 || it.y > 0.0 }
            .forEach { station ->
                val point = Offset(
                    x = (station.x / MAP_WIDTH * size.width).toFloat(),
                    y = (station.y / MAP_HEIGHT * size.height).toFloat(),
                )
                val color = if (station.stationCode == startStation?.stationCode) TransitBlue else TransitCoral
                drawCircle(color = Color.White, radius = 13f / adjustedScale, center = point)
                drawCircle(color = color, radius = 8f / adjustedScale, center = point)
            }
    }
}

@Composable
private fun RouteMarkers(
    maxWidth: Dp,
    maxHeight: Dp,
    route: RoutePath?,
    startStation: Station?,
    targetStation: Station?,
    scale: Float,
) {
    val markerPulse = rememberInfiniteTransition(label = "routeMarkerPulse")
    val markerScale by markerPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "routeMarkerScale",
    )
    val markerHaloAlpha by markerPulse.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 780),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "routeMarkerHalo",
    )
    val hasActiveRoute = route != null
    val adjustedScale = scale.coerceAtLeast(1f)
    val markerSize = (18f / adjustedScale).dp
    val markerDotSize = (7f / adjustedScale).dp
    val markerBorder = (2f / adjustedScale).dp
    val markerStations = buildList {
        addAll(route?.stations.orEmpty())
        startStation?.let { add(RouteStation(it.stationCode, it.x, it.y)) }
        targetStation?.let { add(RouteStation(it.stationCode, it.x, it.y)) }
    }
        .filter { it.x > 0.0 || it.y > 0.0 }
        .distinctBy { it.stationCode }

    markerStations.forEach { station ->
        val markerColor = when (station.stationCode) {
            startStation?.stationCode -> TransitBlue
            targetStation?.stationCode -> TransitCoral
            else -> TransitGreen
        }
        val markerOffset = DpOffset(
            x = maxWidth * (station.x / MAP_WIDTH).toFloat(),
            y = maxHeight * (station.y / MAP_HEIGHT).toFloat(),
        )
        MapMarker(
            color = markerColor,
            size = markerSize,
            dotSize = markerDotSize,
            borderWidth = markerBorder,
            pulseScale = if (hasActiveRoute) markerScale else 1f,
            haloAlpha = if (hasActiveRoute) markerHaloAlpha else 0f,
            modifier = Modifier
                .offset(
                    x = markerOffset.x - markerSize / 2f,
                    y = markerOffset.y - markerSize / 2f,
                )
                .zIndex(4f),
        )
    }
}

@Composable
private fun MapMarker(
    color: Color,
    size: Dp,
    dotSize: Dp,
    borderWidth: Dp,
    pulseScale: Float,
    haloAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(size * 1.65f)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = haloAlpha
                    }
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Surface(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                .clip(CircleShape),
            color = Color.White,
            shape = CircleShape,
            border = BorderStroke(borderWidth, color),
            shadowElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = TransitInk,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TransitLine),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(19.dp))
        }
    }
}

private data class DpOffset(
    val x: Dp,
    val y: Dp,
)

private data class MapGeometry(
    val widthPx: Float,
    val heightPx: Float,
    val baseX: Float,
    val baseY: Float,
)

private data class MapDpSize(
    val width: Dp,
    val height: Dp,
)

private fun fittedMapSize(maxWidth: Dp, maxHeight: Dp): MapDpSize {
    val heightWhenWidthFits = maxWidth * MAP_IMAGE_ASPECT_RATIO
    return if (heightWhenWidthFits <= maxHeight) {
        MapDpSize(width = maxWidth, height = heightWhenWidthFits)
    } else {
        MapDpSize(width = maxHeight / MAP_IMAGE_ASPECT_RATIO, height = maxHeight)
    }
}

private fun fittedMapGeometry(viewportWidth: Float, viewportHeight: Float): MapGeometry {
    val heightWhenWidthFits = viewportWidth * MAP_IMAGE_ASPECT_RATIO
    val mapWidth: Float
    val mapHeight: Float
    if (heightWhenWidthFits <= viewportHeight) {
        mapWidth = viewportWidth
        mapHeight = heightWhenWidthFits
    } else {
        mapHeight = viewportHeight
        mapWidth = viewportHeight / MAP_IMAGE_ASPECT_RATIO
    }

    return MapGeometry(
        widthPx = mapWidth,
        heightPx = mapHeight,
        baseX = (viewportWidth - mapWidth) / 2f,
        baseY = (viewportHeight - mapHeight) / 2f,
    )
}

private fun nearestStationAt(
    tapOffset: Offset,
    stations: List<Station>,
    viewportWidth: Float,
    viewportHeight: Float,
    scale: Float,
    offset: Offset,
    maxDistancePx: Float,
): Station? {
    if (viewportWidth <= 0f || viewportHeight <= 0f) return null
    val mapGeometry = fittedMapGeometry(viewportWidth, viewportHeight)

    return stations
        .asSequence()
        .filter { it.hasMapPosition() }
        .map { station ->
            val stationOffset = station.viewportOffset(mapGeometry, scale, offset)
            val dx = stationOffset.x - tapOffset.x
            val dy = stationOffset.y - tapOffset.y
            station to (dx * dx + dy * dy)
        }
        .filter { (_, distanceSquared) -> distanceSquared <= maxDistancePx * maxDistancePx }
        .minByOrNull { (_, distanceSquared) -> distanceSquared }
        ?.first
}

private fun Station.viewportOffset(
    mapGeometry: MapGeometry,
    scale: Float,
    offset: Offset,
): Offset {
    return Offset(
        x = mapGeometry.baseX + offset.x + (x / MAP_WIDTH * mapGeometry.widthPx * scale).toFloat(),
        y = mapGeometry.baseY + offset.y + (y / MAP_HEIGHT * mapGeometry.heightPx * scale).toFloat(),
    )
}

private fun Station.hasMapPosition(): Boolean = x > 0.0 || y > 0.0

private const val MAP_WIDTH = 841.89
private const val MAP_HEIGHT = 841.89
private const val MAP_BITMAP_WIDTH = 1959f
private const val MAP_BITMAP_HEIGHT = 2048f
private const val MAP_IMAGE_ASPECT_RATIO = MAP_BITMAP_HEIGHT / MAP_BITMAP_WIDTH
private const val ROUTE_BACKGROUND_DIM_ALPHA = 0.24f
