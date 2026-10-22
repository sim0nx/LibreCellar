package org.librecellar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.LabelReader
import org.librecellar.data.model.CountryHelper
import org.librecellar.data.model.LabelSuggestions
import org.librecellar.data.model.LocaleFormat
import org.librecellar.ui.theme.AppIcons
import org.librecellar.ui.theme.CellarGold
import org.librecellar.ui.theme.WineBurgundyLight

/** Which of a wine's two pictures. */
enum class LabelSide { FRONT, BACK }

/** Wine fields that can be filled from the label. */
enum class LabelField(val label: String) {
    NAME("Name"), VINTAGE("Vintage"), SIZE("Size"), ALCOHOL("Alcohol"), COUNTRY("Country"), PRODUCER("Producer")
}

/** The suggestions as values for the form fields, in the order they are shown. */
fun LabelSuggestions.formValues(): List<Pair<LabelField, String>> = listOfNotNull(
    vintage?.let { LabelField.VINTAGE to it.toString() },
    size?.let { LabelField.SIZE to it },
    alcoholPercent?.let { LabelField.ALCOHOL to LocaleFormat.formatDecimalInput(it) },
    name?.let { LabelField.NAME to it },
    country?.let { LabelField.COUNTRY to it },
    producer?.let { LabelField.PRODUCER to it }
)

/** The front picture and the optional back label picture side by side; tap a tile to take or pick one. */
@Composable
fun LabelPhotoTiles(
    front: String?,
    back: String?,
    onPick: (LabelSide) -> Unit,
    onClear: (LabelSide) -> Unit,
    modifier: Modifier = Modifier,
    wineType: String = "RED",
    tileSize: Dp = 140.dp
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
        PhotoTile(front, "Front", "Tap to take or pick", wineType, tileSize, { onPick(LabelSide.FRONT) }, { onClear(LabelSide.FRONT) }, "front_photo_tile")
        PhotoTile(back, "Back label", "Optional", wineType, tileSize, { onPick(LabelSide.BACK) }, { onClear(LabelSide.BACK) }, "back_photo_tile")
    }
}

@Composable
private fun PhotoTile(
    photo: String?,
    title: String,
    hint: String,
    wineType: String,
    size: Dp,
    onPick: () -> Unit,
    onClear: () -> Unit,
    tag: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = size, height = size * 1.25f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (photo.isNullOrBlank()) 1.dp else 2.dp,
                    color = if (photo.isNullOrBlank()) CellarGold.copy(alpha = 0.35f) else CellarGold.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onPick)
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            if (!photo.isNullOrBlank()) {
                WineBottleImage(
                    photoUrl = photo,
                    wineType = wineType,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(14.dp)
                )
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(30.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                        .testTag("${tag}_clear")
                ) {
                    Icon(AppIcons.Close, contentDescription = "Remove $title picture", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                    Icon(AppIcons.CameraAlt, contentDescription = null, tint = CellarGold.copy(alpha = 0.8f), modifier = Modifier.size(34.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(hint, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

/**
 * Reads the label pictures on the device and offers what it found as chips: tap one to fill that
 * field, or "Use all" to fill the fields that are still empty ([isUnset]). Nothing is filled
 * without a tap.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabelSuggestionsPanel(
    front: String?,
    back: String?,
    currentValue: (LabelField) -> String,
    isUnset: (LabelField) -> Boolean,
    onApply: (LabelField, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var reading by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<LabelSuggestions?>(null) }

    LaunchedEffect(front, back) {
        if (front.isNullOrBlank() && back.isNullOrBlank()) {
            suggestions = null
            return@LaunchedEffect
        }
        reading = true
        suggestions = LabelReader.read(context, front, back)
        reading = false
    }

    val values = suggestions?.formValues().orEmpty()
    AnimatedVisibility(visible = reading || suggestions != null, modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("label_suggestions")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.AutoAwesome, contentDescription = null, tint = CellarGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                reading -> "Reading the label…"
                                values.isEmpty() -> "No details found on the label"
                                else -> "Found on the label"
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (values.isEmpty() && !reading) "Try a sharper, straight photo of the label"
                            else "On this device only · tap to use",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reading) {
                        CircularProgressIndicator(color = CellarGold, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else if (values.any { (field, value) -> isUnset(field) && currentValue(field) != value }) {
                        TextButton(
                            onClick = {
                                values.forEach { (field, value) -> if (isUnset(field)) onApply(field, value) }
                            },
                            modifier = Modifier.testTag("label_use_all")
                        ) { Text("Use all", color = CellarGold) }
                    }
                }
                if (values.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        values.forEach { (field, value) ->
                            val applied = currentValue(field).trim().equals(value, ignoreCase = true)
                            AssistChip(
                                onClick = { onApply(field, value) },
                                enabled = !applied,
                                label = {
                                    Text(
                                        text = chipText(field, value),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = if (applied) {
                                    { Icon(AppIcons.Check, contentDescription = "Used", modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = WineBurgundyLight.copy(alpha = 0.25f),
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.testTag("label_chip_${field.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun chipText(field: LabelField, value: String): String = when (field) {
    LabelField.VINTAGE -> value
    LabelField.SIZE -> value.replace("L", " L")
    LabelField.ALCOHOL -> "$value %"
    LabelField.COUNTRY -> "${CountryHelper.getFlagForCountry(value)} $value"
    LabelField.NAME -> "“$value”"
    LabelField.PRODUCER -> "by $value"
}
