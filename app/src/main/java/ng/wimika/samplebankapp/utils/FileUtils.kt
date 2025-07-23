package ng.wimika.samplebankapp.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap

object FileUtils {
    
    fun isFileAnImage(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }
    
    fun isFileAVideo(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }
    
    fun isFileAnAudio(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }
    
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "Unknown"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex) ?: "Unknown"
                }
            }
        }
        return fileName
    }
    
    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
} 