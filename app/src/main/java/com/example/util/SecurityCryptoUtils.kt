package com.example.util

import com.google.firebase.Timestamp
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

object SecurityCryptoUtils {
    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun formatRegistryId(innovationId: String): String {
        val clean = innovationId.replace("-", "").take(10).uppercase(Locale.US).ifEmpty { "0000000000" }
        return "I2I-$clean"
    }

    fun formatSignatureId(innovationId: String): String {
        val prefix = innovationId.take(8).uppercase(Locale.US).ifEmpty { "DEAL" }
        val suffix = (System.currentTimeMillis() % 1_000_000L).toString(36).uppercase(Locale.US)
        return "NDA-$prefix-$suffix"
    }

    fun formatReceiptId(): String {
        val suffix = System.currentTimeMillis().toString(36).uppercase(Locale.US)
        return "I2I-TX-$suffix"
    }

    fun formatPrice(price: Double): String {
        if (price <= 0.0) return "Open to connect"
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
        }
        return formatter.format(price)
    }

    fun formatDate(timestamp: Timestamp?): String {
        val date = (timestamp ?: Timestamp.now()).toDate()
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
    }

    fun formatTime(epochMillis: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(epochMillis)
    }
}
