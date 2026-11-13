package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BackendInnovation
import com.example.data.model.ExactCategories
import com.example.data.model.UiState
import com.example.ui.components.BackendInnovationCard
import com.example.ui.components.MarketplaceSkeletonList
import com.example.ui.components.MarketplaceTesseract3D
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    innovationsState: UiState<List<BackendInnovation>>,
    isRefreshing: Boolean = false,
    isOfflineCache: Boolean = false,
    searchQuery: String,
    selectedCategory: String,
    selectedStage: String,
    selectedSort: String = "Featured",
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStageChange: (String) -> Unit,
    onSortChange: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRetry: () -> Unit,
    onOpenDetail: (BackendInnovation) -> Unit,
    onToggleLike: (BackendInnovation) -> Unit,
    onToggleBookmark: (BackendInnovation) -> Unit
) {
    val categoryChips = listOf("All") + ExactCategories.defaultItems.map { it.name }
    val sortOptions = listOf("Featured", "Top Voted", "Funding Goal")
    val hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != "All" || selectedStage != "All" || selectedSort != "Featured"

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Explore Breakthrough Innovations",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search AI, CleanTech, robotics, hardware...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.testTag("marketplace_clear_search")) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("marketplace_search_input")
            )

            // Horizontal Category Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categoryChips) { cat ->
                    val label = if (cat == "All") "All (40)" else cat
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(label) },
                        modifier = Modifier.heightIn(min = 40.dp)
                    )
                }
            }

            // Stage & Sort Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(ExactCategories.stages) { stage ->
                    FilterChip(
                        selected = selectedStage == stage,
                        onClick = { onStageChange(stage) },
                        label = { Text(stage) },
                        modifier = Modifier.heightIn(min = 38.dp)
                    )
                }
                items(sortOptions) { sort ->
                    FilterChip(
                        selected = selectedSort == sort,
                        onClick = { onSortChange(sort) },
                        leadingIcon = if (sort == sortOptions.first()) {
                            { Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(15.dp)) }
                        } else null,
                        label = { Text(sort) },
                        modifier = Modifier.heightIn(min = 38.dp)
                    )
                }
            }

            if (innovationsState is UiState.Success) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${innovationsState.data.size} innovation${if (innovationsState.data.size == 1) "" else "s"} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.heightIn(min = 36.dp).testTag("marketplace_clear_filters")
                        ) {
                            Text("Clear filters")
                        }
                    }
                }
            }

            if (isOfflineCache) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("marketplace_offline_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = "Offline cached data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Offline mode · Showing cached innovations",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onRetry,
                            enabled = !isRefreshing,
                            modifier = Modifier.heightIn(min = 48.dp).testTag("marketplace_sync_button")
                        ) {
                            Text(if (isRefreshing) "Syncing..." else "Sync")
                        }
                    }
                }
            }

            if (isRefreshing && innovationsState is UiState.Success) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .testTag("marketplace_sync_progress")
                )
            }
        }

        val pullToRefreshState = rememberPullToRefreshState()
        val showPullIndicator = isRefreshing && innovationsState !is UiState.Loading

        PullToRefreshBox(
            isRefreshing = showPullIndicator,
            onRefresh = onRetry,
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = showPullIndicator,
                    state = pullToRefreshState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("marketplace_pull_refresh")
        ) {
        when (innovationsState) {
            UiState.Loading -> {
                MarketplaceSkeletonList()
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Unable to load innovations", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = innovationsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val itemsList = innovationsState.data
                if (itemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_marketplace),
                                contentDescription = stringResource(R.string.cd_empty_marketplace),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(150.dp)
                            )
                            Text("No innovations match your filters", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Try clearing search terms or switching back to All categories.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (hasActiveFilters) {
                                OutlinedButton(onClick = onClearFilters, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text("Reset Filters")
                                }
                            }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isWide = maxWidth >= 700.dp
                        val listState = rememberLazyListState()
                        val gridState = rememberLazyGridState()
                        val scope = rememberCoroutineScope()
                        val showScrollToTop by remember(isWide) {
                            derivedStateOf {
                                if (isWide) gridState.firstVisibleItemIndex >= 2
                                else listState.firstVisibleItemIndex >= 2
                            }
                        }

                        val cycleNextCategory: () -> Unit = {
                            val idx = categoryChips.indexOf(selectedCategory)
                            val next = categoryChips[(idx + 1) % categoryChips.size]
                            onCategoryChange(next)
                        }

                        if (isWide) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(
                                    key = "marketplace_3d_tesseract",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    MarketplaceTesseract3D(
                                        selectedCategory = selectedCategory,
                                        onCycleCategory = cycleNextCategory
                                    )
                                }
                                items(itemsList, key = { it.id }) { item ->
                                    BackendInnovationCard(
                                        innovation = item,
                                        onClick = { onOpenDetail(item) },
                                        onToggleLike = { onToggleLike(item) },
                                        onToggleBookmark = { onToggleBookmark(item) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "marketplace_3d_tesseract") {
                                    MarketplaceTesseract3D(
                                        selectedCategory = selectedCategory,
                                        onCycleCategory = cycleNextCategory
                                    )
                                }
                                items(itemsList, key = { it.id }) { item ->
                                    BackendInnovationCard(
                                        innovation = item,
                                        onClick = { onOpenDetail(item) },
                                        onToggleLike = { onToggleLike(item) },
                                        onToggleBookmark = { onToggleBookmark(item) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                                item { Spacer(Modifier.height(12.dp)) }
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showScrollToTop,
                            enter = fadeIn() + scaleIn(initialScale = 0.8f),
                            exit = fadeOut() + scaleOut(targetScale = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                        ) {
                            SmallFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        if (isWide) gridState.animateScrollToItem(0)
                                        else listState.animateScrollToItem(0)
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(48.dp).testTag("marketplace_scroll_top")
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
