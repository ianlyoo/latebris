package io.github.ianlyoo.latebris.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.ianlyoo.latebris.ui.theme.Figures
import io.github.ianlyoo.latebris.ui.theme.LatebrisSpacing
import io.github.ianlyoo.latebris.ui.theme.LocalSignColors

/**
 * The location sign: an ink panel that names where the device "is". It is the one
 * element every screen leads with; yellow lettering means the place is live.
 */
@Composable
fun LocationSign(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val signs = LocalSignColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = signs.ink,
        contentColor = signs.onInk,
        border = if (isSystemInDarkTheme()) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LatebrisSpacing.lg, vertical = LatebrisSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.xs),
            content = content,
        )
    }
}

enum class SignalState { Live, Staged, Off }

/** A stack of sign lines, one per signal, like an overhead direction sign. */
@Composable
fun SignalBoard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(content = content)
    }
}

@Composable
fun SignalLine(
    pictogram: Pictogram,
    label: String,
    detail: String,
    state: SignalState,
    stateLabel: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val signs = LocalSignColors.current
    val (container, content, muted) = when (state) {
        SignalState.Live -> Triple(signs.yellow, signs.onYellow, signs.onYellow.copy(alpha = 0.78f))
        SignalState.Staged -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SignalState.Off -> Triple(
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(modifier = modifier) {
        Surface(color = container, contentColor = content) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .padding(horizontal = LatebrisSpacing.lg, vertical = LatebrisSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.md),
            ) {
                PictogramIcon(pictogram = pictogram, tint = if (state == SignalState.Off) muted else content)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall.merge(Figures),
                        color = muted,
                    )
                }
                Text(
                    text = stateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.End,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = LatebrisSpacing.sectionGap, bottom = LatebrisSpacing.sm)
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        trailing?.invoke(this)
    }
}

enum class Requirement { Met, Missing, Optional }

/** One requirement and whether it is met; an optional one that is absent is not an error. */
@Composable
fun RequirementLine(
    label: String,
    value: String,
    status: Requirement,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val signs = LocalSignColors.current
    val tint = when (status) {
        Requirement.Met -> signs.ready
        Requirement.Missing -> MaterialTheme.colorScheme.error
        Requirement.Optional -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = LatebrisSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.md),
    ) {
        Icon(
            imageVector = when (status) {
                Requirement.Met -> Icons.Filled.CheckCircle
                Requirement.Missing -> Icons.Filled.Warning
                Requirement.Optional -> Icons.Filled.Info
            },
            contentDescription = null,
            tint = tint,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = if (status == Requirement.Missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

enum class NoticeTone { Info, Problem }

/** A tinted note with a leading icon; it explains why something is blocked or what happens next. */
@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    tone: NoticeTone = NoticeTone.Info,
    action: @Composable (() -> Unit)? = null,
) {
    val container = when (tone) {
        NoticeTone.Info -> MaterialTheme.colorScheme.surfaceContainerHigh
        NoticeTone.Problem -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when (tone) {
        NoticeTone.Info -> MaterialTheme.colorScheme.onSurface
        NoticeTone.Problem -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(LatebrisSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LatebrisSpacing.sm),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.md)) {
                Icon(
                    imageVector = if (tone == NoticeTone.Problem) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(text = text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
            action?.let {
                Box(modifier = Modifier.padding(start = 32.dp)) { it() }
            }
        }
    }
}

/** The small yellow plate in the top bar while a simulation runs. */
@Composable
fun LivePlate(modifier: Modifier = Modifier) {
    val signs = LocalSignColors.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = signs.yellow,
        contentColor = signs.onYellow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LatebrisSpacing.sm, vertical = LatebrisSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LatebrisSpacing.xs),
        ) {
            PictogramIcon(pictogram = Pictogram.Gps, size = 16.dp)
            Text(text = "Live", style = MaterialTheme.typography.labelLarge)
        }
    }
}
