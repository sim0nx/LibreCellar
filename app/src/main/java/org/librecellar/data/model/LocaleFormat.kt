package org.librecellar.data.model

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Number and currency formatting that follows the device's locale settings
 * (decimal separator, digit grouping, currency) instead of US conventions.
 * Values are always stored and sent to the backend as plain numbers.
 */
object LocaleFormat {

    private val locale: Locale
        get() = Locale.getDefault()

    /** Currency of the device's region (e.g. EUR for Luxembourg), falling back to the locale's default. */
    val currency: Currency
        get() = try {
            Currency.getInstance(locale)
        } catch (e: IllegalArgumentException) {
            // Locale without a region, e.g. plain "en"
            NumberFormat.getCurrencyInstance(locale).currency ?: Currency.getInstance("USD")
        }

    /** Currency symbol as used in the device's locale, e.g. "€", "$" or "CHF". */
    val currencySymbol: String
        get() = currency.getSymbol(locale)

    /** The currency for an ISO 4217 [code], or the device's currency if it is null or unknown. */
    fun currencyOf(code: String?): Currency =
        code?.let { runCatching { Currency.getInstance(it) }.getOrNull() } ?: currency

    /** Symbol of the currency with ISO 4217 [code] (device currency if null), e.g. "€". */
    fun currencySymbol(code: String?): String = currencyOf(code).getSymbol(locale)

    /** Currencies offered when entering a price: the device's and common ones first, then all others. */
    fun selectableCurrencies(): List<Currency> {
        val preferred = (listOf(currency.currencyCode) + COMMON_CURRENCIES)
            .distinct()
            .mapNotNull { runCatching { Currency.getInstance(it) }.getOrNull() }
        val others = Currency.getAvailableCurrencies()
            .filter { it !in preferred }
            .sortedBy { it.currencyCode }
        return preferred + others
    }

    private val COMMON_CURRENCIES = listOf("EUR", "USD", "GBP", "CHF")

    /** The device's decimal separator, e.g. ',' or '.'. */
    val decimalSeparator: Char
        get() = java.text.DecimalFormatSymbols.getInstance(locale).decimalSeparator

    /**
     * Parses a user-entered decimal number. Accepts the device's decimal separator as well as
     * '.' or ',' typed by habit; returns null for anything that is not a number.
     */
    fun parseDecimal(text: String): Double? {
        val cleaned = text.trim().replace(" ", "").replace(" ", "")
        if (cleaned.isEmpty()) return null
        val separators = cleaned.count { it == '.' || it == ',' }
        val normalized = when {
            separators == 0 -> cleaned
            // A single separator is the decimal separator, whichever one was typed
            separators == 1 -> cleaned.replace(',', '.')
            else -> {
                // Grouping and decimal separators mixed (e.g. "1.234,50"): the last one is decimal
                val last = cleaned.indexOfLast { it == '.' || it == ',' }
                cleaned.substring(0, last).replace(".", "").replace(",", "") + "." +
                        cleaned.substring(last + 1)
            }
        }
        return normalized.toDoubleOrNull()
    }

    /** Formats a number for an input field: locale decimal separator, no grouping. */
    fun formatDecimalInput(value: Double?): String {
        if (value == null) return ""
        return NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }.format(value)
    }

    /** Formats a number for display, e.g. alcohol 13,5 or temperature 12,5. */
    fun formatDecimal(value: Double, maxFractionDigits: Int = 1): String =
        NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = maxFractionDigits
            minimumFractionDigits = 0
        }.format(value)

    /**
     * Formats a price in the currency with ISO 4217 [currencyCode] (device currency if null) using the
     * device's number conventions, e.g. "45,00 €" or "$45.00".
     */
    fun formatPrice(value: Double, currencyCode: String? = null): String =
        NumberFormat.getCurrencyInstance(locale).apply { currency = currencyOf(currencyCode) }
            .format(value)

    /** A price without cents, for chart labels, e.g. "15 €" or "$15". */
    fun formatWholePrice(value: Double, currencyCode: String? = null): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = currencyOf(currencyCode)
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }.format(value)

    /** Example value for placeholders, e.g. "45,00" or "45.00". */
    fun example(value: Double): String =
        NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed = false
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(value)

    /**
     * Short symbol for compact places: drops region letters a locale adds to tell currencies apart
     * ("£GB" -> "£", "US$" -> "$"); letter-only symbols such as "CHF" stay as they are.
     */
    fun shortCurrencySymbol(code: String?): String {
        val symbol = currencySymbol(code)
        return symbol.filterNot { it.isLetter() }.ifBlank { symbol }
    }

    /** Price tier marking built from a currency (device currency if null), e.g. "€€€" (or "CHF×3"). */
    fun tierSymbol(level: Int, currencyCode: String? = null): String {
        val symbol = shortCurrencySymbol(currencyCode)
        return if (symbol.length == 1) symbol.repeat(level) else "$symbol×$level"
    }
}
