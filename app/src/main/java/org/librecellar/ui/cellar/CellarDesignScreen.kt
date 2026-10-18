package org.librecellar.ui.cellar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.content.res.Configuration
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Wine
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.CellarOakWood
import org.librecellar.ui.theme.CellarStoneSurfaceElevated
import org.librecellar.ui.theme.StatusEmpty
import org.librecellar.ui.theme.StatusEmptyBorder
import org.librecellar.ui.theme.StatusOccupied
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyDark
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellarDesignScreen(
    locations: List<CellarLocation>,
    wines: List<Wine>,
    onAddLocation: (CellarLocation) -> Unit,
    onUpdateLocation: (CellarLocation) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onAssignSlot: (wineId: String, locationId: String?, row: Int?, col: Int?) -> Unit,
    onWineSelected: (Wine) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    var selectedLocationId by remember(locations) {
        mutableStateOf(locations.firstOrNull()?.id ?: "")
    }

    val currentLocation = locations.find { it.id == selectedLocationId } ?: locations.firstOrNull()

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var locationToEdit by remember { mutableStateOf<CellarLocation?>(null) }
    var selectedWineToMove by remember { mutableStateOf<Wine?>(null) }
    var showSlotWinePicker by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showSlotInfoDialog by remember { mutableStateOf<Wine?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val unassignedWines = wines.filter { it.locationId == null && it.quantity > 0 }
    val winesInCurrentLocation = wines.filter { it.locationId == currentLocation?.id }

    val configuration = LocalConfiguration.current
    val isHorizontal = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp > configuration.screenHeightDp

    var isGridExpanded by remember { mutableStateOf(false) }
    var slotZoomSize by remember(isHorizontal) {
        mutableStateOf(if (isHorizontal) 96.dp else 78.dp)
    }

    val onSlotClickAction: (Int, Int, Wine?) -> Unit = { rowIndex, colIndex, wineInSlot ->
        if (selectedWineToMove != null) {
            val wineId = selectedWineToMove!!.id
            onAssignSlot(wineId, currentLocation?.id, rowIndex, colIndex)
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Placed ${selectedWineToMove!!.name} at Row ${rowIndex + 1}, Slot ${colIndex + 1}"
                )
            }
            selectedWineToMove = null
        } else if (wineInSlot != null) {
            showSlotInfoDialog = wineInSlot
        } else {
            showSlotWinePicker = Pair(rowIndex, colIndex)
        }
    }

    val onSlotLongClickAction: (Wine) -> Unit = { wineInSlot ->
        selectedWineToMove = wineInSlot
        scope.launch {
            snackbarHostState.showSnackbar("Moving ${wineInSlot.name}. Tap target slot to place.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isHorizontal || !isGridExpanded) {
                FloatingActionButton(
                    onClick = { showAddLocationDialog = true },
                    containerColor = WineBurgundyLight,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_location_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Cellar Location")
                }
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
            if (currentLocation == null) {
                // Empty state if no locations exist
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Grain,
                            contentDescription = null,
                            tint = CellarGold,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Cellar Locations Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Define your wine racks, shelves, and coolers to start organizing bottles into slots.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddLocationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                        ) {
                            Text("Create First Cellar Location")
                        }
                    }
                }
            } else if (isHorizontal) {
                // Horizontal / Landscape View: Cellar Grid is extended and takes full available space!
                if (isGridExpanded) {
                    // Fullscreen Extended Grid Mode
                    ExtendedCellarGrid(
                        currentLocation = currentLocation,
                        winesInCurrentLocation = winesInCurrentLocation,
                        selectedWineToMove = selectedWineToMove,
                        onCancelMove = { selectedWineToMove = null },
                        onSlotClick = onSlotClickAction,
                        onSlotLongClick = onSlotLongClickAction,
                        cellSize = slotZoomSize,
                        onZoomIn = { slotZoomSize = (slotZoomSize + 12.dp).coerceAtMost(130.dp) },
                        onZoomOut = { slotZoomSize = (slotZoomSize - 12.dp).coerceAtLeast(64.dp) },
                        isExpanded = true,
                        onToggleExpand = { isGridExpanded = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    // Side-by-Side Horizontal View: Left pane for rack info & unassigned bottles, Right pane for giant Extended Cellar Grid!
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Left Control & Unassigned Panel
                        Column(
                            modifier = Modifier
                                .width(270.dp)
                                .fillMaxHeight()
                                .padding(end = 4.dp)
                        ) {
                            // Sync & Title Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = CellarGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRefreshing) "Syncing..." else "Cellar Architect",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = onRefresh,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = CellarGold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sync",
                                            tint = CellarGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Rack selector chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                items(locations) { loc ->
                                    val isSelected = loc.id == currentLocation.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedLocationId = loc.id },
                                        label = {
                                            Text(
                                                text = loc.name,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        try {
                                                            Color(android.graphics.Color.parseColor(loc.colorHex ?: "#6B1D2F"))
                                                        } catch (e: Exception) {
                                                            CellarGold
                                                        }
                                                    )
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WineBurgundyLight,
                                            selectedLabelColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("location_chip_${loc.id}")
                                    )
                                }
                                item {
                                    TextButton(
                                        onClick = { showAddLocationDialog = true },
                                        modifier = Modifier.testTag("create_new_location_chip")
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CellarGold, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("New", color = CellarGold, fontSize = 11.sp)
                                    }
                                }
                            }

                            // Location Overview Card (Compact)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${currentLocation.rows}R × ${currentLocation.columns}C (${currentLocation.totalSlots} slots)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row {
                                            IconButton(
                                                onClick = { locationToEdit = currentLocation },
                                                modifier = Modifier.size(24.dp).testTag("edit_current_location_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Rack",
                                                    tint = CellarGold,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteLocation(currentLocation.id) },
                                                modifier = Modifier.size(24.dp).testTag("delete_current_location_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Rack",
                                                    tint = WineRose.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        currentLocation.temperatureCelsius?.let { temp ->
                                            Text(
                                                text = "$temp°C",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = CellarGold
                                            )
                                        }
                                        currentLocation.humidityPercent?.let { hum ->
                                            Text(
                                                text = "$hum% RH",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64B5F6)
                                            )
                                        }
                                    }
                                }
                            }

                            // Unassigned Bottles Section (Scrollable vertically in left panel)
                            if (unassignedWines.isNotEmpty()) {
                                Text(
                                    text = "Unassigned Bottles (${unassignedWines.size}):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CellarGold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(unassignedWines) { wine ->
                                        val isMoving = selectedWineToMove?.id == wine.id
                                        Card(
                                            onClick = {
                                                selectedWineToMove = if (isMoving) null else wine
                                                if (!isMoving) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            "Tap any empty slot in ${currentLocation.name} to place ${wine.name}"
                                                        )
                                                    }
                                                }
                                            },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isMoving) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("unassigned_wine_${wine.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "${wine.vintage} ${wine.name}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = wine.producer ?: "Estate bottle",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "×${wine.quantity}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CellarGold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "All bottles assigned to slots!",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }

                        // Right Pane: Extended Cellar Grid with Zoom and Fullscreen
                        ExtendedCellarGrid(
                            currentLocation = currentLocation,
                            winesInCurrentLocation = winesInCurrentLocation,
                            selectedWineToMove = selectedWineToMove,
                            onCancelMove = { selectedWineToMove = null },
                            onSlotClick = onSlotClickAction,
                            onSlotLongClick = onSlotLongClickAction,
                            cellSize = slotZoomSize,
                            onZoomIn = { slotZoomSize = (slotZoomSize + 12.dp).coerceAtMost(130.dp) },
                            onZoomOut = { slotZoomSize = (slotZoomSize - 12.dp).coerceAtLeast(64.dp) },
                            isExpanded = false,
                            onToggleExpand = { isGridExpanded = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(start = 4.dp)
                        )
                    }
                }
            } else {
                // Portrait Layout: Stacked with Extended Cellar Grid
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Pull-down Sync & Status Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = CellarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRefreshing) "Syncing racks from backend..." else "Pull down to sync racks with backend",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("refresh_cellar_btn")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CellarGold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync",
                                    tint = CellarGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Header Title
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cellar Visual Architect",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Interactive shelf & slot layout manager",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { locationToEdit = currentLocation },
                                modifier = Modifier.testTag("edit_current_location_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Rack Details",
                                    tint = CellarGold
                                )
                            }
                            IconButton(
                                onClick = { onDeleteLocation(currentLocation.id) },
                                modifier = Modifier.testTag("delete_current_location_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Location",
                                    tint = WineRose.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Location Selection Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        items(locations) { loc ->
                            val isSelected = loc.id == currentLocation.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLocationId = loc.id },
                                label = {
                                    Text(
                                        text = loc.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                try {
                                                    Color(android.graphics.Color.parseColor(loc.colorHex ?: "#6B1D2F"))
                                                } catch (e: Exception) {
                                                    CellarGold
                                                }
                                            )
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WineBurgundyLight,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("location_chip_${loc.id}")
                            )
                        }

                        item {
                            TextButton(
                                onClick = { showAddLocationDialog = true },
                                modifier = Modifier.testTag("create_new_location_chip")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CellarGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Rack", color = CellarGold)
                            }
                        }
                    }

                    // Location Details & Environmental Bar
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentLocation.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentLocation.rows} Rows × ${currentLocation.columns} Slots (${currentLocation.totalSlots} total)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                currentLocation.temperatureCelsius?.let { temp ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = "Temperature",
                                            tint = CellarGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "$temp°C",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                currentLocation.humidityPercent?.let { hum ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WaterDrop,
                                            contentDescription = "Humidity",
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "$hum%",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Extended Cellar Grid taking maximum available height in portrait
                    ExtendedCellarGrid(
                        currentLocation = currentLocation,
                        winesInCurrentLocation = winesInCurrentLocation,
                        selectedWineToMove = selectedWineToMove,
                        onCancelMove = { selectedWineToMove = null },
                        onSlotClick = onSlotClickAction,
                        onSlotLongClick = onSlotLongClickAction,
                        cellSize = slotZoomSize,
                        onZoomIn = { slotZoomSize = (slotZoomSize + 12.dp).coerceAtMost(130.dp) },
                        onZoomOut = { slotZoomSize = (slotZoomSize - 12.dp).coerceAtLeast(64.dp) },
                        isExpanded = isGridExpanded,
                        onToggleExpand = { isGridExpanded = !isGridExpanded },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bottom Tray: Unassigned bottles ready to place
                    if (unassignedWines.isNotEmpty()) {
                        Text(
                            text = "Unassigned Bottles (${unassignedWines.size}) — Tap to place into slot:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            items(unassignedWines) { wine ->
                                val isMoving = selectedWineToMove?.id == wine.id
                                Card(
                                    onClick = {
                                        selectedWineToMove = if (isMoving) null else wine
                                        if (!isMoving) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    "Tap any empty slot in ${currentLocation.name} to place ${wine.name}"
                                                )
                                            }
                                        }
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMoving) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .width(160.dp)
                                        .testTag("unassigned_wine_${wine.id}")
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${wine.vintage}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = CellarGold
                                            )
                                            Text(
                                                text = "Qty: ${wine.quantity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = wine.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = wine.producer ?: "Estate bottle",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 0. Edit Location Dialog
    locationToEdit?.let { loc ->
        RackFormDialog(
            initialLocation = loc,
            onDismiss = { locationToEdit = null },
            onConfirm = { updatedLoc ->
                onUpdateLocation(updatedLoc)
                locationToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Rack '${updatedLoc.name}' updated successfully")
                }
            }
        )
    }

    // 1. Add Location Dialog
    if (showAddLocationDialog) {
        RackFormDialog(
            initialLocation = null,
            onDismiss = { showAddLocationDialog = false },
            onConfirm = { newLoc ->
                onAddLocation(newLoc)
                selectedLocationId = newLoc.id
                showAddLocationDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Created rack '${newLoc.name}'")
                }
            }
        )
    }

    // 2. Slot Information Dialog (when tapping occupied slot)
    showSlotInfoDialog?.let { wine ->
        AlertDialog(
            onDismissRequest = { showSlotInfoDialog = null },
            icon = {
                Icon(imageVector = Icons.Default.WineBar, contentDescription = null, tint = CellarGold)
            },
            title = {
                Text(text = "${wine.name} (${wine.vintage})", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(text = "Producer: ${wine.producer ?: "Not specified"}")
                    Text(text = "Varietal: ${wine.varietal ?: "N/A"}")
                    Text(text = "Position: ${wine.slotLabel ?: "Cellar slot"}")
                    Text(text = "Rating: ${wine.tastingRating?.let { "$it ★" } ?: "Not rated"}")
                    Text(text = "In Stock: ${wine.quantity} bottles")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedWineToMove = wine
                        showSlotInfoDialog = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Tap an empty slot to relocate ${wine.name}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                ) {
                    Text("Move Bottle Slot")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            // Unassign from slot
                            onAssignSlot(wine.id, null, null, null)
                            showSlotInfoDialog = null
                        }
                    ) {
                        Text("Remove from Slot", color = WineRose)
                    }
                    TextButton(onClick = {
                        onWineSelected(wine)
                        showSlotInfoDialog = null
                    }) {
                        Text("View Details")
                    }
                }
            }
        )
    }

    // 3. Pick Wine for Empty Slot Dialog
    showSlotWinePicker?.let { (row, col) ->
        val candidateWines = wines.filter { it.quantity > 0 }
        AlertDialog(
            onDismissRequest = { showSlotWinePicker = null },
            title = {
                Text(text = "Assign Wine to Slot (R${row + 1}, C${col + 1})")
            },
            text = {
                if (candidateWines.isEmpty()) {
                    Text("No wines available in your collection. Add a wine first.")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        candidateWines.forEach { candidate ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onAssignSlot(candidate.id, currentLocation?.id, row, col)
                                        showSlotWinePicker = null
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${candidate.name} (${candidate.vintage})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = candidate.formattedLocationSummary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${candidate.quantity} in stock",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CellarGold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSlotWinePicker = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExtendedCellarGrid(
    currentLocation: CellarLocation,
    winesInCurrentLocation: List<Wine>,
    selectedWineToMove: Wine?,
    onCancelMove: () -> Unit,
    onSlotClick: (rowIndex: Int, colIndex: Int, wineInSlot: Wine?) -> Unit,
    onSlotLongClick: (Wine) -> Unit,
    cellSize: Dp,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CellarStoneSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            // Grid Header Controls (Rack title, dimensions, zoom, expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(currentLocation.colorHex ?: "#6B1D2F"))
                                } catch (e: Exception) {
                                    CellarGold
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${currentLocation.name} (${currentLocation.rows}R × ${currentLocation.columns}C)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${winesInCurrentLocation.size}/${currentLocation.totalSlots} filled",
                        style = MaterialTheme.typography.labelSmall,
                        color = CellarGold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Zoom Out
                    IconButton(
                        onClick = onZoomOut,
                        enabled = cellSize > 64.dp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomOut,
                            contentDescription = "Zoom Out Grid",
                            tint = if (cellSize > 64.dp) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "${cellSize.value.toInt()}dp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )

                    // Zoom In
                    IconButton(
                        onClick = onZoomIn,
                        enabled = cellSize < 130.dp,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom In Grid",
                            tint = if (cellSize < 130.dp) Color.White else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Expand / Fullscreen
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isExpanded) "Exit Fullscreen" else "Extend Grid",
                            tint = CellarGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Moving Wine Banner (if currently moving)
            if (selectedWineToMove != null) {
                Surface(
                    color = WineBurgundyDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WineBar,
                                contentDescription = null,
                                tint = CellarGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Placing: ${selectedWineToMove.name} (${selectedWineToMove.vintage}) — Tap any slot to assign",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = onCancelMove,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Move",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // The scrollable 2D Cellar Grid
            val horizScroll = rememberScrollState()
            val vertScroll = rememberScrollState()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(vertScroll)
                    .horizontalScroll(horizScroll)
            ) {
                Column {
                    // Column labels header (C1, C2, C3, ...)
                    Row(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Empty corner for row labels
                        Box(modifier = Modifier.size(width = 34.dp, height = 20.dp))

                        for (colIndex in 0 until currentLocation.columns) {
                            Box(
                                modifier = Modifier
                                    .width(cellSize)
                                    .height(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "C${colIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CellarGold.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }

                    // Grid rows
                    for (rowIndex in 0 until currentLocation.rows) {
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Row label (R1, R2, ...)
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = cellSize)
                                    .padding(end = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R${rowIndex + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CellarGold
                                )
                            }

                            for (colIndex in 0 until currentLocation.columns) {
                                val wineInSlot = winesInCurrentLocation.find {
                                    it.slotRow == rowIndex && it.slotColumn == colIndex
                                }

                                CellarSlotCell(
                                    row = rowIndex,
                                    col = colIndex,
                                    wine = wineInSlot,
                                    isSelectedToMove = selectedWineToMove?.id == wineInSlot?.id,
                                    onSlotClick = { onSlotClick(rowIndex, colIndex, wineInSlot) },
                                    onSlotLongClick = {
                                        if (wineInSlot != null) onSlotLongClick(wineInSlot)
                                    },
                                    cellSize = cellSize
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CellarSlotCell(
    row: Int,
    col: Int,
    wine: Wine?,
    isSelectedToMove: Boolean,
    onSlotClick: () -> Unit,
    onSlotLongClick: () -> Unit,
    cellSize: Dp = 80.dp
) {
    val isOccupied = wine != null

    Box(
        modifier = Modifier
            .size(cellSize)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isSelectedToMove -> WineBurgundy
                    isOccupied -> Color(0xFF33161F)
                    else -> Color(0xFF1E1719)
                }
            )
            .border(
                width = if (isSelectedToMove) 2.dp else 1.dp,
                color = when {
                    isSelectedToMove -> CellarGold
                    isOccupied -> WineBurgundyLight.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                },
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSlotClick() }
            .padding(4.dp)
            .testTag("slot_r${row}_c${col}"),
        contentAlignment = Alignment.Center
    ) {
        if (isOccupied) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Bottle Icon with badge
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.WineBar,
                        contentDescription = "Occupied Slot",
                        tint = if (isSelectedToMove) CellarGold else WineRose,
                        modifier = Modifier.size(if (cellSize >= 90.dp) 26.dp else 20.dp)
                    )
                }
                Text(
                    text = "${wine!!.vintage}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (cellSize >= 90.dp) 11.sp else 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CellarGold
                )
                Text(
                    text = wine.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = if (cellSize >= 90.dp) 10.sp else 8.5.sp,
                    maxLines = if (cellSize >= 90.dp) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
                if (cellSize >= 96.dp && wine.quantity > 1) {
                    Text(
                        text = "×${wine.quantity}",
                        fontSize = 8.sp,
                        color = CellarGold.copy(alpha = 0.85f)
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = if (cellSize >= 90.dp) 20.sp else 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                if (cellSize >= 85.dp) {
                    Text(
                        text = "${row + 1}-${col + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
fun RackFormDialog(
    initialLocation: CellarLocation? = null,
    onDismiss: () -> Unit,
    onConfirm: (CellarLocation) -> Unit
) {
    val isEditMode = initialLocation != null
    var name by remember(initialLocation) { mutableStateOf(initialLocation?.name ?: "") }
    var rows by remember(initialLocation) { mutableIntStateOf(initialLocation?.rows ?: 4) }
    var columns by remember(initialLocation) { mutableIntStateOf(initialLocation?.columns ?: 6) }
    var description by remember(initialLocation) { mutableStateOf(initialLocation?.description ?: "") }
    var tempText by remember(initialLocation) {
        mutableStateOf(initialLocation?.temperatureCelsius?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var humText by remember(initialLocation) {
        mutableStateOf(initialLocation?.humidityPercent?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var selectedColor by remember(initialLocation) { mutableStateOf(initialLocation?.colorHex ?: "#6B1D2F") }

    val colorOptions = listOf(
        "#6B1D2F" to "Burgundy",
        "#2B4C5F" to "Cooler Blue",
        "#5D4037" to "Oak Brown",
        "#2E4A32" to "Cellar Green",
        "#8D6E63" to "Warm Walnut"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Cellar Rack" else "Define Cellar Location",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Location Name / Label *") },
                    placeholder = { Text("e.g. Grand Cru Rack B, Wine Fridge") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("location_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Dimensions (Rows and Columns)
                Text(
                    text = "Shelf Grid Dimensions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Rows: $rows", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (rows > 1) rows-- }) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text(text = "$rows", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (rows < 12) rows++ }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Columns: $columns", style = MaterialTheme.typography.labelSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (columns > 1) columns-- }) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text(text = "$columns", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { if (columns < 16) columns++ }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                Text(
                    text = "Total Slots: ${rows * columns}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CellarGold
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Shelving notes, zone purpose...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Climate Monitoring (Optional)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = tempText,
                        onValueChange = { tempText = it },
                        label = { Text("Temp (°C, optional)") },
                        placeholder = { Text("e.g. 13") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rack_temp_input")
                    )
                    OutlinedTextField(
                        value = humText,
                        onValueChange = { humText = it },
                        label = { Text("Humidity (%, optional)") },
                        placeholder = { Text("e.g. 70") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rack_hum_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Accent Color:",
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { (hex, _) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (selectedColor == hex) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cleanTemp = tempText.trim().toDoubleOrNull()
                        val cleanHum = humText.trim().toDoubleOrNull()
                        val result = CellarLocation(
                            id = initialLocation?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            rows = rows,
                            columns = columns,
                            description = description.ifBlank { null },
                            temperatureCelsius = cleanTemp,
                            humidityPercent = cleanHum,
                            colorHex = selectedColor
                        )
                        onConfirm(result)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                modifier = Modifier.testTag(if (isEditMode) "confirm_edit_location_button" else "confirm_add_location_button")
            ) {
                Text(if (isEditMode) "Save Changes" else "Create Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddLocationDialog(
    onDismiss: () -> Unit,
    onConfirm: (CellarLocation) -> Unit
) {
    RackFormDialog(initialLocation = null, onDismiss = onDismiss, onConfirm = onConfirm)
}
