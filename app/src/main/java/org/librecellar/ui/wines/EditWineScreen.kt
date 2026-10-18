package org.librecellar.ui.wines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WineBar
import org.librecellar.ui.components.BarcodeScannerDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import org.librecellar.data.model.WineType
import org.librecellar.ui.components.CountryDropdownSelect
import org.librecellar.ui.components.RatingBar
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWineScreen(
    wine: Wine,
    locations: List<CellarLocation>,
    suppliers: List<Supplier>,
    allWines: List<Wine>,
    isPriceTierEnabled: Boolean = true,
    onSaveWine: (Wine) -> Unit,
    onCancel: () -> Unit
) {
    // Identity Fields
    var name by remember(wine) { mutableStateOf(wine.name) }
    var vintageText by remember(wine) { mutableStateOf(wine.vintage.toString()) }
    var selectedType by remember(wine) {
        mutableStateOf(
            try {
                WineType.valueOf(wine.wineType)
            } catch (e: Exception) {
                WineType.RED
            }
        )
    }
    var producer by remember(wine) { mutableStateOf(wine.producer ?: "") }
    var varietal by remember(wine) { mutableStateOf(wine.varietal ?: "") }
    var country by remember(wine) { mutableStateOf(wine.country ?: "") }
    var region by remember(wine) { mutableStateOf(wine.region ?: "") }

    val assignedCountries = remember(allWines, suppliers) {
        (allWines.mapNotNull { it.country } + suppliers.mapNotNull { it.country })
            .filter { it.isNotBlank() }
            .toSet()
    }

    // Inventory & Placement
    var quantity by remember(wine) { mutableIntStateOf(wine.quantity) }
    var selectedLocationId by remember(wine) { mutableStateOf(wine.locationId) }
    var slotRow by remember(wine) { mutableIntStateOf(wine.slotRow ?: 0) }
    var slotColumn by remember(wine) { mutableIntStateOf(wine.slotColumn ?: 0) }

    // Valuation & Merchant
    var priceText by remember(wine) { mutableStateOf(wine.purchasePrice?.toString() ?: "") }
    var selectedTier by remember(wine) { mutableStateOf(WinePriceTier.fromKey(wine.priceTier)) }
    var purchaseLocation by remember(wine) { mutableStateOf(wine.purchaseLocation ?: "") }
    var purchaseDate by remember(wine) { mutableStateOf(wine.purchaseDate ?: "") }
    var merchantDropdownExpanded by remember { mutableStateOf(false) }

    // Specs & Tasting
    var bottleSize by remember(wine) { mutableStateOf(wine.size ?: "0.75L") }
    var alcoholText by remember(wine) { mutableStateOf(wine.alcoholPercent?.toString() ?: "") }
    var tastingRating by remember(wine) { mutableIntStateOf(wine.tastingRating ?: 0) }
    var comments by remember(wine) { mutableStateOf(wine.comments ?: "") }
    var barcode by remember(wine) { mutableStateOf(wine.barcode ?: "") }
    var photoUrl by remember(wine) { mutableStateOf(wine.photoUrl ?: "") }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    val selectedLocation = locations.find { it.id == selectedLocationId }

    val vintageInt = vintageText.toIntOrNull()
    val isFormValid = name.isNotBlank() && vintageInt != null && vintageInt > 1800

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarPadding = maxOf(statusBarTop, 28.dp)

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(topBarPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("edit_wine_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Edit Wine Dossier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = wine.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = CellarGold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val cleanPrice = priceText.toDoubleOrNull()
                                val cleanAlcohol = alcoholText.toDoubleOrNull()
                                val label = if (selectedLocation != null) {
                                    "Row ${slotRow + 1}, Slot ${slotColumn + 1}"
                                } else null

                                val updated = wine.copy(
                                    name = name.trim(),
                                    vintage = vintageInt!!,
                                    wineType = selectedType.name,
                                    producer = producer.trim().ifBlank { null },
                                    varietal = varietal.trim().ifBlank { null },
                                    country = country.trim().ifBlank { null },
                                    region = region.trim().ifBlank { null },
                                    quantity = quantity,
                                    locationId = selectedLocationId,
                                    slotRow = if (selectedLocationId != null) slotRow else null,
                                    slotColumn = if (selectedLocationId != null) slotColumn else null,
                                    slotLabel = label,
                                    purchasePrice = cleanPrice,
                                    priceTier = selectedTier?.key,
                                    purchaseLocation = purchaseLocation.trim().ifBlank { null },
                                    purchaseDate = purchaseDate.trim().ifBlank { null },
                                    size = bottleSize.trim().ifBlank { "0.75L" },
                                    alcoholPercent = cleanAlcohol,
                                    tastingRating = if (tastingRating > 0) tastingRating else null,
                                    comments = comments.trim().ifBlank { null },
                                    barcode = barcode.trim().ifBlank { null },
                                    photoUrl = photoUrl.trim().ifBlank { null }
                                )
                                onSaveWine(updated)
                            }
                        },
                        enabled = isFormValid,
                        colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                        modifier = Modifier.testTag("edit_wine_top_save_button")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Essential Identity & Classification
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.WineBar, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Wine Identity & Varietal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Wine / Label Name *") },
                        placeholder = { Text("e.g. Château Margaux, Barolo Monfortino") },
                        isError = name.isBlank(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_wine_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = vintageText,
                            onValueChange = { vintageText = it },
                            label = { Text("Vintage Year *") },
                            placeholder = { Text("e.g. 2018") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = vintageInt == null || vintageInt < 1800,
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wine_vintage_input")
                        )

                        OutlinedTextField(
                            value = producer,
                            onValueChange = { producer = it },
                            label = { Text("Producer / Winery") },
                            placeholder = { Text("e.g. Château Margaux") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("edit_wine_producer_input")
                        )
                    }

                    // Country Dropdown & Region Text Field (Split into 2 fields)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CountryDropdownSelect(
                            selectedCountry = country,
                            onCountrySelected = { country = it },
                            assignedCountries = assignedCountries,
                            label = "Country",
                            modifier = Modifier.weight(1.2f),
                            testTag = "edit_wine_country_input"
                        )

                        OutlinedTextField(
                            value = region,
                            onValueChange = { region = it },
                            label = { Text("Region / Appellation") },
                            placeholder = { Text("e.g. Bordeaux, Napa") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("edit_wine_region_input")
                        )
                    }

                    // Wine Type Selector Chips
                    Text(
                        text = "Wine Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(WineType.entries) { type ->
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WineBurgundyLight,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("edit_wine_type_${type.name}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = varietal,
                        onValueChange = { varietal = it },
                        label = { Text("Varietal / Grape Blend") },
                        placeholder = { Text("e.g. Cabernet Sauvignon 70%, Merlot 30%") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_wine_varietal_input")
                    )
                }
            }

            // Card 2: Inventory Stock & Cellar Rack Slot Placement
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stock & Cellar Rack Placement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Stock Quantity Counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quantity in Stock",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (quantity == 0) "Depleted / Out of stock" else "$quantity bottles available",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (quantity == 0) WineRose else CellarGold
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 0) quantity-- },
                                    modifier = Modifier.testTag("edit_wine_qty_decrement")
                                ) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = WineRose)
                                }
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.testTag("edit_wine_qty_increment")
                                ) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CellarGold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Cellar Rack Selection
                    Text(
                        text = "Assigned Cellar Rack",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedLocationId == null,
                                onClick = {
                                    selectedLocationId = null
                                },
                                label = { Text("Not in cellar (Unassigned)") },
                                leadingIcon = {
                                    Icon(imageVector = Icons.Default.LocationOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WineBurgundyLight,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        items(locations) { loc ->
                            val isSelected = loc.id == selectedLocationId
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLocationId = loc.id
                                    // Ensure row/col fit in new location dimensions
                                    if (slotRow >= loc.rows) slotRow = 0
                                    if (slotColumn >= loc.columns) slotColumn = 0
                                },
                                label = { Text(loc.name) },
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
                                )
                            )
                        }
                    }

                    // Shelf Slot Coordinates (if assigned)
                    if (selectedLocation != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Target Slot: Row ${slotRow + 1}, Slot ${slotColumn + 1} (${selectedLocation.name})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CellarGold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Row Picker
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Row (1 to ${selectedLocation.rows})", style = MaterialTheme.typography.labelSmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (slotRow > 0) slotRow-- }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "Row ${slotRow + 1}",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            IconButton(onClick = { if (slotRow < selectedLocation.rows - 1) slotRow++ }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Column Picker
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Slot (1 to ${selectedLocation.columns})", style = MaterialTheme.typography.labelSmall)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { if (slotColumn > 0) slotColumn-- }) {
                                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = "Slot ${slotColumn + 1}",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            IconButton(onClick = { if (slotColumn < selectedLocation.columns - 1) slotColumn++ }) {
                                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 3: Valuation, Price Range Marking & Vendor
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Purchase & Price Tier Markings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = { Text("Purchase Price ($)") },
                            placeholder = { Text("e.g. 45.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = CellarGold) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wine_price_input")
                        )

                        OutlinedTextField(
                            value = purchaseDate,
                            onValueChange = { purchaseDate = it },
                            label = { Text("Purchase Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("edit_wine_purchase_date_input")
                        )
                    }

                    // Price Range Suggestion & Manual Override Selector
                    if (isPriceTierEnabled) {
                        PriceRangeSuggestionSelector(
                            purchasePrice = priceText.toDoubleOrNull(),
                            existingWines = allWines,
                            selectedTier = selectedTier,
                            onTierSelected = { selectedTier = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Merchant / Supplier Selection
                    ExposedDropdownMenuBox(
                        expanded = merchantDropdownExpanded,
                        onExpandedChange = { merchantDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = purchaseLocation,
                            onValueChange = { purchaseLocation = it },
                            label = { Text("Merchant / Purchased From") },
                            placeholder = { Text("e.g. Berry Bros. & Rudd, Caviste") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = merchantDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("edit_wine_merchant_input")
                        )

                        if (suppliers.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = merchantDropdownExpanded,
                                onDismissRequest = { merchantDropdownExpanded = false }
                            ) {
                                suppliers.forEach { sup ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("${sup.name} (${sup.type})") },
                                        onClick = {
                                            purchaseLocation = sup.name
                                            merchantDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 4: Bottle Specs & Tasting Dossier
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bottle Specs & Tasting Dossier",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = bottleSize,
                            onValueChange = { bottleSize = it },
                            label = { Text("Bottle Size") },
                            placeholder = { Text("0.75L, 1.5L") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wine_size_input")
                        )

                        OutlinedTextField(
                            value = alcoholText,
                            onValueChange = { alcoholText = it },
                            label = { Text("Alcohol %") },
                            placeholder = { Text("e.g. 13.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Icon(imageVector = Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_wine_alcohol_input")
                        )
                    }

                    // Tasting Rating
                    Column {
                        Text(
                            text = "Personal Tasting Rating (${if (tastingRating > 0) "$tastingRating Stars" else "Unrated"})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { tastingRating = if (tastingRating == star) 0 else star },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WineBar,
                                        contentDescription = "$star Stars",
                                        tint = if (star <= tastingRating) CellarGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            if (tastingRating > 0) {
                                TextButton(onClick = { tastingRating = 0 }) {
                                    Text("Clear", fontSize = 11.sp, color = WineRose)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = comments,
                        onValueChange = { comments = it },
                        label = { Text("Cellar Tasting Notes & Comments") },
                        placeholder = { Text("Tasting profile, decanting recommendation, pairing ideas...") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_wine_comments_input")
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Bottle Barcode / UPC") },
                        placeholder = { Text("Scan or enter barcode number") },
                        leadingIcon = { Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(
                                onClick = { showBarcodeScanner = true },
                                modifier = Modifier.testTag("edit_wine_scan_barcode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode with Camera",
                                    tint = org.librecellar.ui.theme.CellarGold
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_wine_barcode_input")
                    )
                }
            }

            // Bottom Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (isFormValid) {
                            val cleanPrice = priceText.toDoubleOrNull()
                            val cleanAlcohol = alcoholText.toDoubleOrNull()
                            val label = if (selectedLocation != null) {
                                "Row ${slotRow + 1}, Slot ${slotColumn + 1}"
                            } else null

                            val updated = wine.copy(
                                name = name.trim(),
                                vintage = vintageInt!!,
                                wineType = selectedType.name,
                                producer = producer.trim().ifBlank { null },
                                varietal = varietal.trim().ifBlank { null },
                                country = country.trim().ifBlank { null },
                                region = region.trim().ifBlank { null },
                                quantity = quantity,
                                locationId = selectedLocationId,
                                slotRow = if (selectedLocationId != null) slotRow else null,
                                slotColumn = if (selectedLocationId != null) slotColumn else null,
                                slotLabel = label,
                                purchasePrice = cleanPrice,
                                priceTier = selectedTier?.key,
                                purchaseLocation = purchaseLocation.trim().ifBlank { null },
                                purchaseDate = purchaseDate.trim().ifBlank { null },
                                size = bottleSize.trim().ifBlank { "0.75L" },
                                alcoholPercent = cleanAlcohol,
                                tastingRating = if (tastingRating > 0) tastingRating else null,
                                comments = comments.trim().ifBlank { null },
                                barcode = barcode.trim().ifBlank { null },
                                photoUrl = photoUrl.trim().ifBlank { null }
                            )
                            onSaveWine(updated)
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_wine_changes_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Wine Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("cancel_wine_edit_button")
                ) {
                    Text("Cancel")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Live Barcode Scanner Dialog for setting wine barcode
        if (showBarcodeScanner) {
            BarcodeScannerDialog(
                initialBarcode = barcode,
                onBarcodeScanned = { scannedCode ->
                    barcode = scannedCode
                    showBarcodeScanner = false
                },
                onDismiss = { showBarcodeScanner = false }
            )
        }
    }
}
