package com.example.jobhunter.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

object FileUtils {

    data class SelectedFileInfo(
        val uri: Uri,
        val name: String,
        val sizeFormatted: String,
        val mimeType: String,
        val extension: String
    )

    fun getFileInfo(context: Context, uri: Uri): SelectedFileInfo {
        var name = "resume"
        var sizeBytes: Long = 0

        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex) ?: "resume"
                }
                if (sizeIndex != -1) {
                    sizeBytes = it.getLong(sizeIndex)
                }
            }
        }

        var mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        var extension = ""
        val lastDotIndex = name.lastIndexOf('.')
        if (lastDotIndex != -1) {
            extension = name.substring(lastDotIndex + 1).lowercase(Locale.getDefault())
        }

        if (mimeType == "application/octet-stream" && extension.isNotEmpty()) {
            val fromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (fromExtension != null) {
                mimeType = fromExtension
            }
        }

        val sizeFormatted = formatFileSize(sizeBytes)
        return SelectedFileInfo(uri, name, sizeFormatted, mimeType, extension)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    fun isSupportedResume(fileName: String, mimeType: String): Boolean {
        val lowerName = fileName.lowercase(Locale.getDefault())
        val isPdf = lowerName.endsWith(".pdf") || mimeType == "application/pdf"
        val isDoc = lowerName.endsWith(".doc") || mimeType == "application/msword"
        val isDocx = lowerName.endsWith(".docx") || 
            mimeType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        return isPdf || isDoc || isDocx
    }

    /**
     * Copies the content URI to a temporary cache file so OkHttp can read and stream it.
     */
    fun createTempFileFromUri(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
            context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
