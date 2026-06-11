package com.onlytechtake.repairapp.core.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.onlytechtake.repairapp.R
import java.io.File

class ShareDocumentIntentFactory(
    private val context: Context,
) {
    fun createPdfShareIntent(file: File, title: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_pdf_text))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
