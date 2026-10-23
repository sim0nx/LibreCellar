package org.librecellar.ui.suppliers

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import org.librecellar.data.model.CountryHelper
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.ui.components.CountryDropdownSelect
import org.librecellar.ui.components.SharedEntryBadge
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    suppliers: List<Supplier>,
    wines: List<Wine>,
    onAddSupplier: (Supplier) -> Unit,
    onUpdateSupplier: (Supplier) -> Unit,
    onDeleteSupplier: (String) -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    hasMoreSuppliers: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var supplierToEdit by remember { mutableStateOf<Supplier?>(null) }
    var supplierToDelete by remember { mutableStateOf<Supplier?>(null) }

    val supplierListState = rememberLazyListState()

    // Continuous scroll as you go pagination live from backend
    val shouldLoadMoreSuppliers by remember {
        derivedStateOf {
            val totalItems = supplierListState.layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false
            val lastVisible = supplierListState.layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index >= totalItems - 3
        }
    }
    LaunchedEffect(shouldLoadMoreSuppliers, hasMoreSuppliers, isLoadingMore) {
        if (shouldLoadMoreSuppliers && hasMoreSuppliers && !isLoadingMore) {
            onLoadMore()
        }
    }

    val assignedCountries = remember(wines, suppliers) {
        (wines.mapNotNull { it.country } + suppliers.mapNotNull { it.country })
            .filter { it.isNotBlank() }
            .toSet()
    }

    val filteredSuppliers = suppliers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                (it.city?.contains(searchQuery, ignoreCase = true) == true) ||
                (it.country?.contains(searchQuery, ignoreCase = true) == true) ||
                it.type.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = WineBurgundyLight,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(imageVector = AppIcons.Add, contentDescription = "Add Supplier")
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
                // Compact Header with Sync Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.Storefront,
                            contentDescription = null,
                            tint = CellarGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Merchants & Suppliers",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRefreshing) "Syncing from backend..." else "Wine shops, distributors & wineries (${filteredSuppliers.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("refresh_suppliers_btn")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = CellarGold
                                )
                            } else {
                                Icon(
                                    imageVector = AppIcons.Refresh,
                                    contentDescription = "Sync",
                                    tint = CellarGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search merchants, shops, distributors...") },
                    leadingIcon = {
                        Icon(imageVector = AppIcons.Search, contentDescription = "Search", tint = CellarGold)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .testTag("search_suppliers_input")
                )

                // Supplier List
                if (filteredSuppliers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No merchants found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                            ) {
                                Text("Add Purchase Location")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = supplierListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSuppliers) { supplier ->
                            val winesCount = wines.count { it.purchaseLocation.equals(supplier.name, ignoreCase = true) }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("supplier_card_${supplier.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = supplier.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = WineBurgundyLight.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Text(
                                                text = supplier.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WineRose,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        SharedEntryBadge(entry = supplier, modifier = Modifier.padding(top = 4.dp))
                                    }

                                    // Action buttons: Edit and Delete (not for merchants shared read-only)
                                    if (!supplier.isReadOnly) Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { supplierToEdit = supplier },
                                            modifier = Modifier.testTag("edit_supplier_${supplier.id}")
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Edit,
                                                contentDescription = "Edit Merchant",
                                                tint = CellarGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { supplierToDelete = supplier },
                                            modifier = Modifier.testTag("delete_supplier_${supplier.id}")
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.Delete,
                                                contentDescription = "Delete",
                                                tint = WineRose.copy(alpha = 0.8f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (!supplier.city.isNullOrBlank() || !supplier.country.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AppIcons.LocationOn,
                                            contentDescription = null,
                                            tint = CellarGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = listOfNotNull(supplier.city, supplier.country).joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!supplier.website.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AppIcons.Language,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = supplier.website,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (!supplier.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = supplier.notes,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Footer: Wine Count and Date Added Tracking
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AppIcons.WineBar,
                                            contentDescription = null,
                                            tint = CellarGold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (winesCount == 1) "1 wine sourced" else "$winesCount wines sourced",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CellarGold
                                        )
                                    }

                                    // Date Added Tracker
                                    if (supplier.dateAdded != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = AppIcons.CalendarToday,
                                            contentDescription = "Date Added",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Added ${supplier.dateAdded}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
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
                                    color = CellarGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Loading next 20 merchants from backend...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (!hasMoreSuppliers && suppliers.isNotEmpty()) {
                        item {
                            Text(
                                text = "All ${suppliers.size} merchants loaded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Merchant Dialog
    if (showAddDialog) {
        SupplierFormDialog(
            supplierToEdit = null,
            assignedCountries = assignedCountries,
            onDismiss = { showAddDialog = false },
            onConfirm = { newSupplier ->
                onAddSupplier(newSupplier)
                showAddDialog = false
            }
        )
    }

    // Edit Merchant Dialog
    supplierToEdit?.let { existing ->
        SupplierFormDialog(
            supplierToEdit = existing,
            assignedCountries = assignedCountries,
            onDismiss = { supplierToEdit = null },
            onConfirm = { updatedSupplier ->
                onUpdateSupplier(updatedSupplier)
                supplierToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    supplierToDelete?.let { supplier ->
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            title = { Text("Delete Merchant?") },
            text = { Text("Are you sure you want to remove '${supplier.name}' from your suppliers list?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSupplier(supplier.id)
                        supplierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormDialog(
    supplierToEdit: Supplier? = null,
    initialName: String = "",
    assignedCountries: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (Supplier) -> Unit
) {
    val isEditMode = supplierToEdit != null
    var name by remember { mutableStateOf(supplierToEdit?.name ?: initialName) }
    var selectedType by remember { mutableStateOf(supplierToEdit?.type ?: "Wine Merchant") }
    var city by remember { mutableStateOf(supplierToEdit?.city ?: "") }
    var country by remember { mutableStateOf(supplierToEdit?.country ?: "") }
    var website by remember { mutableStateOf(supplierToEdit?.website ?: "") }
    var notes by remember { mutableStateOf(supplierToEdit?.notes ?: "") }
    var dateAdded by remember {
        mutableStateOf(if (supplierToEdit == null) LocalDate.now().toString() else supplierToEdit.dateAdded.orEmpty())
    }

    val types = listOf(
        "Wine Merchant",
        "Winery Direct",
        "Online Distributor",
        "Auction House",
        "Boutique Caviste",
        "Supermarket / Retail"
    )

    var typeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Merchant / Supplier" else "Add Purchase Location / Merchant",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Merchant / Shop Name *") },
                    placeholder = { Text("e.g. Berry Bros. & Rudd, Total Wine") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("supplier_name_input")
                )

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Supplier Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f)
                    )
                    CountryDropdownSelect(
                        selectedCountry = country,
                        onCountrySelected = { country = it },
                        assignedCountries = assignedCountries,
                        label = "Country",
                        modifier = Modifier.weight(1.2f),
                        testTag = "supplier_country_dropdown"
                    )
                }

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website / Contact") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Allocations, contact person)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Added field (editable or tracked)
                OutlinedTextField(
                    value = dateAdded,
                    onValueChange = { dateAdded = it },
                    label = { Text("Date Added (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val baseSupplier = supplierToEdit ?: Supplier(name = name.trim())
                        onConfirm(
                            baseSupplier.copy(
                                name = name.trim(),
                                type = selectedType,
                                city = city.ifBlank { null },
                                country = country.ifBlank { null },
                                website = website.ifBlank { null },
                                notes = notes.ifBlank { null },
                                dateAdded = dateAdded.ifBlank { null }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                modifier = Modifier.testTag("confirm_supplier_form_button")
            ) {
                Text(if (isEditMode) "Save Changes" else "Save Supplier")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Backward compatibility alias
@Composable
fun AddSupplierDialog(
    initialName: String = "",
    assignedCountries: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (Supplier) -> Unit
) {
    SupplierFormDialog(
        supplierToEdit = null,
        initialName = initialName,
        assignedCountries = assignedCountries,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}
