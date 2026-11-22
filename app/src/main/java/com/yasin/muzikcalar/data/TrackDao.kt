package com.yasin.muzikcalar.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TrackDao {

    @Query(
        "SELECT tracks.id, tracks.uri, tracks.title, folders.name AS folderName, " +
            "tracks.lastPositionMs, tracks.lastPlayedAt " +
            "FROM tracks INNER JOIN folders ON folders.id = tracks.folderId"
    )
    abstract fun observeAll(): Flow<List<TrackItem>>

    @Query(
        "SELECT tracks.id, tracks.uri, tracks.title, folders.name AS folderName, " +
            "tracks.lastPositionMs, tracks.lastPlayedAt " +
            "FROM tracks INNER JOIN folders ON folders.id = tracks.folderId " +
            "WHERE tracks.lastPlayedAt IS NOT NULL ORDER BY tracks.lastPlayedAt DESC LIMIT 50"
    )
    abstract fun observeRecent(): Flow<List<TrackItem>>

    @Query("SELECT lastPositionMs FROM tracks WHERE uri = :uri")
    abstract suspend fun getPosition(uri: String): Long?

    @Query("SELECT uri FROM tracks WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 1")
    abstract suspend fun getLastPlayedUri(): String?

    @Query("UPDATE tracks SET lastPositionMs = :positionMs WHERE uri = :uri")
    abstract suspend fun savePosition(uri: String, positionMs: Long)

    @Query("UPDATE tracks SET lastPlayedAt = :time WHERE uri = :uri")
    abstract suspend fun markPlayed(uri: String, time: Long)

    @Query("SELECT uri FROM tracks WHERE folderId = :folderId")
    abstract suspend fun getUris(folderId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE folderId = :folderId AND uri = :uri")
    abstract suspend fun deleteByUri(folderId: Long, uri: String)

    @Query("DELETE FROM tracks WHERE folderId = :folderId")
    abstract suspend fun deleteByFolder(folderId: Long)

    /**
     * Klasörün parçalarını tarama sonucuyla eşitler. Var olan kayıtlara dokunmaz
     * (ileride kaldığı yer gibi bilgiler bu kayıtlarda tutulacak), sadece
     * yeni dosyaları ekler ve artık olmayanları siler.
     */
    @Transaction
    open suspend fun syncFolder(folderId: Long, tracks: List<TrackEntity>) {
        val foundUris = tracks.map { it.uri }.toSet()
        for (uri in getUris(folderId)) {
            if (uri !in foundUris) {
                deleteByUri(folderId, uri)
            }
        }
        insertAll(tracks)
    }
}
