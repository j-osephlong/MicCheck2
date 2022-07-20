package com.jlong.miccheck.ui.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedChip (
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    leadingIcon: ImageVector? = null,
    showLeadingIcon: Boolean = true,
    trailingIcon: ImageVector? = null,
    colors: ChipColors = ChipColors.outlinedChipColors(),
    shadowElevation: Dp = 0.dp,
    onClickLeadingIcon: (() -> Unit)? = null,
    onClickTrailingIcon: (() -> Unit)? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val startPadding by animateDpAsState(targetValue = if (showLeadingIcon && leadingIcon != null) 8.dp else 16.dp)
    val color by animateColorAsState(targetValue = if (enabled) colors.color else colors.disabledColor)
    val contentColor by animateColorAsState(targetValue = if (enabled) colors.contentColor else colors.disabledContentColor)
    val leadingIconColor by animateColorAsState(targetValue = if (enabled) colors.leadingIconColor else colors.disabledContentColor)

    Surface(
        border = BorderStroke(1.dp, colors.outline),
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(32.dp),
        onClick = onClick ?: {},
        shadowElevation = shadowElevation
    ) {
        Row (
            Modifier
                .fillMaxHeight()
                .animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(startPadding))
            AnimatedVisibility (leadingIcon!=null && showLeadingIcon, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                Row {
                    Icon(
                        leadingIcon ?: Icons.Rounded.Error,
                        null,
                        modifier = Modifier
                            .size(18.dp)
                            .then(if (onClickLeadingIcon != null) Modifier.clickable { onClickLeadingIcon() } else Modifier),
                        tint = leadingIconColor
                    )
                    Spacer(Modifier.width(8.dp))
                }
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
                text()
            }
            if (trailingIcon!=null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    trailingIcon,
                    null,
                    Modifier
                        .size(18.dp)
                        .apply {
                            if (onClickTrailingIcon != null) clickable { onClickTrailingIcon() }
                        }
                )
            }
            Spacer(modifier = Modifier.width(if (trailingIcon!=null) 8.dp else 16.dp))
        }
    }
}

data class ChipColors (
    val color: Color,
    val contentColor: Color,
    val leadingIconColor: Color,
    val outline: Color,
    val disabledColor: Color,
    val disabledContentColor: Color
) {
    companion object {
        @Composable
        fun outlinedChipColors() : ChipColors =
            ChipColors(
                Color.Transparent,
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            )

        @Composable
        fun filledChipColors() : ChipColors =
            ChipColors(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
                Color.Transparent,
                MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)
            )
    }
}