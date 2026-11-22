package com.yasin.muzikcalar.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasin.muzikcalar.R

private data class Tab(val label: String, @DrawableRes val icon: Int)

private val tabs = listOf(
    Tab("Parçalar", R.drawable.ic_tracks),
    Tab("Favorilerim", R.drawable.ic_favorite),
    Tab("Son Oynatılanlar", R.drawable.ic_history),
    Tab("Klasörler", R.drawable.ic_folder)
)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val recentTracks by viewModel.recentTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    if (showPlayer && viewModel.player.currentTitle != null) {
        val currentTrack = tracks.find { it.uri == viewModel.player.currentMediaId }
        PlayerScreen(
            player = viewModel.player,
            isFavorite = currentTrack?.isFavorite == true,
            onToggleFavorite = { currentTrack?.let { viewModel.toggleFavorite(it) } },
            onClose = { showPlayer = false }
        )
        return
    }

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(player = viewModel.player, onOpen = { showPlayer = true })
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(tab.label, maxLines = 2, textAlign = TextAlign.Center) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> TracksScreen(
                    tracks = tracks,
                    emptyText = "Henüz parça yok.\nKlasörler tabından klasör ekleyebilirsin.",
                    onTrackClick = { viewModel.playTrack(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
                1 -> TracksScreen(
                    tracks = favoriteTracks,
                    emptyText = "Henüz favorin yok.\nParçaların yanındaki kalbe dokunarak ekleyebilirsin.",
                    onTrackClick = { viewModel.playFavorite(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
                2 -> TracksScreen(
                    tracks = recentTracks,
                    emptyText = "Henüz bir parça dinlemedin.",
                    onTrackClick = { viewModel.playRecent(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
                else -> FoldersScreen(
                    folders = folders,
                    isScanning = isScanning,
                    onAddFolder = { viewModel.addFolder(it) },
                    onRemoveFolder = { viewModel.removeFolder(it) },
                    onRescan = { viewModel.rescanAll() }
                )
            }
        }
    }
}
