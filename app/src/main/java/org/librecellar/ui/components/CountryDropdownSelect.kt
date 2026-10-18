package org.librecellar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.CountryHelper
import org.librecellar.data.model.CountryItem
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDropdownSelect(
    selectedCountry: String?,
    onCountrySelected: (String) -> Unit,
    assignedCountries: Set<String> = emptySet(),
    label: String = "Country",
    placeholder: String = "Select country...",
    isOptional: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = "country_dropdown_select"
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val resolvedCountryItem = remember(selectedCountry) {
        CountryHelper.findCountry(selectedCountry)
    }

    val (assignedList, remainingList) = remember(assignedCountries, searchQuery) {
        CountryHelper.getOrganizedCountries(assignedCountries, searchQuery)
    }

    val displayValue = if (expanded) {
        searchQuery
    } else {
        resolvedCountryItem?.name
            ?: selectedCountry?.replace(Regex("^[\\p{So}\\p{Sk}\\p{Cs}\\p{Cn}\\s]+"), "")?.trim()
            ?: ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            if (!it) searchQuery = ""
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {
                searchQuery = it
                if (!expanded) expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            leadingIcon = {
                Text(
                    text = resolvedCountryItem?.flagEmoji ?: "🌐",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOptional && !selectedCountry.isNullOrBlank()) {
                        IconButton(
                            onClick = {
                                onCountrySelected("")
                                searchQuery = ""
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear country",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .testTag(testTag)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            },
            modifier = Modifier
                .heightIn(max = 380.dp)
                .testTag("${testTag}_menu")
        ) {
            // Section 1: Assigned Countries (already in user's cellar or merchants)
            if (assignedList.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "★ IN USE / ASSIGNED (${assignedList.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CellarGold,
                            fontSize = 10.sp
                        )
                    }
                }

                assignedList.forEach { country ->
                    CountryDropdownOption(
                        country = country,
                        isSelected = resolvedCountryItem?.code == country.code,
                        onClick = {
                            onCountrySelected(country.name)
                            searchQuery = ""
                            expanded = false
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            // Section 2: All Other Countries
            if (remainingList.isNotEmpty()) {
                if (assignedList.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌍 ALL COUNTRIES (${remainingList.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                remainingList.forEach { country ->
                    CountryDropdownOption(
                        country = country,
                        isSelected = resolvedCountryItem?.code == country.code,
                        onClick = {
                            onCountrySelected(country.name)
                            searchQuery = ""
                            expanded = false
                        }
                    )
                }
            } else if (assignedList.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "No countries match '$searchQuery'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun CountryDropdownOption(
    country: CountryItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = {
            Text(text = country.flagEmoji, fontSize = 20.sp)
        },
        text = {
            Text(
                text = country.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) CellarGold else MaterialTheme.colorScheme.onSurface
            )
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = CellarGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        onClick = onClick,
        modifier = Modifier.testTag("country_option_${country.code}")
    )
}
