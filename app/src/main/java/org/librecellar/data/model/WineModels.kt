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

    /** Tier marking in the device's currency, e.g. "€€€" for MEDIUM. */
    val currencySymbol: String
        get() = LocaleFormat.tierSymbol(symbol.length)

    /** Tier marking in the currency with ISO 4217 [currencyCode], e.g. "£££". */
    fun symbolFor(currencyCode: String?): String = LocaleFormat.tierSymbol(symbol.length, currencyCode)

    companion object {
        fun fromKey(key: String?): WinePriceTier? {
            if (key == null) return null
            return entries.find { it.key.equals(key, ignoreCase = true) || it.name.equals(key, ignoreCase = true) }
        }

        /**
         * Dynamically calculates the price tier based on the distribution of wine prices in the user's cellar.
         * Only wines priced in the same currency are compared (no exchange rates are applied).
         */
        fun calculateDynamicTier(price: Double?, existingWines: List<Wine>, currency: String? = null): WinePriceTier {
            if (price == null || price <= 0.0) return NORMAL

            val targetCurrency = LocaleFormat.currencyOf(currency).currencyCode
            val knownPrices = existingWines
                .filter { it.effectiveCurrency == targetCurrency }
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

/**
 * Access level the backend reports for an entry of a (possibly shared) collection.
 * Entries created locally and not yet synced have no access value and count as own.
 */
enum class CollectionAccess(val key: String) {
    OWNER("owner"),
    WRITE("write"),
    READ("read");

    companion object {
        fun fromKey(key: String?): CollectionAccess =
            entries.find { it.key == key } ?: OWNER
    }
}

/** Wines, cellar locations and merchants all belong to a collection that may be shared with the user. */
interface SharedEntry {
    val owner: String?
    val access: String?

    val collectionAccess: CollectionAccess
        get() = CollectionAccess.fromKey(access)

    /** Shared with the user read-only: it can be viewed but not changed. */
    val isReadOnly: Boolean
        get() = collectionAccess == CollectionAccess.READ

    /** Belongs to someone else's collection the user may edit, e.g. a household collection. */
    val isSharedWritable: Boolean
        get() = collectionAccess == CollectionAccess.WRITE
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
    // ISO 4217 code of purchasePrice, e.g. "EUR"; null for wines saved before currencies existed
    @Json(name = "currency") val currency: String? = null,
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
    @Json(name = "wine_type") val wineType: String = WineType.RED.name,
    // Set by the backend: collection owner and the user's access to it (see CollectionAccess)
    @Json(name = "owner") override val owner: String? = null,
    @Json(name = "access") override val access: String? = null
) : SharedEntry {
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

    /** Currency of the price; wines without one are shown in the device's currency. */
    val effectiveCurrency: String
        get() = LocaleFormat.currencyOf(currency).currencyCode

    val varietalList: List<String>
        get() = varietal?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun getResolvedPriceTier(allWines: List<Wine>): WinePriceTier {
        return WinePriceTier.fromKey(priceTier)
            ?: WinePriceTier.calculateDynamicTier(purchasePrice, allWines, currency)
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
    @Json(name = "color_hex") val colorHex: String? = "#6B1D2F",
    @Json(name = "owner") override val owner: String? = null,
    @Json(name = "access") override val access: String? = null
) : SharedEntry {
    val totalSlots: Int
        get() = rows * columns

    /**
     * Whether [wine] may be placed in this rack: the rack must be editable and, for a wine that
     * already belongs to a collection, in the same collection (the backend enforces this too).
     * New wines join the collection of the rack they are placed in.
     */
    fun acceptsWine(wine: Wine?): Boolean =
        !isReadOnly && (wine?.owner == null || owner == null || owner == wine.owner)
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
    @Json(name = "date_added") val dateAdded: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    @Json(name = "owner") override val owner: String? = null,
    @Json(name = "access") override val access: String? = null
) : SharedEntry

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

@JsonClass(generateAdapter = true)
data class ImageUploadResponse(
    @Json(name = "filename") val filename: String,
    @Json(name = "url") val url: String? = null
)


@JsonClass(generateAdapter = true)
data class UserInfo(
    @Json(name = "username") val username: String,
    @Json(name = "is_admin") val isAdmin: Boolean = false,
    // Owner username of the collection new entries go to
    @Json(name = "default_collection") val defaultCollection: String? = null
)

/** A collection the user can access: their own ("owner") or one shared with them ("write"/"read"). */
@JsonClass(generateAdapter = true)
data class CollectionInfo(
    @Json(name = "owner") val owner: String,
    @Json(name = "permission") val permission: String
) {
    val collectionAccess: CollectionAccess
        get() = CollectionAccess.fromKey(permission)
}

@JsonClass(generateAdapter = true)
data class DefaultCollectionUpdate(
    // Null selects the user's own collection
    @Json(name = "owner") val owner: String?
)
