package org.librecellar.data.model

import java.time.Year
import java.util.Locale

/**
 * A line of text recognised on a label: [height] is its text height in pixels (bigger = more
 * prominent), [top] its distance from the top of the picture.
 */
data class LabelLine(val text: String, val height: Float, val isBackLabel: Boolean = false, val top: Float = 0f)

/** What could be read from the label pictures; every field is only a suggestion. */
data class LabelSuggestions(
    val name: String? = null,
    val vintage: Int? = null,
    /** In the app's format, e.g. "0.75L" */
    val size: String? = null,
    val alcoholPercent: Double? = null,
    /** English country name, as stored for wines */
    val country: String? = null,
    val producer: String? = null
) {
    val isEmpty: Boolean
        get() = name == null && vintage == null && size == null && alcoholPercent == null &&
            country == null && producer == null
}

/**
 * Extracts wine details from text recognised on the front and (optional) back label.
 *
 * Plain Kotlin on purpose: the recognition runs on the device (ML Kit), this only interprets the
 * text, so it can be unit-tested with label texts. The front label wins for the name; the other
 * values are taken from whichever side shows them (EU labels may put them on either side).
 */
object LabelTextParser {

    private const val NBSP = ' '

    // 750 ml, 75 cl, 0,75 L, 1.5L, 75cle (℮ often reads as "e"), 37,5cl
    private val VOLUME = Regex(
        """(?<![\d.,])(\d{1,4}(?:[.,]\d{1,3})?)\s*(ml|cl|l|ltr|lt|litre|liter|litri|litros?)(?![a-z])""",
        RegexOption.IGNORE_CASE
    )

    // 13% vol, 13,5 % alc./vol., alc. 12.5% by vol, 14.5%vol
    private val ALCOHOL = Regex("""(?<![\d.,])(\d{1,2}(?:[.,]\d{1,2})?)\s*%""")
    private val ALCOHOL_CONTEXT = Regex("""vol|alc|alk|gradazione|graduaci""", RegexOption.IGNORE_CASE)

    private val YEAR = Regex("""(?<![\d.,/-])(19\d{2}|20\d{2})(?![\d.,%])""")
    private val VINTAGE_CONTEXT = Regex(
        """vintage|mill[ée]sime|jahrgang|annata|cosecha|vendange|harvest|récolte|recolte""",
        RegexOption.IGNORE_CASE
    )

    // Years that are not the vintage: founding dates, addresses, legal text
    private val NOT_VINTAGE_CONTEXT = Regex(
        """depuis|since|seit|fond[ée]|founded|established|est\.|gegr|dal\s|desde|tel|phone|fax|www\.|©|\(c\)|regulation|décret|decret|reg\.""",
        RegexOption.IGNORE_CASE
    )

    private val COUNTRY_PHRASE = Regex(
        """(?:product\s+of|produce\s+of|produit\s+(?:de|d['’]|en)|prodotto\s+in|producto\s+de|produzido\s+em|hergestellt\s+in|erzeugnis\s+aus|wine\s+of|vin\s+de|made\s+in)\s+(?:la\s+|l['’]\s*)?([\p{L} ]{3,30})""",
        RegexOption.IGNORE_CASE
    )

    // Country names in the languages commonly printed on labels
    private val LOCAL_COUNTRY_NAMES = mapOf(
        "frankreich" to "FR", "francia" to "FR", "frança" to "FR",
        "italia" to "IT", "italien" to "IT", "italie" to "IT",
        "españa" to "ES", "espagne" to "ES", "spanien" to "ES", "spagna" to "ES",
        "deutschland" to "DE", "allemagne" to "DE", "germania" to "DE", "alemania" to "DE",
        "österreich" to "AT", "autriche" to "AT",
        "luxemburg" to "LU", "lëtzebuerg" to "LU",
        "schweiz" to "CH", "suisse" to "CH", "svizzera" to "CH",
        "portogallo" to "PT", "portugal" to "PT",
        "grèce" to "GR", "griechenland" to "GR", "grecia" to "GR",
        "afrique du sud" to "ZA", "südafrika" to "ZA", "sudáfrica" to "ZA",
        "usa" to "US", "u.s.a" to "US", "united states of america" to "US"
    )

    private val PRODUCER_PHRASE = Regex(
        """(?:mis\s+en\s+bouteilles?\s+(?:au\s+ch[âa]teau\s+|[àa]\s+la\s+propri[ée]t[ée]\s+|au\s+domaine\s+)?par|abgef[üu]llt\s+(?:von|durch|bei)|erzeugerabf[üu]llung|gutsabf[üu]llung|imbottigliato\s+(?:all['’]origine\s+)?da|embotellado\s+(?:en\s+origen\s+)?por|bottled\s+(?:at\s+the\s+\w+\s+)?by|produced\s+(?:and\s+bottled\s+)?by|engarrafado\s+por)\s*:?\s*(.*)""",
        RegexOption.IGNORE_CASE
    )

