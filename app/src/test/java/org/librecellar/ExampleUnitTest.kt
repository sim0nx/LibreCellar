package org.librecellar

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `rack temp and humidity are optional and nullable`() {
    val unmonitoredRack = org.librecellar.data.model.CellarLocation(
      name = "Basement Wall Shelf",
      rows = 4,
      columns = 5,
      temperatureCelsius = null,
      humidityPercent = null
    )
    assertNull(unmonitoredRack.temperatureCelsius)
    assertNull(unmonitoredRack.humidityPercent)
    assertEquals(20, unmonitoredRack.totalSlots)

    val monitoredRack = unmonitoredRack.copy(
      temperatureCelsius = 12.5,
      humidityPercent = 68.0
    )
    assertEquals(12.5, monitoredRack.temperatureCelsius!!, 0.01)
    assertEquals(68.0, monitoredRack.humidityPercent!!, 0.01)
  }

  @Test
  fun `wine editing updates attributes and location slots`() {
    val initial = org.librecellar.data.model.Wine(
      name = "Bordeaux Rouge",
      vintage = 2018,
      quantity = 3,
      purchasePrice = 35.0,
      tastingRating = 4
    )
    val edited = initial.copy(
      name = "Château Bordeaux Grand Cru",
      vintage = 2019,
      quantity = 6,
      purchasePrice = 85.0,
      priceTier = org.librecellar.data.model.WinePriceTier.EXPENSIVE.key,
      locationId = "rack_1",
      slotRow = 1,
      slotColumn = 3,
      slotLabel = "Row 2, Slot 4",
      tastingRating = 5,
      comments = "Superb depth, decanted for 2 hours."
    )
    assertEquals("Château Bordeaux Grand Cru", edited.name)
    assertEquals(2019, edited.vintage)
    assertEquals(6, edited.quantity)
    assertEquals("Row 2, Slot 4", edited.slotLabel)
    assertEquals(5, edited.tastingRating)
    assertEquals("EXPENSIVE", edited.priceTier)
  }
}
