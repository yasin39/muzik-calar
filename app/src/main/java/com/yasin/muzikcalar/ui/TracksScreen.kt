package com.yasin.muzikcalar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yasin.muzikcalar.R
import com.yasin.muzikcalar.data.TrackItem

val FavoriteColor = Color(0xFFE53935)

@Composable
fun TracksScreen(
    tracks: List<TrackItem>,
    emptyText: String,
    onTrackClick: (Int) -> Unit,
    onToggleFavorite: (TrackItem) -> Unit
) {
    if (tracks.isEmpty()) {
        MessageScreen(text = emptyText)
        return
    }
    val positionColor = if (isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            ListItem(
                modifier = Modifier.clickable { onTrackClick(index) },
                headlineContent = { Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    val detail = buildAnnotatedString {
                        append(track.folderName)
                        if (track.lastPositionMs > 0) {
                            append(" · ")
                            withStyle(SpanStyle(color = positionColor)) {
                                append("Kaldığın yer: ${formatTime(track.lastPositionMs)}")
                            }
                        }
                    }
                    Text(detail, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                trailingContent = {
                    IconButton(onClick = { onToggleFavorite(track) }) {
                        if (track.isFavorite) {
                            Icon(
                                painterResource(R.drawable.ic_favorite),
                                contentDescription = "Favorilerden çıkar",
                                tint = FavoriteColor
                            )
                        } else {
                            Icon(painterResource(R.drawable.ic_favorite_border), contentDescription = "Favorilere ekle")
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MessageScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
