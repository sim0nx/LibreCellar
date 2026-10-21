package org.librecellar.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.librecellar.data.model.CollectionAccess
import org.librecellar.data.model.SharedEntry
import org.librecellar.ui.theme.AppIcons

/**
 * Unobtrusive marker for an entry of a collection shared read-only with the user:
 * a small lock and the owner's name in muted text. Own entries and entries of
 * collections shared read/write (e.g. a household collection) show nothing,
 * so they look exactly like the user's own.
 *
 * With [showWritable], read/write shared entries get a matching people marker
 * (used on detail screens, where the extra context does not clutter a list).
 */
@Composable
fun SharedEntryBadge(
    entry: SharedEntry,
    modifier: Modifier = Modifier,
    showOwner: Boolean = true,
    showWritable: Boolean = false
) {
    val access = entry.collectionAccess
    if (access == CollectionAccess.OWNER || (access == CollectionAccess.WRITE && !showWritable)) return

    val tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val isReadOnly = access == CollectionAccess.READ
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isReadOnly) AppIcons.LockOutline else AppIcons.PeopleOutline,
            contentDescription = if (isReadOnly) "Shared read-only" else "Shared collection",
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        if (showOwner && !entry.owner.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (isReadOnly) "${entry.owner} · read-only" else "${entry.owner}'s collection",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
