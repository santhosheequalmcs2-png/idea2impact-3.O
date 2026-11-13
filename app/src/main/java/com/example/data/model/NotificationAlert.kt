package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    PROPOSAL,
    LIKE,
    PLATFORM_UPDATE
}

@Serializable
data class NotificationAlert(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val createdAtEpochMs: Long,
    val isRead: Boolean = false,
    val innovationId: String? = null
)
