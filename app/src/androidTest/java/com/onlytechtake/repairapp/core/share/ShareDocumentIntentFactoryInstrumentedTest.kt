package com.onlytechtake.repairapp.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShareDocumentIntentFactoryInstrumentedTest {
    @Test
    fun pdfShareIntentUsesFileProviderReadGrantAndCopiesToSharedCache() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.cacheDir, "source-status.pdf").apply {
            writeBytes(byteArrayOf(37, 80, 68, 70))
        }

        val intent = ShareDocumentIntentFactory(context).createPdfShareIntent(source, "Stato avanzamento")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/pdf", intent.type)
        assertEquals("Stato avanzamento", intent.getStringExtra(Intent.EXTRA_SUBJECT))
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)

        @Suppress("DEPRECATION")
        val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        assertNotNull(streamUri)
        assertEquals("content", streamUri!!.scheme)
        assertEquals("${context.packageName}.fileprovider", streamUri.authority)
        assertTrue(streamUri.path.orEmpty().contains("source-status.pdf"))
        assertArrayEquals(
            byteArrayOf(37, 80, 68, 70),
            context.contentResolver.openInputStream(streamUri)!!.use { it.readBytes() },
        )
    }
}
