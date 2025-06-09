package com.nsicyber.scrollfocus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsicyber.scrollfocus.ui.theme.ScrollFocusTheme
import kotlin.random.Random

/**
 * Data class to track viewing time metrics for LazyColumn items
 *
 * @property itemId Unique identifier for the tracked item
 * @property totalVisibleTimeMs Total accumulated visible time in milliseconds
 * @property isCurrentlyVisible Current visibility state of the item
 * @property lastVisibilityStart Timestamp when the item became visible (for current session)
 */
data class ViewingTimeMetrics(
    val itemId: Int,
    val totalVisibleTimeMs: Long = 0,
    val isCurrentlyVisible: Boolean = false,
    val lastVisibilityStart: Long = 0
)

/**
 * A LazyColumn component that tracks viewing time for each item in real-time.
 *
 * This component automatically measures how long each item is visible on screen
 * and provides real-time callbacks with the accumulated viewing times.
 *
 * Key Features:
 * - Real-time visibility tracking using LazyListState.layoutInfo
 * - Accurate time measurement in milliseconds
 * - Flexible content support (mix different composables)
 * - Performance optimized with native LazyColumn mechanics
 * - Zero configuration required for basic usage
 * - Configurable visibility threshold (e.g., 70% visible)
 *
 * @param content List of composable functions to be displayed in the LazyColumn
 * @param onViewingTimesChanged Callback invoked when viewing times change,
 *                             provides List<Long> where each index corresponds
 *                             to viewing time in milliseconds for that item
 * @param visibilityThreshold Minimum percentage (0.0 to 1.0) of item that must be visible
 *                           to be considered "viewed". Default is 0.7 (70%)
 * @param modifier Standard Compose modifier for styling
 * @param state LazyListState for controlling scroll behavior
 * @param contentPadding Padding applied to the content
 * @param verticalArrangement Vertical arrangement strategy for items
 */
@Composable
fun ViewingTimeTrackingLazyColumn(
    modifier: Modifier = Modifier,
    content: List<@Composable () -> Unit>,
    onViewingTimesChanged: (List<Long>) -> Unit,
    visibilityThreshold: Float = 0.7f,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top
) {
    // State to track viewing metrics for each item
    var viewingMetrics by remember {
        mutableStateOf(
            content.indices.map { ViewingTimeMetrics(itemId = it) }
        )
    }

    /**
     * Calculates the visibility percentage of an item based on its position and size
     *
     * @param itemOffset The offset of the item from the top of the viewport
     * @param itemSize The height of the item
     * @param viewportHeight The height of the visible viewport
     * @return Visibility percentage as a float between 0.0 and 1.0
     */
    fun calculateVisibilityPercentage(
        itemOffset: Int,
        itemSize: Int,
        viewportHeight: Int
    ): Float {
        val itemTop = itemOffset
        val itemBottom = itemOffset + itemSize

        // Calculate visible bounds
        val visibleTop = maxOf(0, itemTop)
        val visibleBottom = minOf(viewportHeight, itemBottom)

        // If item is completely outside viewport
        if (visibleTop >= visibleBottom) return 0f

        val visibleHeight = visibleBottom - visibleTop
        return visibleHeight.toFloat() / itemSize.toFloat()
    }

    /**
     * Updates the viewing time metrics for a specific item based on visibility changes
     *
     * @param itemId The ID of the item whose visibility changed
     * @param isVisible Current visibility state of the item
     */
    fun updateViewingTime(itemId: Int, isVisible: Boolean) {
        val currentTime = System.currentTimeMillis()
        viewingMetrics = viewingMetrics.map { metrics ->
            if (metrics.itemId == itemId) {
                when {
                    // Item became visible - start tracking
                    isVisible && !metrics.isCurrentlyVisible -> {
                        metrics.copy(
                            isCurrentlyVisible = true,
                            lastVisibilityStart = currentTime
                        )
                    }
                    // Item became invisible - accumulate time
                    !isVisible && metrics.isCurrentlyVisible -> {
                        val sessionTime = currentTime - metrics.lastVisibilityStart
                        metrics.copy(
                            isCurrentlyVisible = false,
                            totalVisibleTimeMs = metrics.totalVisibleTimeMs + sessionTime,
                            lastVisibilityStart = 0
                        )
                    }
                    // No state change
                    else -> metrics
                }
            } else metrics
        }
    }

    /**
     * Calculates current viewing times including active viewing sessions
     *
     * @return List of viewing times in milliseconds for each item
     */
    fun getCurrentViewingTimes(): List<Long> {
        val currentTime = System.currentTimeMillis()
        return viewingMetrics.map { metrics ->
            if (metrics.isCurrentlyVisible) {
                val currentSessionTime = currentTime - metrics.lastVisibilityStart
                metrics.totalVisibleTimeMs + currentSessionTime
            } else {
                metrics.totalVisibleTimeMs
            }
        }
    }

    // Track visible items using LazyColumn's native layoutInfo with visibility threshold
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo }
            .collect {
                val layoutInfo = state.layoutInfo
                val viewportHeight = layoutInfo.viewportSize.height
                val visibleItems = layoutInfo.visibleItemsInfo

                // Create a set of items that meet the visibility threshold
                val thresholdVisibleIndices = visibleItems.mapNotNull { itemInfo ->
                    val visibilityPercentage = calculateVisibilityPercentage(
                        itemOffset = itemInfo.offset,
                        itemSize = itemInfo.size,
                        viewportHeight = viewportHeight
                    )

                    if (visibilityPercentage >= visibilityThreshold) {
                        itemInfo.index
                    } else {
                        null
                    }
                }.toSet()

                // Update visibility state for all items based on threshold
                content.indices.forEach { index ->
                    val isVisible = thresholdVisibleIndices.contains(index)
                    updateViewingTime(index, isVisible)
                }
            }
    }

    // Emit viewing times to callback whenever metrics change
    LaunchedEffect(viewingMetrics) {
        onViewingTimesChanged(getCurrentViewingTimes())
    }

    LazyColumn(
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = verticalArrangement
    ) {
        itemsIndexed(content) { index, composableContent ->
            composableContent()
        }
    }
}

