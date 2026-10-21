package org.librecellar.ui.wines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.CountryHelper
import androidx.compose.ui.layout.ContentScale
import org.librecellar.data.model.Wine
import org.librecellar.ui.components.GrapeBadge
import org.librecellar.ui.components.GrapeBadgesFlow
import org.librecellar.ui.components.RatingBar
import org.librecellar.ui.components.SharedEntryBadge
import org.librecellar.ui.components.WineBottleImage
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

@Composable
fun WineDetailScreen(
    wine: Wine,
    locations: List<CellarLocation>,
    allWines: List<Wine> = emptyList(),
    isPriceTierEnabled: Boolean = true,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val location = locations.find { it.id == wine.locationId }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarPadding = maxOf(statusBarTop, 28.dp)

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(topBarPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("wine_detail_back_button")) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    Text(
                        text = "Wine Dossier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (wine.isReadOnly) {
                            // Keeps the title centered where edit & delete would be
                            Spacer(modifier = Modifier.width(96.dp))
                        } else {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.testTag("edit_wine_button")
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Wine", tint = CellarGold)
                            }
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.testTag("delete_wine_button")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Wine", tint = WineRose)
                            }
                        }
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
                .padding(16.dp)
        ) {
            // Hero Bottle Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!wine.photoUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1418))
                                .border(1.5.dp, CellarGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            WineBottleImage(
                                photoUrl = wine.photoUrl,
                                wineType = wine.wineType,
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(12.dp),
                                contentScale = ContentScale.Fit,
                                iconSize = 64.dp,
                                contentDescription = "${wine.name} bottle photo"
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(WineBurgundy)
                                .border(2.dp, CellarGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WineBar,
                                contentDescription = null,
                                tint = CellarGold,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = wine.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Vintage ${wine.vintage}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CellarGold
                    )

                    if (!wine.producer.isNullOrBlank()) {
                        Text(
                            text = wine.producer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Price Range Tier Badge
                    if (isPriceTierEnabled) {
                        val tier = wine.getResolvedPriceTier(allWines)
                        val tierColor = try {
                            Color(android.graphics.Color.parseColor(tier.colorHex))
                        } catch (e: Exception) {
                            CellarGold
                        }
                        Surface(
                            color = tierColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tierColor.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .testTag("detail_price_tier_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tier.symbolFor(wine.currency)} ${tier.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor
                                )
                                if (wine.priceTier != null) {
                                    Text(
                                        text = " (Custom)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = tierColor.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // Out of stock warning banner
                    if (wine.quantity == 0) {
                        Surface(
                            color = WineRose.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .testTag("detail_out_of_stock_badge")
                        ) {
                            Text(
                                text = "OUT OF STOCK / DEPLETED",
                                color = WineRose,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (wine.tastingRating != null && wine.tastingRating > 0) {
                        RatingBar(rating = wine.tastingRating, starSize = 24.dp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Inventory Controls
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            IconButton(
                                onClick = onDecrement,
                                enabled = wine.quantity > 0 && !wine.isReadOnly,
                                modifier = Modifier.testTag("detail_decrement_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = "Consume 1 bottle", tint = WineRose)
                            }

                            Text(
                                text = "${wine.quantity} Bottles in Stock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (wine.quantity == 0) WineRose else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            IconButton(
                                onClick = onIncrement,
                                enabled = !wine.isReadOnly,
                                modifier = Modifier.testTag("detail_increment_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add 1 bottle", tint = CellarGold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SharedEntryBadge(
                        entry = wine,
                        showWritable = true,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("detail_shared_badge")
                    )

                    if (!wine.isReadOnly) OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_edit_wine_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CellarGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Wine Details & Specs",
                            color = CellarGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cellar Placement Details
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, tint = CellarGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cellar Placement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (location != null) "Location: ${location.name}" else "Location: None (Not in cellar)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Shelf Slot: ${wine.slotLabel ?: "Unassigned slot"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (wine.slotLabel != null) CellarGold else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (location != null) {
                        val climateInfo = buildList {
                            location.temperatureCelsius?.let { add("${LocaleFormat.formatDecimal(it)}°C") }
                            location.humidityPercent?.let { add("${LocaleFormat.formatDecimal(it)}% humidity") }
                        }
                        if (climateInfo.isNotEmpty()) {
                            Text(
                                text = "Rack Climate: ${climateInfo.joinToString(" • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wine Characteristics & Specs Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Wine Properties & Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val countryFlag = CountryHelper.getFlagForCountry(wine.country)
                    val originLocation = when {
                        !wine.country.isNullOrBlank() && !wine.region.isNullOrBlank() ->
                            "$countryFlag ${wine.country} • ${wine.region}"
                        !wine.country.isNullOrBlank() ->
                            "$countryFlag ${wine.country}"
                        !wine.region.isNullOrBlank() ->
                            wine.region
                        else -> null
                    }
                    originLocation?.let {
                        WineDetailRow(label = "Country / Region", value = it, icon = Icons.Default.Public)
                    }
                    wine.varietal?.let {
                        WineDetailRow(label = "Varietal / Blend", value = it, icon = Icons.Default.WineBar)
                        if (wine.varietalList.isNotEmpty()) {
                            GrapeBadgesFlow(
                                grapes = wine.varietalList,
                                modifier = Modifier.padding(start = 28.dp, top = 2.dp, bottom = 4.dp)
                            )
                        }
                    }
                    wine.size?.let {
                        WineDetailRow(label = "Bottle Size", value = it, icon = Icons.Default.LocalDrink)
                    }
                    wine.alcoholPercent?.let {
                        WineDetailRow(label = "Alcohol Percentage", value = "${LocaleFormat.formatDecimal(it)} %", icon = Icons.Default.Percent)
                    }
                    wine.purchasePrice?.let { price ->
                        val tierSuffix = if (isPriceTierEnabled) {
                            val tier = wine.getResolvedPriceTier(allWines)
                            " • ${tier.displayName} (${tier.symbolFor(wine.currency)})"
                        } else ""
                        WineDetailRow(
                            label = "Purchase Price",
                            value = LocaleFormat.formatPrice(price, wine.currency) + tierSuffix,
                            icon = Icons.Default.Payments
                        )
                    }
                    wine.purchaseLocation?.let {
                        WineDetailRow(label = "Purchase Location / Vendor", value = it, icon = Icons.Default.Storefront)
                    }
                    wine.purchaseDate?.let {
                        WineDetailRow(label = "Purchase Date", value = it, icon = Icons.Default.Event)
                    }
                    wine.barcode?.let {
                        WineDetailRow(label = "Bottle Barcode", value = it, icon = Icons.Default.QrCode, isMono = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tasting Notes & Comments
            if (!wine.comments.isNullOrBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Tasting Notes & Cellar Comments",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = wine.comments,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Quick Tick Off Bottle Button
            Button(
                onClick = onDecrement,
                enabled = wine.quantity > 0 && !wine.isReadOnly,
                colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_tick_off_bottle_button")
            ) {
                Icon(imageVector = Icons.Default.LocalDrink, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Drink Bottle / Tick Off (-1)")
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Wine Entry?") },
            text = { Text("Are you sure you want to remove '${wine.name} (${wine.vintage})' from your cellar collection?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WineBurgundyLight)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WineDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CellarGold,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}
