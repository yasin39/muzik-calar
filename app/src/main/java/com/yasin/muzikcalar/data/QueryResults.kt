package com.yasin.muzikcalar.data

import androidx.room.Embedded

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val trackCount: Int
)

data class TrackItem(
    val id: Long,
    val uri: String,
    val title: String,
    val folderName: String
)
