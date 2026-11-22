package com.yasin.muzikcalar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class FolderDao {

    @Query(
        "SELECT folders.*, " +
            "(SELECT COUNT(*) FROM tracks WHERE tracks.folderId = folders.id) AS trackCount " +
            "FROM folders ORDER BY name COLLATE NOCASE"
    )
    abstract fun observeWithCounts(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders")
    abstract suspend fun getAll(): List<FolderEntity>

    /** Klasör zaten ekliyse -1 döner. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(folder: FolderEntity): Long

    @Delete
    abstract suspend fun delete(folder: FolderEntity)
}
