package org.librecellar.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class WineType(val displayName: String) {
    RED("Red Wine"),
    WHITE("White Wine"),
    ROSE("Rosé Wine"),
    SPARKLING("Champagne / Sparkling"),
    DESSERT("Dessert Wine"),
    FORTIFIED("Fortified / Port")
}

enum class WinePriceTier(
    val key: String,
    val displayName: String,
    val shortLabel: String,
    val symbol: String,
    val colorHex: String
) {
    CHEAP("CHEAP", "Cheap / Value", "Cheap", "$", "#68875B"),
    NORMAL("NORMAL", "Normal / Standard", "Normal", "$$", "#4A7C59"),
    MEDIUM("MEDIUM", "Medium / Mid-Range", "Medium", "$$$", "#C68B25"),
    EXPENSIVE("EXPENSIVE", "Expensive / Premium", "Expensive", "$$$$", "#C75D4D"),
    VERY_EXPENSIVE("VERY_EXPENSIVE", "Very Expensive / Prestige", "Very Expensive", "$$$$$", "#88253B");

    companion object {
        fun fromKey(key: String?): WinePriceTier? {
            if (key == null) return null
            return entries.find { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
        }

        /**
         * Dynamically calculates the price tier based on the distribution of wine prices in the user's cellar.
         */
        fun calculateDynamicTier(price: Double?, existingWines: List<Wine>): WinePriceTier {
            if (price == null || price <= 0.0) return NORMAL

            val knownPrices = existingWines
                .mapNotNull { it.purchasePrice }
                .filter { it > 0.0 }
                .sorted()

            // If user has fewer than 3 priced wines, use sensible baseline tiers
            if (knownPrices.size < 3) {
                return when {
                    price < 20.0 -> CHEAP
                    price < 45.0 -> NORMAL
                    price < 95.0 -> MEDIUM
                    price < 250.0 -> EXPENSIVE
                    else -> VERY_EXPENSIVE
                }
            }

            // Calculate dynamic quintiles from user's owned wines
            fun percentile(p: Double): Double {
                val index = (p * (knownPrices.size - 1)).toInt()
                return knownPrices[index.coerceIn(0, knownPrices.size - 1)]
            }

            val p20 = percentile(0.20)
            val p40 = percentile(0.40)
            val p60 = percentile(0.60)
            val p80 = percentile(0.80)

            return when {
                price <= p20 -> CHEAP
                price <= p40 -> NORMAL
                price <= p60 -> MEDIUM
                price <= p80 -> EXPENSIVE
                else -> VERY_EXPENSIVE
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class Wine(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    // Mandatory properties:
    @Json(name = "name") val name: String,
    @Json(name = "vintage") val vintage: Int,
    // Optional properties:
    @Json(name = "country") val country: String? = null,
    @Json(name = "region") val region: String? = null,
    @Json(name = "varietal") val varietal: String? = null,
    @Json(name = "producer") val producer: String? = null,
    @Json(name = "purchase_date") val purchaseDate: String? = null,
    @Json(name = "alcohol_percent") val alcoholPercent: Double? = null,
    @Json(name = "size") val size: String? = "0.75L",
    @Json(name = "tasting_rating") val tastingRating: Int? = null, // 1 to 5 stars
    @Json(name = "comments") val comments: String? = null,
    @Json(name = "purchase_price") val purchasePrice: Double? = null,
    @Json(name = "purchase_location") val purchaseLocation: String? = null, // Vendor/Shop name
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "price_tier") val priceTier: String? = null, // Optional user-assigned or suggested tier
    // Inventory & Cellar tracking:
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "location_id") val locationId: String? = null,
    @Json(name = "slot_row") val slotRow: Int? = null,
    @Json(name = "slot_column") val slotColumn: Int? = null,
    @Json(name = "slot_label") val slotLabel: String? = null,
    @Json(name = "wine_type") val wineType: String = WineType.RED.name
) {
    val isInCellar: Boolean
        get() = !locationId.isNullOrBlank()

    val formattedLocationSummary: String
        get() {
            if (locationId.isNullOrBlank()) return "Unassigned / Not in cellar"
            return if (slotRow != null && slotColumn != null) {
                "Row ${slotRow + 1}, Slot ${slotColumn + 1}"
            } else {
                "Cellar assigned"
            }
        }

    fun getResolvedPriceTier(allWines: List<Wine>): WinePriceTier {
        return WinePriceTier.fromKey(priceTier)
            ?: WinePriceTier.calculateDynamicTier(purchasePrice, allWines)
    }
}

@JsonClass(generateAdapter = true)
data class CellarLocation(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "name") val name: String,
    @Json(name = "rows") val rows: Int = 4,
    @Json(name = "columns") val columns: Int = 6,
    @Json(name = "description") val description: String? = null,
    @Json(name = "temperature_celsius") val temperatureCelsius: Double? = null,
    @Json(name = "humidity_percent") val humidityPercent: Double? = null,
    @Json(name = "color_hex") val colorHex: String? = "#6B1D2F"
) {
    val totalSlots: Int
        get() = rows * columns
}

@JsonClass(generateAdapter = true)
data class Supplier(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "name") val name: String,
    @Json(name = "type") val type: String = "Wine Merchant",
    @Json(name = "city") val city: String? = null,
    @Json(name = "country") val country: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "date_added") val dateAdded: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
)

@JsonClass(generateAdapter = true)
data class AssignSlotRequest(
    @Json(name = "wine_id") val wineId: String,
    @Json(name = "location_id") val locationId: String?,
    @Json(name = "slot_row") val slotRow: Int?,
    @Json(name = "slot_column") val slotColumn: Int?
)

@JsonClass(generateAdapter = true)
data class DecrementInventoryResponse(
    @Json(name = "wine_id") val wineId: String,
    @Json(name = "previous_quantity") val previousQuantity: Int,
    @Json(name = "remaining_quantity") val remainingQuantity: Int,
    @Json(name = "message") val message: String
)
