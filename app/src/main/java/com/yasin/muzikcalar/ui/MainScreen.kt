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
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    var showPlayer by rememberSaveable { mutableStateOf(false) }

    if (showPlayer && viewModel.player.currentTitle != null) {
        PlayerScreen(player = viewModel.player, onClose = { showPlayer = false })
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
                0 -> TracksScreen(tracks = tracks, onTrackClick = { viewModel.playTrack(it) })
                1 -> MessageScreen(text = "Favorilerin burada görünecek.")
                2 -> MessageScreen(text = "Son oynattığın parçalar burada görünecek.")
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
