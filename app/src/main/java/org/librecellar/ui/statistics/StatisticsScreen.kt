package org.librecellar.ui.statistics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.StatGroup
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WineStatistics
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

/** Bars grow from a darker burgundy at the baseline to rosé at the value. */
private val BarBrushHorizontal = Brush.horizontalGradient(listOf(WineBurgundyLight.copy(alpha = 0.55f), WineRose))
private val BarBrushVertical = Brush.verticalGradient(listOf(WineRose, WineBurgundyLight.copy(alpha = 0.55f)))

private fun plural(n: Int, word: String, many: String = "${word}s") = "$n ${if (n == 1) word else many}"

/**
 * The web app's statistics: wines per country, price per bottle and vintages. Tapping a country or
 * a vintage opens the wine list filtered on it ([includeEmpty] tells whether empty wines count).
 */
@Composable
fun StatisticsScreen(
    wines: List<Wine>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onCountrySelected: (country: String, includeEmpty: Boolean) -> Unit,
    onVintageSelected: (vintage: Int, includeEmpty: Boolean) -> Unit
) {
    var countriesEmpty by rememberSaveable { mutableStateOf(false) }
    var pricesEmpty by rememberSaveable { mutableStateOf(false) }
    var vintagesEmpty by rememberSaveable { mutableStateOf(false) }

    val topPadding = maxOf(WindowInsets.statusBars.asPaddingValues().calculateTopPadding(), 28.dp)

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(topPadding))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("statistics_back_button")) {
                            Icon(imageVector = AppIcons.ArrowBack, contentDescription = "Back")
                        }
                        Column {
                            Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (isLoading) "Loading all wines…" else "Tap a country or vintage to see its wines",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HeadlineTiles(wines)

            // --- Wines per country ---
            val (countries, withoutCountry) = WineStatistics.byCountry(wines, countriesEmpty)
            ChartCard(
                title = "Wines per country",
                subtitle = plural(countries.size, "country", "countries") +
                    if (withoutCountry > 0) " · $withoutCountry without a country" else "",
                includeEmpty = countriesEmpty,
                onIncludeEmptyChange = { countriesEmpty = it },
                tag = "chart_countries"
            ) {
                if (countries.isEmpty()) {
                    EmptyChart("No wines with a country")
                } else {
                    HorizontalBars(countries) { onCountrySelected(it.key, countriesEmpty) }
                }
            }

            // --- Price per bottle ---
            val currencies = WineStatistics.priceCurrencies(wines, pricesEmpty)
            var currency by rememberSaveable { mutableStateOf<String?>(null) }
            LaunchedEffect(currencies) {
                if (currency !in currencies) currency = currencies.firstOrNull()
            }
            val selected = currency?.takeIf { it in currencies } ?: currencies.firstOrNull()
            val prices = selected?.let { WineStatistics.priceDistribution(wines, pricesEmpty, it) }
            val unpriced = WineStatistics.unpriced(wines, pricesEmpty)
            ChartCard(
                title = "Price per bottle",
                subtitle = listOfNotNull(
                    plural(prices?.count ?: 0, "wine"),
                    prices?.median?.let { "median ${LocaleFormat.formatPrice(it, prices.currency)}" },
                    if (unpriced > 0) "$unpriced without a price" else null
                ).joinToString(" · "),
                includeEmpty = pricesEmpty,
                onIncludeEmptyChange = { pricesEmpty = it },
                tag = "chart_prices"
            ) {
                if (currencies.size > 1) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        currencies.forEach { code ->
                            FilterChip(
                                selected = code == selected,
                                onClick = { currency = code },
                                label = { Text(code, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WineBurgundyLight,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
                if (prices == null || prices.count == 0) {
                    EmptyChart("No wines with a price")
                } else {
                    Columns(prices.buckets, columnWidth = 40.dp)
                }
            }

            // --- Vintages ---
            val vintages = WineStatistics.byVintage(wines, vintagesEmpty)
            ChartCard(
                title = "Vintages",
                subtitle = plural(vintages.count { it.wines.isNotEmpty() }, "vintage"),
                includeEmpty = vintagesEmpty,
                onIncludeEmptyChange = { vintagesEmpty = it },
                tag = "chart_vintages"
            ) {
                if (vintages.isEmpty()) {
                    EmptyChart("No wines")
                } else {
                    Columns(vintages, columnWidth = 30.dp, rotateLabels = true) { group ->
                        if (group.wines.isNotEmpty()) onVintageSelected(group.key, vintagesEmpty)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeadlineTiles(wines: List<Wine>) {
    val inStock = wines.filter { it.quantity > 0 }
    val countries = inStock.mapNotNull { WineStatistics.countryName(it.country) }.toSet().size
    val years = inStock.map { it.vintage }
    val tiles = listOf(
        Triple(AppIcons.WineBar, "Wines in stock", "${inStock.size}"),
        Triple(AppIcons.LocalDrink, "Bottles", "${inStock.sumOf { it.quantity }}"),
        Triple(AppIcons.Public, "Countries", "$countries"),
        Triple(
            AppIcons.CalendarToday,
            "Vintages",
            if (years.isEmpty()) "–" else "${years.min()}–${years.max()}"
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (icon, label, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(WineBurgundyLight.copy(alpha = 0.22f), Color.Transparent)
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = CellarGold, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    value,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    includeEmpty: Boolean,
    onIncludeEmptyChange: (Boolean) -> Unit,
    tag: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = includeEmpty,
                        onCheckedChange = onIncludeEmptyChange,
                        colors = SwitchDefaults.colors(checkedTrackColor = WineBurgundyLight),
                        modifier = Modifier.testTag("${tag}_include_empty")
                    )
                    Text("Include empty", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun EmptyChart(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

/** Grows from 0 to 1 when the chart (or its data) appears. */
@Composable
private fun rememberGrowth(key: Any): Float {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    return progress.value
}

/** Ranked bars with labels on the left; the whole row is the tap target. */
@Composable
private fun <K> HorizontalBars(groups: List<StatGroup<K>>, onSelect: (StatGroup<K>) -> Unit) {
    val max = groups.maxOf { it.wines.size }.coerceAtLeast(1)
    val growth = rememberGrowth(groups.map { it.key to it.wines.size })
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        groups.forEach { group ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(group) }
                    .padding(vertical = 5.dp, horizontal = 2.dp)
                    .testTag("bar_${group.key}")
            ) {
                Text(
                    group.label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(118.dp)
                )
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    val fraction = group.wines.size.toFloat() / max * growth
                    if (fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction * 0.88f)
                                .height(18.dp)
                                .clip(RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
                                .background(BarBrushHorizontal)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${group.wines.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Columns from a common baseline; scrolls sideways when they do not fit. */
@Composable
private fun <K> Columns(
    groups: List<StatGroup<K>>,
    columnWidth: Dp,
    rotateLabels: Boolean = false,
    chartHeight: Dp = 170.dp,
    onSelect: ((StatGroup<K>) -> Unit)? = null
) {
    val max = groups.maxOf { it.wines.size }.coerceAtLeast(1)
    val growth = rememberGrowth(groups.map { it.key to it.wines.size })
    val scroll = rememberScrollState()
    // Start at the newest values (the right end) when the chart scrolls
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groups.forEach { group ->
            val count = group.wines.size
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(columnWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (onSelect != null) Modifier.clickable { onSelect(group) } else Modifier)
                    .testTag("column_${group.key}")
            ) {
                Box(
                    modifier = Modifier
                        .height(chartHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (count > 0) {
                            Text("$count", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        val barHeight = (chartHeight - 18.dp) * (count.toFloat() / max * growth)
                        Box(
                            modifier = Modifier
                                .width(minOf(columnWidth - 8.dp, 22.dp))
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(BarBrushVertical)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                Text(
                    text = if (rotateLabels) "'${group.label.takeLast(2)}" else group.label,
                    fontSize = if (rotateLabels) 10.sp else 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    if (groups.size * (columnWidth.value + 4) > 320) {
        Text(
            "Swipe sideways for more",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * The wine list's summary line, doubling as the way into the statistics: the stock figures next
 * to a small vintage histogram, so the bottom bar needs no extra tab.
 */
@Composable
fun InsightsStrip(wines: List<Wine>, summary: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val vintages = WineStatistics.byVintage(wines, includeEmpty = false).takeLast(24)
    val max = vintages.maxOfOrNull { it.wines.size }?.coerceAtLeast(1) ?: 1
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.testTag("insights_strip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (vintages.size > 1) {
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier.height(18.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    vintages.forEach { v ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((18f * v.wines.size / max).coerceAtLeast(1.5f).dp)
                                .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                                .background(if (v.wines.isEmpty()) Color.Transparent else WineRose)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(AppIcons.BarChart, contentDescription = "Statistics", tint = CellarGold, modifier = Modifier.size(16.dp))
            Icon(AppIcons.ArrowForward, contentDescription = null, tint = CellarGold, modifier = Modifier.size(12.dp))
        }
    }
}
