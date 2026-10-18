package org.librecellar.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.librecellar.ui.theme.CellarGold

@Composable
fun RatingBar(
    rating: Int?,
    onRatingChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    maxStars: Int = 5,
    starSize: Dp = 24.dp,
    tint: Color = CellarGold
) {
    val currentRating = rating ?: 0
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxStars) {
            val isFilled = i <= currentRating
            val icon = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline
            val clickableModifier = if (onRatingChanged != null) {
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onRatingChanged(i)
                    }
                    .padding(2.dp)
                    .testTag("rating_star_$i")
            } else {
                Modifier.padding(1.dp)
            }

            Icon(
                imageVector = icon,
                contentDescription = "$i stars",
                tint = if (isFilled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = clickableModifier.size(starSize)
            )
        }
    }
}
