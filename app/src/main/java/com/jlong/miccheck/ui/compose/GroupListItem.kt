package com.jlong.miccheck.ui.compose

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jlong.miccheck.RecordingGroup
import com.jlong.miccheck.ui.theme.surfaceColorAtElevation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCard(group: RecordingGroup, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        onClick = onClick
    ) {
        Column {
            if (group.imgUri != null)
                AsyncImage(
                    model = Uri.parse((group.imgUri)),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .height(148.dp),
                    contentScale = ContentScale.Crop
                )
            else
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .height(148.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                ) {
                    Row (Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        if (group.uuid == starredGroupUUID)
                            Surface (
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(62.dp)
                            ){
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(18.dp)){
                                    Icon(
                                        Icons.Rounded.Star, null
                                    )
                                }
                            }
                        else
                            Text(
                                group.name.first().toString(),
                                style = MaterialTheme.typography.displayMedium
                            )
                    }
                }
            Text(
                group.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(12.dp, 12.dp - (if (group.imgUri == null) 2.dp else 0.dp), 12.dp, 0.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${group.recordings.size} Recording${if (group.recordings.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(12.dp, 0.dp, 12.dp, 12.dp)
            )
        }
    }
}

@Composable
fun SmallGroupListItem(
    group: RecordingGroup,
    onClick: () -> Unit
) {
    if (group.imgUri != null)
        group.imgUri?.let {
            AsyncImage(
                model = Uri.parse(it),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick() },
                contentScale = ContentScale.Crop,
            )
        }
    else
        FilledTonalButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(72.dp),
        ) {
            if (group.uuid == starredGroupUUID)
                Icon(
                    Icons.Rounded.Star, null
                )
            else
                Text(group.name.first().toString(), style = MaterialTheme.typography.headlineLarge)
        }
}
