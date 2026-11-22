package com.yasin.muzikcalar.scan

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

data class ScannedFile(val uri: String, val title: String)

/** Kullanıcının seçtiği klasörü (alt klasörleriyle) tarar ve ses dosyalarını bulur. */
class FolderScanner(private val context: Context) {

    private val extensions = setOf("mp3", "m4a", "opus")

    fun scan(treeUri: Uri): List<ScannedFile> {
        val result = mutableListOf<ScannedFile>()
        scanDirectory(treeUri, DocumentsContract.getTreeDocumentId(treeUri), result)
        return result
    }

    fun getFolderName(treeUri: Uri): String {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val projection = arrayOf(Document.COLUMN_DISPLAY_NAME)
        context.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) {
                    return name
                }
            }
        }
        return rootId.substringAfterLast(':').substringAfterLast('/').ifBlank { rootId }
    }

    private fun scanDirectory(treeUri: Uri, documentId: String, result: MutableList<ScannedFile>) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE
        )
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val childId = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: continue
                val mimeType = cursor.getString(2)
                if (mimeType == Document.MIME_TYPE_DIR) {
                    scanDirectory(treeUri, childId, result)
                } else if (name.substringAfterLast('.', "").lowercase() in extensions) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    result.add(ScannedFile(fileUri.toString(), name.substringBeforeLast('.')))
                }
            }
        }
    }
}
