package org.librecellar

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librecellar.data.api.ApiJson
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.CollectionInfo
import org.librecellar.data.model.LoginResponse
import org.librecellar.data.model.Supplier
import org.librecellar.data.model.Wine

/** Pins down the wire format the backend expects (see ApiJson). */
class JsonSerializationTest {

    private fun encode(wine: Wine): JsonObject =
        ApiJson.parseToJsonElement(ApiJson.encodeToString(Wine.serializer(), wine)).jsonObject

    @Test
    fun unknownFieldsFromTheBackendAreIgnored() {
        val json = """{"id":"w1","name":"Riesling","vintage":2020,"wine_type":"WHITE","quantity":3,
            "purchase_price":24.9,"currency":"EUR","owner":"alice","access":"read","added_later":1}"""
        val wine = ApiJson.decodeFromString(Wine.serializer(), json)
        assertEquals("Riesling", wine.name)
        assertEquals("EUR", wine.currency)
        assertTrue(wine.isReadOnly)
        assertNull(wine.locationId)
    }

    @Test
    fun nullsAreLeftOutWhenSending() {
        // The backend rejects "size": null (not nullable there); it must be omitted instead
        val sent = encode(Wine(name = "Riesling", vintage = 2020, size = null))
        assertFalse("size" in sent)
        assertFalse("location_id" in sent)
        assertFalse("owner" in sent)
    }

    @Test
    fun defaultsAreSent() {
        val sent = encode(Wine(name = "Riesling", vintage = 2020))
        assertEquals("RED", sent.getValue("wine_type").jsonPrimitive.content)
        assertEquals("1", sent.getValue("quantity").jsonPrimitive.content)
        assertEquals("0.75L", sent.getValue("size").jsonPrimitive.content)
        assertTrue("id" in sent)
        // Computed properties are not part of the wire format
        assertFalse("isReadOnly" in sent)
        assertFalse("effectiveCurrency" in sent)
    }

    @Test
    fun nullForANonNullableFieldFallsBackToTheDefault() {
        val wine = ApiJson.decodeFromString(
            Wine.serializer(),
            """{"id":"w1","name":"Riesling","vintage":2020,"wine_type":null,"quantity":null}"""
        )
        assertEquals("RED", wine.wineType)
        assertEquals(1, wine.quantity)
    }

    @Test
    fun roundTripAndOtherModels() {
        val wine = Wine(name = "Riesling", vintage = 2020, purchasePrice = 24.9, currency = "EUR")
        assertEquals(wine, ApiJson.decodeFromString(Wine.serializer(), ApiJson.encodeToString(Wine.serializer(), wine)))

        val racks = ApiJson.decodeFromString(
            ListSerializer(CellarLocation.serializer()),
            """[{"id":"r1","name":"Rack","rows":2,"columns":3}]"""
        )
        assertEquals(6, racks.single().totalSlots)
        ApiJson.decodeFromString(Supplier.serializer(), """{"id":"s1","name":"Shop"}""")
        ApiJson.decodeFromString(CollectionInfo.serializer(), """{"owner":"bob","permission":"write"}""")
        val login = ApiJson.decodeFromString(
            LoginResponse.serializer(),
            """{"token":"t","expires_at":"2026-11-05T10:00:00Z","user":{"username":"bob","is_admin":false}}"""
        )
        assertEquals("bob", login.user?.username)
        val health = ApiJson.decodeFromString(
            MapSerializer(String.serializer(), String.serializer()),
            """{"status":"ok","app":"LibreCellar Server"}"""
        )
        assertEquals("ok", health["status"])
    }
}
