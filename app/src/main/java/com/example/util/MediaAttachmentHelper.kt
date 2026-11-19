package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PickedImagePayload(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val previewBase64: String,
    val bitmap: Bitmap? = null
)

data class PickedDocumentPayload(
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val previewBase64: String? = null
)

object MediaAttachmentHelper {
    private const val MAX_BASE64_CHARS = 180_000
    private const val MAX_IMAGE_DIM_PX = 960

    suspend fun extractCompressedImage(context: Context, uri: Uri): PickedImagePayload? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (name, reportedSize) = queryDisplayNameAndSize(context, uri, fallbackName = "image_${System.currentTimeMillis()}.jpg")
        val mime = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "image/jpeg"

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxSide = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sampleSize = 1
        while (maxSide / sampleSize > MAX_IMAGE_DIM_PX) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: return@withContext null

        val scaled = scaleBitmapDown(decoded, MAX_IMAGE_DIM_PX)
        if (scaled != decoded) {
            decoded.recycle()
        }

        var quality = 78
        var encoded = ""
        val baos = ByteArrayOutputStream()
        while (quality >= 25) {
            baos.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            val candidate = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            if (candidate.length <= MAX_BASE64_CHARS || quality <= 30) {
                encoded = candidate.take(MAX_BASE64_CHARS)
                break
            }
            quality -= 15
        }

        PickedImagePayload(
            fileName = name,
            mimeType = mime,
            sizeBytes = maxOf(reportedSize, baos.size().toLong()),
            previewBase64 = encoded,
            bitmap = scaled
        )
    }

    suspend fun extractDocumentMetadata(context: Context, uri: Uri): PickedDocumentPayload = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (name, size) = queryDisplayNameAndSize(context, uri, fallbackName = "document_${System.currentTimeMillis()}")
        val mime = resolver.getType(uri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        PickedDocumentPayload(
            fileName = name,
            mimeType = mime,
            sizeBytes = size.coerceAtLeast(0L)
        )
    }

    fun decodeBase64Bitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Document"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatConversationTime(ts: Timestamp?): String {
        if (ts == null) return ""
        val date = ts.toDate()
        val now = Calendar.getInstance()
        val msgCal = Calendar.getInstance().apply { time = date }
        return when {
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) ->
                SimpleDateFormat("h:mm a", Locale.US).format(date)
            now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - msgCal.get(Calendar.DAY_OF_YEAR) == 1 ->
                "Yesterday"
            else -> SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    }

    fun formatMessageTimestamp(ts: Timestamp?): String {
        if (ts == null) return "Sending…"
        return SimpleDateFormat("h:mm a", Locale.US).format(ts.toDate())
    }

    fun formatLastSeen(ts: Timestamp?): String {
        if (ts == null) return "Offline"
        val diffMs = (System.currentTimeMillis() - ts.toDate().time).coerceAtLeast(0L)
        val mins = diffMs / 60_000L
        val hours = mins / 60L
        return when {
            mins < 2L -> "Active just now"
            mins < 60L -> "Last seen ${mins}m ago"
            hours < 24L -> "Last seen ${hours}h ago"
            else -> "Last seen ${SimpleDateFormat("MMM d", Locale.US).format(ts.toDate())}"
        }
    }

    fun formatDueDate(epochMs: Long?): String {
        if (epochMs == null || epochMs <= 0L) return "No deadline"
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMs))
    }

    private fun queryDisplayNameAndSize(context: Context, uri: Uri, fallbackName: String): Pair<String, Long> {
        var name = fallbackName
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                    if (sizeIdx >= 0) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        }
        return name.take(180) to size
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * ratio).toInt().coerceAtLeast(1)
        val targetH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
