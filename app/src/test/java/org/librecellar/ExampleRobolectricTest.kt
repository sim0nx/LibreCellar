package org.librecellar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.librecellar.data.model.CellarLocation
import org.librecellar.data.model.CountryHelper
import org.librecellar.data.model.Wine

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LibreCellar", appName)
  }

  @Test
  fun `test country helper finds country and generates flags`() {
    val france = CountryHelper.findCountry("France")
    assertNotNull(france)
    assertEquals("FR", france?.code)
    assertEquals("🇫🇷", france?.flagEmoji)

    val flagUs = CountryHelper.getFlagForCountry("United States")
    assertEquals("🇺🇸", flagUs)

    val (assigned, remaining) = CountryHelper.getOrganizedCountries(setOf("France", "Italy"))
    assertEquals(2, assigned.size)
    assertEquals("France", assigned[0].name)
    assertEquals("Italy", assigned[1].name)
    assertTrue(remaining.none { it.code == "FR" || it.code == "IT" })
  }

  @Test
  fun `test rack climate optional fields`() {
    val rackWithoutClimate = CellarLocation(
      name = "Ambient Pantry Rack",
      temperatureCelsius = null,
      humidityPercent = null
    )
    assertNull(rackWithoutClimate.temperatureCelsius)
    assertNull(rackWithoutClimate.humidityPercent)

    val rackWithClimate = CellarLocation(
      name = "Wine Fridge Zone 1",
      temperatureCelsius = 13.5,
      humidityPercent = 70.0
    )
    assertEquals(13.5, rackWithClimate.temperatureCelsius!!, 0.01)
    assertEquals(70.0, rackWithClimate.humidityPercent!!, 0.01)
  }

  @Test
  fun `test wine varietal list tags parsing`() {
    val wine = Wine(
      name = "Château Margaux",
      vintage = 2015,
      varietal = "Cabernet Sauvignon, Merlot, Petit Verdot"
    )
    val grapes = wine.varietalList
    assertEquals(3, grapes.size)
    assertEquals("Cabernet Sauvignon", grapes[0])
    assertEquals("Merlot", grapes[1])
    assertEquals("Petit Verdot", grapes[2])
  }

  @Test
  fun `test grape registration and autocomplete in repository`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = org.librecellar.data.repository.WineRepository(context)

    val initialGrapes = repository.knownGrapes.value
    assertTrue(initialGrapes.isNotEmpty())

    // Register a new grape not previously in standard list
    val newGrape = "Rkatsiteli"
    repository.registerGrape(newGrape)

    val updatedGrapes = repository.knownGrapes.value
    assertTrue(updatedGrapes.any { it.equals(newGrape, ignoreCase = true) })
  }
}

