package org.librecellar.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.librecellar.data.model.Grape
import org.librecellar.data.repository.WineRepository
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.wineTypeColor

/**
 * The user's grape list (stored on the server, shared with the web app): red and white grapes
 * suggested when entering wines. Grapes used by wines show how many and cannot be removed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrapeSettingsContent(repository: WineRepository) {
    val grapes by repository.grapes.collectAsState()
    val wines by repository.wines.collectAsState()
    val scope = rememberCoroutineScope()
    var available by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        available = repository.reloadGrapes()
        // Usage counts need every wine, not only the pages scrolled through
        repository.loadAllWines()
    }

    // Wines naming each grape (lowercase); these cannot be removed
    val usage = remember(wines) {
        wines.flatMap { w -> w.varietalList.map { it.lowercase() }.distinct() }
            .groupingBy { it }.eachCount()
    }
    val listed = grapes.map { it.name.lowercase() }.toSet()
    val unlisted = remember(wines, grapes) {
        wines.flatMap { it.varietalList }.distinctBy { it.lowercase() }
            .filter { it.lowercase() !in listed }
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    fun save(list: List<Grape>) {
        busy = true
        scope.launch {
            repository.saveGrapes(list)
            busy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Grapes suggested when entering a wine, red ones first for red and rosé wines. " +
                "Tap a grape to switch it between red and white. Grapes used by wines (number shown) " +
                "cannot be removed. The list is shared with the web app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when (available) {
            null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CellarGold, modifier = Modifier.size(28.dp))
            }

            false -> Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "The grape list could not be loaded. It needs a connection to an up-to-date LibreCellar server.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }

            true -> {
                listOf("RED" to "Red grapes", "WHITE" to "White grapes").forEach { (color, title) ->
                    GrapeColorCard(
                        color = color,
                        title = title,
                        grapes = grapes.filter { it.color == color }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }),
                        usage = usage,
                        enabled = !busy,
                        onAdd = { name ->
                            if (name.lowercase() in listed) {
                                repository.reportProblem("$name is already listed")
                                false
                            } else {
                                save(grapes + Grape(name, color))
                                true
                            }
                        },
                        onSwitch = { grape ->
                            val other = if (grape.color == "RED") "WHITE" else "RED"
                            save(grapes.map { if (it.name == grape.name) it.copy(color = other) else it })
                        },
                        onRemove = { grape -> save(grapes.filter { it.name != grape.name }) }
                    )
                }

                if (unlisted.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("On your wines, not in the list", fontWeight = FontWeight.Bold)
                            Text(
                                "Tap to add them as red or white grapes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                unlisted.forEach { name ->
                                    var menuOpen by remember { mutableStateOf(false) }
                                    Box {
                                        InputChip(
                                            selected = false,
                                            enabled = !busy,
                                            onClick = { menuOpen = true },
                                            label = { Text(name, fontSize = 12.sp) },
                                            leadingIcon = { Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        )
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Add as red grape") },
                                                onClick = {
                                                    menuOpen = false
                                                    save(grapes + Grape(name, "RED"))
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Add as white grape") },
                                                onClick = {
                                                    menuOpen = false
                                                    save(grapes + Grape(name, "WHITE"))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showReset = true },
                    enabled = !busy,
                    modifier = Modifier.testTag("reset_grapes_button")
                ) {
                    Icon(AppIcons.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset to defaults")
                }
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset grape list?") },
            text = {
                Text(
                    "Your changes to the list are discarded and the default red and white grapes come back. " +
                        "Grapes you added that are still used by wines are kept. Wines are not changed."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReset = false
                    busy = true
                    scope.launch {
                        repository.resetGrapes()
                        busy = false
                    }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrapeColorCard(
    color: String,
    title: String,
    grapes: List<Grape>,
    usage: Map<String, Int>,
    enabled: Boolean,
    onAdd: (String) -> Boolean,
    onSwitch: (Grape) -> Unit,
    onRemove: (Grape) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    val tint = wineTypeColor(color)
    val add = {
        val name = newName.trim()
        if (name.isNotEmpty() && onAdd(name)) newName = ""
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("grapes_${color.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(tint)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    "${grapes.size} varieties",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(if (color == "RED") "Add a red grape" else "Add a white grape") },
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                trailingIcon = {
                    IconButton(onClick = add, enabled = enabled && newName.isNotBlank()) {
                        Icon(AppIcons.Add, contentDescription = "Add")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("add_grape_${color.lowercase()}")
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                grapes.forEach { grape ->
                    val used = usage[grape.name.lowercase()] ?: 0
                    InputChip(
                        selected = true,
                        enabled = enabled,
                        onClick = { onSwitch(grape) },
                        label = { Text(grape.name, fontSize = 12.sp) },
                        trailingIcon = {
                            if (used > 0) {
                                Surface(color = tint, shape = CircleShape) {
                                    Text(
                                        "$used",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            } else {
                                IconButton(onClick = { onRemove(grape) }, enabled = enabled, modifier = Modifier.size(20.dp)) {
                                    Icon(AppIcons.Close, contentDescription = "Remove ${grape.name}", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = tint.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier.testTag("grape_chip_${grape.name}")
                    )
                }
            }
        }
    }
}
