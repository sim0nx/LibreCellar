package org.librecellar

import org.junit.Assert.assertEquals
import org.junit.Test
import org.librecellar.data.model.Wine
import org.librecellar.data.model.WineStatistics

class WineStatisticsTest {

    private fun wine(
        vintage: Int,
        country: String? = null,
        quantity: Int = 1,
        price: Double? = null,
        currency: String? = "EUR"
    ) = Wine(name = "Wine $vintage", vintage = vintage, country = country, quantity = quantity,
        purchasePrice = price, currency = currency)

    @Test
    fun countriesMergeSpellingsAndHideEmptyWines() {
        val wines = listOf(
            wine(2015, "France"), wine(2016, "FR"), wine(2017, "France", quantity = 0),
            wine(2018, "Italy"), wine(2019, null)
        )
        val (groups, withoutCountry) = WineStatistics.byCountry(wines, includeEmpty = false)
        assertEquals(listOf("France", "Italy"), groups.map { it.key })
        assertEquals(2, groups.first().wines.size)
        assertEquals(1, withoutCountry)
        assertEquals(3, WineStatistics.byCountry(wines, includeEmpty = true).first.first().wines.size)
    }

    @Test
    fun vintagesFillGaps() {
        val years = WineStatistics.byVintage(listOf(wine(2015), wine(2018), wine(2018)), includeEmpty = false)
        assertEquals(listOf(2015, 2016, 2017, 2018), years.map { it.key })
        assertEquals(listOf(1, 0, 0, 2), years.map { it.wines.size })
    }

    @Test
    fun pricesStayWithinOneCurrency() {
        val wines = listOf(
            wine(2015, price = 12.0), wine(2016, price = 25.0), wine(2017, price = 27.0),
            wine(2018, price = 99.0, currency = "CHF"), wine(2019, price = 8.0, quantity = 0)
        )
        assertEquals(listOf("EUR", "CHF"), WineStatistics.priceCurrencies(wines, includeEmpty = false))
        val eur = WineStatistics.priceDistribution(wines, includeEmpty = false, currency = "EUR")
        assertEquals(3, eur.count)
        assertEquals(25.0, eur.median!!, 0.001)
        // 0, 5, 10, 15, 20 (up to 30 at least): 12 in 10–15, 25 and 27 in 20–30
        assertEquals(listOf(0, 0, 1, 0, 2), eur.buckets.map { it.wines.size })
        val withEmpty = WineStatistics.priceDistribution(wines, includeEmpty = true, currency = "EUR")
        assertEquals(1, withEmpty.buckets[1].wines.size)
    }
}