    // Lines that are legal or decorative text, never the wine's name
    private val BOILERPLATE = Regex(
        """appellation|contr[ôo]l[ée]e|prot[ée]g[ée]e|origine|d\.?o\.?c|denominazione|qualit[äa]tswein|pr[äa]dikat|product|produit|prodotto|contains|contient|enth[äa]lt|sulfit|sulphit|solfiti|allerg|mis\s+en\s+bouteille|bottled|abgef[üu]llt|imbottigliato|embotellado|grand\s+vin\b|vin\s+de\s+france|wine\s+of|red\s+wine|white\s+wine|vin\s+rouge|vin\s+blanc|rotwein|wei[ßs]wein|vino\s+(rosso|bianco|tinto)|www\.|\.com|\.fr\b|\.de\b|\.it\b|recycl|pregnan|enceinte|schwanger|lot\b|^l\s?\d|serve|servir|temperature""",
        RegexOption.IGNORE_CASE
    )

    // Name particles kept lower case when converting an all-caps name, e.g. "Château de la Tour"
    private val PARTICLES = setOf("de", "du", "des", "la", "le", "les", "d'", "di", "del", "della", "von", "vom", "zu", "y", "et", "and", "the", "of")

    // Accents text recognition confuses on labels (â read as ấ, as in "CHẤTEAU")
    private val MISREAD_ACCENTS = mapOf(
        'ấ' to 'â', 'ầ' to 'â', 'ẩ' to 'â', 'ẫ' to 'â', 'ậ' to 'â',
        'Ấ' to 'Â', 'Ầ' to 'Â', 'Ẩ' to 'Â', 'Ẫ' to 'Â', 'Ậ' to 'Â',
        'ế' to 'ê', 'ề' to 'ê', 'ể' to 'ê', 'ễ' to 'ê', 'ệ' to 'ê',
        'Ế' to 'Ê', 'Ề' to 'Ê', 'Ể' to 'Ê', 'Ễ' to 'Ê', 'Ệ' to 'Ê',
        'ố' to 'ô', 'ồ' to 'ô', 'ổ' to 'ô', 'ỗ' to 'ô', 'ộ' to 'ô',
        'Ố' to 'Ô', 'Ồ' to 'Ô', 'Ổ' to 'Ô', 'Ỗ' to 'Ô', 'Ộ' to 'Ô'
    )

    private fun normalise(text: String): String =
        text.replace(NBSP, ' ').map { MISREAD_ACCENTS[it] ?: it }.joinToString("").trim()

    fun parse(lines: List<LabelLine>, currentYear: Int = Year.now().value): LabelSuggestions {
        // Front label first, each side top to bottom as printed (recognition returns text
        // blocks in its own order)
        val clean = lines.map { it.copy(text = normalise(it.text)) }
            .filter { it.text.isNotEmpty() }
            .sortedWith(compareBy({ it.isBackLabel }, { it.top }))
        if (clean.isEmpty()) return LabelSuggestions()
        val fullText = clean.joinToString("\n") { it.text }

        return LabelSuggestions(
            name = findName(clean),
            vintage = findVintage(clean, currentYear),
            size = findSize(fullText),
            alcoholPercent = findAlcohol(clean),
            country = findCountry(fullText),
            producer = findProducer(clean)
        )
    }

    fun findSize(text: String): String? {
        val litres = VOLUME.findAll(text).mapNotNull { m ->
            val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
            val unit = m.groupValues[2].lowercase(Locale.ROOT)
            val l = when (unit) {
                "ml" -> value / 1000
                "cl" -> value / 100
                else -> value
            }
            l.takeIf { it in 0.05..18.0 }
        }.firstOrNull() ?: return null
        return formatLitres(litres)
    }

    /** 0.75 -> "0.75L", 1.5 -> "1.5L", 3.0 -> "3L" (the app's bottle size format). */
    fun formatLitres(litres: Double): String {
        val rounded = Math.round(litres * 1000) / 1000.0
        val text = if (rounded == Math.floor(rounded)) rounded.toLong().toString()
        else rounded.toBigDecimal().stripTrailingZeros().toPlainString()
        return "${text}L"
    }

    fun findAlcohol(lines: List<LabelLine>): Double? {
        data class Candidate(val value: Double, val inContext: Boolean)
        val candidates = lines.flatMap { line ->
            ALCOHOL.findAll(line.text).mapNotNull { m ->
                val value = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
                if (value < 5.0 || value > 23.0) return@mapNotNull null
                val around = line.text.substring(
                    (m.range.first - 12).coerceAtLeast(0),
                    (m.range.last + 12).coerceAtMost(line.text.length - 1) + 1
                )
                Candidate(value, ALCOHOL_CONTEXT.containsMatchIn(around))
            }.toList()
        }
        return (candidates.firstOrNull { it.inContext } ?: candidates.singleOrNull())?.value
    }

