package org.librecellar.ui.wines

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.CountryHelper
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import org.librecellar.data.model.WineStatistics
import org.librecellar.ui.statistics.InsightsStrip
import org.librecellar.ui.components.BarcodeScannerDialog
import org.librecellar.ui.components.GrapeBadge
import org.librecellar.ui.components.GrapeBadgesRow
import org.librecellar.ui.components.RatingBar
import org.librecellar.ui.components.SharedEntryBadge
import org.librecellar.ui.components.WineBottleImage
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyDark
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import kotlinx.coroutines.launch

/** Filters opened from the statistics screen; [includeEmpty] also shows wines without bottles left. */
data class WineListFilter(
    val country: String? = null,
    val vintage: Int? = null,
    val includeEmpty: Boolean = false
) {
    val isActive: Boolean get() = country != null || vintage != null
}

enum class ViewDisplayMode {
    TILE,
    TABLE_LIST
}

enum class WineSortField(val label: String) {
    STOCK("Stock"),
    RATING("Rating"),
    PRICE("Price"),
    VINTAGE("Vintage"),
    NAME("Name")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WineListScreen(
    wines: List<Wine>,
    locations: List<CellarLocation>,
    isPriceTierEnabled: Boolean = true,
    onWineSelected: (Wine) -> Unit,
    onAddWineClick: () -> Unit,
    onDecrementQuantity: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    hasMoreWines: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    hideReadOnlyShared: Boolean = false,
    hideWritableShared: Boolean = false,
    onHideReadOnlySharedChange: (Boolean) -> Unit = {},
    onHideWritableSharedChange: (Boolean) -> Unit = {},
    filter: WineListFilter = WineListFilter(),
    onFilterChange: (WineListFilter) -> Unit = {},
    onOpenStatistics: () -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(ViewDisplayMode.TILE) }
    val gridState = rememberLazyGridState()
    val tableListState = rememberLazyListState()

