package org.librecellar.data.model

import java.util.Locale

data class CountryItem(
    val code: String,
    val name: String,
    val flagEmoji: String
) {
    val displayLabel: String
        get() = "$flagEmoji $name"
}

object CountryHelper {

    fun countryCodeToEmoji(code: String): String {
        if (code.length != 2) return "🌐"
        val upper = code.uppercase(Locale.US)
        if (upper[0] !in 'A'..'Z' || upper[1] !in 'A'..'Z') return "🌐"
        val first = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
        val second = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(first)) + String(Character.toChars(second))
    }

    /**
     * Complete list of all ISO 3166-1 countries with their flag emojis,
     * sorted alphabetically by English display name.
     */
    val allCountries: List<CountryItem> by lazy {
        val isoCodes = Locale.getISOCountries()
        isoCodes.mapNotNull { code ->
            val locale = Locale.Builder().setRegion(code).build()
            val name = locale.getDisplayCountry(Locale.ENGLISH)
            if (name.isNotBlank()) {
                CountryItem(
                    code = code,
                    name = name,
                    flagEmoji = countryCodeToEmoji(code)
                )
            } else null
        }
            .distinctBy { it.name }
            .sortedBy { it.name }
    }

    /**
     * Find a country by code, exact name, or common alias.
     */
    fun findCountry(query: String?): CountryItem? {
        if (query.isNullOrBlank()) return null
        val trimmed = query.trim()

        // Remove any leading emoji if present (e.g. "🇫🇷 France" -> "France")
        val cleanName = trimmed.replace(Regex("^[\\p{So}\\p{Sk}\\p{Cs}\\p{Cn}\\s]+"), "").trim()
        val searchTarget = cleanName.ifBlank { trimmed }

        return allCountries.find {
            it.name.equals(searchTarget, ignoreCase = true) ||
            it.code.equals(searchTarget, ignoreCase = true)
        } ?: allCountries.find {
            when (searchTarget.lowercase(Locale.US)) {
                "usa", "united states of america", "us" -> it.code == "US"
                "uk", "great britain", "england", "scotland" -> it.code == "GB"
                else -> false
            }
        } ?: allCountries.find {
            // Check if input is "France / Bordeaux" -> matches France
            searchTarget.startsWith(it.name, ignoreCase = true) ||
            it.name.contains(searchTarget, ignoreCase = true)
        }
    }

    fun getFlagForCountry(countryName: String?): String {
        return findCountry(countryName)?.flagEmoji ?: "🌐"
    }

    /**
     * Splits countries into:
     * 1. Assigned countries (already in use in bottles or merchants), sorted alphabetically.
     * 2. Remaining countries, sorted alphabetically.
     * Optionally filters both by a search query.
     */
    fun getOrganizedCountries(
        assignedCountryNames: Set<String>,
        filterQuery: String = ""
    ): Pair<List<CountryItem>, List<CountryItem>> {
        val normalizedAssignedCodes = assignedCountryNames.mapNotNull { findCountry(it)?.code }.toSet()

        val assigned = allCountries.filter { it.code in normalizedAssignedCodes }
            .sortedBy { it.name }

        val remaining = allCountries.filterNot { it.code in normalizedAssignedCodes }
            .sortedBy { it.name }

        if (filterQuery.isBlank()) {
            return Pair(assigned, remaining)
        }

        val q = filterQuery.trim()
        val filteredAssigned = assigned.filter {
            it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
        }
        val filteredRemaining = remaining.filter {
            it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true)
        }

        return Pair(filteredAssigned, filteredRemaining)
    }
}
