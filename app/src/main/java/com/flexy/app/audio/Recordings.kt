package com.flexy.app.audio

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Saved recordings live in the app's PRIVATE folder. Nothing is uploaded anywhere. */
object Recordings {
    fun dir(context: Context): File = File(context.filesDir, "recordings").apply { mkdirs() }

    fun list(context: Context): List<File> =
        dir(context).listFiles { f -> f.extension == "wav" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun deleteAll(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }

    /** Opens Android's share sheet. Only runs when the user taps Share. */
    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share recording"))
    }
}
