package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.AnalyticsTracker
import com.example.analytics.NoOpAnalyticsTracker
import com.example.data.model.AiFailureCategory
import com.example.data.model.AiRequestPhase
import com.example.data.model.AiStatusUiState
import com.example.data.model.AuthUser
import com.example.data.model.BackendComment
import com.example.data.model.BackendInnovation
import com.example.data.model.BackendTalent
import com.example.data.model.CategoryItem
import com.example.data.model.CopilotMessage
import com.example.data.model.DashboardStats
import com.example.data.model.DigitalNdaRecord
import com.example.data.model.EcosystemMatchResult
import com.example.data.model.ExactCategories
import com.example.data.model.NotificationAlert
import com.example.data.model.PitchDeckResult
import com.example.data.model.Proposal
import com.example.data.model.ShareIdeaScope
import com.example.data.model.TalentProfile
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VerifiedOrganization
import com.example.data.preferences.NotificationLocalDataStore
import com.example.data.preferences.ThemePreferences
import com.example.data.preferences.TokenManager
import com.example.data.repository.AiCopilotRepository
import com.example.data.repository.BackendApiException
import com.example.data.repository.BackendRepository
import com.example.data.repository.EcosystemMatcher
import com.example.data.repository.EcosystemRepository
import com.example.data.repository.IdeaRepository
import com.example.data.repository.SanaPromptEngine
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
import kotlinx.coroutines.isActive
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
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
    private val ecosystemRepository: EcosystemRepository? = null,
    private val ideaRepository: IdeaRepository? = null
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

    private val _firebaseUid = MutableStateFlow<String?>(runCatching { Firebase.auth.currentUser?.uid }.getOrNull())
    val firebaseUid: StateFlow<String?> = _firebaseUid.asStateFlow()

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

    val recentSearches: StateFlow<List<String>> = repository.observeRecentSearches(limit = 10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

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

    private val _backendTalentsState = MutableStateFlow<UiState<List<BackendTalent>>>(UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val firestoreTalents: StateFlow<List<TalentProfile>> = _firebaseUid
        .flatMapLatest { uid ->
            if (uid != null && ideaRepository != null) ideaRepository.observeTalents()
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val talentsState: StateFlow<UiState<List<BackendTalent>>> = combine(
        _backendTalentsState,
        firestoreTalents
    ) { backendState, cloudTalents ->
        when (backendState) {
            is UiState.Success -> {
                val mappedCloud = cloudTalents.map { it.toBackendTalent() }
                val merged = linkedMapOf<String, BackendTalent>()
                mappedCloud.forEach { merged[it.id] = it }
                backendState.data.forEach { if (!merged.containsKey(it.id)) merged[it.id] = it }
                UiState.Success(merged.values.toList())
            }
            is UiState.Error -> {
                if (cloudTalents.isNotEmpty()) {
                    UiState.Success(cloudTalents.map { it.toBackendTalent() })
                } else {
                    backendState
                }
            }
            UiState.Loading -> {
                if (cloudTalents.isNotEmpty()) {
                    UiState.Success(cloudTalents.map { it.toBackendTalent() })
                } else {
                    UiState.Loading
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val myTalentProfile: StateFlow<TalentProfile?> = _firebaseUid
        .flatMapLatest { uid ->
            if (uid != null && ideaRepository != null) ideaRepository.observeMyTalentProfile(uid)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val myFirestoreProfile: StateFlow<UserProfile?> = _firebaseUid
        .flatMapLatest { uid ->
            if (uid != null && ideaRepository != null) ideaRepository.observeUserProfile(uid)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val incomingFirestoreProposals: StateFlow<List<Proposal>> = _firebaseUid
        .flatMapLatest { uid ->
            if (uid != null && ideaRepository != null) ideaRepository.observeIncomingProposals(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val sentFirestoreProposals: StateFlow<List<Proposal>> = _firebaseUid
        .flatMapLatest { uid ->
            if (uid != null && ideaRepository != null) ideaRepository.observeSentProposals(uid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val verifiedOrganizations: StateFlow<List<VerifiedOrganization>> = _firebaseUid
        .flatMapLatest { uid ->
            ecosystemRepository?.observeCombinedOrganizations(isSignedIn = uid != null)
                ?: flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val savedOpportunityIds: StateFlow<Set<String>> = _firebaseUid
        .flatMapLatest { uid ->
            val eco = ecosystemRepository ?: return@flatMapLatest flowOf(emptySet())
            if (uid != null) {
                combine(eco.observeLocalSavedOpportunityIds(), eco.observeCloudSavedOpportunityIds(uid)) { local, cloud ->
                    local + cloud
                }
            } else {
                eco.observeLocalSavedOpportunityIds()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

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

    private val _aiStatusState = MutableStateFlow(AiStatusUiState())
    val aiStatusState: StateFlow<AiStatusUiState> = _aiStatusState.asStateFlow()

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
    private val timedOutPayloads = mutableMapOf<String, TimedOutPromptPayload>()

    private data class TimedOutPromptPayload(
        val prompt: String,
        val attachedImage: Bitmap?,
        val promptSource: String,
        val userMessageId: String,
        val hasPartialOutput: Boolean
    )

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
                                _firebaseUid.value = fbUser.uid
                                _currentUser.value = fbUser.toAuthUser()
                                loadDashboardStats()
                            }
                        }
                    }
            } else {
                Firebase.auth.currentUser?.let { fbUser ->
                    _firebaseUid.value = fbUser.uid
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
        _firebaseUid.value = fbUser.uid
        _currentUser.value = authUser
        _authError.value = null
        _showAuthModal.value = false
        if (!isAutoSignIn) {
            _snackbarMessage.value = "Signed in with Google as ${authUser.name}!"
        }
        viewModelScope.launch {
            runCatching {
                ideaRepository?.ensureUserAndSeedCatalog(
                    displayName = fbUser.displayName,
                    email = fbUser.email,
                    photoUrl = fbUser.photoUrl?.toString()
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                Log.w(TAG, "Firestore user bootstrap skipped/deferred: ${e.message}")
            }
        }
        loadDashboardStats()
    }

    fun logout() {
        repository.signOut()
        runCatching { Firebase.auth.signOut() }
        _firebaseUid.value = null
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
            if (clean.length >= 2) {
                delay(900L)
                repository.recordMarketplaceSearch(clean)
            }
        }
    }

    fun submitSearchQuery(query: String = _searchQuery.value) {
        val clean = query.trim()
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (clean.isNotEmpty()) {
                analyticsTracker.logMarketplaceSearch(
                    queryPreview = clean,
                    queryLength = clean.length
                )
                repository.recordMarketplaceSearch(clean)
            }
            loadInnovations()
        }
    }

    fun selectRecentSearch(query: String) {
        submitSearchQuery(query)
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
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
        val activeQuery = _searchQuery.value.trim()
        if (source == "marketplace" && activeQuery.isNotBlank()) {
            viewModelScope.launch {
                repository.recordMarketplaceSearch(activeQuery)
            }
        }
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
            _backendTalentsState.value = UiState.Loading
            try {
                val talents = repository.fetchTalents()
                _backendTalentsState.value = UiState.Success(talents)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Unable to load talents: ${e.message}")
                _backendTalentsState.value = UiState.Error(
                    friendlyErrorMessage(e, "Failed to load builders & partners directory.")
                )
            }
        }
    }

    // --- Step 4, 5, 6, 7: Directory, Matching, Connections & Funding ---

    fun importVerifiedDirectoryJson(rawJson: String): Result<Int> {
        val eco = ecosystemRepository
            ?: return Result.failure(IllegalStateException("Directory repository unavailable."))
        val result = eco.importVerifiedJson(rawJson)
        result.onSuccess { count ->
            _snackbarMessage.value = "Loaded $count source-verified organization record(s)."
        }
        return result
    }

    fun clearImportedDirectory() {
        ecosystemRepository?.clearImportedOrganizations()
        _snackbarMessage.value = "Cleared locally imported directory records."
    }

    fun toggleSaveOpportunity(organization: VerifiedOrganization) {
        val eco = ecosystemRepository ?: return
        val currentlySaved = savedOpportunityIds.value.contains(organization.id)
        viewModelScope.launch {
            runCatching {
                eco.toggleSavedOpportunity(organization, currentlySaved)
            }.onSuccess {
                _snackbarMessage.value = if (currentlySaved) {
                    "Removed \"${organization.name}\" from saved opportunities."
                } else {
                    "Saved \"${organization.name}\" to your opportunities."
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update saved opportunity."
            }
        }
    }

    fun computeEcosystemMatches(
        ideaTitle: String,
        ideaDescription: String,
        ideaCategory: String,
        ideaStage: String = "",
        preferredLocation: String = ""
    ): List<EcosystemMatchResult> {
        val talents = (talentsState.value as? UiState.Success)?.data.orEmpty()
        return EcosystemMatcher.matchIdeaToEcosystem(
            ideaTitle = ideaTitle,
            ideaDescription = ideaDescription,
            ideaCategory = ideaCategory,
            ideaStage = ideaStage,
            preferredLocation = preferredLocation,
            organizations = verifiedOrganizations.value,
            talents = talents
        )
    }

    fun toggleTalentVisibility(isPublic: Boolean) {
        if (Firebase.auth.currentUser == null || ideaRepository == null) {
            _snackbarMessage.value = "Sign in with Google to control your public Firestore profile visibility."
            openAuthModal()
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                ideaRepository.updateTalentVisibility(isPublic)
                _snackbarMessage.value = if (isPublic) {
                    "Your profile is now discoverable in Collaborators & Builders."
                } else {
                    "Your profile is now hidden from public talent discovery."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to update profile visibility."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun saveMyTalentProfile(
        name: String,
        role: String,
        headline: String,
        bio: String,
        skills: List<String>,
        location: String,
        isPublic: Boolean,
        onDone: () -> Unit = {}
    ) {
        if (Firebase.auth.currentUser == null || ideaRepository == null) {
            _snackbarMessage.value = "Sign in with Google to update your public collaborator profile."
            openAuthModal()
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                val count = (dashboardState.value as? UiState.Success)?.data?.innovationsPosted?.toLong() ?: 0L
                ideaRepository.updateUserProfile(
                    name = name,
                    role = role,
                    headline = headline,
                    bio = bio,
                    skills = skills,
                    location = location,
                    existingProjectsCount = count,
                    isPublic = isPublic
                )
                _snackbarMessage.value = "Collaborator profile saved."
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Failed to save profile."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun sendCollaborationProposal(
        recipientTalent: BackendTalent,
        innovation: BackendInnovation?,
        proposalType: String,
        amount: String,
        message: String,
        contactInfo: String,
        shareScope: ShareIdeaScope,
        customProblemSummary: String = "",
        onSuccess: () -> Unit
    ) {
        if (!isUserSignedIn()) {
            _snackbarMessage.value = "Sign in to send a collaboration proposal."
            openAuthModal()
            return
        }
        val currentFbUid = Firebase.auth.currentUser?.uid
        val controlledSummary = if (innovation != null) {
            EcosystemMatcher.buildControlledShareSummary(
                ideaTitle = innovation.title,
                ideaCategory = innovation.category,
                ideaStage = innovation.stage,
                ideaDescription = innovation.description,
                scope = shareScope,
                customProblemSummary = customProblemSummary
            )
        } else null

        val sharedInnovationId = if (shareScope == ShareIdeaScope.NONE) null else innovation?.id
        val sharedTitle = when {
            shareScope == ShareIdeaScope.NONE || innovation == null -> "Direct $proposalType Intro"
            else -> innovation.title
        }

        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                if (currentFbUid != null && ideaRepository != null && recipientTalent.id != currentFbUid) {
                    ideaRepository.submitCollaborationProposal(
                        recipientId = recipientTalent.id,
                        recipientName = recipientTalent.name,
                        innovationId = sharedInnovationId,
                        innovationTitle = sharedTitle,
                        proposalType = proposalType,
                        amount = amount,
                        message = message,
                        contactInfo = contactInfo,
                        sharedIdeaScope = shareScope,
                        sharedIdeaSummary = controlledSummary,
                        senderProfile = myFirestoreProfile.value
                    )
                    _snackbarMessage.value = "Proposal sent to ${recipientTalent.name}!"
                    onSuccess()
                } else if (repository.hasSavedToken() && sharedInnovationId != null) {
                    val composedMsg = buildString {
                        append("To ${recipientTalent.name}: $message")
                        if (!controlledSummary.isNullOrBlank()) {
                            append("\n\n$controlledSummary")
                        }
                    }
                    repository.submitProposal(sharedInnovationId, proposalType, amount, composedMsg, contactInfo)
                    _snackbarMessage.value = "Proposal sent to ${recipientTalent.name}!"
                    loadDashboardStats()
                    onSuccess()
                } else if (currentFbUid == null) {
                    _snackbarMessage.value = "Sign in with Google to send direct collaboration proposals."
                    openAuthModal()
                } else {
                    _snackbarMessage.value = "You cannot send a proposal to your own profile."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage
                    ?: "Could not deliver proposal. Target profile may not yet be synced on Firestore."
            } finally {
                _isActionBusy.value = false
            }
        }
    }

    fun respondToProposal(proposalId: String, accept: Boolean) {
        if (Firebase.auth.currentUser == null || ideaRepository == null) {
            _snackbarMessage.value = "Sign in with Google to respond to proposals."
            openAuthModal()
            return
        }
        viewModelScope.launch {
            _isActionBusy.value = true
            try {
                ideaRepository.updateProposalStatus(proposalId, if (accept) "ACCEPTED" else "DECLINED")
                _snackbarMessage.value = if (accept) "Proposal accepted." else "Proposal declined."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "Unable to update proposal status."
            } finally {
                _isActionBusy.value = false
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
        val talentSnapshot = (talentsState.value as? UiState.Success)?.data.orEmpty().take(8)
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

        val directoryLines = verifiedOrganizations.value
            .take(15)
            .map { it.toDirectorySummaryLine() }

        val isCustomRoute = userKey != null && attachedImage == null
        val requestStartMs = System.currentTimeMillis()
        val activePhase = MutableStateFlow(AiRequestPhase.CHECKING_CONNECTION)

        fun updateAiStatus(phase: AiRequestPhase) {
            activePhase.value = phase
            val elapsedMs = System.currentTimeMillis() - requestStartMs
            _aiStatusState.value = AiStatusUiState(
                phase = phase,
                label = SanaPromptEngine.resolveStatusLabel(
                    phase = phase,
                    elapsedMs = elapsedMs,
                    hasAttachedImage = attachedImage != null,
                    isCustomKey = isCustomRoute
                ),
                hint = SanaPromptEngine.resolveStatusHint(
                    phase = phase,
                    elapsedMs = elapsedMs,
                    isFastMode = _useFastAiModel.value
                )
            )
        }

        updateAiStatus(AiRequestPhase.CHECKING_CONNECTION)

        var resolvedModelLabel = when {
            isCustomRoute -> "Custom AI Backend"
            _useFastAiModel.value -> "AI Sana Fast (gemini-3.5-flash-lite)"
            else -> "AI Sana Deep (gemini-3.8-flash)"
        }
        var resolvedModelNotice: String? = null

        aiStreamJob = viewModelScope.launch {
            var responseLength = 0
            val statusTickerJob = launch {
                while (isActive) {
                    delay(1000L)
                    updateAiStatus(activePhase.value)
                }
            }
            try {
                if (isCustomRoute) {
                    val contextPayload = SanaPromptEngine.buildCustomKeyContextString(
                        prompt = trimmed,
                        history = priorHistory,
                        activeInnovation = targetInnovation,
                        catalogContext = catalogSnapshot,
                        talentContext = talentSnapshot,
                        directorySummaryLines = directoryLines,
                        isFastMode = _useFastAiModel.value
                    )
                    updateAiStatus(AiRequestPhase.THINKING)
                    val reply = repository.sendAiChat(
                        prompt = trimmed,
                        context = contextPayload,
                        customApiKey = userKey!!,
                        useFastModel = _useFastAiModel.value
                    )
                    updateAiStatus(AiRequestPhase.STREAMING)
                    responseLength = reply.length
                    _copilotMessages.update { msgs ->
                        msgs.map {
                            if (it.id == streamMsgId) {
                                it.copy(
                                    content = reply,
                                    respondingModelLabel = resolvedModelLabel
                                )
                            } else it
                        }
                    }
                } else {
                    aiCopilotRepository.streamBackendCopilotReply(
                        prompt = trimmed,
                        history = priorHistory,
                        activeInnovation = targetInnovation,
                        attachedImage = attachedImage,
                        useFastModel = _useFastAiModel.value,
                        catalogContext = catalogSnapshot,
                        talentContext = talentSnapshot,
                        directorySummaryLines = directoryLines,
                        onPhaseChange = { phase -> updateAiStatus(phase) },
                        onModelResolved = { route, modelName, switchedFromModel, fallbackReason ->
                            val isFastResolved = route == "fast_flash_lite"
                            resolvedModelLabel = if (isFastResolved) {
                                "AI Sana Fast ($modelName)"
                            } else {
                                "AI Sana Deep ($modelName)"
                            }
                            if (switchedFromModel != null) {
                                _useFastAiModel.value = true
                                val reasonLabel = when (fallbackReason) {
                                    AiFailureCategory.MODEL_UNAVAILABLE -> "Deep mode was unavailable"
                                    AiFailureCategory.QUOTA_OR_RATE_LIMIT -> "Deep mode hit a rate/quota limit"
                                    AiFailureCategory.TIMEOUT -> "Deep mode timed out before responding"
                                    else -> "Deep mode hit temporary provider overload"
                                }
                                resolvedModelNotice = "Switched from $switchedFromModel to AI Sana Fast ($modelName) ($reasonLabel)."
                                _snackbarMessage.value = "Switched to AI Sana Fast ($modelName)."
                            }
                            _copilotMessages.update { msgs ->
                                msgs.map {
                                    if (it.id == streamMsgId) {
                                        it.copy(
                                            respondingModelLabel = resolvedModelLabel,
                                            modelNotice = resolvedModelNotice
                                        )
                                    } else it
                                }
                            }
                        }
                    ).collect { partial ->
                        responseLength = partial.length
                        _copilotMessages.update { msgs ->
                            msgs.map {
                                if (it.id == streamMsgId) {
                                    it.copy(
                                        content = partial,
                                        respondingModelLabel = resolvedModelLabel,
                                        modelNotice = resolvedModelNotice
                                    )
                                } else it
                            }
                        }
                    }
                }
                analyticsTracker.logCopilotResponseCompleted(
                    modelTier = if (resolvedModelNotice != null) "fast_flash_lite_fallback" else modelTier,
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
                                it.copy(
                                    content = updated,
                                    respondingModelLabel = resolvedModelLabel,
                                    modelNotice = resolvedModelNotice
                                )
                            } else it
                        }
                    }
                }
                throw e
            } catch (e: Exception) {
                val category = SanaPromptEngine.classifyAiError(e)
                val httpCode = (e as? BackendApiException)?.code
                val diag = e as? com.example.data.repository.AiCopilotDiagnosticException
                val safeCause = SanaPromptEngine.redactSensitiveInfo(e.message.orEmpty()).take(160)
                Log.e(
                    TAG,
                    "AI Sana failure [tier=$modelTier, category=$category, code=$httpCode, phase=${diag?.phase ?: "request"}, appCheck=${diag?.appCheckStatus ?: "n/a"}, fallbackAttempted=${diag?.fallbackAttempted ?: false}, class=${e.javaClass.simpleName}, detail=$safeCause]"
                )
                analyticsTracker.logCopilotResponseCompleted(
                    modelTier = modelTier,
                    success = false,
                    responseLength = responseLength
                )
                val isTimeout = category == AiFailureCategory.TIMEOUT
                val diagnosed = SanaPromptEngine.diagnoseAiError(e, modelTier)
                val partialPrefix = diag
                    ?.partialContent
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                val hasPartialOutput = partialPrefix != null
                timedOutPayloads[streamMsgId] = TimedOutPromptPayload(
                    prompt = trimmed,
                    attachedImage = attachedImage,
                    promptSource = promptSource,
                    userMessageId = userMsg.id,
                    hasPartialOutput = hasPartialOutput
                )
                val offerFast = !isCustomRoute &&
                    !_useFastAiModel.value &&
                    !hasPartialOutput &&
                    diag?.fallbackAttempted != true &&
                    category in setOf(
                        AiFailureCategory.PROVIDER_OVERLOAD,
                        AiFailureCategory.TIMEOUT,
                        AiFailureCategory.QUOTA_OR_RATE_LIMIT,
                        AiFailureCategory.MODEL_UNAVAILABLE
                    )
                val offerDefault = isCustomRoute
                val actionHint = when {
                    isTimeout && hasPartialOutput -> "Tap **Resume** below to re-trigger your request without re-typing."
                    isTimeout -> "Tap **Retry** below to re-trigger your request without re-typing."
                    offerDefault -> "Tap **Use Default AI** or **Retry** below, or verify your key in Key Settings."
                    offerFast -> "Tap **Switch to Fast Mode** or **Retry** below."
                    else -> "Tap **Retry** below or adjust model/key settings."
                }
                val errorText = if (partialPrefix != null) {
                    "$partialPrefix\n\n---\n### AI Sana Error\n- $diagnosed\n- $actionHint"
                } else {
                    "### AI Sana Error\n- $diagnosed\n- $actionHint"
                }
                _copilotMessages.update { msgs ->
                    msgs.map {
                        if (it.id == streamMsgId) {
                            it.copy(
                                content = errorText,
                                isError = true,
                                isTimeout = isTimeout,
                                hasPartialTimeoutOutput = hasPartialOutput,
                                respondingModelLabel = resolvedModelLabel,
                                modelNotice = resolvedModelNotice,
                                offerFastFallback = offerFast,
                                offerDefaultAiFallback = offerDefault
                            )
                        } else {
                            it
                        }
                    }
                }
            } finally {
                statusTickerJob.cancel()
                _isAiLoading.value = false
                _aiStatusState.value = AiStatusUiState()
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
        _aiStatusState.value = AiStatusUiState()
        _canRegenerateLastReply.value = lastPromptPayload != null
    }

    fun switchToFastAndRetry(messageId: String, isPopup: Boolean = false) {
        if (_isAiLoading.value) return
        setFastAiModel(true, surface = if (isPopup) "floating_popup_fallback" else "full_screen_fallback")
        retryTimedOutCopilotMessage(messageId, isPopup = isPopup)
    }

    fun switchToDefaultAiAndRetry(messageId: String, isPopup: Boolean = false) {
        if (_isAiLoading.value) return
        saveCustomApiKey("")
        retryTimedOutCopilotMessage(messageId, isPopup = isPopup)
    }

    fun retryTimedOutCopilotMessage(messageId: String, isPopup: Boolean = false) {
        if (_isAiLoading.value) return
        val payload = timedOutPayloads.remove(messageId)
        val prompt: String
        val bitmap: Bitmap?
        val userMessageId: String?
        val retrySource: String
        if (payload != null) {
            prompt = payload.prompt
            bitmap = payload.attachedImage
            userMessageId = payload.userMessageId
            retrySource = if (payload.hasPartialOutput) "timeout_resume" else "timeout_retry"
        } else {
            val fallback = lastPromptPayload ?: return
            prompt = fallback.first
            bitmap = fallback.second
            userMessageId = null
            retrySource = "timeout_retry"
        }

        _copilotMessages.update { current ->
            val updated = current.toMutableList()
            val errIdx = updated.indexOfFirst { it.id == messageId }
            if (errIdx >= 0) {
                val isLastTurn = errIdx == updated.lastIndex
                updated.removeAt(errIdx)
                if (isLastTurn) {
                    val prevUserIdx = if (userMessageId != null) {
                        updated.indexOfLast { it.id == userMessageId && !it.isAssistant }
                    } else {
                        if (updated.lastOrNull()?.isAssistant == false) updated.lastIndex else -1
                    }
                    if (prevUserIdx >= 0 && prevUserIdx == updated.lastIndex) {
                        updated.removeAt(prevUserIdx)
                    }
                }
            }
            updated
        }

        sendCopilotPrompt(
            prompt = prompt,
            attachedImage = bitmap,
            promptSource = retrySource,
            isPopup = isPopup
        )
    }

    fun regenerateLastCopilotReply(isPopup: Boolean = false) {
        val (prompt, bitmap, _) = lastPromptPayload ?: return
        if (_isAiLoading.value) return
        _copilotMessages.update { current ->
            val trimmedList = current.toMutableList()
            val lastErrId = trimmedList.lastOrNull()?.id
            if (trimmedList.lastOrNull()?.isAssistant == true && lastErrId != "welcome") {
                if (lastErrId != null) timedOutPayloads.remove(lastErrId)
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
        _aiStatusState.value = AiStatusUiState()
        lastPromptPayload = null
        timedOutPayloads.clear()
        _canRegenerateLastReply.value = false
        analyticsTracker.logCopilotChatReset()
        _copilotMessages.value = listOf(createWelcomeMessage())
    }

    private fun createWelcomeMessage(): CopilotMessage {
        return CopilotMessage(
            id = "welcome",
            isAssistant = true,
            content = "### Meet AI Sana\n" +
                "Your conversational assistant and startup/idea advisor inside IDEA2IMPACT. Ask any question, share an idea for honest feedback, or attach a prototype image.\n\n" +
                "- **Idea Feedback & Validation:** Strengths, risks, open questions, practical tests, and an early 1–10 assessment.\n" +
                "- **Technical & Venture Advice:** Architecture planning, IP considerations, or browsing verified organizations and collaborators.",
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

    private fun TalentProfile.toBackendTalent(): BackendTalent {
        val roleCat = when (role) {
            "Creator" -> "Creators"
            "Investor" -> "Investors"
            "Patent Professional" -> "Patent Professionals"
            else -> "Builders"
        }
        return BackendTalent(
            id = userId.ifBlank { id },
            name = name,
            role = role,
            roleCategory = roleCat,
            headline = headline,
            skills = skills,
            location = location.ifBlank { "India" },
            avatarUrl = avatarUrl,
            verified = projectsCount > 0
        )
    }

    private companion object {
        const val TAG = "AppViewModel"
    }
}
