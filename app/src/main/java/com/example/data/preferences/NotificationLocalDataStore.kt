package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BackendInnovation
import com.example.data.model.DashboardStats
import com.example.data.model.NotificationAlert
import com.example.data.model.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.notificationsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "idea2impact_notifications"
)

/**
 * Local DataStore-backed repository for IDEA2IMPACT alerts covering
 * proposal submissions, innovation likes, and platform updates.
 */
class NotificationLocalDataStore(
    private val dataStore: DataStore<Preferences>,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    constructor(context: Context) : this(context.applicationContext.notificationsDataStore)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val alertsJsonKey = stringPreferencesKey("alerts_json")
    private val seededDefaultsKey = booleanPreferencesKey("seeded_platform_updates")
    private val seenProposalIdsKey = stringSetPreferencesKey("seen_proposal_ids")
    private val lastKnownUpvotesKey = intPreferencesKey("last_known_upvotes")

    val alerts: Flow<List<NotificationAlert>> = dataStore.data.map { prefs ->
        decodeAlerts(prefs[alertsJsonKey]).sortedByDescending { it.createdAtEpochMs }
    }

    val unreadCount: Flow<Int> = alerts.map { list ->
        list.count { !it.isRead }
    }

    suspend fun ensureSeeded() {
        dataStore.edit { prefs ->
            if (prefs[seededDefaultsKey] == true) return@edit
            val now = timeProvider()
            val existing = decodeAlerts(prefs[alertsJsonKey])
            val seedAlerts = listOf(
                NotificationAlert(
                    id = "platform_gemini_copilot_v38",
                    type = NotificationType.PLATFORM_UPDATE,
                    title = "AI Sana Vision Live",
                    message = "Analyze prototype photos, calculate TAM/SAM/SOM, and draft 5-slide investor pitch decks directly from any listing.",
                    createdAtEpochMs = now - 4 * 60_000L,
                    isRead = false
                ),
                NotificationAlert(
                    id = "platform_nda_deal_rooms",
                    type = NotificationType.PLATFORM_UPDATE,
                    title = "Mutual Digital NDA Deal Rooms",
                    message = "Every disclosure supports SHA-256 priority proof and encrypted mutual NDAs before sharing technical specs.",
                    createdAtEpochMs = now - 38 * 60_000L,
                    isRead = false
                ),
                NotificationAlert(
                    id = "platform_resilient_sync",
                    type = NotificationType.PLATFORM_UPDATE,
                    title = "Resilient Cloud Sync Enabled",
                    message = "Automatic exponential backoff retry and offline Room caching keep your innovation feeds synchronized on mobile networks.",
                    createdAtEpochMs = now - 2 * 3_600_000L,
                    isRead = true
                )
            )
            val merged = (seedAlerts + existing)
                .distinctBy { it.id }
                .sortedByDescending { it.createdAtEpochMs }
                .take(MAX_STORED_ALERTS)
            prefs[alertsJsonKey] = json.encodeToString(merged)
            prefs[seededDefaultsKey] = true
        }
    }

    suspend fun recordProposalSubmitted(
        innovation: BackendInnovation?,
        innovationId: String,
        proposalType: String,
        amount: String
    ) {
        val targetTitle = innovation?.title ?: "Innovation Listing"
        val cleanAmount = amount.trim().takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
        appendAlert(
            NotificationAlert(
                id = "proposal_out_${UUID.randomUUID()}",
                type = NotificationType.PROPOSAL,
                title = "Proposal Submitted ($proposalType)",
                message = "Your $proposalType proposal$cleanAmount for \"$targetTitle\" has been transmitted to the founder.",
                createdAtEpochMs = timeProvider(),
                isRead = false,
                innovationId = innovationId
            )
        )
    }

    suspend fun recordLikeAction(innovation: BackendInnovation) {
        if (!innovation.isLiked) return
        appendAlert(
            NotificationAlert(
                id = "like_${innovation.id}_${timeProvider()}",
                type = NotificationType.LIKE,
                title = "Upvoted \"${innovation.title}\"",
                message = "Saved your upvote (${innovation.likeCount} total upvotes · ${innovation.category}).",
                createdAtEpochMs = timeProvider(),
                isRead = false,
                innovationId = innovation.id
            )
        )
    }

    suspend fun recordPlatformUpdate(
        title: String,
        message: String,
        innovationId: String? = null,
        dedupeKey: String? = null
    ) {
        val alertId = dedupeKey ?: "update_${UUID.randomUUID()}"
        dataStore.edit { prefs ->
            val current = decodeAlerts(prefs[alertsJsonKey]).toMutableList()
            if (dedupeKey != null && current.any { it.id == dedupeKey }) {
                return@edit
            }
            current.add(
                0,
                NotificationAlert(
                    id = alertId,
                    type = NotificationType.PLATFORM_UPDATE,
                    title = title,
                    message = message,
                    createdAtEpochMs = timeProvider(),
                    isRead = false,
                    innovationId = innovationId
                )
            )
            prefs[alertsJsonKey] = json.encodeToString(current.take(MAX_STORED_ALERTS))
        }
    }

    suspend fun syncDashboardActivity(stats: DashboardStats) {
        dataStore.edit { prefs ->
            val now = timeProvider()
            val current = decodeAlerts(prefs[alertsJsonKey]).toMutableList()
            val seenProposals = prefs[seenProposalIdsKey]?.toMutableSet() ?: mutableSetOf()
            var offsetMs = 0L

            stats.inboundProposals.forEach { proposal ->
                val propKey = proposal.id.ifBlank {
                    "${proposal.innovationId}:${proposal.senderName}:${proposal.proposalType}"
                }
                if (seenProposals.add(propKey)) {
                    val amountSuffix = proposal.amount.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                    current.add(
                        0,
                        NotificationAlert(
                            id = "proposal_in_$propKey",
                            type = NotificationType.PROPOSAL,
                            title = "New ${proposal.proposalType} Proposal$amountSuffix",
                            message = "${proposal.senderName} reached out regarding \"${proposal.innovationTitle}\".",
                            createdAtEpochMs = now - offsetMs,
                            isRead = false,
                            innovationId = proposal.innovationId.takeIf { it.isNotBlank() }
                        )
                    )
                    offsetMs += 100L
                }
            }

            val prevUpvotes = prefs[lastKnownUpvotesKey] ?: -1
            if (prevUpvotes >= 0 && stats.totalUpvotes > prevUpvotes) {
                val delta = stats.totalUpvotes - prevUpvotes
                current.add(
                    0,
                    NotificationAlert(
                        id = "likes_delta_${now}",
                        type = NotificationType.LIKE,
                        title = "+$delta New Innovation Upvote${if (delta == 1) "" else "s"}",
                        message = "Your published innovations now have ${stats.totalUpvotes} total upvotes across the exchange.",
                        createdAtEpochMs = now,
                        isRead = false,
                        innovationId = stats.myInnovations.firstOrNull()?.id
                    )
                )
            }

            prefs[seenProposalIdsKey] = seenProposals
            prefs[lastKnownUpvotesKey] = stats.totalUpvotes
            prefs[alertsJsonKey] = json.encodeToString(
                current.distinctBy { it.id }
                    .sortedByDescending { it.createdAtEpochMs }
                    .take(MAX_STORED_ALERTS)
            )
        }
    }

    suspend fun markAsRead(alertId: String) {
        dataStore.edit { prefs ->
            val updated = decodeAlerts(prefs[alertsJsonKey]).map { alert ->
                if (alert.id == alertId) alert.copy(isRead = true) else alert
            }
            prefs[alertsJsonKey] = json.encodeToString(updated)
        }
    }

    suspend fun markAllRead() {
        dataStore.edit { prefs ->
            val updated = decodeAlerts(prefs[alertsJsonKey]).map { alert ->
                if (alert.isRead) alert else alert.copy(isRead = true)
            }
            prefs[alertsJsonKey] = json.encodeToString(updated)
        }
    }

    suspend fun deleteAlert(alertId: String) {
        dataStore.edit { prefs ->
            val updated = decodeAlerts(prefs[alertsJsonKey]).filterNot { it.id == alertId }
            prefs[alertsJsonKey] = json.encodeToString(updated)
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs[alertsJsonKey] = "[]"
        }
    }

    private suspend fun appendAlert(alert: NotificationAlert) {
        dataStore.edit { prefs ->
            val current = decodeAlerts(prefs[alertsJsonKey]).toMutableList()
            current.add(0, alert)
            prefs[alertsJsonKey] = json.encodeToString(
                current.distinctBy { it.id }
                    .sortedByDescending { it.createdAtEpochMs }
                    .take(MAX_STORED_ALERTS)
            )
        }
    }

    private fun decodeAlerts(raw: String?): List<NotificationAlert> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<NotificationAlert>>(raw)
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val MAX_STORED_ALERTS = 50
    }
}
