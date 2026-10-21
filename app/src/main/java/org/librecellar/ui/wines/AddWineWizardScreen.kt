package org.librecellar.ui.wines

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import org.librecellar.data.model.WineType
import org.librecellar.ui.cellar.AddLocationDialog
import org.librecellar.ui.components.CurrencyPicker
import org.librecellar.ui.components.BarcodeScannerDialog
import org.librecellar.ui.components.CountryDropdownSelect
import org.librecellar.ui.components.GrapeTagInput
import org.librecellar.ui.components.PhotoCaptureDialog
import org.librecellar.ui.components.RatingBar
import org.librecellar.ui.components.WineBottleImage
import org.librecellar.ui.suppliers.AddSupplierDialog
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import java.util.Calendar

@Composable
fun PriceRangeSuggestionSelector(
    purchasePrice: Double?,
    existingWines: List<Wine>,
    selectedTier: WinePriceTier?,
    onTierSelected: (WinePriceTier?) -> Unit,
    modifier: Modifier = Modifier,
    currency: String? = null
) {
    val dynamicSuggested = remember(purchasePrice, existingWines, currency) {
        WinePriceTier.calculateDynamicTier(purchasePrice, existingWines, currency)
    }
    val currentActive = selectedTier ?: dynamicSuggested

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null,
                    tint = CellarGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Price Range Tier",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val suggestedColor = try {
                Color(android.graphics.Color.parseColor(dynamicSuggested.colorHex))
            } catch (e: Exception) {
                CellarGold
            }

            Surface(
                color = suggestedColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Suggested: ${dynamicSuggested.shortLabel} (${dynamicSuggested.symbolFor(currency)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = suggestedColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (selectedTier != null && selectedTier != dynamicSuggested) {
                "Manually set to '${selectedTier.shortLabel}' (override active)"
            } else {
                "Dynamically suggested based on your cellar collection"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5 Tiers row: Cheap, Normal, Medium, Expensive, Very Expensive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WinePriceTier.entries.forEach { tier ->
                val isSelected = currentActive == tier
                val isSuggested = dynamicSuggested == tier
                val tierColor = try {
                    Color(android.graphics.Color.parseColor(tier.colorHex))
                } catch (e: Exception) {
                    CellarGold
                }

                Surface(
                    color = if (isSelected) tierColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else if (isSuggested) 1.dp else 0.5.dp,
                        color = if (isSelected) Color.White else if (isSuggested) CellarGold else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selectedTier == tier) {
                                onTierSelected(null)
                            } else {
                                onTierSelected(tier)
                            }
                        }
                        .testTag("tier_button_${tier.key}")
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tier.symbolFor(currency),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else tierColor
                        )
                        Text(
                            text = tier.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSuggested) {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.9f) else CellarGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class EntryMode {
    UNSELECTED,
    WIZARD,
    MANUAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWineWizardScreen(
    cellarLocations: List<CellarLocation>,
    suppliers: List<Supplier>,
    existingWines: List<Wine> = emptyList(),
    knownGrapes: List<String> = emptyList(),
    onRegisterGrape: (String) -> Unit = {},
    isPriceTierEnabled: Boolean = true,
    onWineCreated: (Wine) -> Unit,
    onAddLocation: (CellarLocation) -> Unit,
    onAddSupplier: (Supplier) -> Unit,
    onCancel: () -> Unit
) {
    var entryMode by remember { mutableStateOf(EntryMode.UNSELECTED) }
    var currentWizardStep by remember { mutableIntStateOf(1) } // 1: Barcode, 2: Photo, 3: Details, 4: Location

    // Wine Form State
    var name by remember { mutableStateOf("") }
    var vintageText by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }
    var country by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    var selectedGrapes by remember { mutableStateOf<List<String>>(emptyList()) }
    var producer by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var alcoholText by remember { mutableStateOf("") }
    var sizeText by remember { mutableStateOf("0.75L") }
    var tastingRating by remember { mutableStateOf<Int?>(null) }
    var comments by remember { mutableStateOf("") }
    var purchasePriceText by remember { mutableStateOf("") }
    // New wines are priced in the device's currency unless the user picks another one
    var currency by remember { mutableStateOf(LocaleFormat.currency.currencyCode) }
    var selectedPriceTier by remember { mutableStateOf<WinePriceTier?>(null) }
    var purchaseLocation by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var barcode by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var wineType by remember { mutableStateOf(WineType.RED.name) }

    val assignedCountries = remember(existingWines, suppliers) {
        (existingWines.mapNotNull { it.country } + suppliers.mapNotNull { it.country })
            .filter { it.isNotBlank() }
            .toSet()
    }

    val usedGrapes = remember(existingWines) {
        existingWines.flatMap { it.varietalList }.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    // Location Assignment State
    var selectedLocationId by remember { mutableStateOf<String?>(null) } // null = None
    var selectedRow by remember { mutableStateOf<Int?>(null) }
    var selectedCol by remember { mutableStateOf<Int?>(null) }

    // Dialogs
    var showScannerDialog by remember { mutableStateOf(false) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showNewLocationDialog by remember { mutableStateOf(false) }
    var showNewSupplierDialog by remember { mutableStateOf(false) }

    fun commitWine() {
        val vintage = vintageText.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val quantity = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val alc = LocaleFormat.parseDecimal(alcoholText)
        val price = LocaleFormat.parseDecimal(purchasePriceText)

        val dynamicTier = WinePriceTier.calculateDynamicTier(price, existingWines, currency)
        val finalTier = (selectedPriceTier ?: dynamicTier).key

        val slotLabel = if (selectedLocationId != null && selectedRow != null && selectedCol != null) {
            "Row ${selectedRow!! + 1}, Slot ${selectedCol!! + 1}"
        } else null

        val newWine = Wine(
            name = name.trim(),
            vintage = vintage,
            country = country.ifBlank { null },
            region = region.ifBlank { null },
            varietal = selectedGrapes.joinToString(", ").ifBlank { null },
            producer = producer.ifBlank { null },
            purchaseDate = purchaseDate.ifBlank { null },
            alcoholPercent = alc,
            size = sizeText.ifBlank { "0.75L" },
            tastingRating = tastingRating,
            comments = comments.ifBlank { null },
            purchasePrice = price,
            currency = currency,
            purchaseLocation = purchaseLocation.ifBlank { null },
            photoUrl = photoUrl,
            barcode = barcode.ifBlank { null },
            priceTier = finalTier,
            quantity = quantity,
            locationId = selectedLocationId,
            slotRow = selectedRow,
            slotColumn = selectedCol,
            slotLabel = slotLabel,
            wineType = wineType
        )
        onWineCreated(newWine)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarPadding = maxOf(statusBarTop, 28.dp)

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(topBarPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (entryMode == EntryMode.WIZARD && currentWizardStep > 1) {
                                    currentWizardStep--
                                } else if (entryMode != EntryMode.UNSELECTED) {
                                    entryMode = EntryMode.UNSELECTED
                                } else {
                                    onCancel()
                                }
                            },
                            modifier = Modifier.testTag("back_button_add_wine")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Text(
                            text = when (entryMode) {
                                EntryMode.UNSELECTED -> "Add New Wine"
                                EntryMode.WIZARD -> "Wine Intake Wizard (Step $currentWizardStep of 4)"
                                EntryMode.MANUAL -> "Manual Wine Entry"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onCancel) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            }
        }
    }
) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (entryMode) {
                EntryMode.UNSELECTED -> {
                    // Initial Choice: Wizard vs Manual Entry
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(WineBurgundy)
                                .border(2.dp, CellarGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WineBar,
                                contentDescription = null,
                                tint = CellarGold,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Add Wine to Cellar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose your preferred method to register new bottles into the cellar inventory.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Wizard Card Option
                        Card(
                            onClick = {
                                entryMode = EntryMode.WIZARD
                                currentWizardStep = 1
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_wizard_mode_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(WineBurgundyLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Add via Wizard (Recommended)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CellarGold
                                    )
                                    Text(
                                        text = "Scan barcode → bottle photo → wine details → cellar location slot",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = CellarGold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Manual Entry Card Option
                        Card(
                            onClick = {
                                entryMode = EntryMode.MANUAL
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_manual_mode_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Manual Entry",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Fill all properties directly in a comprehensive single form",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                EntryMode.WIZARD -> {
                    // Wizard Stepper Content
                    Column(modifier = Modifier.fillMaxSize()) {
                        LinearProgressIndicator(
                            progress = { currentWizardStep / 4f },
                            modifier = Modifier.fillMaxWidth(),
                            color = CellarGold,
                            trackColor = WineBurgundy.copy(alpha = 0.3f)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            when (currentWizardStep) {
                                1 -> {
                                    // Step 1: Scan Barcode
                                    WizardStepBarcode(
                                        barcode = barcode,
                                        onScanClick = { showScannerDialog = true },
                                        onBarcodeChanged = { barcode = it },
                                        onNext = { currentWizardStep = 2 }
                                    )
                                }

                                2 -> {
                                    // Step 2: Photo of bottle
                                    WizardStepPhoto(
                                        photoUrl = photoUrl,
                                        onPhotoClick = { showPhotoDialog = true },
                                        onClearPhoto = { photoUrl = null },
                                        onNext = { currentWizardStep = 3 }
                                    )
                                }

                                3 -> {
                                    // Step 3: Name & details
                                    WizardStepDetails(
                                        name = name,
                                        onNameChange = { name = it },
                                        vintageText = vintageText,
                                        onVintageChange = { vintageText = it },
                                        country = country,
                                        onCountryChange = { country = it },
                                        region = region,
                                        onRegionChange = { region = it },
                                        assignedCountries = assignedCountries,
                                        selectedGrapes = selectedGrapes,
                                        onGrapesChange = { selectedGrapes = it },
                                        availableGrapes = knownGrapes,
                                        usedGrapes = usedGrapes,
                                        onNewGrapeEntered = onRegisterGrape,
                                        producer = producer,
                                        onProducerChange = { producer = it },
                                        sizeText = sizeText,
                                        onSizeChange = { sizeText = it },
                                        alcoholText = alcoholText,
                                        onAlcoholChange = { alcoholText = it },
                                        purchasePriceText = purchasePriceText,
                                        onPriceChange = { purchasePriceText = it },
                                        currency = currency,
                                        onCurrencyChange = { currency = it },
                                        existingWines = existingWines,
                                        isPriceTierEnabled = isPriceTierEnabled,
                                        selectedPriceTier = selectedPriceTier,
                                        onPriceTierChange = { selectedPriceTier = it },
                                        purchaseDate = purchaseDate,
                                        onDateChange = { purchaseDate = it },
                                        purchaseLocation = purchaseLocation,
                                        onLocationChange = { purchaseLocation = it },
                                        suppliers = suppliers,
                                        onAddSupplierClick = { showNewSupplierDialog = true },
                                        tastingRating = tastingRating,
                                        onRatingChange = { tastingRating = it },
                                        comments = comments,
                                        onCommentsChange = { comments = it },
                                        wineType = wineType,
                                        onWineTypeChange = { wineType = it },
                                        quantityText = quantityText,
                                        onQuantityChange = { quantityText = it },
                                        onNext = { currentWizardStep = 4 }
                                    )
                                }

                                4 -> {
                                    // Step 4: Final Step - Choose Location (Existing, Create new, or None)
                                    WizardStepLocation(
                                        cellarLocations = cellarLocations,
                                        selectedLocationId = selectedLocationId,
                                        selectedRow = selectedRow,
                                        selectedCol = selectedCol,
                                        onSelectLocation = { locId, row, col ->
                                            selectedLocationId = locId
                                            selectedRow = row
                                            selectedCol = col
                                        },
                                        onCreateNewLocation = { showNewLocationDialog = true },
                                        onFinish = { commitWine() }
                                    )
                                }
                            }
                        }
                    }
                }

                EntryMode.MANUAL -> {
                    // Manual Single Page Entry
                    ManualWineEntryForm(
                        name = name,
                        onNameChange = { name = it },
                        vintageText = vintageText,
                        onVintageChange = { vintageText = it },
                        country = country,
                        onCountryChange = { country = it },
                        region = region,
                        onRegionChange = { region = it },
                        assignedCountries = assignedCountries,
                        selectedGrapes = selectedGrapes,
                        onGrapesChange = { selectedGrapes = it },
                        availableGrapes = knownGrapes,
                        usedGrapes = usedGrapes,
                        onNewGrapeEntered = onRegisterGrape,
                        producer = producer,
                        onProducerChange = { producer = it },
                        purchaseDate = purchaseDate,
                        onDateChange = { purchaseDate = it },
                        alcoholText = alcoholText,
                        onAlcoholChange = { alcoholText = it },
                        sizeText = sizeText,
                        onSizeChange = { sizeText = it },
                        tastingRating = tastingRating,
                        onRatingChange = { tastingRating = it },
                        comments = comments,
                        onCommentsChange = { comments = it },
                        purchasePriceText = purchasePriceText,
                        onPriceChange = { purchasePriceText = it },
                        currency = currency,
                        onCurrencyChange = { currency = it },
                        existingWines = existingWines,
                        isPriceTierEnabled = isPriceTierEnabled,
                        selectedPriceTier = selectedPriceTier,
                        onPriceTierChange = { selectedPriceTier = it },
                        purchaseLocation = purchaseLocation,
                        onLocationChange = { purchaseLocation = it },
                        suppliers = suppliers,
                        onAddSupplierClick = { showNewSupplierDialog = true },
                        photoUrl = photoUrl,
                        onPhotoClick = { showPhotoDialog = true },
                        barcode = barcode,
                        onBarcodeScanClick = { showScannerDialog = true },
                        onBarcodeChange = { barcode = it },
                        quantityText = quantityText,
                        onQuantityChange = { quantityText = it },
                        wineType = wineType,
                        onWineTypeChange = { wineType = it },
                        cellarLocations = cellarLocations,
                        selectedLocationId = selectedLocationId,
                        selectedRow = selectedRow,
                        selectedCol = selectedCol,
                        onSelectLocation = { locId, row, col ->
                            selectedLocationId = locId
                            selectedRow = row
                            selectedCol = col
                        },
                        onCreateNewLocation = { showNewLocationDialog = true },
                        onSave = { commitWine() }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showScannerDialog) {
        BarcodeScannerDialog(
            initialBarcode = barcode,
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showScannerDialog = false
            },
            onDismiss = { showScannerDialog = false }
        )
    }

    if (showPhotoDialog) {
        PhotoCaptureDialog(
            onPhotoSelected = { path ->
                photoUrl = path
                showPhotoDialog = false
            },
            onDismiss = { showPhotoDialog = false }
        )
    }

    if (showNewLocationDialog) {
        AddLocationDialog(
            onDismiss = { showNewLocationDialog = false },
            onConfirm = { newLoc ->
                onAddLocation(newLoc)
                selectedLocationId = newLoc.id
                selectedRow = 0
                selectedCol = 0
                showNewLocationDialog = false
            }
        )
    }

    if (showNewSupplierDialog) {
        AddSupplierDialog(
            initialName = purchaseLocation,
            assignedCountries = assignedCountries,
            onDismiss = { showNewSupplierDialog = false },
            onConfirm = { newSup ->
                onAddSupplier(newSup)
                purchaseLocation = newSup.name
                showNewSupplierDialog = false
            }
        )
    }
}

// --- WIZARD SUB-STEPS ---

@Composable
fun WizardStepBarcode(
    barcode: String,
    onScanClick: () -> Unit,
    onBarcodeChanged: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = CellarGold,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Step 1: Scan Bottle Barcode",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scan the UPC/EAN code on the bottle for instant identification or enter it manually. You can also skip this step.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("wizard_open_scanner_button")
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Barcode Scanner")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = barcode,
                onValueChange = onBarcodeChanged,
                label = { Text("Barcode (Optional)") },
                placeholder = { Text("e.g. 3421580001015") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .testTag("wizard_barcode_input")
            )

            if (barcode.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = WineBurgundy.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Scanned: $barcode",
                        style = MaterialTheme.typography.labelSmall,
                        color = CellarGold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (barcode.isBlank()) "Skip Step" else "Next")
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                modifier = Modifier.weight(1f)
            ) {
                Text("Next: Bottle Photo")
            }
        }
    }
}

@Composable
fun WizardStepPhoto(
    photoUrl: String?,
    onPhotoClick: () -> Unit,
    onClearPhoto: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = CellarGold,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Step 2: Bottle & Label Photo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Take a picture or select one from your device to visually identify the bottle in your cellar.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Photo Preview or Placeholder
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(2.dp, CellarGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { onPhotoClick() },
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        WineBottleImage(
                            photoUrl = photoUrl,
                            wineType = "RED",
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        IconButton(
                            onClick = onClearPhoto,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                .testTag("clear_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = CellarGold.copy(alpha = 0.8f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to Take / Select Photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Camera or Device Photos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onPhotoClick,
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .testTag("wizard_pick_photo_button")
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = CellarGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (photoUrl == null) "Take Photo or Select" else "Change Bottle Photo")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (photoUrl == null) "Skip Step" else "Next")
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                modifier = Modifier.weight(1f)
            ) {
                Text("Next: Wine Details")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardStepDetails(
    name: String,
    onNameChange: (String) -> Unit,
    vintageText: String,
    onVintageChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    region: String,
    onRegionChange: (String) -> Unit,
    assignedCountries: Set<String> = emptySet(),
    selectedGrapes: List<String>,
    onGrapesChange: (List<String>) -> Unit,
    availableGrapes: List<String> = emptyList(),
    usedGrapes: Set<String> = emptySet(),
    onNewGrapeEntered: (String) -> Unit = {},
    producer: String,
    onProducerChange: (String) -> Unit,
    sizeText: String,
    onSizeChange: (String) -> Unit,
    alcoholText: String,
    onAlcoholChange: (String) -> Unit,
    purchasePriceText: String,
    onPriceChange: (String) -> Unit,
    currency: String = LocaleFormat.currency.currencyCode,
    onCurrencyChange: (String) -> Unit = {},
    existingWines: List<Wine> = emptyList(),
    isPriceTierEnabled: Boolean = true,
    selectedPriceTier: WinePriceTier? = null,
    onPriceTierChange: (WinePriceTier?) -> Unit = {},
    purchaseDate: String,
    onDateChange: (String) -> Unit,
    purchaseLocation: String,
    onLocationChange: (String) -> Unit,
    suppliers: List<Supplier>,
    onAddSupplierClick: () -> Unit,
    tastingRating: Int?,
    onRatingChange: (Int) -> Unit,
    comments: String,
    onCommentsChange: (String) -> Unit,
    wineType: String,
    onWineTypeChange: (String) -> Unit,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Step 3: Wine Information",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mandatory: Name & Vintage. Enter optional details to enrich your cellar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Wine Type Selection
            Text(text = "Wine Style", style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(WineType.RED, WineType.WHITE, WineType.SPARKLING, WineType.ROSE).forEach { type ->
                    FilterChip(
                        selected = wineType == type.name,
                        onClick = { onWineTypeChange(type.name) },
                        label = { Text(type.displayName.split(" ")[0], fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WineBurgundyLight,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mandatory: Name
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Wine Name * (Mandatory)") },
                placeholder = { Text("e.g. Château Margaux, Sassicaia") },
                singleLine = true,
                isError = name.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wine_name_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mandatory: Vintage + Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = vintageText,
                    onValueChange = onVintageChange,
                    label = { Text("Vintage * (Mandatory)") },
                    placeholder = { Text("e.g. 2018") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wine_vintage_input")
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Bottle Count") },
                    placeholder = { Text("1") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Producer / Estate
            OutlinedTextField(
                value = producer,
                onValueChange = onProducerChange,
                label = { Text("Producer / Estate") },
                placeholder = { Text("e.g. Antinori") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Varietal / Grape Tags with Autocomplete Dropdown
            GrapeTagInput(
                selectedGrapes = selectedGrapes,
                onGrapesChange = onGrapesChange,
                availableGrapes = availableGrapes,
                usedGrapes = usedGrapes,
                onNewGrapeEntered = onNewGrapeEntered,
                label = "Varietal / Grape Blend",
                placeholder = "Type to search or add grape...",
                testTag = "wizard_grape_tag_input"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Country Dropdown & Region (Split into 2 fields)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CountryDropdownSelect(
                    selectedCountry = country,
                    onCountrySelected = onCountryChange,
                    assignedCountries = assignedCountries,
                    label = "Country",
                    modifier = Modifier.weight(1.2f),
                    testTag = "wizard_wine_country_input"
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = onRegionChange,
                    label = { Text("Region") },
                    placeholder = { Text("e.g. Bordeaux, Rioja") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wizard_wine_region_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottle Size & Alcohol %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = sizeText,
                    onValueChange = onSizeChange,
                    label = { Text("Bottle Size") },
                    placeholder = { Text("0.75L, 1.5L...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = alcoholText,
                    onValueChange = onAlcoholChange,
                    label = { Text("Alcohol %") },
                    placeholder = { Text("e.g. ${LocaleFormat.formatDecimal(13.5)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Purchase Price
            OutlinedTextField(
                value = purchasePriceText,
                onValueChange = onPriceChange,
                label = { Text("Purchase Price ($currency)") },
                placeholder = { Text("e.g. ${LocaleFormat.example(75.0)}") },
                leadingIcon = { CurrencyPicker(currencyCode = currency, onCurrencyChange = onCurrencyChange) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (isPriceTierEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                PriceRangeSuggestionSelector(
                    purchasePrice = LocaleFormat.parseDecimal(purchasePriceText),
                    existingWines = existingWines,
                    selectedTier = selectedPriceTier,
                    onTierSelected = onPriceTierChange,
                    currency = currency
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Purchase Location / Vendor with Search / Add on the fly
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchase Location / Vendor",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TextButton(onClick = onAddSupplierClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ New Vendor", fontSize = 12.sp, color = CellarGold)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = supplierDropdownExpanded,
                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = purchaseLocation,
                        onValueChange = onLocationChange,
                        label = { Text("Vendor / Wine Shop") },
                        placeholder = { Text("Search or type vendor name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    val matchingSuppliers = suppliers.filter {
                        it.name.contains(purchaseLocation, ignoreCase = true)
                    }
                    if (matchingSuppliers.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = supplierDropdownExpanded,
                            onDismissRequest = { supplierDropdownExpanded = false }
                        ) {
                            matchingSuppliers.forEach { supplier ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(supplier.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${supplier.type} • ${supplier.city ?: ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onLocationChange(supplier.name)
                                        supplierDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = purchaseDate,
                onValueChange = onDateChange,
                label = { Text("Purchase Date") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tasting Rating 1-5 Stars
            Text(text = "Tasting Rating (1 to 5 Stars)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            RatingBar(
                rating = tastingRating,
                onRatingChanged = onRatingChange,
                starSize = 30.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tasting Comments
            OutlinedTextField(
                value = comments,
                onValueChange = onCommentsChange,
                label = { Text("Tasting Comments / Cellaring Notes") },
                placeholder = { Text("Aromas, tannin structure, peak drinking window...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = name.isNotBlank() && vintageText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("wizard_next_to_location_button")
        ) {
            Text("Next: Choose Location (Final Step)")
        }
    }
}

@Composable
fun WizardStepLocation(
    cellarLocations: List<CellarLocation>,
    selectedLocationId: String?,
    selectedRow: Int?,
    selectedCol: Int?,
    onSelectLocation: (String?, Int?, Int?) -> Unit,
    onCreateNewLocation: () -> Unit,
    onFinish: () -> Unit
) {
    var chosenLocId by remember { mutableStateOf(selectedLocationId) }
    var chosenRow by remember { mutableStateOf(selectedRow ?: 0) }
    var chosenCol by remember { mutableStateOf(selectedCol ?: 0) }

    val activeLocation = cellarLocations.find { it.id == chosenLocId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Step 4: Cellar Placement",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose an existing rack position, create a new cellar rack, or select None (unassigned).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 1: None (Not in cellar)
            Card(
                onClick = {
                    chosenLocId = null
                    onSelectLocation(null, null, null)
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (chosenLocId == null) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("choose_location_none_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = chosenLocId == null,
                        onClick = {
                            chosenLocId = null
                            onSelectLocation(null, null, null)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "None (Not yet stored in cellar)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Store in inventory as unassigned bottle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Defined Locations
            Text(
                text = "Or choose a cellar location & shelf slot:",
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(6.dp))

            cellarLocations.forEach { loc ->
                val isSelected = chosenLocId == loc.id
                Card(
                    onClick = {
                        chosenLocId = loc.id
                        onSelectLocation(loc.id, chosenRow, chosenCol)
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) WineBurgundy else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("choose_location_${loc.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                chosenLocId = loc.id
                                onSelectLocation(loc.id, chosenRow, chosenCol)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = loc.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val tempStr = if (loc.temperatureCelsius != null) " • ${LocaleFormat.formatDecimal(loc.temperatureCelsius)}°C" else ""
                            Text(
                                text = "${loc.rows} rows × ${loc.columns} slots$tempStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 3: Create New Location button
            OutlinedButton(
                onClick = onCreateNewLocation,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_create_new_location_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CellarGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Cellar Location", color = MaterialTheme.colorScheme.onSurface)
            }

            // Slot coordinates selector if a location is chosen
            if (activeLocation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Specific Slot Assignment in ${activeLocation.name}:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = CellarGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Row (1-${activeLocation.rows})", style = MaterialTheme.typography.labelSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (chosenRow > 0) chosenRow-- }) { Text("-") }
                                    Text("Row ${chosenRow + 1}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { if (chosenRow < activeLocation.rows - 1) chosenRow++ }) { Text("+") }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Slot (1-${activeLocation.columns})", style = MaterialTheme.typography.labelSmall)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (chosenCol > 0) chosenCol-- }) { Text("-") }
                                    Text("Slot ${chosenCol + 1}", fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { if (chosenCol < activeLocation.columns - 1) chosenCol++ }) { Text("+") }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (chosenLocId != null) {
                    onSelectLocation(chosenLocId, chosenRow, chosenCol)
                } else {
                    onSelectLocation(null, null, null)
                }
                onFinish()
            },
            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("finish_add_wine_button")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Complete & Save Wine Entry")
        }
    }
}

// --- MANUAL ENTRY SINGLE FORM ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualWineEntryForm(
    name: String,
    onNameChange: (String) -> Unit,
    vintageText: String,
    onVintageChange: (String) -> Unit,
    country: String,
    onCountryChange: (String) -> Unit,
    region: String,
    onRegionChange: (String) -> Unit,
    assignedCountries: Set<String> = emptySet(),
    selectedGrapes: List<String>,
    onGrapesChange: (List<String>) -> Unit,
    availableGrapes: List<String> = emptyList(),
    usedGrapes: Set<String> = emptySet(),
    onNewGrapeEntered: (String) -> Unit = {},
    producer: String,
    onProducerChange: (String) -> Unit,
    purchaseDate: String,
    onDateChange: (String) -> Unit,
    alcoholText: String,
    onAlcoholChange: (String) -> Unit,
    sizeText: String,
    onSizeChange: (String) -> Unit,
    tastingRating: Int?,
    onRatingChange: (Int) -> Unit,
    comments: String,
    onCommentsChange: (String) -> Unit,
    purchasePriceText: String,
    onPriceChange: (String) -> Unit,
    currency: String = LocaleFormat.currency.currencyCode,
    onCurrencyChange: (String) -> Unit = {},
    existingWines: List<Wine> = emptyList(),
    isPriceTierEnabled: Boolean = true,
    selectedPriceTier: WinePriceTier? = null,
    onPriceTierChange: (WinePriceTier?) -> Unit = {},
    purchaseLocation: String,
    onLocationChange: (String) -> Unit,
    suppliers: List<Supplier>,
    onAddSupplierClick: () -> Unit,
    photoUrl: String?,
    onPhotoClick: () -> Unit,
    barcode: String,
    onBarcodeScanClick: () -> Unit,
    onBarcodeChange: (String) -> Unit,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    wineType: String,
    onWineTypeChange: (String) -> Unit,
    cellarLocations: List<CellarLocation>,
    selectedLocationId: String?,
    selectedRow: Int?,
    selectedCol: Int?,
    onSelectLocation: (String?, Int?, Int?) -> Unit,
    onCreateNewLocation: () -> Unit,
    onSave: () -> Unit
) {
    var chosenLocId by remember { mutableStateOf(selectedLocationId) }
    var chosenRow by remember { mutableStateOf(selectedRow ?: 0) }
    var chosenCol by remember { mutableStateOf(selectedCol ?: 0) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val activeLoc = cellarLocations.find { it.id == chosenLocId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Wine Details",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Wine Type Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(WineType.RED, WineType.WHITE, WineType.SPARKLING, WineType.ROSE).forEach { type ->
                FilterChip(
                    selected = wineType == type.name,
                    onClick = { onWineTypeChange(type.name) },
                    label = { Text(type.displayName.split(" ")[0]) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WineBurgundyLight,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Mandatory fields
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Wine Name * (Mandatory)") },
            placeholder = { Text("e.g. Château Margaux") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manual_name_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = vintageText,
                onValueChange = onVintageChange,
                label = { Text("Vintage * (Mandatory)") },
                placeholder = { Text("2018") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("manual_vintage_input")
            )
            OutlinedTextField(
                value = quantityText,
                onValueChange = onQuantityChange,
                label = { Text("Bottle Count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Producer
        OutlinedTextField(
            value = producer,
            onValueChange = onProducerChange,
            label = { Text("Producer") },
            placeholder = { Text("e.g. Domaine de la Romanée-Conti") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Varietal / Grape Tags with Autocomplete Dropdown
        GrapeTagInput(
            selectedGrapes = selectedGrapes,
            onGrapesChange = onGrapesChange,
            availableGrapes = availableGrapes,
            usedGrapes = usedGrapes,
            onNewGrapeEntered = onNewGrapeEntered,
            label = "Varietal / Grape Blend",
            placeholder = "Type to search or add grape...",
            testTag = "manual_grape_tag_input"
        )

        // Country Dropdown & Region (Split into 2 fields)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CountryDropdownSelect(
                selectedCountry = country,
                onCountrySelected = onCountryChange,
                assignedCountries = assignedCountries,
                label = "Country",
                modifier = Modifier.weight(1.2f),
                testTag = "manual_wine_country_input"
            )
            OutlinedTextField(
                value = region,
                onValueChange = onRegionChange,
                label = { Text("Region") },
                placeholder = { Text("e.g. Bordeaux, Rioja") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_wine_region_input")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottle Size & Alcohol %
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = sizeText,
                onValueChange = onSizeChange,
                label = { Text("Bottle Size") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = alcoholText,
                onValueChange = onAlcoholChange,
                label = { Text("Alcohol %") },
                placeholder = { Text("e.g. ${LocaleFormat.formatDecimal(13.5)}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Purchase Price
        OutlinedTextField(
            value = purchasePriceText,
            onValueChange = onPriceChange,
            label = { Text("Purchase Price ($currency)") },
            placeholder = { Text("e.g. ${LocaleFormat.example(75.0)}") },
            leadingIcon = { CurrencyPicker(currencyCode = currency, onCurrencyChange = onCurrencyChange) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (isPriceTierEnabled) {
            PriceRangeSuggestionSelector(
                purchasePrice = LocaleFormat.parseDecimal(purchasePriceText),
                existingWines = existingWines,
                selectedTier = selectedPriceTier,
                onTierSelected = onPriceTierChange,
                currency = currency
            )
        }

        // Barcode with quick scan trigger
        OutlinedTextField(
            value = barcode,
            onValueChange = onBarcodeChange,
            label = { Text("Barcode") },
            placeholder = { Text("Scan or enter barcode") },
            trailingIcon = {
                IconButton(onClick = onBarcodeScanClick) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = CellarGold)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Photo row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!photoUrl.isNullOrBlank()) {
                    WineBottleImage(
                        photoUrl = photoUrl,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        shape = RoundedCornerShape(8.dp),
                        iconSize = 24.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text("Bottle Photo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (!photoUrl.isNullOrBlank()) "Photo attached" else "No photo selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onPhotoClick) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = CellarGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (!photoUrl.isNullOrBlank()) "Change Photo" else "Add Photo")
            }
        }

        // Supplier with search & add on the fly
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Purchase Location / Vendor", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onAddSupplierClick) {
                    Text("+ Add Vendor on-the-fly", fontSize = 12.sp, color = CellarGold)
                }
            }

            ExposedDropdownMenuBox(
                expanded = supplierDropdownExpanded,
                onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = purchaseLocation,
                    onValueChange = onLocationChange,
                    label = { Text("Vendor / Wine Shop") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                )
                val matching = suppliers.filter { it.name.contains(purchaseLocation, ignoreCase = true) }
                if (matching.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = supplierDropdownExpanded,
                        onDismissRequest = { supplierDropdownExpanded = false }
                    ) {
                        matching.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    onLocationChange(s.name)
                                    supplierDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = purchaseDate,
            onValueChange = onDateChange,
            label = { Text("Purchase Date (YYYY-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Rating
        Text("Tasting Rating (1 to 5 Stars)", style = MaterialTheme.typography.labelMedium)
        RatingBar(rating = tastingRating, onRatingChanged = onRatingChange, starSize = 28.dp)

        OutlinedTextField(
            value = comments,
            onValueChange = onCommentsChange,
            label = { Text("Tasting Comments / Tasting Notes") },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Location Selector
        Text(
            text = "Cellar Placement",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // None option
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = chosenLocId == null,
                onClick = {
                    chosenLocId = null
                    onSelectLocation(null, null, null)
                }
            )
            Text("None (Not yet stored in cellar)")
        }

        cellarLocations.forEach { loc ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = chosenLocId == loc.id,
                    onClick = {
                        chosenLocId = loc.id
                        onSelectLocation(loc.id, chosenRow, chosenCol)
                    }
                )
                Text("${loc.name} (${loc.rows}x${loc.columns})")
            }
        }

        TextButton(onClick = onCreateNewLocation) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CellarGold)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Create New Cellar Location", color = CellarGold)
        }

        if (activeLoc != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = (chosenRow + 1).toString(),
                    onValueChange = { chosenRow = (it.toIntOrNull() ?: 1) - 1 },
                    label = { Text("Row (1-${activeLoc.rows})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = (chosenCol + 1).toString(),
                    onValueChange = { chosenCol = (it.toIntOrNull() ?: 1) - 1 },
                    label = { Text("Slot (1-${activeLoc.columns})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (chosenLocId != null) {
                    onSelectLocation(chosenLocId, chosenRow, chosenCol)
                } else {
                    onSelectLocation(null, null, null)
                }
                onSave()
            },
            enabled = name.isNotBlank() && vintageText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp)
                .testTag("save_manual_wine_button")
        ) {
            Text("Save Wine to Inventory")
        }
    }
}
