package org.librecellar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.librecellar.data.model.LabelLine
import org.librecellar.data.model.LabelTextParser

class LabelTextParserTest {

    private fun front(vararg lines: Pair<String, Float>) = lines.map { LabelLine(it.first, it.second) }
    private fun back(vararg lines: String) = lines.map { LabelLine(it, 20f, isBackLabel = true) }

    @Test
    fun bordeauxFrontAndBack() {
        val lines = front(
            "GRAND VIN DE BORDEAUX" to 22f,
            "CHÂTEAU" to 60f,
            "LA TOUR CARNET" to 58f,
            "2018" to 34f,
            "HAUT-MÉDOC" to 26f,
            "APPELLATION HAUT-MÉDOC CONTRÔLÉE" to 14f
        ) + back(
            "Mis en bouteille au château par SCEA Château La Tour Carnet, 33112 Saint-Laurent-Médoc",
            "PRODUIT DE FRANCE",
            "13,5 % vol.",
            "℮ 75 cl",
            "Contient des sulfites",
            "www.latourcarnet.fr  Depuis 1407"
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Château la Tour Carnet", s.name)
        assertEquals(2018, s.vintage)
        assertEquals("0.75L", s.size)
        assertEquals(13.5, s.alcoholPercent!!, 0.001)
        assertEquals("France", s.country)
        assertEquals("SCEA Château La Tour Carnet", s.producer)
    }

    @Test
    fun germanRiesling() {
        val lines = front(
            "Weingut Dr. Loosen" to 30f,
            "Riesling" to 48f,
            "Trocken" to 20f,
            "Jahrgang 2021" to 18f
        ) + back(
            "Qualitätswein Mosel",
            "Gutsabfüllung Weingut Dr. Loosen, D-54470 Bernkastel-Kues",
            "alc. 12,0% vol",
            "750 ml",
            "Erzeugnis aus Deutschland",
            "Enthält Sulfite"
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Riesling", s.name)
        assertEquals(2021, s.vintage)
        assertEquals("0.75L", s.size)
        assertEquals(12.0, s.alcoholPercent!!, 0.001)
        assertEquals("Germany", s.country)
        assertEquals("Weingut Dr. Loosen", s.producer)
    }

    @Test
    fun italianMagnumWithFoundingYear() {
        val lines = front(
            "dal 1898" to 16f,
            "BAROLO" to 55f,
            "DENOMINAZIONE DI ORIGINE CONTROLLATA E GARANTITA" to 12f,
            "2016" to 30f
        ) + back(
            "Imbottigliato all'origine da Azienda Agricola Rossi - Barolo (CN) Italia",
            "1,5 L",
            "14,5% vol",
            "Prodotto in Italia"
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Barolo", s.name)
        assertEquals(2016, s.vintage)
        assertEquals("1.5L", s.size)
        assertEquals(14.5, s.alcoholPercent!!, 0.001)
        assertEquals("Italy", s.country)
        assertEquals("Azienda Agricola Rossi", s.producer)
    }

    @Test
    fun englishFrontOnly() {
        val lines = front(
            "CLOUDY BAY" to 44f,
            "Sauvignon Blanc" to 30f,
            "MARLBOROUGH 2023" to 20f,
            "WINE OF NEW ZEALAND" to 14f,
            "13% vol   750ml" to 12f
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Cloudy Bay", s.name)
        assertEquals(2023, s.vintage)
        assertEquals("0.75L", s.size)
        assertEquals(13.0, s.alcoholPercent!!, 0.001)
        assertEquals("New Zealand", s.country)
    }

    @Test
    fun nameLinesAreJoinedByPositionNotRecognitionOrder() {
        // Text recognition returned the second name line first, as a separate block
        val lines = listOf(
            LabelLine("BELLEVUE", 92f, top = 330f),
            LabelLine("2018", 70f, top = 470f),
            LabelLine("GRAND VIN DE BORDEAUX", 30f, top = 80f),
            LabelLine("CHÂTEAU", 98f, top = 180f)
        )
        assertEquals("Château Bellevue", LabelTextParser.findName(lines))
    }

    @Test
    fun realRecognitionOutputOfATestLabel() {
        // What ML Kit returned on the emulator for a rendered front and back label
        val lines = listOf(
            LabelLine("CHẤTEAU", 106f, top = 167f),
            LabelLine("APPELLATION HAUT-MÉDOC CONTRÔLÉE", 24f, top = 659f),
            LabelLine("GRAND VIN DE BORDEAUX", 25f, top = 87f),
            LabelLine("HAUT-MÉDOC", 45f, top = 572f),
            LabelLine("2018", 60f, top = 471f),
            LabelLine("BELLEVUE", 82f, top = 328f),
            LabelLine("Contient des sulfites", 21f, true, 459f),
            LabelLine("SCEA Château Bellevue, 33112 Saint-Laurent", 26f, true, 234f),
            LabelLine("PRODUIT DE FRANCE", 26f, true, 306f),
            LabelLine("Mis en bouteille au chấteau par", 29f, true, 164f),
            LabelLine("e 75 cl", 35f, true, 379f),
            LabelLine("13,5 % vol.", 40f, true, 378f),
            LabelLine("Château Bellevue", 40f, true, 84f)
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Château Bellevue", s.name)
        assertEquals(2018, s.vintage)
        assertEquals("0.75L", s.size)
        assertEquals(13.5, s.alcoholPercent!!, 0.001)
        assertEquals("France", s.country)
        assertEquals("SCEA Château Bellevue", s.producer)
    }

    @Test
    fun ignoresImplausibleValues() {
        val lines = front(
            "Cuvée Spéciale" to 40f,
            "100% Merlot" to 20f,
            "Since 1950 · Tel 0033 5 56 00 00 00" to 10f,
            "Lot L2029" to 8f
        )
        val s = LabelTextParser.parse(lines, currentYear = 2026)
        assertEquals("Cuvée Spéciale", s.name)
        assertNull(s.vintage) // founding year and lot number are not vintages
        assertNull(s.alcoholPercent) // 100 % Merlot is not the alcohol
        assertNull(s.size)
    }

    @Test
    fun futureYearsAreNotVintages() {
        val s = LabelTextParser.parse(front("Best before 2031" to 10f, "Vino" to 30f), currentYear = 2026)
        assertNull(s.vintage)
    }

    @Test
    fun sizesAreNormalised() {
        assertEquals("0.375L", LabelTextParser.findSize("37,5 cl"))
        assertEquals("3L", LabelTextParser.findSize("3 L"))
        assertEquals("0.5L", LabelTextParser.findSize("500ML"))
        assertNull(LabelTextParser.findSize("nothing here"))
    }

    @Test
    fun emptyInputGivesNothing() {
        assertTrue(LabelTextParser.parse(emptyList()).isEmpty)
    }
}
