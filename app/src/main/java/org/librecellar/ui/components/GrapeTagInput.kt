package org.librecellar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundy
import org.librecellar.ui.theme.WineBurgundyLight
import org.librecellar.ui.theme.WineRose

/**
 * Interactive Tag-based Grape / Varietal Selector with Autocomplete.
 * Allows adding multiple grape tags (e.g. Cabernet Sauvignon, Merlot).
 * If a new grape is entered that does not exist yet, it is added and automatically
 * registered for future autocompletes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GrapeTagInput(
    selectedGrapes: List<String>,
    onGrapesChange: (List<String>) -> Unit,
    availableGrapes: List<String>,
    onNewGrapeEntered: (String) -> Unit,
    modifier: Modifier = Modifier,
    usedGrapes: Set<String> = emptySet(),
    label: String = "Varietal / Grape Blend",
    placeholder: String = "Type to search or add grape...",
    testTag: String = "grape_tag_input"
) {
    var expanded by remember { mutableStateOf(false) }
    var inputQuery by remember { mutableStateOf("") }

    val filteredSuggestions = remember(inputQuery, availableGrapes, selectedGrapes, usedGrapes) {
        val query = inputQuery.trim()
        val available = availableGrapes.filter { candidate ->
            selectedGrapes.none { it.equals(candidate, ignoreCase = true) }
        }
        val matches = if (query.isBlank()) {
            available
        } else {
            available.filter { it.contains(query, ignoreCase = true) }
        }
        // Prioritize grapes already used in the cellar
        matches.sortedWith { a, b ->
            val aUsed = usedGrapes.any { it.equals(a, ignoreCase = true) }
            val bUsed = usedGrapes.any { it.equals(b, ignoreCase = true) }
            when {
                aUsed && !bUsed -> -1
                !aUsed && bUsed -> 1
                else -> String.CASE_INSENSITIVE_ORDER.compare(a, b)
            }
        }.take(30)
    }

    val isNewGrapeQuery = remember(inputQuery, availableGrapes, selectedGrapes) {
        val query = inputQuery.trim()
        query.isNotBlank() &&
                availableGrapes.none { it.equals(query, ignoreCase = true) } &&
                selectedGrapes.none { it.equals(query, ignoreCase = true) }
    }

    val addGrapeTag: (String) -> Unit = { rawGrape ->
        val trimmed = rawGrape.trim()
        if (trimmed.isNotBlank() && selectedGrapes.none { it.equals(trimmed, ignoreCase = true) }) {
            val updated = selectedGrapes + trimmed
            onGrapesChange(updated)
            // If it's not already in availableGrapes, register it for autocomplete next time
            if (availableGrapes.none { it.equals(trimmed, ignoreCase = true) }) {
                onNewGrapeEntered(trimmed)
            }
            inputQuery = ""
            expanded = false
        }
    }

    val removeGrapeTag: (String) -> Unit = { grapeToRemove ->
        val updated = selectedGrapes.filterNot { it.equals(grapeToRemove, ignoreCase = true) }
        onGrapesChange(updated)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Selected Grape Badges / Tags
        if (selectedGrapes.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selectedGrapes.forEach { grape ->
                    GrapeEditableTag(
                        grape = grape,
                        onRemove = { removeGrapeTag(grape) }
                    )
                }
            }
        }

        // 2. Autocomplete Dropdown Input Field
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { newValue ->
                    // Handle comma-separated quick typing (e.g. "Merlot,")
                    if (newValue.contains(",")) {
                        val parts = newValue.split(",")
                        parts.filter { it.trim().isNotBlank() }.forEach { addGrapeTag(it) }
                        inputQuery = ""
                    } else {
                        inputQuery = newValue
                        if (!expanded) expanded = true
                    }
                },
                label = { Text(label) },
                placeholder = { Text(placeholder) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.WineBar,
                        contentDescription = null,
                        tint = CellarGold,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (inputQuery.isNotBlank()) {
                            IconButton(
                                onClick = { addGrapeTag(inputQuery) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("${testTag}_add_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add grape tag",
                                    tint = CellarGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputQuery.isNotBlank()) {
                            addGrapeTag(inputQuery)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    .testTag(testTag)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .testTag("${testTag}_menu")
            ) {
                // If user entered a custom grape that doesn't exist yet, show option to create and register it
                if (isNewGrapeQuery) {
                    val cleanQuery = inputQuery.trim()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = CellarGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Add \"$cleanQuery\"",
                                        fontWeight = FontWeight.Bold,
                                        color = CellarGold
                                    )
                                    Text(
                                        text = "New grape variety (will be saved for autocomplete)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = { addGrapeTag(cleanQuery) },
                        modifier = Modifier
                            .background(WineBurgundy.copy(alpha = 0.2f))
                            .testTag("${testTag}_new_grape_item")
                    )
                }

                // Existing Grapes Suggestions
                if (filteredSuggestions.isNotEmpty()) {
                    filteredSuggestions.forEach { grape ->
                        val isUsedInCellar = usedGrapes.any { it.equals(grape, ignoreCase = true) }
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WineBar,
                                            contentDescription = null,
                                            tint = if (isUsedInCellar) CellarGold else CellarGold.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = grape,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isUsedInCellar) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                    if (isUsedInCellar) {
                                        Surface(
                                            color = WineBurgundy.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, CellarGold.copy(alpha = 0.45f)),
                                            modifier = Modifier.padding(start = 6.dp)
                                        ) {
                                            Text(
                                                text = "In Cellar",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.5.sp,
                                                color = CellarGold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = { addGrapeTag(grape) },
                            modifier = Modifier.testTag("${testTag}_suggestion_$grape")
                        )
                    }
                } else if (!isNewGrapeQuery && inputQuery.isNotBlank()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Press enter or '+' to add \"${inputQuery.trim()}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { addGrapeTag(inputQuery) }
                    )
                }
            }
        }

        // 3. Quick Suggestions for Common Grapes (when few or none selected)
        val popularSuggestions = remember(availableGrapes, selectedGrapes) {
            val popularDefaults = listOf(
                "Cabernet Sauvignon", "Merlot", "Pinot Noir", "Chardonnay",
                "Sauvignon Blanc", "Syrah", "Sangiovese", "Nebbiolo", "Tempranillo", "Riesling"
            )
            popularDefaults.filter { pop ->
                selectedGrapes.none { it.equals(pop, ignoreCase = true) }
            }.take(5)
        }

        if (popularSuggestions.isNotEmpty() && selectedGrapes.size < 4) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick add: ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularSuggestions) { suggestion ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { addGrapeTag(suggestion) }
                                .testTag("${testTag}_quick_$suggestion")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ $suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = CellarGold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tag badge used in the editor with an 'x' to remove.
 */
@Composable
fun GrapeEditableTag(
    grape: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WineBurgundy.copy(alpha = 0.35f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WineRose.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(CellarGold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = grape,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(18.dp)
                    .testTag("remove_grape_$grape")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $grape",
                    tint = WineRose,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Display badge for grape varietals shown in the wine list and wine detail views.
 */
@Composable
fun GrapeBadge(
    grape: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WineBurgundy.copy(alpha = 0.28f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.6.dp, WineRose.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(CellarGold)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = grape,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = CellarGold
            )
        }
    }
}

/**
 * Horizontal row of grape badges with an overflow indicator (+N).
 */
@Composable
fun GrapeBadgesRow(
    grapes: List<String>,
    modifier: Modifier = Modifier,
    maxBadges: Int = 3
) {
    if (grapes.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayed = grapes.take(maxBadges)
        displayed.forEach { grape ->
            GrapeBadge(grape = grape)
        }
        if (grapes.size > maxBadges) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "+${grapes.size - maxBadges}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Flow layout of grape badges that wraps onto multiple lines.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrapeBadgesFlow(
    grapes: List<String>,
    modifier: Modifier = Modifier
) {
    if (grapes.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        grapes.forEach { grape ->
            GrapeBadge(grape = grape)
        }
    }
}
