package org.librecellar

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.librecellar.data.model.LocaleFormat
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WinePriceTier
import java.util.Locale

class LocaleFormatTest {

    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun parsesCommaAndDotDecimals() {
        Locale.setDefault(Locale.forLanguageTag("fr-LU"))
        assertEquals(12.5, LocaleFormat.parseDecimal("12,5")!!, 0.0001)
        assertEquals(12.5, LocaleFormat.parseDecimal(" 12.5 ")!!, 0.0001)
        assertEquals(1234.5, LocaleFormat.parseDecimal("1.234,50")!!, 0.0001)
        assertEquals(1234.5, LocaleFormat.parseDecimal("1,234.50")!!, 0.0001)
        assertEquals(45.0, LocaleFormat.parseDecimal("45")!!, 0.0001)
        assertNull(LocaleFormat.parseDecimal(""))
        assertNull(LocaleFormat.parseDecimal("abc"))
    }

    @Test
    fun formatsWithDeviceSeparatorAndCurrency() {
        Locale.setDefault(Locale.forLanguageTag("fr-LU"))
        assertEquals("EUR", LocaleFormat.currency.currencyCode)
        assertEquals("13,5", LocaleFormat.formatDecimalInput(13.5))
        assertEquals("€€€", LocaleFormat.tierSymbol(3))
        assertEquals(true, LocaleFormat.formatPrice(45.0).contains("45,00"))
        assertEquals(true, LocaleFormat.formatPrice(45.0).contains("€"))

        Locale.setDefault(Locale.US)
        assertEquals("USD", LocaleFormat.currency.currencyCode)
        assertEquals("13.5", LocaleFormat.formatDecimalInput(13.5))
        assertEquals("$45.00", LocaleFormat.formatPrice(45.0))
    }

    @Test
    fun formatsPricesInTheWinesOwnCurrency() {
        Locale.setDefault(Locale.forLanguageTag("fr-LU"))
        assertEquals(true, LocaleFormat.formatPrice(45.0, "GBP").contains("£"))
        assertEquals("£", LocaleFormat.shortCurrencySymbol("GBP"))
        assertEquals("CHF", LocaleFormat.shortCurrencySymbol("CHF"))
        assertEquals("EUR", LocaleFormat.currencyOf(null).currencyCode)
        assertEquals("EUR", LocaleFormat.currencyOf("not-a-code").currencyCode)
        assertEquals("£££", LocaleFormat.tierSymbol(3, "GBP"))
        assertEquals("EUR", LocaleFormat.selectableCurrencies().first().currencyCode)
    }

    @Test
    fun priceTiersOnlyCompareWinesOfTheSameCurrency() {
        Locale.setDefault(Locale.forLanguageTag("fr-LU"))
        val eur = (1..5).map { Wine(name = "e$it", vintage = 2020, purchasePrice = it * 10.0, currency = "EUR") }
        val gbp = (1..5).map { Wine(name = "g$it", vintage = 2020, purchasePrice = it * 1000.0, currency = "GBP") }
        // 50 is the most expensive EUR wine; the far pricier GBP wines are not compared
        assertEquals(
            WinePriceTier.VERY_EXPENSIVE,
            WinePriceTier.calculateDynamicTier(50.0, eur + gbp, "EUR")
        )
    }
}
