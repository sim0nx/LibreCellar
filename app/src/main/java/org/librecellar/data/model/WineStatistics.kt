package org.librecellar.data.model

/** Wines grouped for a bar of a statistics chart. */
data class StatGroup<K>(val key: K, val label: String, val wines: List<Wine>) {
    val bottles: Int get() = wines.sumOf { it.quantity.coerceAtLeast(0) }
}

/**
 * The statistics of the web app's statistics page, computed on the device from the wines the
 * server sent. Wines without bottles left are left out unless `includeEmpty` is set.
 */
object WineStatistics {

    private fun considered(wines: List<Wine>, includeEmpty: Boolean) =
        wines.filter { includeEmpty || it.quantity > 0 }

    /** Same country however it is written (code or alias): its English name. */
    fun countryName(country: String?): String? {
        val trimmed = country?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        return CountryHelper.findCountry(trimmed)?.name ?: trimmed
    }

    /** Most wines first; wines without a country are counted separately. */
    fun byCountry(wines: List<Wine>, includeEmpty: Boolean): Pair<List<StatGroup<String>>, Int> {
        val list = considered(wines, includeEmpty)
        val withoutCountry = list.count { countryName(it.country) == null }
        val groups = list.filter { countryName(it.country) != null }
            .groupBy { countryName(it.country)!! }
            .map { (name, group) ->
                val flag = CountryHelper.findCountry(name)?.flagEmoji ?: "🌐"
                StatGroup(name, "$flag $name", group)
            }
            .sortedWith(compareByDescending<StatGroup<String>> { it.wines.size }.thenBy { it.key })
        return groups to withoutCountry
    }

    /** Every year from the oldest to the newest vintage, so gaps show as gaps. */
    fun byVintage(wines: List<Wine>, includeEmpty: Boolean): List<StatGroup<Int>> {
        val list = considered(wines, includeEmpty)
        if (list.isEmpty()) return emptyList()
        val byYear = list.groupBy { it.vintage }
        return (list.minOf { it.vintage }..list.maxOf { it.vintage }).map { year ->
            StatGroup(year, year.toString(), byYear[year].orEmpty())
        }
    }

    private val PRICE_EDGES = listOf(0, 5, 10, 15, 20, 30, 50, 75, 100, 150, 250)

    data class PriceRange(val from: Int, val to: Int?)

    data class PriceDistribution(
        val currency: String,
        val buckets: List<StatGroup<PriceRange>>,
        val median: Double?,
        val count: Int
    )

    /** Currencies of the priced wines, the most used first: prices only compare within one. */
    fun priceCurrencies(wines: List<Wine>, includeEmpty: Boolean): List<String> =
        considered(wines, includeEmpty).filter { it.purchasePrice != null }
            .groupingBy { it.effectiveCurrency }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }

    fun unpriced(wines: List<Wine>, includeEmpty: Boolean): Int =
        considered(wines, includeEmpty).count { it.purchasePrice == null }

    fun priceDistribution(wines: List<Wine>, includeEmpty: Boolean, currency: String): PriceDistribution {
        val priced = considered(wines, includeEmpty)
            .filter { it.purchasePrice != null && it.effectiveCurrency == currency }
        val perBucket = PRICE_EDGES.indices.map { mutableListOf<Wine>() }
        for (wine in priced) {
            val i = PRICE_EDGES.indexOfLast { wine.purchasePrice!! >= it }.coerceAtLeast(0)
            perBucket[i] += wine
        }
        // Up to the most expensive wine, at least up to 30 so a cheap cellar still has a scale
        val last = maxOf(perBucket.indexOfLast { it.isNotEmpty() }, PRICE_EDGES.indexOf(20))
        val buckets = (0..last).map { i ->
            val range = PriceRange(PRICE_EDGES[i], PRICE_EDGES.getOrNull(i + 1))
            val label = if (range.to == null) {
                "${LocaleFormat.formatWholePrice(range.from.toDouble(), currency)}+"
            } else {
                "${LocaleFormat.formatWholePrice(range.from.toDouble(), currency)}–" +
                    LocaleFormat.formatWholePrice(range.to.toDouble(), currency)
            }
            StatGroup(range, label, perBucket[i])
        }
        val sorted = priced.map { it.purchasePrice!! }.sorted()
        val median = if (sorted.isEmpty()) null else sorted[(sorted.size - 1) / 2]
        return PriceDistribution(currency, buckets, median, priced.size)
    }
}