    fun findVintage(lines: List<LabelLine>, currentYear: Int): Int? {
        data class Candidate(val year: Int, val score: Int)
        val candidates = lines.flatMap { line ->
            if (NOT_VINTAGE_CONTEXT.containsMatchIn(line.text)) return@flatMap emptyList()
            YEAR.findAll(line.text).mapNotNull { m ->
                val year = m.value.toInt()
                if (year < 1900 || year > currentYear) return@mapNotNull null
                var score = 1
                if (VINTAGE_CONTEXT.containsMatchIn(line.text)) score += 10
                if (line.text.length <= 6) score += 3 // the year on its own, typical for vintages
                if (!line.isBackLabel) score += 2
                Candidate(year, score)
            }.toList()
        }
        return candidates.maxByOrNull { it.score }?.year
    }

    fun findCountry(text: String): String? {
        for (m in COUNTRY_PHRASE.findAll(text)) {
            val words = m.groupValues[1].trim().split(Regex("\\s+"))
            // "Product of France Bordeaux" -> try "France Bordeaux", then "France"
            for (n in words.size downTo 1) {
                countryNamed(words.take(n).joinToString(" "))?.let { return it }
            }
        }
        // A line that is only a country name, e.g. "FRANCE" or "ITALIA"
        return text.lines().firstNotNullOfOrNull { countryNamed(it.trim()) }
    }

    private fun countryNamed(candidate: String): String? {
        val q = candidate.trim().trimEnd('.', ',').lowercase(Locale.ROOT)
        if (q.length < 3) return LOCAL_COUNTRY_NAMES[q]?.let { codeToName(it) }
        LOCAL_COUNTRY_NAMES[q]?.let { return codeToName(it) }
        return CountryHelper.allCountries.firstOrNull { it.name.lowercase(Locale.ROOT) == q }?.name
    }

    private fun codeToName(code: String): String? =
        CountryHelper.allCountries.firstOrNull { it.code == code }?.name

    fun findProducer(lines: List<LabelLine>): String? {
        lines.forEachIndexed { i, line ->
            val m = PRODUCER_PHRASE.find(line.text) ?: return@forEachIndexed
            // The producer may follow on the next line ("Mis en bouteille par" / "SCEA Domaine X")
            val rest = m.groupValues[1].trim().ifEmpty { lines.getOrNull(i + 1)?.text.orEmpty() }
            val producer = rest.split(',', '-', '–', '|').first().trim().trimEnd('.')
            if (producer.count { it.isLetter() } >= 3) return tidyCase(producer)
        }
        return null
    }

    fun findName(lines: List<LabelLine>): String? {
        // Top to bottom as printed (recognition may return text blocks in another order)
        val front = lines.filter { !it.isBackLabel }.ifEmpty { lines }.sortedBy { it.top }
        val candidates = front.filter { isNameLike(it.text) }
        val tallest = candidates.maxByOrNull { it.height } ?: return null
        // Names often span lines of the same size, e.g. "CHÂTEAU" / "LA TOUR": take the
        // neighbours above and below that are about as big
        // 70 %: accents make a line measure taller ("CHÂTEAU" above "BELLEVUE")
        fun similar(line: LabelLine?) = line != null && line in candidates && line.height >= tallest.height * 0.7f
        val index = front.indexOf(tallest)
        var first = index
        var last = index
        while (similar(front.getOrNull(first - 1))) first--
        while (similar(front.getOrNull(last + 1))) last++
        return tidyCase(front.subList(first, last + 1).joinToString(" ") { it.text })
    }

    private fun isNameLike(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        if (letters < 3 || letters < text.length / 2) return false
        if (BOILERPLATE.containsMatchIn(text)) return false
        if (VOLUME.containsMatchIn(text) || ALCOHOL.containsMatchIn(text)) return false
        if (countryNamed(text) != null) return false
        return true
    }

    /** "CHÂTEAU DE LA TOUR" -> "Château de la Tour"; mixed-case text is kept as printed. */
    fun tidyCase(text: String): String {
        val squeezed = text.replace(Regex("\\s+"), " ").trim()
        if (squeezed.any { it.isLowerCase() }) return squeezed
        return squeezed.lowercase(Locale.ROOT).split(' ').mapIndexed { i, word ->
            if (i > 0 && word in PARTICLES) word
            else word.split('-').joinToString("-") { part ->
                part.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }.joinToString(" ")
    }
}
