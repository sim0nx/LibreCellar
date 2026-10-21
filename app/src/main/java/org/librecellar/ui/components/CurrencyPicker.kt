package org.librecellar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.librecellar.data.model.LocaleFormat
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import java.util.Locale

/**
 * Currency selector shown as the leading element of a price field: the currency symbol,
 * tapping it lists the device's and common currencies first, then all others.
 */
@Composable
fun CurrencyPicker(
    currencyCode: String,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = remember { LocaleFormat.selectableCurrencies() }
    val preferredCount = remember(currencies) {
        currencies.indexOfFirst { it.currencyCode !in setOf(LocaleFormat.currency.currencyCode, "EUR", "USD", "GBP", "CHF") }
            .takeIf { it > 0 } ?: currencies.size
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { expanded = true }
            .padding(start = 12.dp, end = 2.dp)
            .testTag("currency_picker")
    ) {
        Text(LocaleFormat.shortCurrencySymbol(currencyCode), fontWeight = FontWeight.Bold, color = CellarGold)
        Icon(AppIcons.ArrowDropDown, contentDescription = "Change currency", tint = CellarGold, modifier = Modifier.size(18.dp))
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        currencies.forEachIndexed { index, currency ->
            if (index == preferredCount) HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        "${currency.currencyCode}  ${currency.getSymbol(Locale.getDefault())}  ·  " +
                                currency.getDisplayName(Locale.getDefault()),
                        fontWeight = if (currency.currencyCode == currencyCode) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onCurrencyChange(currency.currencyCode)
                    expanded = false
                }
            )
        }
    }
}
