package org.librecellar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.R
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.cellar.CellarDesignScreen
import org.librecellar.ui.inventory.QuickScanScreen
import org.librecellar.ui.settings.ServerSettingsScreen
import org.librecellar.ui.suppliers.SuppliersScreen
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyDark
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.wines.AddWineWizardScreen
import org.librecellar.ui.wines.EditWineScreen
import org.librecellar.ui.wines.WineDetailScreen
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
}

@Composable
fun MainScreen(repository: WineRepository) {
    val wines by repository.wines.collectAsState()
    val locations by repository.locations.collectAsState()
    val suppliers by repository.suppliers.collectAsState()
    val isOnline by repository.isBackendOnline.collectAsState()
    val backendUrl by repository.backendUrl.collectAsState()
    val isPriceTierEnabled by repository.isPriceTierMarkingEnabled.collectAsState()

    var activeTab by remember { mutableStateOf(MainNavigationTab.WINES) }
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.Main) }

    val scope = rememberCoroutineScope()

    when (val screen = currentScreen) {
        is ScreenState.AddWine -> {
            BackHandler {
                currentScreen = ScreenState.Main
            }
            AddWineWizardScreen(
                cellarLocations = locations,
                suppliers = suppliers,
                existingWines = wines,
                isPriceTierEnabled = isPriceTierEnabled,
                onWineCreated = { newWine ->
                    scope.launch {
                        repository.addWine(newWine)
                        currentScreen = ScreenState.Main
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
            if (wine != null) {
                EditWineScreen(
                    wine = wine,
                    locations = locations,
                    suppliers = suppliers,
                    allWines = wines,
                    isPriceTierEnabled = isPriceTierEnabled,
                    onSaveWine = { updatedWine ->
                        scope.launch {
                            repository.updateWine(updatedWine)
                            currentScreen = ScreenState.WineDetail(updatedWine.id)
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
                        scope.launch {
                            repository.updateWine(wine.copy(quantity = wine.quantity + 1))
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repository.deleteWine(wine.id)
                            currentScreen = ScreenState.Main
                        }
                    }
                )
            } else {
                currentScreen = ScreenState.Main
            }
        }

        ScreenState.Main -> {
            Scaffold(
                topBar = {
                    Surface(
                        color = WineBurgundyDark,
                        tonalElevation = 4.dp
                    ) {
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
                                        imageVector = Icons.Default.WineBar,
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
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Server Settings",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
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
                                    imageVector = if (activeTab == MainNavigationTab.WINES) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
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
                                    imageVector = if (activeTab == MainNavigationTab.CELLAR) Icons.Filled.GridOn else Icons.Outlined.GridOn,
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
                                    imageVector = if (activeTab == MainNavigationTab.QUICK_SCAN) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
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
                                    imageVector = if (activeTab == MainNavigationTab.SUPPLIERS) Icons.Filled.Storefront else Icons.Outlined.Storefront,
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
                                    imageVector = if (activeTab == MainNavigationTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
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
                                }
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
                                }
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
                                }
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
}
