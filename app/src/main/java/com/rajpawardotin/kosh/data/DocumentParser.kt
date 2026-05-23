package com.rajpawardotin.kosh.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.io.IOException

class SizeLimitedInputStream(
    private val delegate: InputStream,
    private val maxBytes: Long
) : InputStream() {
    private var bytesRead = 0L

    private fun track(read: Long) {
        if (read > 0) {
            bytesRead += read
            if (bytesRead > maxBytes) {
                throw IOException("File exceeds secure size limit ($maxBytes bytes)")
            }
        }
    }

    override fun read(): Int {
        val result = delegate.read()
        if (result != -1) {
            track(1L)
        }
        return result
    }

    override fun read(b: ByteArray): Int {
        val result = delegate.read(b)
        track(result.toLong())
        return result
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val result = delegate.read(b, off, len)
        track(result.toLong())
        return result
    }

    override fun skip(n: Long): Long {
        val skipped = delegate.skip(n)
        track(skipped)
        return skipped
    }

    override fun available(): Int = delegate.available()

    override fun close() {
        delegate.close()
    }

    override fun mark(readlimit: Int) {
        delegate.mark(readlimit)
    }

    override fun reset() {
        delegate.reset()
    }

    override fun markSupported(): Boolean = delegate.markSupported()
}

object DocumentParser {
    private const val MAX_FILE_SIZE = 10L * 1024 * 1024 // 10MB
    private const val MAX_PDF_PAGES = 50

    fun extractText(context: Context, uri: Uri, fileName: String): String {
        val contentResolver = context.contentResolver
        val extension = fileName.substringAfterLast('.', "").lowercase()

        val rawStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Unable to open file input stream")

        val limitStream = SizeLimitedInputStream(rawStream, MAX_FILE_SIZE)

        return try {
            when (extension) {
                "pdf" -> {
                    parsePdf(limitStream)
                }
                "txt", "md" -> {
                    parseText(limitStream)
                }
                else -> {
                    throw IOException("Unsupported file format: .$extension")
                }
            }
        } finally {
            try {
                limitStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun parseText(inputStream: InputStream): String {
        return inputStream.bufferedReader().use { it.readText() }
    }

    private fun parsePdf(inputStream: InputStream): String {
        var document: PDDocument? = null
        try {
            document = PDDocument.load(inputStream)
            val pageCount = document.numberOfPages
            if (pageCount > MAX_PDF_PAGES) {
                throw IOException("PDF exceeds page limit ($MAX_PDF_PAGES pages max)")
            }
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        } catch (oom: OutOfMemoryError) {
            System.gc()
            throw IOException("Failed to parse PDF: Out of Memory")
        } finally {
            try {
                document?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
