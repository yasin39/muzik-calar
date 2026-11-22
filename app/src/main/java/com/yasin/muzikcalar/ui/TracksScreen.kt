package com.yasin.muzikcalar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yasin.muzikcalar.data.TrackItem

@Composable
fun TracksScreen(tracks: List<TrackItem>, emptyText: String, onTrackClick: (Int) -> Unit) {
    if (tracks.isEmpty()) {
        MessageScreen(text = emptyText)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            ListItem(
                modifier = Modifier.clickable { onTrackClick(index) },
                headlineContent = { Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    val detail = if (track.lastPositionMs > 0) {
                        "${track.folderName} · Kaldığın yer: ${formatTime(track.lastPositionMs)}"
                    } else {
                        track.folderName
                    }
                    Text(detail, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
