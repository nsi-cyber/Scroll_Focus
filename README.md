# ViewingTimeTrackingLazyColumn

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose BOM](https://img.shields.io/badge/Compose%20BOM-2024.02.00-4285f4.svg?logo=jetpack-compose)](https://developer.android.com/jetpack/compose)

A powerful Jetpack Compose component that provides real-time viewing time analytics for LazyColumn items with configurable visibility thresholds.

## 🚀 Features

- **Real-time Tracking**: Measures viewing time in milliseconds with high precision
- **Configurable Threshold**: Set custom visibility percentages (50%, 70%, 90%, etc.)
- **Native Performance**: Uses LazyColumn's native `layoutInfo` for optimal performance
- **Flexible Content**: Support for any composable content mix
- **Zero Configuration**: Works out of the box with sensible defaults
- **Production Ready**: Memory efficient and performance optimized

## 📱 Demo

 

*Real-time viewing analytics with configurable thresholds*

## 🛠 Installation

Add the component to your Jetpack Compose project:

```kotlin
// Copy the ViewingTimeTrackingLazyColumn.kt file to your project
// ViewingTimeTrackingLazyColumn(
//    content = yourComposables,
//    onViewingTimesChanged = { times ->
//        // Real-time viewing data in milliseconds
//    },
//    visibilityThreshold = 0.7f // 70% visible
)
```

### Requirements

- Kotlin 1.9.0+
- Jetpack Compose BOM 2024.02.00+
- Android API 24+

## 📖 Quick Start

### Basic Usage

```kotlin
@Composable
fun MyScreen() {
    val content = listOf<@Composable () -> Unit>(
        { Text("Item 1") },
        { Card { Text("Item 2") } },
        { MyCustomComposable() }
    )
    
    ViewingTimeTrackingLazyColumn(
        content = content,
        onViewingTimesChanged = { viewingTimes ->
            // viewingTimes[0] = Item 1 viewing time in milliseconds
            // viewingTimes[1] = Item 2 viewing time in milliseconds
            // viewingTimes[2] = Item 3 viewing time in milliseconds
            println("Item 0 viewed for: ${viewingTimes[0]}ms")
        }
    )
}
```

### Advanced Usage with Custom Threshold

```kotlin
@Composable
fun AdvancedTracking() {
    var viewingData by remember { mutableStateOf(listOf<Long>()) }
    
    ViewingTimeTrackingLazyColumn(
        content = myContentList,
        onViewingTimesChanged = { times -> viewingData = times },
        visibilityThreshold = 0.7f, // 70% of item must be visible
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    )
}
```

## 📚 API Reference

### ViewingTimeTrackingLazyColumn

```kotlin
@Composable
fun ViewingTimeTrackingLazyColumn(
    content: List<@Composable () -> Unit>,
    onViewingTimesChanged: (List<Long>) -> Unit,
    visibilityThreshold: Float = 0.7f,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top
)
```

#### Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `content` | `List<@Composable () -> Unit>` | Required | List of composable functions to display |
| `onViewingTimesChanged` | `(List<Long>) -> Unit` | Required | Callback with viewing times in milliseconds |
| `visibilityThreshold` | `Float` | `0.7f` | Minimum visibility percentage (0.0-1.0) |
| `modifier` | `Modifier` | `Modifier` | Standard Compose modifier |
| `state` | `LazyListState` | `rememberLazyListState()` | LazyColumn state |
| `contentPadding` | `PaddingValues` | `PaddingValues(0.dp)` | Content padding |
| `verticalArrangement` | `Arrangement.Vertical` | `Arrangement.Top` | Item arrangement |

### ViewingTimeMetrics

```kotlin
data class ViewingTimeMetrics(
    val itemId: Int,
    val totalVisibleTimeMs: Long = 0,
    val isCurrentlyVisible: Boolean = false,
    val lastVisibilityStart: Long = 0
)
```

## 🎯 Use Cases

### Analytics & User Behavior
```kotlin
ViewingTimeTrackingLazyColumn(
    content = newsArticles,
    onViewingTimesChanged = { times ->
        // Send analytics data
        analytics.trackItemViewingTimes(times)
    },
    visibilityThreshold = 0.8f // Strict visibility for analytics
)
```

### Content Personalization
```kotlin
ViewingTimeTrackingLazyColumn(
    content = recommendedContent,
    onViewingTimesChanged = { times ->
        // Adjust recommendations based on viewing patterns
        personalizer.updatePreferences(times)
    }
)
```

### A/B Testing
```kotlin
val threshold = if (isTestGroup) 0.5f else 0.7f

ViewingTimeTrackingLazyColumn(
    content = testContent,
    onViewingTimesChanged = { times ->
        abTesting.recordMetrics(threshold, times)
    },
    visibilityThreshold = threshold
)
```

## 🔧 How It Works

The component leverages LazyColumn's native `layoutInfo` to:

1. **Track Visibility**: Calculate precise visibility percentages for each item
2. **Apply Threshold**: Only count items meeting the visibility threshold
3. **Measure Time**: Accumulate viewing time in milliseconds
4. **Real-time Updates**: Emit updates on every scroll change

### Visibility Calculation

```kotlin
fun calculateVisibilityPercentage(
    itemOffset: Int,
    itemSize: Int,
    viewportHeight: Int
): Float {
    val visibleHeight = /* calculate visible portion */
    return visibleHeight / itemSize.toFloat()
}
```

## 🎨 Customization

### Custom Visibility Thresholds

### Mixed Content Types

```kotlin
val mixedContent = listOf<@Composable () -> Unit>(
    { Text("Header") },
    { ImageCard(imageUrl) },
    { VideoPlayer(videoUrl) },
    { AdBanner() },
    { UserComment(comment) }
)
```

## 📊 Performance

- **Memory Efficient**: Only tracks visible items
- **CPU Optimized**: Uses native LazyColumn mechanics
- **Battery Friendly**: Minimal overhead on scroll events
- **Scalable**: Tested with 1000+ items

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run the demo app

### Guidelines

- Follow Kotlin coding conventions
- Add tests for new features
- Update documentation
- Ensure backwards compatibility

## 📄 License

```
MIT License

Copyright (c) 2024 ViewingTimeTrackingLazyColumn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 🙏 Acknowledgments

- Jetpack Compose team for the amazing framework
- Android community for inspiration and feedback

---

**⭐ If this project helped you, please consider giving it a star!** 
