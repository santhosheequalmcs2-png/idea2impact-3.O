package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.AnalyticsTracker
import com.example.analytics.NoOpAnalyticsTracker
import com.example.data.model.AuthUser
import com.example.data.model.BackendComment
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.CategoryItem
import com.example.data.model.CopilotMessage
import com.example.data.model.DashboardStats
import com.example.data.model.DigitalNdaRecord
import com.example.data.model.ExactCategories
import com.example.data.model.NotificationAlert
import com.example.data.model.PitchDeckResult
import com.example.data.model.UiState
import com.example.data.preferences.NotificationLocalDataStore
import com.example.data.preferences.ThemePreferences
import com.example.data.preferences.TokenManager
import com.example.data.repository.AiCopilotRepository
import com.example.data.repository.BackendApiException
import com.example.data.repository.BackendRepository
import com.example.util.SecurityCryptoUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

class AppViewModel(
    private val repository: BackendRepository,
    private val tokenManager: TokenManager,
    private val themePreferences: ThemePreferences,
    private val aiCopilotRepository: AiCopilotRepository = AiCopilotRepository(),
    private val notificationDataStore: NotificationLocalDataStore? = null,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = themePreferences.isDarkTheme

    val isSoundEnabled: StateFlow<Boolean> = themePreferences.isSoundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val hasCompletedOnboarding: StateFlow<Boolean?> = themePreferences.hasCompletedOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val notifications: StateFlow<List<NotificationAlert>> = (notificationDataStore?.alerts ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = (notificationDataStore?.unreadCount ?: flowOf(0))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _showAuthModal = MutableStateFlow(false)
    val showAuthModal: StateFlow<Boolean> = _showAuthModal.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    val categories: StateFlow<List<CategoryItem>> = repository.observeCachedCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), ExactCategories.defaultItems)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedStage = MutableStateFlow("All")
    val selectedStage: StateFlow<String> = _selectedStage.asStateFlow()

    private val _selectedSort = MutableStateFlow("Featured")
    val selectedSort: StateFlow<String> = _selectedSort.asStateFlow()

    private val _isSyncingInnovations = MutableStateFlow(true)
    val isSyncingInnovations: StateFlow<Boolean> = _isSyncingInnovations.asStateFlow()

    private val _isShowingOfflineCache = MutableStateFlow(false)
    val isShowingOfflineCache: StateFlow<Boolean> = _isShowingOfflineCache.asStateFlow()

    private val _innovationsSyncError = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val cachedFilteredInnovations = combine(
        _searchQuery,
        _selectedCategory,
        _selectedStage
    ) { query, category, stage ->
        Triple(query, category, stage)
    }.flatMapLatest { (query, category, stage) ->
        repository.observeCachedInnovations(
            search = query,
            category = category,
            stage = stage
        )
    }

    private val hasAnyCachedInnovations = repository.observeCachedInnovations()
        .map { it.isNotEmpty() }

    val innovationsState: StateFlow<UiState<List<BackendInnovation>>> = combine(
        cachedFilteredInnovations,
        hasAnyCachedInnovations,
        _isSyncingInnovations,
        _innovationsSyncError,
        _selectedSort
    ) { filtered, hasAnyCached, isSyncing, syncError, sortMode ->
        val sorted = when (sortMode) {
            "Top Voted" -> filtered.sortedByDescending { it.likeCount }
            "Funding Goal" -> filtered.sortedByDescending { it.fundingGoal }
            else -> filtered
        }
        when {
            sorted.isNotEmpty() -> UiState.Success(sorted)
            hasAnyCached -> UiState.Success(emptyList())
            isSyncing -> UiState.Loading
            syncError != null -> UiState.Error(syncError)
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _selectedDetail = MutableStateFlow<BackendInnovation?>(null)
    val selectedDetail: StateFlow<BackendInnovation?> = _selectedDetail.asStateFlow()

    private val _detailComments = MutableStateFlow<List<BackendComment>>(emptyList())
    val detailComments: StateFlow<List<BackendComment>> = _detailComments.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val _pitchDeckState = MutableStateFlow<UiState<PitchDeckResult>?>(null)
    val pitchDeckState: StateFlow<UiState<PitchDeckResult>?> = _pitchDeckState.asStateFlow()

    private val _signedNdas = MutableStateFlow<Map<String, DigitalNdaRecord>>(emptyMap())
    val signedNdas: StateFlow<Map<String, DigitalNdaRecord>> = _signedNdas.asStateFlow()

    private val _talentsState = MutableStateFlow<UiState<List<BackendTalent>>>(UiState.Loading)
    val talentsState: StateFlow<UiState<List<BackendTalent>>> = _talentsState.asStateFlow()

    private val _selectedTalentRole = MutableStateFlow("All")
    val selectedTalentRole: StateFlow<String> = _selectedTalentRole.asStateFlow()

    private val _dashboardState = MutableStateFlow<UiState<DashboardStats>>(UiState.Success(DashboardStats()))
    val dashboardState: StateFlow<UiState<DashboardStats>> = _dashboardState.asStateFlow()

    private val _isRefreshingDashboard = MutableStateFlow(false)
    val isRefreshingDashboard: StateFlow<Boolean> = _isRefreshingDashboard.asStateFlow()

    val availableInnovations: StateFlow<List<BackendInnovation>> = repository.observeCachedInnovations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _focusedInnovation = MutableStateFlow<BackendInnovation?>(null)
    val focusedInnovation: StateFlow<BackendInnovation?> = _focusedInnovation.asStateFlow()

    private val _useFastAiModel = MutableStateFlow(false)
    val useFastAiModel: StateFlow<Boolean> = _useFastAiModel.asStateFlow()

    private val _copilotMessages = MutableStateFlow(listOf(createWelcomeMessage()))
    val copilotMessages: StateFlow<List<CopilotMessage>> = _copilotMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _canRegenerateLastReply = MutableStateFlow(false)
    val canRegenerateLastReply: StateFlow<Boolean> = _canRegenerateLastReply.asStateFlow()

    private val _customApiKey = MutableStateFlow(tokenManager.getCustomApiKey().orEmpty())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isActionBusy = MutableStateFlow(false)
    val isActionBusy: StateFlow<Boolean> = _isActionBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var searchJob: Job? = null
    private var innovationsJob: Job? = null
    private var talentsJob: Job? = null
    private var aiStreamJob: Job? = null
    private var activeStreamMsgId: String? = null
    private var userRequestedAiStop = false
    private var lastPromptPayload: Triple<String, Bitmap?, String>? = null

    init {
        viewModelScope.launch {
            notificationDataStore?.ensureSeeded()
        }
        refreshAllInitialData()
    }

    fun refreshAllInitialData() {
        loadInnovations()
        loadTalents()
        loadCategories()
        viewModelScope.launch {
            if (repository.hasSavedToken()) {
                runCatching { repository.fetchCurrentUser() }
                    .onSuccess { user ->
                        _currentUser.value = user ?: Firebase.auth.currentUser?.toAuthUser()
                        if (_currentUser.value != null) loadDashboardStats()
                    }
                    .onFailure { e ->
                        if (e is CancellationException) throw e
                        Log.w(TAG, "Deferred profile refresh skipped: ${e.message}")
                        if (_currentUser.value == null) {
                            Firebase.auth.currentUser?.let { fbUser ->
                                _currentUser.value = fbUser.toAuthUser()
                                loadDashboardStats()
                            }
                        }
                    }
            } else {
                Firebase.auth.currentUser?.let { fbUser ->
                    _currentUser.value = fbUser.toAuthUser()
                    loadDashboardStats()
                }
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

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferences.setSoundEnabled(enabled)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            themePreferences.setOnboardingCompleted(true)
        }
    }

    fun replayOnboarding() {
        analyticsTracker.logOnboardingStarted(source = "replay")
        viewModelScope.launch {
            themePreferences.setOnboardingCompleted(false)
        }
    }

    fun markNotificationRead(alertId: String) {
        viewModelScope.launch {
            notificationDataStore?.markAsRead(alertId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            notificationDataStore?.markAllRead()
        }
    }

    fun deleteNotification(alertId: String) {
        viewModelScope.launch {
            notificationDataStore?.deleteAlert(alertId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationDataStore?.clearAll()
        }
    }

    fun setSelectedSort(sort: String) {
        _selectedSort.value = sort
        analyticsTracker.logMarketplaceSortSelected(sort)
    }

    fun clearMarketplaceFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _selectedStage.value = "All"
        _selectedSort.value = "Featured"
        analyticsTracker.logMarketplaceFiltersCleared()
        loadInnovations()
    }

    fun openAuthModal() {
        _authError.value = null
        _showAuthModal.value = true
    }

    fun dismissAuthModal() {
        _authError.value = null
        _showAuthModal.value = false
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Auth ---

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Please enter both email and password."
            return
        }
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            try {
                val user = repository.login(email, password)
                _currentUser.value = user
                _showAuthModal.value = false
                _snackbarMessage.value = "Welcome back, ${user.name}!"
                loadDashboardStats()
                loadInnovations()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Login failed. Verify your credentials."
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            _authError.value = "Enter your name, valid email, and password (6+ chars)."
            return
        }
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            try {
                val user = repository.register(name, email, password)
                _currentUser.value = user
                _showAuthModal.value = false
                _snackbarMessage.value = "Account created! Welcome, ${user.name}."
                loadDashboardStats()
                loadInnovations()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Registration failed."
            } finally {
                _isAuthBusy.value = false
            }
        }
    }

    fun setAuthError(message: String) {
        _authError.value = message
    }

    fun onGoogleAuthSuccess(isAutoSignIn: Boolean = false) {
        val fbUser = Firebase.auth.currentUser ?: return
        val authUser = fbUser.toAuthUser()
        _currentUser.value = authUser
        _authError.value = null
        _showAuthModal.value = false
        if (!isAutoSignIn) {
            _snackbarMessage.value = "Signed in with Google as ${authUser.name}!"
        }
        loadDashboardStats()
    }

    fun logout() {
        repository.signOut()
        runCatching { Firebase.auth.signOut() }
        _currentUser.value = null
        _dashboardState.value = UiState.Success(DashboardStats())
        _snackbarMessage.value = "Signed out."
    }

    private fun isUserSignedIn(): Boolean {
        return repository.hasSavedToken() || _currentUser.value != null || Firebase.auth.currentUser != null
    }

    private fun FirebaseUser.toAuthUser(): AuthUser {
        val resolvedName = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Google Innovator"
        return AuthUser(
            id = uid,
            name = resolvedName,
            email = email.orEmpty(),
            role = "Creator",
            headline = "Google Account · IDEA2IMPACT",
            avatarUrl = photoUrl?.toString(),
            verified = isEmailVerified
        )
    }

    // --- Categories & Marketplace ---

    fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.fetchCategories()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Using cached/default 8 categories while offline or warming up: ${e.message}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(320L)
            val clean = query.trim()
            if (clean.isNotEmpty()) {
                analyticsTracker.logMarketplaceSearch(
                    queryPreview = clean,
                    queryLength = clean.length
                )
            }
            loadInnovations()
        }
    }

    fun setSelectedCategory(category: String, source: String = "filter_chip") {
        _selectedCategory.value = category
        analyticsTracker.logMarketplaceCategorySelected(category = category, source = source)
        loadInnovations()
    }

    fun setSelectedStage(stage: String) {
        _selectedStage.value = stage
        analyticsTracker.logMarketplaceStageSelected(stage)
        loadInnovations()
    }

    fun refreshMarketplace(source: String = "manual") {
        analyticsTracker.logMarketplaceRefreshed(source)
        loadInnovations()
        loadCategories()
    }

    fun loadInnovations() {
        innovationsJob?.cancel()
        innovationsJob = viewModelScope.launch {
            _isSyncingInnovations.value = true
            _innovationsSyncError.value = null
            try {
                repository.fetchInnovations(
                    search = _searchQuery.value,
                    category = _selectedCategory.value,
                    stage = _selectedStage.value
                )
                _isShowingOfflineCache.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to sync innovations from network: ${e.message}")
                val hasCache = repository.hasCachedInnovations()
                _isShowingOfflineCache.value = hasCache
                if (!hasCache) {
                    _innovationsSyncError.value = friendlyErrorMessage(
                        e,
                        "Unable to connect to production marketplace."
                    )
                }
            } finally {
                _isSyncingInnovations.value = false
            }
        }
    }

    // --- Innovation Detail & Actions ---

    fun openInnovationDetail(item: BackendInnovation, source: String = "marketplace") {
        _selectedDetail.value = item
        _focusedInnovation.value = item
        _pitchDeckState.value = null
        analyticsTracker.logInnovationSelected(
            innovationId = item.id,
            title = item.title,
            category = item.category,
            stage = item.stage,
            source = source
        )
        refreshInnovationDetail(item.id)
    }

    fun setFocusedInnovation(item: BackendInnovation?) {
        _focusedInnovation.value = item
        analyticsTracker.logCopilotContextSelected(
            innovationId = item?.id,
            innovationCategory = item?.category
        )
    }

    fun setFastAiModel(useFast: Boolean, surface: String = "full_screen") {
        _useFastAiModel.value = useFast
        analyticsTracker.logCopilotModelToggled(useFastModel = useFast, surface = surface)
    }

    fun refreshInnovationDetail(id: String) {
        viewModelScope.launch {
            _isDetailLoading.value = true
            try {
                val (detail, comments) = repository.fetchInnovationDetail(id)
                if (detail != null) {
                    _selectedDetail.value = detail
                }
                _detailComments.value = comments
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading detail for $id", e)
            } finally {
                _isDetailLoading.value = false
            }
        }
    }

    fun toggleLike(innovation: BackendInnovation) {
        if (!isUserSignedIn()) {
            _snackbarMessage.value = "Sign in to upvote innovations."
            openAuthModal()
            return
        }
        val prevLiked = innovation.isLiked
        val prevCount = innovation.likeCount
        val optimistic = innovation.copy(
            isLiked = !prevLiked,
            likeCount = if (prevLiked) (prevCount - 1).coerceAtLeast(0) else prevCount + 1
        )
        applyInnovationUpdate(optimistic)

        viewModelScope.launch {
            if (!repository.hasSavedToken()) {
                analyticsTracker.logInnovationLiked(
                    innovationId = optimistic.id,
                    category = optimistic.category,
                    liked = optimistic.isLiked
                )
                if (!prevLiked && optimistic.isLiked) {
                    notificationDataStore?.recordLikeAction(optimistic)
                }
                return@launch
            }
            try {
                val (liked, count) = repository.toggleLike(innovation.id)
                val synced = optimistic.copy(
                    isLiked = liked ?: optimistic.isLiked,
                    likeCount = count ?: optimistic.likeCount
                )
                applyInnovationUpdate(synced)
                analyticsTracker.logInnovationLiked(
                    innovationId = synced.id,
                    category = synced.category,
                    liked = synced.isLiked
                )
                if (!prevLiked && synced.isLiked) {
                    notificationDataStore?.recordLikeAction(synced)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendApiException) {
                applyInnovationUpdate(innovation)
                if (e.code == 401) openAuthModal()
                _snackbarMessage.value = e.message
            } catch (e: Exception) {
                applyInnovationUpdate(innovation)
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update like."
            }
        }
    }

    fun toggleBookmark(innovation: BackendInnovation) {
        if (!isUserSignedIn()) {
            _snackbarMessage.value = "Sign in to bookmark innovations."
            openAuthModal()
            return
        }
        val optimistic = innovation.copy(isBookmarked = !innovation.isBookmarked)
        applyInnovationUpdate(optimistic)

        viewModelScope.launch {
            if (!repository.hasSavedToken()) {
                analyticsTracker.logInnovationBookmarked(
                    innovationId = optimistic.id,
                    category = optimistic.category,
                    bookmarked = optimistic.isBookmarked
                )
                _snackbarMessage.value = if (optimistic.isBookmarked) "Saved to bookmarks" else "Removed from bookmarks"
                loadDashboardStats()
                return@launch
            }
            try {
                val bookmarked = repository.toggleFavourite(innovation.id)
                val synced = optimistic.copy(isBookmarked = bookmarked ?: optimistic.isBookmarked)
                applyInnovationUpdate(synced)
                analyticsTracker.logInnovationBookmarked(
                    innovationId = synced.id,
                    category = synced.category,
                    bookmarked = synced.isBookmarked
                )
                _snackbarMessage.value = if (synced.isBookmarked) "Saved to bookmarks" else "Removed from bookmarks"
                loadDashboardStats()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendApiException) {
                applyInnovationUpdate(innovation)
                if (e.code == 401) openAuthModal()
                _snackbarMessage.value = e.message
            } catch (e: Exception) {
                applyInnovationUpdate(innovation)
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update bookmark."
            }
        }
    }

    private fun applyInnovationUpdate(updated: BackendInnovation) {
        if (_selectedDetail.value?.id == updated.id) {
            _selectedDetail.value = updated
        }
        viewModelScope.launch {
            repository.cacheLikeState(updated.id, updated.isLiked, updated.likeCount)
            repository.cacheBookmarkState(updated.id, updated.isBookmarked)
        }
    }

    fun postComment(innovationId: String, text: String, onDone: () -> Unit) {
        val clean = text.trim()
        if (clean.isBlank()) return
        if (!isUserSignedIn()) {
            _snackbarMessage.value = "Sign in to join the discussion."
            openAuthModal()
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                if (!repository.hasSavedToken()) {
                    val user = _currentUser.value
                    val localComment = BackendComment(
                        id = UUID.randomUUID().toString(),
                        innovationId = innovationId,
                        authorName = user?.name ?: "Google Innovator",
                        authorAvatar = user?.avatarUrl,
                        body = clean,
                        createdAt = SecurityCryptoUtils.formatTime()
                    )
                    _detailComments.update { listOf(localComment) + it }
                    _snackbarMessage.value = "Comment posted."
                    onDone()
                    return@launch
                }
                val created = repository.postComment(innovationId, clean)
                if (created != null && created.body.isNotBlank()) {
                    _detailComments.update { listOf(created) + it }
                } else {
                    refreshInnovationDetail(innovationId)
                }
                _snackbarMessage.value = "Comment posted."
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendApiException) {
                if (e.code == 401) openAuthModal()
                _snackbarMessage.value = e.message
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to post comment."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun submitProposal(
        innovationId: String,
        type: String,
        amount: String,
        message: String,
        contactInfo: String,
        onSuccess: () -> Unit
    ) {
        if (!isUserSignedIn()) {
            _snackbarMessage.value = "Sign in to submit a collaboration proposal."
            openAuthModal()
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                if (repository.hasSavedToken()) {
                    repository.submitProposal(innovationId, type, amount, message, contactInfo)
                }
                val targetInnovation = _selectedDetail.value?.takeIf { it.id == innovationId }
                    ?: availableInnovations.value.firstOrNull { it.id == innovationId }
                notificationDataStore?.recordProposalSubmitted(
                    innovation = targetInnovation,
                    innovationId = innovationId,
                    proposalType = type,
                    amount = amount
                )
                analyticsTracker.logProposalSubmitted(
                    innovationId = innovationId,
                    proposalType = type
                )
                _snackbarMessage.value = "Collaboration proposal submitted!"
                loadDashboardStats()
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendApiException) {
                if (e.code == 401) openAuthModal()
                _snackbarMessage.value = e.message
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to submit proposal."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun signDigitalNda(innovation: BackendInnovation, signerName: String, signerOrg: String) {
        val cleanName = signerName.trim()
        if (cleanName.isBlank()) return
        val hash = SecurityCryptoUtils.sha256Hex("${innovation.id}|$cleanName|${signerOrg.trim()}|${System.currentTimeMillis()}")
            .take(24)
            .uppercase()
        val record = DigitalNdaRecord(
            innovationId = innovation.id,
            signerName = cleanName,
            signerOrg = signerOrg.trim(),
            signatureHash = "NDA-$hash",
            signedAt = SecurityCryptoUtils.formatTime()
        )
        _signedNdas.update { it + (innovation.id to record) }
        analyticsTracker.logDigitalNdaSigned(
            innovationId = innovation.id,
            category = innovation.category
        )
        _snackbarMessage.value = "Digital Mutual NDA confirmed (${record.signatureHash})."
    }

    fun generatePitchDeck(innovation: BackendInnovation) {
        viewModelScope.launch {
            _pitchDeckState.value = UiState.Loading
            try {
                val deck = aiCopilotRepository.generateBackendPitchDeck(innovation)
                _pitchDeckState.value = UiState.Success(deck)
                analyticsTracker.logCopilotPitchDeckGenerated(
                    innovationId = innovation.id,
                    category = innovation.category,
                    success = true
                )
                notificationDataStore?.recordPlatformUpdate(
                    title = "AI Pitch Deck Ready",
                    message = "Synthesized a 5-slide investor deck for \"${innovation.title}\".",
                    innovationId = innovation.id,
                    dedupeKey = "pitch_deck_${innovation.id}"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate 5-slide AI pitch deck", e)
                analyticsTracker.logCopilotPitchDeckGenerated(
                    innovationId = innovation.id,
                    category = innovation.category,
                    success = false
                )
                _pitchDeckState.value = UiState.Error(
                    e.localizedMessage ?: "Failed to generate 5-slide AI pitch deck."
                )
            }
        }
    }

    fun clearPitchDeck() {
        _pitchDeckState.value = null
    }

    // --- Talents ---

    fun setSelectedTalentRole(role: String) {
        _selectedTalentRole.value = role
    }

    fun loadTalents() {
        talentsJob?.cancel()
        talentsJob = viewModelScope.launch {
            _talentsState.value = UiState.Loading
            try {
                val talents = repository.fetchTalents()
                _talentsState.value = UiState.Success(talents)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to load talents: ${e.message}")
                _talentsState.value = UiState.Error(
                    friendlyErrorMessage(e, "Failed to load builders & partners directory.")
                )
            }
        }
    }

    // --- Dashboard ---

    fun loadDashboardStats(isPullRefresh: Boolean = false) {
        if (!repository.hasSavedToken()) {
            _isRefreshingDashboard.value = false
            if (isUserSignedIn()) {
                val saved = availableInnovations.value.filter { it.isBookmarked }
                val totalLikes = availableInnovations.value.count { it.isLiked }
                _dashboardState.value = UiState.Success(
                    DashboardStats(
                        savedOpportunities = saved.size,
                        totalUpvotes = totalLikes,
                        savedList = saved
                    )
                )
            } else {
                _dashboardState.value = UiState.Success(DashboardStats())
                if (isPullRefresh) {
                    _snackbarMessage.value = "Sign in to sync your personal dashboard."
                }
            }
            return
        }
        viewModelScope.launch {
            if (isPullRefresh) {
                _isRefreshingDashboard.value = true
            }
            if (!isPullRefresh || _dashboardState.value !is UiState.Success) {
                _dashboardState.value = UiState.Loading
            }
            try {
                val stats = repository.fetchDashboardStats()
                _dashboardState.value = UiState.Success(stats)
                notificationDataStore?.syncDashboardActivity(stats)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BackendApiException) {
                if (e.code == 401) {
                    repository.signOut()
                    _currentUser.value = null
                }
                if (!isPullRefresh || _dashboardState.value !is UiState.Success) {
                    _dashboardState.value = UiState.Error(e.message ?: "Failed to load dashboard metrics.")
                } else {
                    _snackbarMessage.value = e.message ?: "Unable to re-sync dashboard right now."
                }
            } catch (e: Exception) {
                val message = friendlyErrorMessage(e, "Unable to load dashboard stats.")
                if (!isPullRefresh || _dashboardState.value !is UiState.Success) {
                    _dashboardState.value = UiState.Error(message)
                } else {
                    _snackbarMessage.value = message
                }
            } finally {
                _isRefreshingDashboard.value = false
            }
        }
    }

    // --- AI Copilot ---

    fun saveCustomApiKey(key: String) {
        val clean = key.trim()
        tokenManager.saveCustomApiKey(clean)
        _customApiKey.value = clean
        analyticsTracker.logCopilotApiKeyUpdated(hasCustomKey = clean.isNotBlank())
        _snackbarMessage.value = if (clean.isBlank()) {
            "Custom Google AI Studio key cleared."
        } else {
            "Custom Google AI Studio key saved securely."
        }
    }

    fun sendCopilotPrompt(
        prompt: String,
        attachedImage: Bitmap? = null,
        promptSource: String = "custom_input",
        isPopup: Boolean = false
    ) {
        val trimmed = prompt.trim()
        if (trimmed.isBlank() || _isAiLoading.value) return

        lastPromptPayload = Triple(trimmed, attachedImage, promptSource)
        userRequestedAiStop = false

        val displayUserText = if (attachedImage != null) {
            "[Attached Prototype Image]\n$trimmed"
        } else {
            trimmed
        }

        val userMsg = CopilotMessage(
            id = UUID.randomUUID().toString(),
            isAssistant = false,
            content = displayUserText,
            timestamp = SecurityCryptoUtils.formatTime()
        )
        val priorHistory = _copilotMessages.value
        val streamMsgId = UUID.randomUUID().toString()
        activeStreamMsgId = streamMsgId
        val streamingPlaceholder = CopilotMessage(
            id = streamMsgId,
            isAssistant = true,
            content = "",
            timestamp = SecurityCryptoUtils.formatTime()
        )

        _copilotMessages.value = priorHistory + userMsg + streamingPlaceholder
        _isAiLoading.value = true
        _canRegenerateLastReply.value = false

        val targetInnovation = _focusedInnovation.value ?: _selectedDetail.value
        val catalogSnapshot = if (targetInnovation == null) availableInnovations.value.take(6) else emptyList()
        val userKey = _customApiKey.value.takeIf { it.isNotBlank() }
        val modelTier = when {
            userKey != null && attachedImage == null -> "custom_key"
            _useFastAiModel.value -> "fast_flash_lite"
            else -> "deep_flash"
        }
        analyticsTracker.logCopilotPromptSent(
            promptSource = promptSource,
            modelTier = modelTier,
            hasAttachedImage = attachedImage != null,
            hasInnovationContext = targetInnovation != null,
            innovationCategory = targetInnovation?.category,
            promptLength = trimmed.length,
            surface = if (isPopup) "floating_popup" else "full_screen"
        )

        aiStreamJob = viewModelScope.launch {
            var responseLength = 0
            try {
                if (userKey != null && attachedImage == null) {
                    val contextSummary = targetInnovation?.let {
                        "Innovation: ${it.title} | Category: ${it.category} | Stage: ${it.stage} | Description: ${it.description}"
                    }.orEmpty()
                    val reply = repository.sendAiChat(
                        prompt = trimmed,
                        context = contextSummary,
                        customApiKey = userKey
                    )
                    responseLength = reply.length
                    _copilotMessages.update { msgs ->
                        msgs.map { if (it.id == streamMsgId) it.copy(content = reply) else it }
                    }
                } else {
                    aiCopilotRepository.streamBackendCopilotReply(
                        prompt = trimmed,
                        history = priorHistory,
                        activeInnovation = targetInnovation,
                        attachedImage = attachedImage,
                        useFastModel = _useFastAiModel.value,
                        catalogContext = catalogSnapshot
                    ).collect { partial ->
                        responseLength = partial.length
                        _copilotMessages.update { msgs ->
                            msgs.map { if (it.id == streamMsgId) it.copy(content = partial) else it }
                        }
                    }
                }
                analyticsTracker.logCopilotResponseCompleted(
                    modelTier = modelTier,
                    success = true,
                    responseLength = responseLength
                )
            } catch (e: CancellationException) {
                if (userRequestedAiStop) {
                    _copilotMessages.update { msgs ->
                        msgs.map {
                            if (it.id == streamMsgId) {
                                val updated = if (it.content.isBlank()) {
                                    "*(Generation stopped)*"
                                } else {
                                    "${it.content.trimEnd()}\n\n*(Generation stopped)*"
                                }
                                it.copy(content = updated)
                            } else it
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Sana generation error", e)
                analyticsTracker.logCopilotResponseCompleted(
                    modelTier = modelTier,
                    success = false,
                    responseLength = responseLength
                )
                val errorText = "### AI Request Error\n- ${e.localizedMessage ?: "Unable to complete AI Sana request."}\n- Check network connectivity or tap **Regenerate** to retry."
                _copilotMessages.update { msgs ->
                    msgs.map { if (it.id == streamMsgId) it.copy(content = errorText, isError = true) else it }
                }
            } finally {
                _isAiLoading.value = false
                _canRegenerateLastReply.value = lastPromptPayload != null
                if (activeStreamMsgId == streamMsgId) {
                    activeStreamMsgId = null
                }
            }
        }
    }

    fun stopCopilotGeneration() {
        if (!_isAiLoading.value) return
        userRequestedAiStop = true
        aiStreamJob?.cancel()
        _isAiLoading.value = false
        _canRegenerateLastReply.value = lastPromptPayload != null
    }

    fun regenerateLastCopilotReply(isPopup: Boolean = false) {
        val (prompt, bitmap, _) = lastPromptPayload ?: return
        if (_isAiLoading.value) return
        _copilotMessages.update { current ->
            val trimmedList = current.toMutableList()
            if (trimmedList.lastOrNull()?.isAssistant == true && trimmedList.lastOrNull()?.id != "welcome") {
                trimmedList.removeAt(trimmedList.lastIndex)
            }
            if (trimmedList.lastOrNull()?.isAssistant == false) {
                trimmedList.removeAt(trimmedList.lastIndex)
            }
            trimmedList
        }
        sendCopilotPrompt(
            prompt = prompt,
            attachedImage = bitmap,
            promptSource = "regenerate",
            isPopup = isPopup
        )
    }

    fun resetCopilotChat() {
        userRequestedAiStop = false
        aiStreamJob?.cancel()
        _isAiLoading.value = false
        lastPromptPayload = null
        _canRegenerateLastReply.value = false
        analyticsTracker.logCopilotChatReset()
        _copilotMessages.value = listOf(createWelcomeMessage())
    }

    private fun createWelcomeMessage(): CopilotMessage {
        return CopilotMessage(
            id = "welcome",
            isAssistant = true,
            content = "### Meet AI Sana\n" +
                "Senior technical, IP, and venture architect with multi-turn memory, prototype vision, and live Marketplace awareness.\n\n" +
                "- **Code & Architecture:** Hardware BOMs, firmware stacks, schemas, and MVP blueprints.\n" +
                "- **Diligence & Strategy:** Prior-art risks, bottom-up TAM/SAM/SOM, and concrete next steps.",
            timestamp = SecurityCryptoUtils.formatTime()
        )
    }

    private fun friendlyErrorMessage(e: Throwable, fallback: String): String = when (e) {
        is SocketTimeoutException, is InterruptedIOException ->
            "Cloud server is waking up and timed out. Please tap Retry to connect."
        is UnknownHostException, is ConnectException ->
            "Unable to reach the IDEA2IMPACT server. Check your internet connection and tap Retry."
        else -> e.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
