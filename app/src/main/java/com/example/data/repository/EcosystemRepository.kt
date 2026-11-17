package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.SavedOpportunity
import com.example.data.model.VerifiedDirectoryJsonParser
import com.example.data.model.VerifiedOrganization
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository for the Verified Indian Startup, Organization & Funding Directory (Steps 4 & 7).
 *
 * Strictly avoids fabricating organizations or programs:
 * - Reads admin-curated VerifiedOrganization entries from Cloud Firestore `/verified_organizations` when signed in.
 * - Supports user-imported, source-attributed JSON records persisted locally on the device via [VerifiedDirectoryJsonParser].
 * - Syncs saved funding opportunities to `/users/{uid}/saved_opportunities` when signed in and falls back to local prefs offline.
 */
class EcosystemRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    context: Context? = null
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
        context = context.applicationContext
    )

    private val prefs: SharedPreferences? =
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _importedOrganizations = MutableStateFlow(loadImportedFromPrefs())
    private val _localSavedIds = MutableStateFlow(loadSavedIdsFromPrefs())

    private fun loadImportedFromPrefs(): List<VerifiedOrganization> {
        val raw = prefs?.getString(KEY_IMPORTED_JSON, null) ?: return emptyList()
        return VerifiedDirectoryJsonParser.parse(raw).getOrDefault(emptyList())
    }

    private fun loadSavedIdsFromPrefs(): Set<String> {
        return prefs?.getStringSet(KEY_SAVED_OPPORTUNITIES, emptySet()).orEmpty().toSet()
    }

    fun observeImportedOrganizations(): Flow<List<VerifiedOrganization>> = _importedOrganizations

    fun observeLocalSavedOpportunityIds(): Flow<Set<String>> = _localSavedIds

    fun observeFirestoreVerifiedOrganizations(): Flow<List<VerifiedOrganization>> {
        val path = "verified_organizations"
        return db.collection(path)
            .whereIn("verificationStatus", listOf("VERIFIED", "NEEDS_REVIEW"))
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(VerifiedOrganization::class.java)
                }.sortedBy { it.name.lowercase() }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null) {
                    handleFirestoreError(firestoreError, OperationType.LIST, path, auth)
                }
                emit(emptyList())
            }
    }

    fun observeCombinedOrganizations(isSignedIn: Boolean): Flow<List<VerifiedOrganization>> {
        val cloudFlow = if (isSignedIn) {
            observeFirestoreVerifiedOrganizations()
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
        return combine(cloudFlow, _importedOrganizations) { cloud, imported ->
            val merged = linkedMapOf<String, VerifiedOrganization>()
            cloud.forEach { merged[it.id.ifBlank { it.name.lowercase() }] = it }
            imported.forEach { org ->
                val key = org.id.ifBlank { org.name.lowercase() }
                if (!merged.containsKey(key)) merged[key] = org
            }
            merged.values.sortedBy { it.name.lowercase() }
        }
    }

    fun observeCloudSavedOpportunityIds(userId: String): Flow<Set<String>> {
        val path = "users/$userId/saved_opportunities"
        return db.collection("users").document(userId).collection("saved_opportunities")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (firestoreError != null && auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError, OperationType.LIST, path, auth)
                }
                emit(emptySet())
            }
    }

    fun importVerifiedJson(rawJson: String): Result<Int> {
        val parsedResult = VerifiedDirectoryJsonParser.parse(rawJson)
        val parsed = parsedResult.getOrElse { return Result.failure(it) }
        val existing = _importedOrganizations.value.associateBy { it.id }.toMutableMap()
        parsed.forEach { existing[it.id] = it }
        val updated = existing.values.sortedBy { it.name.lowercase() }
        _importedOrganizations.value = updated
        prefs?.edit()?.putString(KEY_IMPORTED_JSON, rawJson.trim())?.apply()
        return Result.success(parsed.size)
    }

    fun clearImportedOrganizations() {
        _importedOrganizations.value = emptyList()
        prefs?.edit()?.remove(KEY_IMPORTED_JSON)?.apply()
    }

    suspend fun toggleSavedOpportunity(
        organization: VerifiedOrganization,
        currentlySaved: Boolean
    ) {
        val orgId = organization.id.ifBlank {
            organization.name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(64)
        }
        val uid = auth.currentUser?.uid
        if (uid != null && orgId.isNotBlank()) {
            val savedRef = db.collection("users").document(uid)
                .collection("saved_opportunities").document(orgId)
            try {
                if (currentlySaved) {
                    savedRef.delete().await()
                } else {
                    val record = SavedOpportunity(
                        userId = uid,
                        organizationId = orgId,
                        organizationName = organization.name,
                        organizationType = organization.organizationType,
                        sector = organization.sector,
                        sourceUrl = organization.sourceUrl,
                        lastVerifiedAt = organization.lastVerifiedAt
                    )
                    savedRef.set(record.toCreateMap()).await()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, savedRef.path, auth)
                throw e
            }
        }
        val nextIds = if (currentlySaved) {
            _localSavedIds.value - orgId
        } else {
            _localSavedIds.value + orgId
        }
        _localSavedIds.value = nextIds
        prefs?.edit()?.putStringSet(KEY_SAVED_OPPORTUNITIES, nextIds)?.apply()
    }

    private companion object {
        const val PREFS_NAME = "idea2impact_ecosystem_directory"
        const val KEY_IMPORTED_JSON = "imported_verified_organizations_json"
        const val KEY_SAVED_OPPORTUNITIES = "saved_opportunity_ids"
    }
}
