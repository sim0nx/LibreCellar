package org.librecellar.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
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

@Serializable
data class Wine(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    // Mandatory properties:
    @SerialName("name") val name: String,
    @SerialName("vintage") val vintage: Int,
    // Optional properties:
    @SerialName("country") val country: String? = null,
    @SerialName("region") val region: String? = null,
    @SerialName("varietal") val varietal: String? = null,
    @SerialName("producer") val producer: String? = null,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    @SerialName("alcohol_percent") val alcoholPercent: Double? = null,
    @SerialName("size") val size: String? = "0.75L",
    @SerialName("tasting_rating") val tastingRating: Int? = null, // 1 to 5 stars
    @SerialName("comments") val comments: String? = null,
    @SerialName("purchase_price") val purchasePrice: Double? = null,
    // ISO 4217 code of purchasePrice, e.g. "EUR"; null for wines saved before currencies existed
    @SerialName("currency") val currency: String? = null,
    @SerialName("purchase_location") val purchaseLocation: String? = null, // Vendor/Shop name
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("price_tier") val priceTier: String? = null, // Optional user-assigned or suggested tier
    // Inventory & Cellar tracking:
    @SerialName("quantity") val quantity: Int = 1,
    @SerialName("location_id") val locationId: String? = null,
    @SerialName("slot_row") val slotRow: Int? = null,
    @SerialName("slot_column") val slotColumn: Int? = null,
    @SerialName("slot_label") val slotLabel: String? = null,
    @SerialName("wine_type") val wineType: String = WineType.RED.name,
    // Set by the backend: collection owner and the user's access to it (see CollectionAccess)
    @SerialName("owner") override val owner: String? = null,
    @SerialName("access") override val access: String? = null
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

@Serializable
data class CellarLocation(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("name") val name: String,
    @SerialName("rows") val rows: Int = 4,
    @SerialName("columns") val columns: Int = 6,
    @SerialName("description") val description: String? = null,
    @SerialName("temperature_celsius") val temperatureCelsius: Double? = null,
    @SerialName("humidity_percent") val humidityPercent: Double? = null,
    @SerialName("color_hex") val colorHex: String? = "#6B1D2F",
    @SerialName("owner") override val owner: String? = null,
    @SerialName("access") override val access: String? = null
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

@Serializable
data class Supplier(
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    @SerialName("name") val name: String,
    @SerialName("type") val type: String = "Wine Merchant",
    @SerialName("city") val city: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("website") val website: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("date_added") val dateAdded: String = LocalDate.now().toString(), // ISO yyyy-MM-dd
    @SerialName("owner") override val owner: String? = null,
    @SerialName("access") override val access: String? = null
) : SharedEntry

@Serializable
data class AssignSlotRequest(
    @SerialName("wine_id") val wineId: String,
    @SerialName("location_id") val locationId: String?,
    @SerialName("slot_row") val slotRow: Int?,
    @SerialName("slot_column") val slotColumn: Int?
)

@Serializable
data class DecrementInventoryResponse(
    @SerialName("wine_id") val wineId: String,
    @SerialName("previous_quantity") val previousQuantity: Int,
    @SerialName("remaining_quantity") val remainingQuantity: Int,
    @SerialName("message") val message: String
)

@Serializable
data class ImageUploadResponse(
    @SerialName("filename") val filename: String,
    @SerialName("url") val url: String? = null
)


@Serializable
data class UserInfo(
    @SerialName("username") val username: String,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    // Owner username of the collection new entries go to
    @SerialName("default_collection") val defaultCollection: String? = null
)

/** A collection the user can access: their own ("owner") or one shared with them ("write"/"read"). */
@Serializable
data class CollectionInfo(
    @SerialName("owner") val owner: String,
    @SerialName("permission") val permission: String
) {
    val collectionAccess: CollectionAccess
        get() = CollectionAccess.fromKey(permission)
}

@Serializable
data class DefaultCollectionUpdate(
    // Null selects the user's own collection
    @SerialName("owner") val owner: String?
)

@Serializable
data class LoginRequest(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String
)

/** Backend login: the token replaces the password for all further requests. */
@Serializable
data class LoginResponse(
    @SerialName("token") val token: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("user") val user: UserInfo? = null
)
