package com.sentinel.app.features.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentinel.app.domain.model.CameraEventType
import com.sentinel.app.ui.components.EmptyStateView
import com.sentinel.app.ui.components.EventRow
import com.sentinel.app.ui.preview.SampleData
import com.sentinel.app.ui.theme.BackgroundDeep
import com.sentinel.app.ui.theme.CyanPrimary
import com.sentinel.app.ui.theme.CyanSubtle
import com.sentinel.app.ui.theme.SentinelTheme
import com.sentinel.app.ui.theme.SurfaceElevated
import com.sentinel.app.ui.theme.SurfaceStroke
import com.sentinel.app.ui.theme.TextPrimary
import com.sentinel.app.ui.theme.TextSecondary

@Composable
fun EventsScreen(
    onNavigateBack: () -> Unit,
    viewModel: EventsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    EventsContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onCameraFilter = viewModel::setCameraFilter,
        onTypeFilter = viewModel::setEventTypeFilter,
        onClearFilters = viewModel::clearFilters,
        onMarkAllRead = viewModel::markAllRead,
        onMarkRead = viewModel::markRead
    )
}

@Composable
private fun EventsContent(
    state: EventsUiState,
    onNavigateBack: () -> Unit,
    onCameraFilter: (String?) -> Unit,
    onTypeFilter: (CameraEventType?) -> Unit,
    onClearFilters: () -> Unit,
    onMarkAllRead: () -> Unit,
    onMarkRead: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDeep)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
            }
            Text(
                "Events",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            val unread = state.filteredEvents.count { !it.isRead }
            if (unread > 0) {
                IconButton(onClick = onMarkAllRead) {
                    Icon(Icons.Default.DoneAll, "Mark all read", tint = CyanPrimary)
                }
            }
        }

        // ── Filter row — Event Type ───────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip("All Types", state.selectedEventType == null) { onTypeFilter(null) }
            }
            items(CameraEventType.values().toList()) { type ->
                FilterChip(type.displayName, state.selectedEventType == type) { onTypeFilter(type) }
            }
        }
        Spacer(Modifier.height(8.dp))

        // ── Camera filter ─────────────────────────────────────────────────
        if (state.cameras.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip("All Cameras", state.selectedCameraId == null) { onCameraFilter(null) }
                }
                items(state.cameras) { cam ->
                    FilterChip(cam.name, state.selectedCameraId == cam.id) { onCameraFilter(cam.id) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Results count ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${state.filteredEvents.size} events",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            val hasFilter = state.selectedCameraId != null || state.selectedEventType != null
            if (hasFilter) {
                Text(
                    "Clear filters",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanPrimary,
                    modifier = Modifier.clickable(onClick = onClearFilters)
                )
            }
        }

        // ── Event list ────────────────────────────────────────────────────
        if (state.filteredEvents.isEmpty() && !state.isLoading) {
            EmptyStateView(
                icon = Icons.Default.Timeline,
                title = "No events",
                subtitle = "Matching events will appear here as cameras report activity",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.filteredEvents, key = { it.id }) { event ->
                    EventRow(event = event, onClick = { onMarkRead(event.id) })
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 68.dp)
                            .height(1.dp)
                            .background(SurfaceStroke)
                    )
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CyanSubtle else SurfaceElevated)
            .border(1.dp, if (selected) CyanPrimary.copy(alpha = 0.5f) else SurfaceStroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) CyanPrimary else TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060B10)
@Composable
private fun EventsPreview() {
    SentinelTheme {
        EventsContent(
            state = EventsUiState(
                events = SampleData.sampleEvents,
                cameras = SampleData.allCameras,
                isLoading = false
            ),
            onNavigateBack = {}, onCameraFilter = {}, onTypeFilter = {},
            onClearFilters = {}, onMarkAllRead = {}, onMarkRead = {}
        )
    }
}
