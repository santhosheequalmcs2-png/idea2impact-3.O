package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.Idea2ImpactDatabase
import com.example.data.model.UiState
import com.example.data.preferences.ThemePreferences
import com.example.data.preferences.TokenManager
import com.example.data.repository.BackendRepository
import com.example.ui.components.BackendAuthModal
import com.example.ui.components.LocalNavAnimatedVisibilityScope
import com.example.ui.components.LocalSharedTransitionScope
import com.example.ui.components.MovableAiPopup
import com.example.ui.components.OpeningSplashOverlay
import com.example.ui.screens.CopilotScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PricingScreen
import com.example.ui.screens.TalentScreen
import com.example.ui.theme.Idea2ImpactTheme
import com.example.ui.viewmodel.AppViewModel
import com.example.util.StartupSoundPlayer
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable data object Home : AppRoute
    @Serializable data object Marketplace : AppRoute
    @Serializable data object Copilot : AppRoute
    @Serializable data object Talents : AppRoute
    @Serializable data object Dashboard : AppRoute
    @Serializable data object Detail : AppRoute
    @Serializable data object Pricing : AppRoute
}

private data class BottomTab(
    val route: AppRoute,
    val qualifiedName: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private fun routeOrder(route: String?): Int {
    val name = route.orEmpty()
    return when {
        name.contains("Home") -> 0
        name.contains("Marketplace") -> 1
        name.contains("Copilot") -> 2
        name.contains("Talents") -> 3
        name.contains("Dashboard") -> 4
        name.contains("Detail") || name.contains("Pricing") -> 5
        else -> 0
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Idea2ImpactApp() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val database = Idea2ImpactDatabase.getInstance(app)
                val tokenManager = TokenManager(app)
                val themePreferences = ThemePreferences(app)
                val repository = BackendRepository(
                    tokenManager = tokenManager,
                    innovationDao = database.innovationDao()
                )
                AppViewModel(
                    repository = repository,
                    tokenManager = tokenManager,
                    themePreferences = themePreferences
                )
            }
        }
    )

    val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isSoundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val showAuthModal by viewModel.showAuthModal.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val innovationsState by viewModel.innovationsState.collectAsStateWithLifecycle()
    val isSyncingInnovations by viewModel.isSyncingInnovations.collectAsStateWithLifecycle()
    val isShowingOfflineCache by viewModel.isShowingOfflineCache.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStage by viewModel.selectedStage.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()

    val selectedDetail by viewModel.selectedDetail.collectAsStateWithLifecycle()
    val detailComments by viewModel.detailComments.collectAsStateWithLifecycle()
    val signedNdas by viewModel.signedNdas.collectAsStateWithLifecycle()
    val pitchDeckState by viewModel.pitchDeckState.collectAsStateWithLifecycle()

    val talentsState by viewModel.talentsState.collectAsStateWithLifecycle()
    val selectedTalentRole by viewModel.selectedTalentRole.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val isRefreshingDashboard by viewModel.isRefreshingDashboard.collectAsStateWithLifecycle()

    val availableInnovations by viewModel.availableInnovations.collectAsStateWithLifecycle()
    val focusedInnovation by viewModel.focusedInnovation.collectAsStateWithLifecycle()
    val useFastAiModel by viewModel.useFastAiModel.collectAsStateWithLifecycle()
    val copilotMessages by viewModel.copilotMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()

    val isBusy by viewModel.isActionBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showOpeningSplash by rememberSaveable { mutableStateOf(true) }
    var isAiPopupExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isAiPopupExpanded) {
        isAiPopupExpanded = false
    }

    LaunchedEffect(isSoundEnabled) {
        StartupSoundPlayer.setSoundEnabled(context, isSoundEnabled)
    }

    LaunchedEffect(pitchDeckState) {
        if (pitchDeckState is UiState.Success) {
            StartupSoundPlayer.playSuccess(context)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteName = navBackStackEntry?.destination?.route.orEmpty()

    // Exact 5 Bottom Navigation Tabs in exact requested order
    val bottomTabs = remember {
        listOf(
            BottomTab(AppRoute.Home, AppRoute.Home::class.qualifiedName.orEmpty(), "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
            BottomTab(AppRoute.Marketplace, AppRoute.Marketplace::class.qualifiedName.orEmpty(), "Marketplace", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_marketplace"),
            BottomTab(AppRoute.Copilot, AppRoute.Copilot::class.qualifiedName.orEmpty(), "AI Copilot", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_copilot"),
            BottomTab(AppRoute.Talents, AppRoute.Talents::class.qualifiedName.orEmpty(), "Talents", Icons.Filled.Groups, Icons.Outlined.Groups, "nav_talents"),
            BottomTab(AppRoute.Dashboard, AppRoute.Dashboard::class.qualifiedName.orEmpty(), "Dashboard", Icons.Filled.BarChart, Icons.Outlined.BarChart, "nav_dashboard")
        )
    }

    val showMainChrome = !currentRouteName.contains("Detail") && !currentRouteName.contains("Pricing")

    fun navigateTopTab(tab: BottomTab) {
        StartupSoundPlayer.playTap(context)
        navController.navigate(tab.route) {
            popUpTo(AppRoute.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Idea2ImpactTheme(darkTheme = isDark) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 700.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && showMainChrome) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        bottomTabs.forEach { tab ->
                            val selected = currentRouteName.startsWith(tab.qualifiedName)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab) },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = null) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        if (showMainChrome) {
                            var showUserMenu by remember { mutableStateOf(false) }
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                showOpeningSplash = true
                                                StartupSoundPlayer.playAppOpenChime(context)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                            .testTag("brand_logo_replay_intro")
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(9.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.AutoAwesome,
                                                    contentDescription = stringResource(R.string.cd_brand_mark),
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "IDEA2IMPACT",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            val nextEnabled = !isSoundEnabled
                                            StartupSoundPlayer.setSoundEnabled(context, nextEnabled)
                                            viewModel.setSoundEnabled(nextEnabled)
                                            if (nextEnabled) {
                                                StartupSoundPlayer.playTap(context)
                                            }
                                        },
                                        modifier = Modifier.size(48.dp).testTag("sound_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSoundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                            contentDescription = if (isSoundEnabled) "Mute sound effects" else "Enable sound effects",
                                            tint = if (isSoundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            StartupSoundPlayer.playTap(context)
                                            viewModel.toggleTheme()
                                        },
                                        modifier = Modifier.size(48.dp).testTag("theme_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = if (isDark) "Switch to Light Mode" else "Switch to Dark Mode"
                                        )
                                    }

                                    val user = currentUser
                                    if (user == null) {
                                        Button(
                                            onClick = {
                                                StartupSoundPlayer.playTap(context)
                                                viewModel.openAuthModal()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .padding(end = 12.dp)
                                                .heightIn(min = 38.dp)
                                                .testTag("header_login_button")
                                        ) {
                                            Text("Login")
                                        }
                                    } else {
                                        Box(modifier = Modifier.padding(end = 12.dp)) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clickable { showUserMenu = true }
                                                    .testTag("header_user_avatar")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = user.name.take(1).uppercase(),
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }

                                            DropdownMenu(
                                                expanded = showUserMenu,
                                                onDismissRequest = { showUserMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Dashboard") },
                                                    onClick = {
                                                        showUserMenu = false
                                                        navigateTopTab(bottomTabs[4])
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Plans & Pricing") },
                                                    onClick = {
                                                        showUserMenu = false
                                                        navController.navigate(AppRoute.Pricing)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Sign Out") },
                                                    onClick = {
                                                        showUserMenu = false
                                                        viewModel.logout()
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (!useRail && showMainChrome) {
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                bottomTabs.forEach { tab ->
                                    val selected = currentRouteName.startsWith(tab.qualifiedName)
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopTab(tab) },
                                        icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = null) },
                                        label = { Text(tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag(tab.testTag)
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.Home,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            val from = routeOrder(initialState.destination.route)
                            val to = routeOrder(targetState.destination.route)
                            when {
                                to >= 5 -> fadeIn(tween(340, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.94f, animationSpec = tween(340, easing = FastOutSlowInEasing))
                                to > from -> fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { (it * 0.22f).toInt() }
                                to < from -> fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -(it * 0.22f).toInt() }
                                else -> fadeIn(tween(240))
                            }
                        },
                        exitTransition = {
                            val from = routeOrder(initialState.destination.route)
                            val to = routeOrder(targetState.destination.route)
                            when {
                                to >= 5 -> fadeOut(tween(300)) + scaleOut(targetScale = 0.96f, animationSpec = tween(300))
                                to > from -> fadeOut(tween(240)) +
                                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { -(it * 0.16f).toInt() }
                                to < from -> fadeOut(tween(240)) +
                                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { (it * 0.16f).toInt() }
                                else -> fadeOut(tween(200))
                            }
                        },
                        popEnterTransition = {
                            val from = routeOrder(initialState.destination.route)
                            val to = routeOrder(targetState.destination.route)
                            when {
                                from >= 5 -> fadeIn(tween(320, easing = FastOutSlowInEasing)) +
                                    scaleIn(initialScale = 0.96f, animationSpec = tween(320, easing = FastOutSlowInEasing))
                                to < from -> fadeIn(tween(280, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -(it * 0.22f).toInt() }
                                else -> fadeIn(tween(240))
                            }
                        },
                        popExitTransition = {
                            val from = routeOrder(initialState.destination.route)
                            val to = routeOrder(targetState.destination.route)
                            when {
                                from >= 5 -> fadeOut(tween(300)) + scaleOut(targetScale = 0.94f, animationSpec = tween(300))
                                to < from -> fadeOut(tween(240)) +
                                    slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { (it * 0.16f).toInt() }
                                else -> fadeOut(tween(200))
                            }
                        }
                    ) {
                        composable<AppRoute.Home> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                HomeScreen(
                                    categories = categories,
                                    featuredInnovations = availableInnovations,
                                    entranceReady = !showOpeningSplash && hasCompletedOnboarding == true,
                                    onOpenDetail = { item ->
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.openInnovationDetail(item)
                                        navController.navigate(AppRoute.Detail)
                                    },
                                    onExploreInnovations = { cat ->
                                        viewModel.setSelectedCategory(cat)
                                        navigateTopTab(bottomTabs[1])
                                    },
                                    onMeetBuilders = {
                                        navigateTopTab(bottomTabs[3])
                                    },
                                    onOpenPricing = {
                                        StartupSoundPlayer.playTap(context)
                                        navController.navigate(AppRoute.Pricing)
                                    },
                                    onReplayOnboarding = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.replayOnboarding()
                                    }
                                )
                            }
                        }

                        composable<AppRoute.Marketplace> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                MarketplaceScreen(
                                    innovationsState = innovationsState,
                                    isRefreshing = isSyncingInnovations,
                                    isOfflineCache = isShowingOfflineCache,
                                    searchQuery = searchQuery,
                                    selectedCategory = selectedCategory,
                                    selectedStage = selectedStage,
                                    selectedSort = selectedSort,
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onCategoryChange = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setSelectedCategory(it)
                                    },
                                    onStageChange = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setSelectedStage(it)
                                    },
                                    onSortChange = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setSelectedSort(it)
                                    },
                                    onClearFilters = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.clearMarketplaceFilters()
                                    },
                                    onRetry = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.refreshMarketplace()
                                    },
                                    onOpenDetail = { item ->
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.openInnovationDetail(item)
                                        navController.navigate(AppRoute.Detail)
                                    },
                                    onToggleLike = {
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.toggleLike(it)
                                    },
                                    onToggleBookmark = {
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.toggleBookmark(it)
                                    }
                                )
                            }
                        }

                        composable<AppRoute.Copilot> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                CopilotScreen(
                                    messages = copilotMessages,
                                    isLoading = isAiLoading,
                                    customApiKey = customApiKey,
                                    availableInnovations = availableInnovations,
                                    focusedInnovation = focusedInnovation,
                                    useFastModel = useFastAiModel,
                                    onSelectFocusedInnovation = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setFocusedInnovation(it)
                                    },
                                    onToggleFastModel = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setFastAiModel(it)
                                    },
                                    onSaveApiKey = {
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.saveCustomApiKey(it)
                                    },
                                    onSendPrompt = { prompt, bitmap ->
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.sendCopilotPrompt(prompt, bitmap)
                                    },
                                    onResetChat = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.resetCopilotChat()
                                    }
                                )
                            }
                        }

                        composable<AppRoute.Talents> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                TalentScreen(
                                    talentsState = talentsState,
                                    selectedRole = selectedTalentRole,
                                    onRoleChange = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.setSelectedTalentRole(it)
                                    },
                                    onRetry = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.loadTalents()
                                    },
                                    onSendConnectMessage = { talent, _ ->
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.showMessage("Connection proposal sent to ${talent.name}!")
                                    }
                                )
                            }
                        }

                        composable<AppRoute.Dashboard> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                DashboardScreen(
                                    currentUser = currentUser,
                                    dashboardState = dashboardState,
                                    isRefreshing = isRefreshingDashboard,
                                    onOpenAuth = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.openAuthModal()
                                    },
                                    onRefresh = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.loadDashboardStats(isPullRefresh = true)
                                    },
                                    onOpenDetail = { item ->
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.openInnovationDetail(item)
                                        navController.navigate(AppRoute.Detail)
                                    },
                                    onToggleLike = {
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.toggleLike(it)
                                    },
                                    onToggleBookmark = {
                                        StartupSoundPlayer.playSuccess(context)
                                        viewModel.toggleBookmark(it)
                                    },
                                    onOpenPricing = {
                                        StartupSoundPlayer.playTap(context)
                                        navController.navigate(AppRoute.Pricing)
                                    },
                                    onSignOut = {
                                        StartupSoundPlayer.playTap(context)
                                        viewModel.logout()
                                    }
                                )
                            }
                        }

                        composable<AppRoute.Detail> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                BackHandler { navController.popBackStack() }
                                val detail = selectedDetail
                                if (detail != null) {
                                    DetailScreen(
                                        innovation = detail,
                                        comments = detailComments,
                                        currentUserName = currentUser?.name ?: "Authorized Signer",
                                        signedNda = signedNdas[detail.id],
                                        pitchDeckState = pitchDeckState,
                                        isBusy = isBusy,
                                        onBack = {
                                            StartupSoundPlayer.playTap(context)
                                            navController.popBackStack()
                                        },
                                        onToggleLike = {
                                            StartupSoundPlayer.playSuccess(context)
                                            viewModel.toggleLike(detail)
                                        },
                                        onToggleBookmark = {
                                            StartupSoundPlayer.playSuccess(context)
                                            viewModel.toggleBookmark(detail)
                                        },
                                        onPostComment = { text, onDone ->
                                            viewModel.postComment(detail.id, text) {
                                                StartupSoundPlayer.playSuccess(context)
                                                onDone()
                                            }
                                        },
                                        onSubmitProposal = { type, amount, msg, contact, onDone ->
                                            viewModel.submitProposal(detail.id, type, amount, msg, contact) {
                                                StartupSoundPlayer.playSuccess(context)
                                                onDone()
                                            }
                                        },
                                        onSignNda = { name, org ->
                                            StartupSoundPlayer.playSuccess(context)
                                            viewModel.signDigitalNda(detail, name, org)
                                        },
                                        onGeneratePitchDeck = {
                                            StartupSoundPlayer.playTap(context)
                                            viewModel.generatePitchDeck(detail)
                                        },
                                        onDismissPitchDeck = { viewModel.clearPitchDeck() },
                                        onAnalyzeWithCopilot = {
                                            StartupSoundPlayer.playTap(context)
                                            viewModel.setFocusedInnovation(detail)
                                            navigateTopTab(bottomTabs[2])
                                        }
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Select an innovation from Marketplace.")
                                    }
                                }
                            }
                        }

                        composable<AppRoute.Pricing> {
                            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this@composable) {
                                BackHandler { navController.popBackStack() }
                                PricingScreen(
                                    onBack = {
                                        StartupSoundPlayer.playTap(context)
                                        navController.popBackStack()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (!showOpeningSplash && hasCompletedOnboarding == true && !currentRouteName.contains("Copilot") && !showAuthModal) {
                MovableAiPopup(
                    isExpanded = isAiPopupExpanded,
                    onExpandedChange = { expanded ->
                        StartupSoundPlayer.playTap(context)
                        isAiPopupExpanded = expanded
                    },
                    messages = copilotMessages,
                    isLoading = isAiLoading,
                    useFastModel = useFastAiModel,
                    activeContext = focusedInnovation ?: selectedDetail,
                    onToggleFastModel = {
                        StartupSoundPlayer.playTap(context)
                        viewModel.setFastAiModel(it)
                    },
                    onSendPrompt = { prompt ->
                        StartupSoundPlayer.playTap(context)
                        viewModel.sendCopilotPrompt(prompt)
                    },
                    onOpenFullCopilot = {
                        isAiPopupExpanded = false
                        navigateTopTab(bottomTabs[2])
                    }
                )
            }

            if (showAuthModal && hasCompletedOnboarding == true) {
                BackendAuthModal(
                    isBusy = isAuthBusy,
                    errorMessage = authError,
                    onLogin = { email, password -> viewModel.login(email, password) },
                    onRegister = { name, email, password -> viewModel.register(name, email, password) },
                    onDismiss = { viewModel.dismissAuthModal() }
                )
            }

            AnimatedVisibility(
                visible = hasCompletedOnboarding == false,
                enter = fadeIn(tween(260)),
                exit = fadeOut(tween(340)),
                modifier = Modifier.fillMaxSize().zIndex(15f)
            ) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        viewModel.completeOnboarding()
                    }
                )
            }

            AnimatedVisibility(
                visible = showOpeningSplash,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(480)),
                modifier = Modifier.fillMaxSize().zIndex(20f)
            ) {
                OpeningSplashOverlay(
                    onFinished = { showOpeningSplash = false }
                )
            }
        }
            }
        }
    }
}
