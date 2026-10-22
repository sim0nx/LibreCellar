package org.librecellar.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.librecellar.R
import org.librecellar.data.LocalNetwork
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.cellar.CellarDesignScreen
import org.librecellar.ui.inventory.QuickScanScreen
import org.librecellar.ui.settings.ServerSettingsScreen
import org.librecellar.ui.statistics.StatisticsScreen
import org.librecellar.ui.suppliers.SuppliersScreen
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyDark
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.wines.AddWineWizardScreen
import org.librecellar.ui.wines.EditWineScreen
import org.librecellar.ui.wines.WineDetailScreen
import org.librecellar.ui.wines.WineListFilter
import org.librecellar.ui.wines.WineListScreen
import kotlinx.coroutines.launch

enum class MainNavigationTab {
    CELLAR,
    WINES,
    QUICK_SCAN,
    SUPPLIERS,
    SETTINGS
}

sealed class ScreenState {
    object Main : ScreenState()
    object AddWine : ScreenState()
    data class WineDetail(val wineId: String) : ScreenState()
    data class EditWine(val wineId: String) : ScreenState()
    object Statistics : ScreenState()
}

@Composable
fun MainScreen(repository: WineRepository) {
    val wines by repository.wines.collectAsState()
    val locations by repository.locations.collectAsState()
    val suppliers by repository.suppliers.collectAsState()
    val isOnline by repository.isBackendOnline.collectAsState()
    val backendUrl by repository.backendUrl.collectAsState()
    val isPriceTierEnabled by repository.isPriceTierMarkingEnabled.collectAsState()
    val hideReadOnlyShared by repository.hideReadOnlyShared.collectAsState()
    val hideWritableShared by repository.hideWritableShared.collectAsState()

    val isWinesRefreshing by repository.isWinesRefreshing.collectAsState()
    val hasMoreWines by repository.hasMoreWines.collectAsState()
    val isLoadingMoreWines by repository.isLoadingMoreWines.collectAsState()

    val isSuppliersRefreshing by repository.isSuppliersRefreshing.collectAsState()
    val hasMoreSuppliers by repository.hasMoreSuppliers.collectAsState()
    val isLoadingMoreSuppliers by repository.isLoadingMoreSuppliers.collectAsState()

    val isLocationsRefreshing by repository.isLocationsRefreshing.collectAsState()
    val knownGrapes by repository.knownGrapes.collectAsState()

    var activeTab by remember { mutableStateOf(MainNavigationTab.WINES) }
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
    // Country / vintage filter of the wine list, set from the statistics screen
    var wineFilter by remember { mutableStateOf(WineListFilter()) }
    var isLoadingAllWines by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LocalNetworkAccess(repository)

    // Operations the server rejected or could not receive (the server manages all data)
    val operationSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(repository) {
        repository.operationErrors.collect { message -> operationSnackbar.showSnackbar(message) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    when (val screen = currentScreen) {
        is ScreenState.AddWine -> {
            BackHandler {
                currentScreen = ScreenState.Main
            }
            AddWineWizardScreen(
                // New wines join the collection of the rack they are placed in
                cellarLocations = locations.filter { !it.isReadOnly },
                suppliers = suppliers,
                existingWines = wines,
                knownGrapes = knownGrapes,
                onRegisterGrape = { grape -> repository.registerGrape(grape) },
                isPriceTierEnabled = isPriceTierEnabled,
                onWineCreated = { newWine ->
                    scope.launch {
                        // Stay in the wizard if the server rejected the wine
                        if (repository.addWine(newWine)) currentScreen = ScreenState.Main
                    }
                },
                onAddLocation = { newLoc ->
                    scope.launch { repository.addLocation(newLoc) }
                },
                onAddSupplier = { newSup ->
                    scope.launch { repository.addSupplier(newSup) }
                },
                onCancel = { currentScreen = ScreenState.Main }
            )
        }

        is ScreenState.EditWine -> {
            BackHandler {
                currentScreen = ScreenState.WineDetail(screen.wineId)
            }
            val wine = wines.find { it.id == screen.wineId }
            if (wine != null && !wine.isReadOnly) {
                EditWineScreen(
                    wine = wine,
                    locations = locations.filter { it.acceptsWine(wine) || it.id == wine.locationId },
                    suppliers = suppliers,
                    allWines = wines,
                    knownGrapes = knownGrapes,
                    onRegisterGrape = { grape -> repository.registerGrape(grape) },
                    isPriceTierEnabled = isPriceTierEnabled,
                    onSaveWine = { updatedWine ->
                        scope.launch {
                            if (repository.updateWine(updatedWine)) {
                                currentScreen = ScreenState.WineDetail(updatedWine.id)
                            }
                        }
                    },
                    onCancel = {
                        currentScreen = ScreenState.WineDetail(screen.wineId)
                    }
                )
            } else {
                currentScreen = ScreenState.Main
            }
        }

        is ScreenState.WineDetail -> {
            BackHandler {
                currentScreen = ScreenState.Main
            }
            val wine = wines.find { it.id == screen.wineId }
            if (wine != null) {
                WineDetailScreen(
                    wine = wine,
                    locations = locations,
                    allWines = wines,
                    isPriceTierEnabled = isPriceTierEnabled,
                    onBack = { currentScreen = ScreenState.Main },
                    onEdit = {
                        currentScreen = ScreenState.EditWine(wine.id)
                    },
                    onDecrement = {
                        scope.launch { repository.decrementQuantity(wine.id) }
                    },
                    onIncrement = {
                        scope.launch { repository.incrementQuantity(wine.id) }
                    },
                    onDelete = {
                        scope.launch {
                            if (repository.deleteWine(wine.id)) currentScreen = ScreenState.Main
                        }
                    }
                )
            } else {
                currentScreen = ScreenState.Main
            }
        }

        ScreenState.Statistics -> {
            BackHandler { currentScreen = ScreenState.Main }
            // Statistics cover every wine, not only the pages scrolled through
            LaunchedEffect(Unit) {
                isLoadingAllWines = true
                repository.loadAllWines()
                isLoadingAllWines = false
            }
            StatisticsScreen(
                wines = wines,
                isLoading = isLoadingAllWines,
                onBack = { currentScreen = ScreenState.Main },
                onCountrySelected = { country, includeEmpty ->
                    wineFilter = WineListFilter(country = country, includeEmpty = includeEmpty)
                    activeTab = MainNavigationTab.WINES
                    currentScreen = ScreenState.Main
                },
                onVintageSelected = { vintage, includeEmpty ->
                    wineFilter = WineListFilter(vintage = vintage, includeEmpty = includeEmpty)
                    activeTab = MainNavigationTab.WINES
                    currentScreen = ScreenState.Main
                }
            )
        }

        ScreenState.Main -> {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val topBarPadding = maxOf(statusBarTop, 28.dp)

            Scaffold(
                topBar = {
                    Surface(
                        color = WineBurgundyDark,
                        tonalElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(topBarPadding))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(WineBurgundy),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = AppIcons.WineBar,
                                        contentDescription = null,
                                        tint = CellarGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "LibreCellar",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Wine Cellar & Inventory",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CellarGold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Server Status Badge & Settings Trigger
                            Surface(
                                color = if (isOnline) Color(0xFF1B3B22) else Color(0xFF382312),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { activeTab = MainNavigationTab.SETTINGS }
                                    .testTag("server_status_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFFFA000))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isOnline) "Server Online" else "Server (Local)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOnline) Color(0xFF81C784) else CellarGold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = AppIcons.Settings,
                                        contentDescription = "Server Settings",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = WineBurgundyDark,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("main_bottom_nav")
                    ) {
                        NavigationBarItem(
                            selected = activeTab == MainNavigationTab.WINES,
                            onClick = { activeTab = MainNavigationTab.WINES },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainNavigationTab.WINES) AppIcons.Dashboard else AppIcons.DashboardOutline,
                                    contentDescription = "Wines"
                                )
                            },
                            label = { Text("Wines", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CellarGold,
                                selectedTextColor = CellarGold,
                                indicatorColor = WineBurgundy
                            ),
                            modifier = Modifier.testTag("nav_wines_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainNavigationTab.CELLAR,
                            onClick = { activeTab = MainNavigationTab.CELLAR },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainNavigationTab.CELLAR) AppIcons.GridOn else AppIcons.GridOnOutline,
                                    contentDescription = "Cellar Slots"
                                )
                            },
                            label = { Text("Cellar Grid", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CellarGold,
                                selectedTextColor = CellarGold,
                                indicatorColor = WineBurgundy
                            ),
                            modifier = Modifier.testTag("nav_cellar_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainNavigationTab.QUICK_SCAN,
                            onClick = { activeTab = MainNavigationTab.QUICK_SCAN },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainNavigationTab.QUICK_SCAN) AppIcons.QrCodeScanner else AppIcons.QrCodeScannerOutline,
                                    contentDescription = "Quick Scan"
                                )
                            },
                            label = { Text("Scan / Qty", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CellarGold,
                                selectedTextColor = CellarGold,
                                indicatorColor = WineBurgundy
                            ),
                            modifier = Modifier.testTag("nav_quick_scan_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainNavigationTab.SUPPLIERS,
                            onClick = { activeTab = MainNavigationTab.SUPPLIERS },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainNavigationTab.SUPPLIERS) AppIcons.Storefront else AppIcons.StorefrontOutline,
                                    contentDescription = "Suppliers"
                                )
                            },
                            label = { Text("Merchants", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CellarGold,
                                selectedTextColor = CellarGold,
                                indicatorColor = WineBurgundy
                            ),
                            modifier = Modifier.testTag("nav_suppliers_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == MainNavigationTab.SETTINGS,
                            onClick = { activeTab = MainNavigationTab.SETTINGS },
                            icon = {
                                Icon(
                                    imageVector = if (activeTab == MainNavigationTab.SETTINGS) AppIcons.Settings else AppIcons.SettingsOutline,
                                    contentDescription = "Server Settings"
                                )
                            },
                            label = { Text("Settings", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CellarGold,
                                selectedTextColor = CellarGold,
                                indicatorColor = WineBurgundy
                            ),
                            modifier = Modifier.testTag("nav_settings_tab")
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (activeTab) {
                        MainNavigationTab.WINES -> {
                            WineListScreen(
                                wines = wines,
                                locations = locations,
                                isPriceTierEnabled = isPriceTierEnabled,
                                onWineSelected = { selected ->
                                    currentScreen = ScreenState.WineDetail(selected.id)
                                },
                                onAddWineClick = { currentScreen = ScreenState.AddWine },
                                onDecrementQuantity = { wineId ->
                                    scope.launch { repository.decrementQuantity(wineId) }
                                },
                                isRefreshing = isWinesRefreshing,
                                onRefresh = { scope.launch { repository.refreshWines() } },
                                hasMoreWines = hasMoreWines,
                                isLoadingMore = isLoadingMoreWines,
                                onLoadMore = { scope.launch { repository.loadNextWinePage() } },
                                hideReadOnlyShared = hideReadOnlyShared,
                                hideWritableShared = hideWritableShared,
                                onHideReadOnlySharedChange = { hide ->
                                    scope.launch { repository.updateHideReadOnlyShared(hide) }
                                },
                                onHideWritableSharedChange = { hide ->
                                    scope.launch { repository.updateHideWritableShared(hide) }
                                },
                                filter = wineFilter,
                                onFilterChange = { wineFilter = it },
                                onOpenStatistics = { currentScreen = ScreenState.Statistics }
                            )
                        }

                        MainNavigationTab.CELLAR -> {
                            CellarDesignScreen(
                                locations = locations,
                                wines = wines,
                                onAddLocation = { newLoc ->
                                    scope.launch { repository.addLocation(newLoc) }
                                },
                                onUpdateLocation = { updatedLoc ->
                                    scope.launch { repository.updateLocation(updatedLoc) }
                                },
                                onDeleteLocation = { locId ->
                                    scope.launch { repository.deleteLocation(locId) }
                                },
                                onAssignSlot = { wineId, locId, row, col ->
                                    scope.launch { repository.assignSlot(wineId, locId, row, col) }
                                },
                                onWineSelected = { selected ->
                                    currentScreen = ScreenState.WineDetail(selected.id)
                                },
                                isRefreshing = isLocationsRefreshing,
                                onRefresh = { scope.launch { repository.refreshLocations() } }
                            )
                        }

                        MainNavigationTab.QUICK_SCAN -> {
                            QuickScanScreen(
                                wines = wines,
                                locations = locations,
                                onDecrementQuantity = { wineId ->
                                    scope.launch { repository.decrementQuantity(wineId) }
                                },
                                onWineSelected = { selected ->
                                    currentScreen = ScreenState.WineDetail(selected.id)
                                }
                            )
                        }

                        MainNavigationTab.SUPPLIERS -> {
                            SuppliersScreen(
                                suppliers = suppliers,
                                wines = wines,
                                onAddSupplier = { newSup ->
                                    scope.launch { repository.addSupplier(newSup) }
                                },
                                onUpdateSupplier = { updatedSup ->
                                    scope.launch { repository.updateSupplier(updatedSup) }
                                },
                                onDeleteSupplier = { supId ->
                                    scope.launch { repository.deleteSupplier(supId) }
                                },
                                isRefreshing = isSuppliersRefreshing,
                                onRefresh = { scope.launch { repository.refreshSuppliers() } },
                                hasMoreSuppliers = hasMoreSuppliers,
                                isLoadingMore = isLoadingMoreSuppliers,
                                onLoadMore = { scope.launch { repository.loadNextSupplierPage() } }
                            )
                        }

                        MainNavigationTab.SETTINGS -> {
                            ServerSettingsScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = operationSnackbar,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 72.dp)
    )
    }
}

/**
 * Android 17 needs the "Nearby devices" permission to reach a server on the local network (such as
 * a home server). Asks for it whenever the configured server is local, and reconnects once granted.
 */
@Composable
private fun LocalNetworkAccess(repository: WineRepository) {
    if (!LocalNetwork.permissionRequired()) return
    val context = LocalContext.current
    val backendUrl by repository.backendUrl.collectAsState()
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch { repository.checkConnectionAndSync() }
        } else {
            repository.reportProblem(WineRepository.LOCAL_NETWORK_HINT)
        }
    }
    LaunchedEffect(backendUrl) {
        if (withContext(Dispatchers.IO) { LocalNetwork.isBlocked(context, backendUrl) }) {
            launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        }
    }
}
