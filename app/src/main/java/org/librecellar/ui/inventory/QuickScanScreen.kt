package org.librecellar.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.Wine
import org.librecellar.ui.components.BarcodeScannerDialog
import org.librecellar.ui.components.RatingBar
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickScanScreen(
    wines: List<Wine>,
    locations: List<CellarLocation>,
    onDecrementQuantity: (String) -> Unit,
    onWineSelected: (Wine) -> Unit
) {
    var scannedBarcode by remember { mutableStateOf("") }
    var showLiveCameraScanner by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Prefer the user's own bottle, then one of an editable shared collection
    val matchedWine = wines.filter {
        !it.barcode.isNullOrBlank() && it.barcode.trim() == scannedBarcode.trim()
    }.minByOrNull { it.collectionAccess.ordinal }

    val matchedLocation = locations.find { it.id == matchedWine?.locationId }

    // Scanner laser animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Cellar wines with barcodes for quick testing
    val inventoryBarcodes = wines.filter { !it.barcode.isNullOrBlank() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = CellarGold,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Quick Barcode Inventory",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Scan bottle barcode to look up position and tick off inventory",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Viewfinder Camera Simulator / Launcher
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF100B0E))
                    .border(2.dp, CellarGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .clickable { showLiveCameraScanner = true }
                    .testTag("quick_scan_viewfinder_box"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(95.dp)
                        .border(1.dp, WineRose.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CellarGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to open Live Camera Barcode Scanner",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "CameraX & ML Kit auto-detection",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Laser scan line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(2.5.dp)
                        .offset(y = (-45 + laserOffset).dp)
                        .background(CellarGold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Live Camera Scan Button
            Button(
                onClick = { showLiveCameraScanner = true },
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_live_camera_scanner_btn")
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = CellarGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Bottle with Live Camera", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barcode Input
            OutlinedTextField(
                value = scannedBarcode,
                onValueChange = { scannedBarcode = it },
                label = { Text("Scanned Barcode / EAN") },
                placeholder = { Text("e.g. 3421580001015") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (scannedBarcode.isNotBlank()) {
                            IconButton(onClick = { scannedBarcode = "" }) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall, color = WineRose)
                            }
                        }
                        IconButton(
                            onClick = { showLiveCameraScanner = true },
                            modifier = Modifier.testTag("quick_scan_camera_btn")
                        ) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = CellarGold)
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_scan_barcode_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Tap Barcode from In-Stock Cellar Wines
            Text(
                text = "Tap to test with cellar bottle barcodes:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                inventoryBarcodes.forEach { wine ->
                    val isSelected = wine.barcode == scannedBarcode
                    SuggestionChip(
                        onClick = { scannedBarcode = wine.barcode ?: "" },
                        label = {
                            Text(
                                text = "${wine.name} (${wine.vintage})",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) WineBurgundyLight else MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Matched Wine Dossier Result
            if (matchedWine != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CellarGold, RoundedCornerShape(16.dp))
                        .testTag("matched_wine_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = CellarGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "BOTTLE FOUND",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CellarGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = matchedWine.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Vintage ${matchedWine.vintage} • ${matchedWine.producer ?: "Estate bottle"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(WineBurgundy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WineBar,
                                    contentDescription = null,
                                    tint = CellarGold,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cellar Shelf Slot Highlighting
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CellarGold,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (matchedLocation != null) matchedLocation.name else "Unassigned Location",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Shelf Position: ${matchedWine.slotLabel ?: "Not slotted"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CellarGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Rating & Quantity info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (matchedWine.tastingRating != null) {
                                RatingBar(rating = matchedWine.tastingRating, starSize = 18.dp)
                            }
                            Text(
                                text = "Current Stock: ${matchedWine.quantity} bottles",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (matchedWine.quantity > 0) CellarGold else Color.Red
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // THE MAIN ACTION: Tick off bottle easily to update inventory!
                        Button(
                            onClick = {
                                onDecrementQuantity(matchedWine.id)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Ticked off 1 bottle! Remaining stock: ${maxOf(0, matchedWine.quantity - 1)}"
                                    )
                                }
                            },
                            enabled = matchedWine.quantity > 0 && !matchedWine.isReadOnly,
                            colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("tick_off_bottle_action_btn")
                        ) {
                            Icon(imageVector = Icons.Default.LocalDrink, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tick Off Bottle (-1 from Inventory)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { onWineSelected(matchedWine) },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View Full Wine Dossier", color = CellarGold)
                        }
                    }
                }
            } else if (scannedBarcode.isNotBlank()) {
                // Not found
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No wine found for barcode: $scannedBarcode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "This bottle is not yet registered in your cellar inventory.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Live Barcode Scanner Dialog
        if (showLiveCameraScanner) {
            BarcodeScannerDialog(
                initialBarcode = scannedBarcode,
                onBarcodeScanned = { barcode ->
                    scannedBarcode = barcode
                    showLiveCameraScanner = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Scanned barcode: $barcode")
                    }
                },
                onDismiss = { showLiveCameraScanner = false }
            )
        }
    }
}