    // Continuous infinite scrolling / incremental pagination live from backend
    val shouldLoadMoreGrid by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMoreGrid, hasMoreWines, isLoadingMore) {
        if (shouldLoadMoreGrid && hasMoreWines && !isLoadingMore) {
            onLoadMore()
        }
    }

    val shouldLoadMoreTable by remember {
        derivedStateOf {
            val totalItems = tableListState.layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = tableListState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index >= totalItems - 4
        }
    }
    LaunchedEffect(shouldLoadMoreTable, hasMoreWines, isLoadingMore) {
        if (shouldLoadMoreTable && hasMoreWines && !isLoadingMore) {
            onLoadMore()
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // User requirement: if a wine goes down to 0 bottles, hide it by default,
    // and provide the possibility to show all wines even those that are out.
    var showOutOfStockWines by remember { mutableStateOf(false) }
    LaunchedEffect(filter) {
        if (filter.includeEmpty) showOutOfStockWines = true
    }

    // User requirement: sort wines by stock, rating, price (as well as vintage & name)
    var sortField by remember { mutableStateOf(WineSortField.VINTAGE) }
    var isSortAscending by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSharedFilterMenu by remember { mutableStateOf(false) }
    val hasSharedWines = wines.any { it.isReadOnly || it.isSharedWritable }
    val isSharedFilterActive = hideReadOnlyShared || hideWritableShared

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Filtering logic
    val filteredWines = wines.filter { wine ->
        val matchesSearch = wine.name.contains(searchQuery, ignoreCase = true) ||
                wine.vintage.toString().contains(searchQuery) ||
                (wine.producer?.contains(searchQuery, ignoreCase = true) == true) ||
                (wine.varietal?.contains(searchQuery, ignoreCase = true) == true) ||
                (wine.country?.contains(searchQuery, ignoreCase = true) == true) ||
                (wine.region?.contains(searchQuery, ignoreCase = true) == true) ||
                (wine.barcode?.contains(searchQuery) == true)

        val matchesStockVisibility = if (showOutOfStockWines) {
            true // Show all, including 0-bottle wines
        } else {
            wine.quantity > 0 // Hide 0-bottle wines
        }

        val matchesFilter = when (selectedFilter) {
            "IN_CELLAR" -> wine.isInCellar && wine.quantity > 0
            "UNASSIGNED" -> !wine.isInCellar && wine.quantity > 0
            "OUT_OF_STOCK" -> wine.quantity == 0
            "RED" -> wine.wineType == "RED"
            "WHITE" -> wine.wineType == "WHITE"
            "SPARKLING" -> wine.wineType == "SPARKLING"
            "RATING_5" -> wine.tastingRating == 5
            else -> true
        }

        // Entries of collections shared with the user can be hidden (persisted in settings)
        val matchesSharedVisibility = !(hideReadOnlyShared && wine.isReadOnly) &&
                !(hideWritableShared && wine.isSharedWritable)

        val matchesStatistics =
            (filter.country == null || WineStatistics.countryName(wine.country) == filter.country) &&
                (filter.vintage == null || wine.vintage == filter.vintage)

        matchesSearch && matchesStatistics && (matchesStockVisibility || selectedFilter == "OUT_OF_STOCK") && matchesFilter &&
                matchesSharedVisibility
    }

    // Sorting logic
    val sortedWines = filteredWines.sortedWith { a, b ->
        val cmp = when (sortField) {
            WineSortField.STOCK -> a.quantity.compareTo(b.quantity)
            WineSortField.RATING -> (a.tastingRating ?: 0).compareTo(b.tastingRating ?: 0)
            WineSortField.PRICE -> (a.purchasePrice ?: 0.0).compareTo(b.purchasePrice ?: 0.0)
            WineSortField.VINTAGE -> a.vintage.compareTo(b.vintage)
            WineSortField.NAME -> a.name.compareTo(b.name, ignoreCase = true)
        }
        if (isSortAscending) cmp else -cmp
    }

    val totalBottles = wines.sumOf { it.quantity }
    val outOfStockCount = wines.count { it.quantity == 0 }
    val inCellarCount = wines.filter { it.isInCellar && it.quantity > 0 }.sumOf { it.quantity }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddWineClick,
                containerColor = WineBurgundyLight,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_wine_fab")
            ) {
                Icon(imageVector = AppIcons.Add, contentDescription = "Add Wine")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Pull-down Sync & Status Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.CloudSync,
                            contentDescription = null,
                            tint = org.librecellar.ui.theme.CellarGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRefreshing) "Syncing from backend server..." else "Pull down to sync with backend (limit: 20/page)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("refresh_wines_btn")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = org.librecellar.ui.theme.CellarGold
                            )
                        } else {
                            Icon(
                                imageVector = AppIcons.Refresh,
                                contentDescription = "Sync",
                                tint = org.librecellar.ui.theme.CellarGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                    Text(
                        text = "Wine Collection",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    InsightsStrip(
                        wines = wines,
                        // Depleted wines are counted on their filter chip below
                        summary = "$totalBottles in stock • $inCellarCount placed",
                        onClick = onOpenStatistics,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Toggle Tile vs Table List View
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        IconButton(
                            onClick = { viewMode = ViewDisplayMode.TILE },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (viewMode == ViewDisplayMode.TILE) WineBurgundyLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("view_mode_tile_btn")
                        ) {
                            Icon(
                                imageVector = AppIcons.GridView,
                                contentDescription = "Tile View",
                                tint = if (viewMode == ViewDisplayMode.TILE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewMode = ViewDisplayMode.TABLE_LIST },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (viewMode == ViewDisplayMode.TABLE_LIST) WineBurgundyLight else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .testTag("view_mode_table_btn")
                        ) {
                            Icon(
                                imageVector = AppIcons.TableRows,
                                contentDescription = "Table View",
                                tint = if (viewMode == ViewDisplayMode.TABLE_LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar with Barcode Scanner Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, vintage, varietal, barcode...") },
                leadingIcon = {
                    Icon(imageVector = AppIcons.Search, contentDescription = "Search", tint = CellarGold)
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.testTag("scan_lookup_search_button")
                    ) {
                        Icon(
                            imageVector = AppIcons.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            tint = CellarGold
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("wine_search_input")
            )

            // Sorting & Out-of-Stock Controls Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sorting Dropdown Trigger
                Box {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSortMenu = true }
                            .testTag("sort_wines_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = AppIcons.Sort,
                                contentDescription = null,
                                tint = CellarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sort: ${sortField.label}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { isSortAscending = !isSortAscending },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSortAscending) AppIcons.ArrowUpward else AppIcons.ArrowDownward,
                                    contentDescription = "Toggle sort order",
                                    tint = CellarGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        WineSortField.entries.forEach { field ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(field.label, fontWeight = if (sortField == field) FontWeight.Bold else FontWeight.Normal)
                                        if (sortField == field) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(imageVector = AppIcons.Check, contentDescription = null, tint = CellarGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    sortField = field
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                // Shared collections filter: hide read-only and/or read-write shared entries
                if (hasSharedWines || isSharedFilterActive) {
                    Box {
                        Surface(
                            color = if (isSharedFilterActive) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showSharedFilterMenu = true }
                                .testTag("shared_filter_btn")
                        ) {
                            Icon(
                                imageVector = AppIcons.FilterList,
                                contentDescription = "Filter shared wines",
                                tint = if (isSharedFilterActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSharedFilterMenu,
                            onDismissRequest = { showSharedFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hide read-only shared") },
                                leadingIcon = {
                                    Checkbox(checked = hideReadOnlyShared, onCheckedChange = null)
                                },
                                onClick = { onHideReadOnlySharedChange(!hideReadOnlyShared) },
                                modifier = Modifier.testTag("hide_read_only_shared_item")
                            )
                            DropdownMenuItem(
                                text = { Text("Hide read-write shared") },
                                leadingIcon = {
                                    Checkbox(checked = hideWritableShared, onCheckedChange = null)
                                },
                                onClick = { onHideWritableSharedChange(!hideWritableShared) },
                                modifier = Modifier.testTag("hide_writable_shared_item")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Out of stock visibility toggle button
                Surface(
                    color = if (showOutOfStockWines) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showOutOfStockWines = !showOutOfStockWines }
                        .testTag("toggle_out_of_stock_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showOutOfStockWines) AppIcons.Visibility else AppIcons.VisibilityOff,
                            contentDescription = null,
                            tint = if (showOutOfStockWines) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showOutOfStockWines) "Showing Out of Stock" else "Hiding 0-Qty Wines",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showOutOfStockWines) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                }
            }

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All",
                    "IN_CELLAR" to "In Cellar",
                    "UNASSIGNED" to "Unassigned",
                    "RED" to "Reds",
                    "WHITE" to "Whites",
                    "SPARKLING" to "Sparkling",
                    "RATING_5" to "5★ Top Rated",
                    "OUT_OF_STOCK" to "Depleted ($outOfStockCount)"
                )

                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WineBurgundyLight,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Filters opened from the statistics screen
            if (filter.isActive) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    filter.country?.let { country ->
                        InputChip(
                            selected = true,
                            onClick = { onFilterChange(filter.copy(country = null)) },
                            label = { Text("${CountryHelper.getFlagForCountry(country)} $country", fontSize = 12.sp) },
                            trailingIcon = { Icon(AppIcons.Close, contentDescription = "Clear country filter", modifier = Modifier.size(14.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = WineBurgundyLight,
                                selectedLabelColor = Color.White,
                                selectedTrailingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("country_filter_chip")
                        )
                    }
                    filter.vintage?.let { vintage ->
                        InputChip(
                            selected = true,
                            onClick = { onFilterChange(filter.copy(vintage = null)) },
                            label = { Text("Vintage $vintage", fontSize = 12.sp) },
                            leadingIcon = { Icon(AppIcons.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            trailingIcon = { Icon(AppIcons.Close, contentDescription = "Clear vintage filter", modifier = Modifier.size(14.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = WineBurgundyLight,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                selectedTrailingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("vintage_filter_chip")
                        )
                    }
                }
            }

            // Wine Content: Tile View vs Table List
            if (sortedWines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = AppIcons.WineBar,
                            contentDescription = null,
                            tint = CellarGold,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching wines found" else "No wines visible",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (!showOutOfStockWines && outOfStockCount > 0)
                                "Tap 'Hiding 0-Qty Wines' above to reveal depleted bottles"
                            else "Add wines using the intake wizard or manual entry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddWineClick,
                            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                        ) {
                            Icon(imageVector = AppIcons.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Wine")
                        }
                    }
                }
            } else {
                when (viewMode) {
                    ViewDisplayMode.TILE -> {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 76.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(sortedWines) { wine ->
                                WineTileCard(
                                    wine = wine,
                                    allWines = wines,
                                    locations = locations,
                                    isPriceTierEnabled = isPriceTierEnabled,
                                    onClick = { onWineSelected(wine) },
                                    onTickOff = {
                                        onDecrementQuantity(wine.id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Consumed 1 bottle of ${wine.name}")
                                        }
                                    }
                                )
                            }

                            // Incremental Pagination Footer
                            if (isLoadingMore) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = org.librecellar.ui.theme.CellarGold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading next 20 wines from backend...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (!hasMoreWines && sortedWines.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "All ${wines.size} wines loaded",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    ViewDisplayMode.TABLE_LIST -> {
                        // Dense Table List View
                        LazyColumn(
                            state = tableListState,
                            contentPadding = PaddingValues(bottom = 76.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Table Header Row
                            item {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Wine & Vintage",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(2f)
                                        )
                                        Text(
                                            text = "Location",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1.3f)
                                        )
                                        Text(
                                            text = "Tier / Price",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                        Text(
                                            text = "Stock",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(65.dp)
                                        )
                                    }
                                }
                            }

                            items(sortedWines) { wine ->
                                WineTableRowCard(
                                    wine = wine,
                                    allWines = wines,
                                    locations = locations,
                                    isPriceTierEnabled = isPriceTierEnabled,
                                    onClick = { onWineSelected(wine) },
                                    onTickOff = {
                                        onDecrementQuantity(wine.id)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Ticked off bottle of ${wine.name}")
                                        }
                                    }
                                )
                            }

                            // Incremental Pagination Footer
                            if (isLoadingMore) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = org.librecellar.ui.theme.CellarGold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading next 20 wines from backend...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (!hasMoreWines && sortedWines.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "All ${wines.size} wines loaded",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                searchQuery = barcode
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
fun WineTileCard(
    wine: Wine,
    allWines: List<Wine>,
    locations: List<CellarLocation>,
    isPriceTierEnabled: Boolean,
    onClick: () -> Unit,
    onTickOff: () -> Unit
) {
    val location = locations.find { it.id == wine.locationId }
    val isOutOfStock = wine.quantity <= 0

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wine_tile_${wine.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Badge row: Vintage & Stock Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = WineBurgundyDark,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${wine.vintage}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CellarGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = if (!isOutOfStock) CellarGold.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (!isOutOfStock) "${wine.quantity} left" else "Out of stock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (!isOutOfStock) CellarGold else Color.Red,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottle Thumbnail & Type Indicator
            WineBottleImage(
                photoUrl = wine.photoUrl,
                wineType = wine.wineType,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                iconSize = 38.dp,
                contentDescription = "${wine.name} bottle photo"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wine Name (Mandatory)
            Text(
                text = wine.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Producer & Origin (Optional)
            val origin = listOfNotNull(
                wine.country?.let { "${CountryHelper.getFlagForCountry(it)} $it" },
                wine.region
            ).joinToString(" • ")
            val subtitle = listOfNotNull(
                wine.producer,
                origin.ifBlank { null }
            ).joinToString(" • ").ifBlank { "Wine entry" }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            SharedEntryBadge(entry = wine, modifier = Modifier.padding(top = 2.dp))

            // Grape / Varietal Badges
            if (wine.varietalList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                GrapeBadgesRow(
                    grapes = wine.varietalList,
                    maxBadges = 2,
                    modifier = Modifier.testTag("wine_grapes_${wine.id}")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Price Tier Marking & Stars Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars Rating
                if (wine.tastingRating != null && wine.tastingRating > 0) {
                    RatingBar(rating = wine.tastingRating, starSize = 13.dp)
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Price Range Tier Badge (Configurable in Settings)
                if (isPriceTierEnabled) {
                    val tier = wine.getResolvedPriceTier(allWines)
                    val tierColor = try {
                        Color(tier.colorHex.toColorInt())
                    } catch (e: Exception) {
                        CellarGold
                    }
                    Surface(
                        color = tierColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${tier.symbolFor(wine.currency)} ${tier.shortLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location / Shelf Position Chip
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (location != null) "${location.name} (${wine.slotLabel ?: "Cellar"})" else "Unassigned",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = if (location != null) CellarGold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Tick Off / Drink Button
            OutlinedButton(
                onClick = onTickOff,
                enabled = wine.quantity > 0 && !wine.isReadOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("tick_off_bottle_${wine.id}"),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = AppIcons.LocalDrink,
                    contentDescription = "Drink Bottle",
                    tint = if (wine.quantity > 0 && !wine.isReadOnly) CellarGold else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (wine.quantity > 0) "Tick Off (-1)" else "Depleted",
                    fontSize = 11.sp,
                    color = if (wine.quantity > 0) MaterialTheme.colorScheme.onSurface else Color.Gray
                )
            }
        }
    }
}

@Composable
fun WineTableRowCard(
    wine: Wine,
    allWines: List<Wine>,
    locations: List<CellarLocation>,
    isPriceTierEnabled: Boolean,
    onClick: () -> Unit,
    onTickOff: () -> Unit
) {
    val location = locations.find { it.id == wine.locationId }
    val isOutOfStock = wine.quantity <= 0

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wine_row_${wine.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WineBottleImage(
                photoUrl = wine.photoUrl,
                wineType = wine.wineType,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                shape = RoundedCornerShape(6.dp),
                iconSize = 22.dp,
                contentDescription = "${wine.name} thumbnail"
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Wine & Vintage
            Column(modifier = Modifier.weight(2f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wine.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    SharedEntryBadge(
                        entry = wine,
                        showOwner = false,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                val origin = listOfNotNull(
                    wine.country?.let { "${CountryHelper.getFlagForCountry(it)} $it" },
                    wine.region
                ).joinToString(", ")
                val subtitle = listOfNotNull(
                    "${wine.vintage}",
                    wine.producer,
                    origin.ifBlank { null }
                ).joinToString(" • ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Grape / Varietal Badges
                if (wine.varietalList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    GrapeBadgesRow(
                        grapes = wine.varietalList,
                        maxBadges = 2,
                        modifier = Modifier.testTag("table_wine_grapes_${wine.id}")
                    )
                }
            }

            // Location
            Column(modifier = Modifier.weight(1.3f)) {
                if (location != null) {
                    Text(
                        text = location.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = CellarGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    wine.slotLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Unassigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Tier / Price & Rating
            Column(modifier = Modifier.weight(1.2f)) {
                if (isPriceTierEnabled) {
                    val tier = wine.getResolvedPriceTier(allWines)
                    val tierColor = try {
                        Color(tier.colorHex.toColorInt())
                    } catch (e: Exception) {
                        CellarGold
                    }
                    Text(
                        text = "${tier.symbolFor(wine.currency)} ${tier.shortLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                }
                if (wine.tastingRating != null && wine.tastingRating > 0) {
                    Text(
                        text = "${wine.tastingRating}★",
                        style = MaterialTheme.typography.labelSmall,
                        color = CellarGold
                    )
                } else if (wine.purchasePrice != null) {
                    Text(
                        text = LocaleFormat.formatPrice(wine.purchasePrice, wine.currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quantity & Quick Tick Off Action
            Row(
                modifier = Modifier.width(65.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${wine.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (wine.quantity > 0) MaterialTheme.colorScheme.onSurface else Color.Red
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onTickOff,
                    enabled = wine.quantity > 0 && !wine.isReadOnly,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Remove,
                        contentDescription = "Drink 1 bottle",
                        tint = if (wine.quantity > 0 && !wine.isReadOnly) WineRose else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
