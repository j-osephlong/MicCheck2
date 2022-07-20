package com.jlong.miccheck.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SegmentedButton (modifier: Modifier = Modifier, buttons: @Composable RowScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
    ) {
        Row (Modifier.fillMaxWidth()){
            buttons()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.SegmentButtton (
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector? = null,
    text: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    )
    val contentColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    )
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        modifier = modifier
            .height(40.dp)
            .fillMaxWidth()
            .weight(1f),
        onClick = onClick
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null ){
                Icon(icon, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
                text()
            }
        }
    }
    Divider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxHeight()
            .width(1.dp)
    )

}