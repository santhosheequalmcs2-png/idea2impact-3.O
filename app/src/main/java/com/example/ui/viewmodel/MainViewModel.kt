package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiChatMessage
import com.example.data.model.Innovation
import com.example.data.model.InnovationComment
import com.example.data.model.InvestorOnePager
import com.example.data.model.Pledge
import com.example.data.model.Proposal
import com.example.data.model.SavedItem
import com.example.data.model.SignedNda
import com.example.data.model.TalentProfile
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.AiCopilotRepository
import com.example.data.repository.IdeaRepository
import com.example.util.SecurityCryptoUtils
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class MarketplaceSort(val label: String) {
    NEWEST("Newest"),
    POPULAR("Most Popular"),
    PRICE_LOW("Price: Low to High"),
    PRICE_HIGH("Price: High to Low")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: IdeaRepository,
    private val aiRepository: AiCopilotRepository,
    private val themePreferences: ThemePreferences,
    val currentUserId: String,
    initialDisplayName: String?,
    initialEmail: String?,
    initialPhotoUrl: String?
) : ViewModel() {

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> {
        return map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Stream error observing $label", root)
                emit(UiState.Error(root.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )
    }

    val isDarkTheme: StateFlow<Boolean> = themePreferences.isDarkTheme

    val publicInnovationsState: StateFlow<UiState<List<Innovation>>> =
        repository.observePublicInnovations().toUiStateFlow("marketplace innovations")

    val myInnovationsState: StateFlow<UiState<List<Innovation>>> =
        repository.observeMyInnovations(currentUserId).toUiStateFlow("my innovations")

    val talentsState: StateFlow<UiState<List<TalentProfile>>> =
        repository.observeTalents().toUiStateFlow("talents directory")

    val userProfile: StateFlow<UserProfile?> =
        repository.observeUserProfile(currentUserId)
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing user profile", error)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val savedItems: StateFlow<List<SavedItem>> =
        repository.observeSavedItems(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val likedIds: StateFlow<Set<String>> =
        repository.observeLikedIds(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

    val signedNdas: StateFlow<Map<String, SignedNda>> =
        repository.observeSignedNdas(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

    val pledges: StateFlow<List<Pledge>> =
        repository.observePledges(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val incomingProposals: StateFlow<List<Proposal>> =
        repository.observeIncomingProposals(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    val sentProposals: StateFlow<List<Proposal>> =
        repository.observeSentProposals(currentUserId)
            .catch { error -> if (error is CancellationException) throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // --- Selected Innovation & Comments ---
    private val _selectedInnovationId = MutableStateFlow<String?>(null)
    val selectedInnovationId: StateFlow<String?> = _selectedInnovationId.asStateFlow()

    val activeComments: StateFlow<List<InnovationComment>> = _selectedInnovationId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else repository.observeComments(id).catch { err ->
                if (err is CancellationException) throw err
                Log.w(TAG, "Error observing comments", err)
                emit(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

    // --- Marketplace Filters ---
    private val _marketplaceSearch = MutableStateFlow("")
    val marketplaceSearch: StateFlow<String> = _marketplaceSearch.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _marketplaceSort = MutableStateFlow(MarketplaceSort.NEWEST)
    val marketplaceSort: StateFlow<MarketplaceSort> = _marketplaceSort.asStateFlow()

    // --- Talent Directory Filters ---
    private val _talentSearch = MutableStateFlow("")
    val talentSearch: StateFlow<String> = _talentSearch.asStateFlow()

    private val _selectedRoleFilter = MutableStateFlow("All")
    val selectedRoleFilter: StateFlow<String> = _selectedRoleFilter.asStateFlow()

    // --- Action Feedback & Busy Flag ---
    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // --- AI Copilot State ---
    private val _aiContextInnovation = MutableStateFlow<Innovation?>(null)
    val aiContextInnovation: StateFlow<Innovation?> = _aiContextInnovation.asStateFlow()

    private val _aiMessages = MutableStateFlow(listOf(createWelcomeAiMessage()))
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

    private val _onePagerState = MutableStateFlow<UiState<InvestorOnePager>?>(null)
    val onePagerState: StateFlow<UiState<InvestorOnePager>?> = _onePagerState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserAndSeedCatalog(
                    displayName = initialDisplayName,
                    email = initialEmail,
                    photoUrl = initialPhotoUrl
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Initial profile & showcase seed failed", e)
                _snackbarMessage.value = e.localizedMessage ?: "Failed to initialize showcase data."
            }
        }
    }

    fun toggleTheme() {
        val next = !isDarkTheme.value
        themePreferences.setDarkThemeImmediate(next)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            themePreferences.persistDarkTheme(next)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun selectInnovation(innovationId: String?) {
        _selectedInnovationId.value = innovationId
        _onePagerState.value = null
    }

    fun setMarketplaceSearch(query: String) {
        _marketplaceSearch.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setMarketplaceSort(sort: MarketplaceSort) {
        _marketplaceSort.value = sort
    }

    fun setTalentSearch(query: String) {
        _talentSearch.value = query
    }

    fun setSelectedRoleFilter(role: String) {
        _selectedRoleFilter.value = role
    }

    fun setAiContextInnovation(innovation: Innovation?) {
        _aiContextInnovation.value = innovation
    }

    // --- CRUD & Interactions ---

    fun toggleLike(innovation: Innovation) {
        val currentlyLiked = likedIds.value.contains(innovation.id)
        viewModelScope.launch {
            try {
                repository.toggleLike(innovation, currentlyLiked)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update like."
            }
        }
    }

    fun toggleSave(innovation: Innovation) {
        val currentlySaved = savedItems.value.any { it.innovationId == innovation.id }
        viewModelScope.launch {
            try {
                repository.toggleSave(innovation, currentlySaved)
                _snackbarMessage.value = if (currentlySaved) "Removed from saved" else "Saved to workspace"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update saved items."
            }
        }
    }

    fun postComment(innovationId: String, body: String, onDone: () -> Unit = {}) {
        if (body.isBlank()) return
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.addComment(innovationId, body, userProfile.value)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to post comment."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun deleteComment(innovationId: String, commentId: String, currentCount: Long) {
        viewModelScope.launch {
            try {
                repository.deleteComment(innovationId, commentId, currentCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to delete comment."
            }
        }
    }

    fun createInnovation(
        title: String,
        description: String,
        category: String,
        price: Double,
        visibility: String,
        licensing: Boolean,
        funding: Boolean,
        imageUrl: String?,
        stageIndex: Long,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                val id = repository.createInnovation(
                    title = title,
                    description = description,
                    category = category,
                    price = price,
                    visibility = visibility,
                    licensing = licensing,
                    funding = funding,
                    imageUrl = imageUrl,
                    stageIndex = stageIndex,
                    authorProfile = userProfile.value
                )
                _snackbarMessage.value = "Innovation published to registry!"
                onSuccess(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to publish innovation."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun updateInnovation(innovation: Innovation, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.updateInnovation(innovation)
                _snackbarMessage.value = "Innovation updated."
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to update innovation."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun deleteInnovation(innovationId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.deleteInnovation(innovationId)
                _snackbarMessage.value = "Innovation deleted."
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to delete innovation."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun signDigitalNda(
        innovation: Innovation,
        signerName: String,
        signerOrg: String?,
        onSigned: (SignedNda) -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                val signed = repository.signDigitalNda(innovation, signerName, signerOrg)
                _snackbarMessage.value = "Confidential Deal Room unlocked!"
                onSigned(signed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to sign NDA."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun submitMicroPledge(
        innovation: Innovation,
        supporterName: String,
        amount: Double,
        message: String?,
        onPledged: (Pledge) -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                val pledge = repository.submitMicroPledge(
                    innovation = innovation,
                    supporterName = supporterName,
                    amount = amount,
                    message = message,
                    authorProfile = userProfile.value
                )
                _snackbarMessage.value = "Thank you for backing with $${amount.toInt()}!"
                onPledged(pledge)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to submit backing pledge."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun submitProposal(
        innovation: Innovation,
        proposalType: String,
        amount: String?,
        message: String,
        contactInfo: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.submitProposal(
                    innovation = innovation,
                    proposalType = proposalType,
                    amount = amount,
                    message = message,
                    contactInfo = contactInfo,
                    senderProfile = userProfile.value
                )
                _snackbarMessage.value = "$proposalType proposal sent to ${innovation.authorName}!"
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to send proposal."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun updateProposalStatus(proposalId: String, status: String) {
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.updateProposalStatus(proposalId, status)
                _snackbarMessage.value = "Proposal marked as $status."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to update proposal."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun updateProfile(
        name: String,
        role: String,
        headline: String,
        bio: String,
        skillsText: String,
        location: String
    ) {
        val skills = skillsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(20)
        val projectCount = (myInnovationsState.value as? UiState.Success)?.data?.size?.toLong() ?: 0L
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                repository.updateUserProfile(
                    name = name,
                    role = role,
                    headline = headline,
                    bio = bio,
                    skills = skills,
                    location = location,
                    existingProjectsCount = projectCount
                )
                _snackbarMessage.value = "Innovator profile saved & synced to directory!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to save profile."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    // --- Firebase AI Logic Copilot & One-Pager ---

    fun sendCopilotPrompt(prompt: String, contextOverride: Innovation? = null) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isAiStreaming.value) return
        val context = contextOverride ?: _aiContextInnovation.value

        val userMessage = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isAssistant = false,
            content = trimmed,
            timestampLabel = SecurityCryptoUtils.formatTime()
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistant = AiChatMessage(
            id = assistantMsgId,
            isAssistant = true,
            content = "",
            timestampLabel = SecurityCryptoUtils.formatTime(),
            isStreaming = true
        )

        _aiMessages.update { it + userMessage + placeholderAssistant }
        _isAiStreaming.value = true

        viewModelScope.launch {
            try {
                aiRepository.streamCopilotReply(trimmed, context).collect { partial ->
                    _aiMessages.update { list ->
                        list.map { msg ->
                            if (msg.id == assistantMsgId) {
                                msg.copy(content = partial, isStreaming = true)
                            } else msg
                        }
                    }
                }
                _aiMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(isStreaming = false, timestampLabel = SecurityCryptoUtils.formatTime())
                        } else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Copilot error", e)
                val errMsg = e.localizedMessage ?: "AI Sana service failed. Check App Check & network."
                _aiMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(
                                content = "Error: $errMsg",
                                engineLabel = "Error",
                                isStreaming = false
                            )
                        } else msg
                    }
                }
            } finally {
                _isAiStreaming.value = false
            }
        }
    }

    fun resetCopilotChat() {
        _aiMessages.value = listOf(createWelcomeAiMessage())
    }

    fun generateInvestorOnePager(innovation: Innovation) {
        viewModelScope.launch {
            _onePagerState.value = UiState.Loading
            try {
                val memo = aiRepository.generateInvestorOnePager(innovation)
                _onePagerState.value = UiState.Success(memo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "One-Pager generation failed", e)
                _onePagerState.value = UiState.Error(
                    e.localizedMessage ?: "Failed to generate AI Investor One-Pager."
                )
            }
        }
    }

    fun clearOnePager() {
        _onePagerState.value = null
    }

    private fun createWelcomeAiMessage(): AiChatMessage {
        return AiChatMessage(
            id = "welcome",
            isAssistant = true,
            content = "Hi! I'm **AI Sana**, your intelligent innovation advisor.\n\n" +
                "I specialize in **patent novelty scans**, **TAM/SAM/SOM market sizing**, **investor pitch decks**, and **prototyping blueprints**.\n\n" +
                "Tap any quick prompt below or describe your innovation!",
            timestampLabel = SecurityCryptoUtils.formatTime(),
            engineLabel = "AI Sana"
        )
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
