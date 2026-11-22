package com.yasin.muzikcalar.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yasin.muzikcalar.R
import com.yasin.muzikcalar.data.FolderEntity
import com.yasin.muzikcalar.data.FolderWithCount

@Composable
fun FoldersScreen(
    folders: List<FolderWithCount>,
    isScanning: Boolean,
    onAddFolder: (Uri) -> Unit,
    onRemoveFolder: (FolderEntity) -> Unit,
    onRescan: () -> Unit
) {
    var folderToRemove by remember { mutableStateOf<FolderEntity?>(null) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            onAddFolder(uri)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Klasörler",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRescan, enabled = !isScanning) {
                    Icon(painterResource(R.drawable.ic_refresh), contentDescription = "Yeniden tara")
                }
            }
            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (folders.isEmpty()) {
                MessageScreen(text = "Henüz klasör eklemedin.\nAşağıdaki düğmeyle müzik klasörünü seç.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(folders, key = { it.folder.id }) { item ->
                        ListItem(
                            headlineContent = {
                                Text(item.folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = { Text("${item.trackCount} parça") },
                            leadingContent = {
                                Icon(painterResource(R.drawable.ic_folder), contentDescription = null)
                            },
                            trailingContent = {
                                IconButton(onClick = { folderToRemove = item.folder }) {
                                    Icon(painterResource(R.drawable.ic_delete), contentDescription = "Kaldır")
                                }
                            }
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { folderPicker.launch(null) },
            icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
            text = { Text("Klasör ekle") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    val pending = folderToRemove
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { folderToRemove = null },
            title = { Text("Klasörü kaldır") },
            text = { Text("\"${pending.name}\" listeden kaldırılsın mı? Telefondaki dosyaların silinmez.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveFolder(pending)
                    folderToRemove = null
                }) {
                    Text("Kaldır")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRemove = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}
