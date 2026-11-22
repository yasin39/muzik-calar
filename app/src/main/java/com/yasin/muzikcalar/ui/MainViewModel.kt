package com.yasin.muzikcalar.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yasin.muzikcalar.data.AppDatabase
import com.yasin.muzikcalar.data.FolderEntity
import com.yasin.muzikcalar.data.FolderWithCount
import com.yasin.muzikcalar.data.TrackEntity
import com.yasin.muzikcalar.data.TrackItem
import com.yasin.muzikcalar.player.PlayerConnection
import com.yasin.muzikcalar.scan.FolderScanner
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)
    private val scanner = FolderScanner(application)
    private val collator = Collator.getInstance(Locale.forLanguageTag("tr-TR"))
    private val scanning = MutableStateFlow(false)

    val tracks: StateFlow<List<TrackItem>> = database.trackDao().observeAll()
        .map { list -> list.sortedWith(compareBy(collator) { it.title }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderWithCount>> = database.folderDao().observeWithCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isScanning: StateFlow<Boolean> = scanning

    val player = PlayerConnection(application)

    init {
        rescanAll()
    }

    fun playTrack(index: Int) {
        player.play(tracks.value, index)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }

    fun addFolder(treeUri: Uri) {
        viewModelScope.launch {
            scanning.value = true
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val folder = FolderEntity(uri = treeUri.toString(), name = scanner.getFolderName(treeUri))
                    val id = database.folderDao().insert(folder)
                    if (id != -1L) {
                        scanFolder(folder.copy(id = id))
                    }
                }
            } finally {
                scanning.value = false
            }
        }
    }

    fun removeFolder(folder: FolderEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.trackDao().deleteByFolder(folder.id)
                database.folderDao().delete(folder)
                try {
                    getApplication<Application>().contentResolver.releasePersistableUriPermission(
                        Uri.parse(folder.uri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // İzin zaten yoksa yapacak bir şey yok.
                }
            }
        }
    }

    fun rescanAll() {
        if (scanning.value) {
            return
        }
        viewModelScope.launch {
            scanning.value = true
            try {
                withContext(Dispatchers.IO) {
                    for (folder in database.folderDao().getAll()) {
                        scanFolder(folder)
                    }
                }
            } finally {
                scanning.value = false
            }
        }
    }

    private suspend fun scanFolder(folder: FolderEntity) {
        try {
            val files = scanner.scan(Uri.parse(folder.uri))
            val tracks = files.map { TrackEntity(folderId = folder.id, uri = it.uri, title = it.title) }
            database.trackDao().syncFolder(folder.id, tracks)
        } catch (e: SecurityException) {
            // Klasöre erişim izni kalkmış ya da klasör silinmiş; mevcut listeyi koru.
        } catch (e: IllegalArgumentException) {
            // Klasör artık bulunamıyor; mevcut listeyi koru.
        }
    }
}
