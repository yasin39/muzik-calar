package com.yasin.muzikcalar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yasin.muzikcalar.R
import com.yasin.muzikcalar.player.PlayerConnection
import java.util.Locale
import kotlinx.coroutines.delay

private val speeds = listOf(1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f)

/** Alt menünün üstündeki küçük çalma çubuğu. */
@Composable
fun MiniPlayer(player: PlayerConnection, onOpen: () -> Unit) {
    val title = player.currentTitle ?: return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(
                    player.currentFolder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = { player.togglePlayPause() }) {
                Icon(
                    painterResource(if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = if (player.isPlaying) "Duraklat" else "Çal"
                )
            }
            IconButton(onClick = { player.next() }, enabled = player.hasNext) {
                Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Sonraki")
            }
        }
    }
}

/** Tam ekran oynatıcı. */
@Composable
fun PlayerScreen(
    player: PlayerConnection,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var positionMs by remember { mutableLongStateOf(player.positionMs) }
    var durationMs by remember { mutableLongStateOf(player.durationMs) }
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!isDragging) {
                positionMs = player.positionMs
            }
            durationMs = player.durationMs
            delay(500)
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(painterResource(R.drawable.ic_expand_more), contentDescription = "Kapat")
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    if (isFavorite) {
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
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = player.currentTitle ?: "",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = player.currentFolder,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            val shownPosition = if (isDragging) dragValue.toLong() else positionMs
            Slider(
                value = shownPosition.coerceIn(0L, durationMs).toFloat(),
                valueRange = 0f..maxOf(durationMs, 1L).toFloat(),
                onValueChange = {
                    isDragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    player.seekTo(dragValue.toLong())
                    positionMs = dragValue.toLong()
                    isDragging = false
                }
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(shownPosition), style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.weight(1f))
                Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Sol tarafta yer tutucu: oynatma düğmeleri ortada kalsın diye tekrarla düğmesiyle aynı genişlikte.
                Spacer(modifier = Modifier.size(48.dp))
                IconButton(onClick = { player.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_skip_previous), contentDescription = "Önceki")
                }
                FilledIconButton(onClick = { player.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
                    Icon(
                        painterResource(if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                        contentDescription = if (player.isPlaying) "Duraklat" else "Çal",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { player.next() }, enabled = player.hasNext, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = "Sonraki")
                }
                IconButton(onClick = { player.toggleRepeatOne() }, modifier = Modifier.size(48.dp)) {
                    Icon(
                        painterResource(R.drawable.ic_repeat_one),
                        contentDescription = if (player.isRepeatingOne) "Tekrarı kapat" else "Parçayı tekrarla",
                        tint = if (player.isRepeatingOne) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Text("Hız", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (speed in speeds) {
                    FilterChip(
                        selected = kotlin.math.abs(player.speed - speed) < 0.01f,
                        onClick = { player.changeSpeed(speed) },
                        label = { Text(String.format(Locale.US, "%.1fx", speed)) }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