/**
 * Main Activity demonstrating ViewingTimeTrackingLazyColumn usage
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrollFocusTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ViewingTimeTrackingDemo(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

/**
 * Demo screen showcasing the ViewingTimeTrackingLazyColumn component
 *
 * Features demonstrated:
 * - Real-time viewing time tracking
 * - Configurable visibility threshold
 * - Mixed content types (colored boxes)
 * - Dynamic metrics display
 * - Professional UI/UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewingTimeTrackingDemo(modifier: Modifier = Modifier) {
    // State for controlling metrics visibility
    var showMetrics by remember { mutableStateOf(false) }

    // State for storing current viewing times
    var viewingTimes by remember { mutableStateOf(listOf<Long>()) }

    // State for visibility threshold
    var visibilityThreshold by remember { mutableFloatStateOf(0.0f) }

    // Generate demo content with random colored boxes
    val demoContent: List<@Composable () -> Unit> = List(50) { index ->
        @Composable {
            val color = remember {
                Color(
                    red = Random.nextFloat(),
                    green = Random.nextFloat(),
                    blue = Random.nextFloat(),
                    alpha = 1f
                )
            }
            DemoItemCard(itemId = index, backgroundColor = color)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header section with controls and metrics
        MetricsControlPanel(
            showMetrics = showMetrics,
            onToggleMetrics = { showMetrics = !showMetrics },
            viewingTimes = viewingTimes,
            totalItems = demoContent.size,
            visibilityThreshold = visibilityThreshold,
            onThresholdChange = { visibilityThreshold = it }
        )

        // Main scrollable content with viewing time tracking
        ViewingTimeTrackingLazyColumn(
            content = demoContent,
            onViewingTimesChanged = { times ->
                viewingTimes = times
            },
            visibilityThreshold = visibilityThreshold,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        )
    }
}

/**
 * Control panel for displaying metrics and demo information
 */
@Composable
private fun MetricsControlPanel(
    showMetrics: Boolean,
    onToggleMetrics: () -> Unit,
    viewingTimes: List<Long>,
    totalItems: Int,
    visibilityThreshold: Float,
    onThresholdChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ViewingTimeTrackingLazyColumn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Real-time scroll viewing analytics for Jetpack Compose",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Items: $totalItems • Active tracking: ${viewingTimes.count { it > 0 }}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Visibility threshold controls
            Text(
                text = "Visibility Threshold: ${(visibilityThreshold * 100).toInt()}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onThresholdChange(0.5f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("50%", fontSize = 12.sp)
                }
                Button(
                    onClick = { onThresholdChange(0.7f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("70%", fontSize = 12.sp)
                }
                Button(
                    onClick = { onThresholdChange(0.9f) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("90%", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onToggleMetrics
            ) {
                Text(if (showMetrics) "Hide Metrics" else "Show Viewing Metrics")
            }

            if (showMetrics && viewingTimes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Real-time Viewing Times (milliseconds)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Only counting items with ${(visibilityThreshold * 100).toInt()}%+ visibility",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(viewingTimes) { index, time ->
                        MetricsRow(itemIndex = index, viewingTime = time)
                    }
                }
            }
        }
    }
}

/**
 * Individual metrics row for displaying item viewing time
 */
@Composable
private fun MetricsRow(itemIndex: Int, viewingTime: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Item $itemIndex:",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${viewingTime}ms",
            fontSize = 12.sp,
            fontWeight = if (viewingTime > 0) FontWeight.Medium else FontWeight.Normal,
            color = if (viewingTime > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Demo item component - represents a trackable content item
 */
@Composable
fun DemoItemCard(itemId: Int, backgroundColor: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Item #$itemId",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Swipe to track viewing time",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Viewing Time Tracking Demo")
@Composable
fun ViewingTimeTrackingDemoPreview() {
    ScrollFocusTheme {
        ViewingTimeTrackingDemo()
    }
}