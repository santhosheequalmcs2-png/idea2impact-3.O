package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BackendInnovation
import com.example.data.model.ExactCategories
import com.example.data.model.UiState
import com.example.ui.components.BackendInnovationCard
import com.example.ui.components.MarketplaceSkeletonList
import com.example.ui.components.MarketplaceTesseract3D
import com.example.util.LocalHapticFeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onCycleCategory: (String) -> Unit = onCategoryChange,
    onStageChange: (String) -> Unit,
    onSortChange: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onRetry: () -> Unit,
    onPullRefresh: () -> Unit = onRetry,
    onOfflineSync: () -> Unit = onRetry,
    onOpenDetail: (BackendInnovation) -> Unit,
    onToggleLike: (BackendInnovation) -> Unit,
    onToggleBookmark: (BackendInnovation) -> Unit
) {
    val haptics = LocalHapticFeedbackManager.current
    val categoryChips = remember { listOf("All") + ExactCategories.defaultItems.map { it.name } }
    val sortOptions = listOf("Featured", "Top Voted", "Funding Goal")
    val hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != "All" || selectedStage != "All" || selectedSort != "Featured"
    val activeFilterCount = listOf(
        selectedCategory != "All",
        selectedStage != "All",
        selectedSort != "Featured"
    ).count { it }
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Explore Breakthrough Innovations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val searchInteractionSource = remember { MutableInteractionSource() }
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = searchInteractionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("marketplace_search_input")
            ) { innerTextField ->
                OutlinedTextFieldDefaults.DecorationBox(
                    value = searchQuery,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = searchInteractionSource,
                    placeholder = {
                        Text(
                            text = "Search AI, CleanTech, robotics...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    onSearchChange("")
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("marketplace_clear_search")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    container = {
                        OutlinedTextFieldDefaults.Container(
                            enabled = true,
                            isError = false,
                            interactionSource = searchInteractionSource,
                            colors = OutlinedTextFieldDefaults.colors(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                )
            }

            // Compact Filter Button + Active Summary + Result Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        onClick = {
                            haptics.tap()
                            showFilterSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeFilterCount > 0) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (activeFilterCount > 0) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .testTag("marketplace_filter_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = if (activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Filters",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeFilterCount > 0) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (activeFilterCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(activeFilterCount.toString())
                                }
                            }
                        }
                    }

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                haptics.tap()
                                onClearFilters()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("marketplace_clear_filters")
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (innovationsState is UiState.Success) {
                    val activeChipsSummary = buildList {
                        if (selectedCategory != "All") add(selectedCategory)
                        if (selectedStage != "All") add(selectedStage)
                        if (selectedSort != "Featured") add(selectedSort)
                    }.joinToString(" · ")

                    Text(
                        text = if (activeChipsSummary.isNotBlank()) {
                            "$activeChipsSummary (${innovationsState.data.size})"
                        } else {
                            "${innovationsState.data.size} innovation${if (innovationsState.data.size == 1) "" else "s"}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
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
                            onClick = {
                                haptics.tap()
                                onOfflineSync()
                            },
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
            onRefresh = {
                haptics.success()
                onPullRefresh()
            },
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
                            Button(
                                onClick = {
                                    haptics.tap()
                                    onRetry()
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
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
                                OutlinedButton(
                                    onClick = {
                                        haptics.tap()
                                        onClearFilters()
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
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
                            haptics.tap()
                            val idx = categoryChips.indexOf(selectedCategory)
                            val next = categoryChips[(idx + 1) % categoryChips.size]
                            onCycleCategory(next)
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
                                    haptics.tap()
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

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val shownCount = (innovationsState as? UiState.Success)?.data?.size
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.testTag("marketplace_filter_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Filter & Sort",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                haptics.tap()
                                onClearFilters()
                            }
                        ) {
                            Text("Reset All")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Categories (with subtle spring animations)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryChips.forEach { cat ->
                            val label = if (cat == "All") "All (40)" else cat
                            CategoryFilterChip(
                                label = label,
                                selected = selectedCategory == cat,
                                onClick = {
                                    haptics.tap()
                                    onCategoryChange(cat)
                                },
                                modifier = Modifier.testTag(
                                    "marketplace_category_chip_${cat.lowercase().replace(" ", "_")}"
                                )
                            )
                        }
                    }
                }

                // Stage
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Stage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExactCategories.stages.forEach { stage ->
                            FilterChip(
                                selected = selectedStage == stage,
                                onClick = {
                                    haptics.tap()
                                    onStageChange(stage)
                                },
                                label = { Text(stage) },
                                modifier = Modifier.heightIn(min = 38.dp)
                            )
                        }
                    }
                }

                // Sort
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sort By",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortOptions.forEach { sort ->
                            FilterChip(
                                selected = selectedSort == sort,
                                onClick = {
                                    haptics.tap()
                                    onSortChange(sort)
                                },
                                leadingIcon = if (sort == sortOptions.first()) {
                                    { Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                } else null,
                                label = { Text(sort) },
                                modifier = Modifier.heightIn(min = 38.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        haptics.success()
                        showFilterSheet = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("marketplace_filter_apply")
                ) {
                    Text(
                        text = if (shownCount != null) {
                            "Show $shownCount Innovation${if (shownCount == 1) "" else "s"}"
                        } else {
                            "Apply Filters"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipScale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "category_chip_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "category_chip_container_color"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "category_chip_label_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "category_chip_border_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "category_chip_border_width"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "category_chip_elevation"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = borderColor,
            selectedBorderColor = borderColor,
            borderWidth = borderWidth,
            selectedBorderWidth = borderWidth
        ),
        elevation = FilterChipDefaults.filterChipElevation(
            elevation = elevation,
            pressedElevation = elevation,
            focusedElevation = elevation,
            hoveredElevation = elevation
        ),
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + expandHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + scaleIn(
                        initialScale = 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                    exit = fadeOut(
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + shrinkHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) + scaleOut(targetScale = 0.75f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                    }
                }
                Text(
                    text = label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        },
        modifier = modifier
            .heightIn(min = 40.dp)
            .scale(chipScale)
    )
}

