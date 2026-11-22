package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alarm.AlarmNotificationHelper
import com.example.data.model.RoutineActivity
import com.example.data.model.ThemeMode
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.ActivityEditorDialog
import com.example.ui.components.AppSection
import com.example.ui.screens.DailyRoutineScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyingScreen
import com.example.ui.screens.WebDevelopmentScreen
import com.example.ui.screens.WeeklyCalendarScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.screens.YouTubeScreen
import com.example.ui.theme.DailyLifeTheme
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private var openActivityIdState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openActivityIdState = intent?.getStringExtra(AlarmNotificationHelper.EXTRA_OPEN_ACTIVITY_ID)

        setContent {
            val app = application as DailyLifeApplication
            val settings by app.container.settingsDataStore.settingsFlow
                .collectAsStateWithLifecycle(initialValue = com.example.data.model.AssistantSettings())

            DailyLifeTheme(themeMode = settings.themeMode) {
                AppRoot(
                    openActivityId = openActivityIdState,
                    onConsumeOpenActivityId = { openActivityIdState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openActivityIdState = intent.getStringExtra(AlarmNotificationHelper.EXTRA_OPEN_ACTIVITY_ID)
    }
}

@Composable
fun AppRoot(
    openActivityId: String?,
    onConsumeOpenActivityId: () -> Unit,
    auth: FirebaseAuth = Firebase.auth
) {
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = auth.currentUser })
    } else {
        key(user.uid) {
            AuthenticatedAssistantShell(
                currentUser = user,
                openActivityId = openActivityId,
                onConsumeOpenActivityId = onConsumeOpenActivityId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAssistantShell(
    currentUser: FirebaseUser,
    openActivityId: String?,
    onConsumeOpenActivityId: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: RoutineViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as DailyLifeApplication
                RoutineViewModel(
                    repository = app.container.routineRepository,
                    alarmScheduler = app.container.alarmScheduler,
                    soundPlayer = app.container.alarmSoundPlayer,
                    voiceManager = app.container.voiceAnnouncementManager,
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val dashboardUiState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val weeklyStats by viewModel.weeklyStatsState.collectAsStateWithLifecycle()
    val selectedCalendarDay by viewModel.selectedCalendarDay.collectAsStateWithLifecycle()
    val weekOffset by viewModel.selectedWeekOffset.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val navController = rememberNavController()
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: AppSection.HOME.route

    val navigateTo: (AppSection) -> Unit = { section ->
        navController.navigate(section.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    LaunchedEffect(openActivityId) {
        if (!openActivityId.isNullOrBlank()) {
            val targetSection = when {
                openActivityId == "sport" -> AppSection.WORKOUT
                openActivityId == "quran" -> AppSection.QURAN
                openActivityId.startsWith("study") -> AppSection.STUDYING
                openActivityId.startsWith("web") -> AppSection.WEB_DEV
                openActivityId == "youtube" -> AppSection.YOUTUBE
                openActivityId == "daily_check" -> AppSection.PROGRESS
                else -> AppSection.DAILY_ROUTINE
            }
            navigateTo(targetSection)
            onConsumeOpenActivityId()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    var editingActivity by remember { mutableStateOf<RoutineActivity?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var showAllSectionsSheet by remember { mutableStateOf(false) }

    val primaryNavItems = listOf(
        AppSection.HOME,
        AppSection.DAILY_ROUTINE,
        AppSection.WORKOUT,
        AppSection.PROGRESS,
        AppSection.SETTINGS
    )

    val activeTitle = AppSection.entries.firstOrNull { it.route == currentRoute }?.title
        ?: "My Daily Life Assistant"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    IconButton(
                        onClick = { showAllSectionsSheet = true },
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = "All 10 sections", tint = SkyAccent)
                    }
                    primaryNavItems.forEach { section ->
                        NavigationRailItem(
                            selected = currentRoute == section.route,
                            onClick = { navigateTo(section) },
                            icon = { Icon(section.icon, contentDescription = section.title) },
                            label = { Text(section.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showAllSectionsSheet = true },
                                modifier = Modifier.testTag("topbar_sections_menu")
                            ) {
                                Icon(Icons.Default.Apps, contentDescription = "Open all sections menu", tint = SkyAccent)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            primaryNavItems.forEach { section ->
                                NavigationBarItem(
                                    selected = currentRoute == section.route,
                                    onClick = { navigateTo(section) },
                                    icon = { Icon(section.icon, contentDescription = section.title) },
                                    label = { Text(section.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.testTag("bottom_nav_${section.route}")
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (val uiState = dashboardUiState) {
                        UiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = uiState.message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(onClick = { viewModel.rescheduleAllAlarms() }) {
                                    Text("Retry")
                                }
                            }
                        }

                        is UiState.Success<DashboardUiState> -> {
                            val state = uiState.data
                            NavHost(
                                navController = navController,
                                startDestination = AppSection.HOME.route
                            ) {
                                composable(AppSection.HOME.route) {
                                    HomeScreen(
                                        state = state,
                                        onNavigate = navigateTo,
                                        onToggleComplete = { viewModel.toggleCompletion(it) },
                                        onEditActivity = {
                                            editingActivity = it
                                            showEditorDialog = true
                                        },
                                        onPreviewVoice = { viewModel.previewActivityAlarm(it, state.settings) },
                                        onOpenAllSections = { showAllSectionsSheet = true }
                                    )
                                }
                                composable(AppSection.DAILY_ROUTINE.route) {
                                    DailyRoutineScreen(
                                        state = state,
                                        onAddActivity = {
                                            editingActivity = null
                                            showEditorDialog = true
                                        },
                                        onEditActivity = {
                                            editingActivity = it
                                            showEditorDialog = true
                                        },
                                        onDeleteActivity = { viewModel.deleteActivity(it.id) },
                                        onToggleComplete = { viewModel.toggleCompletion(it) },
                                        onPreviewVoice = { viewModel.previewActivityAlarm(it, state.settings) },
                                        onRestoreOriginal = { viewModel.restoreOriginalTimetable() }
                                    )
                                }
                                composable(AppSection.WEEKLY_CALENDAR.route) {
                                    WeeklyCalendarScreen(
                                        state = state,
                                        selectedDay = selectedCalendarDay,
                                        onSelectDay = { viewModel.selectCalendarDay(it) },
                                        onEditActivity = {
                                            editingActivity = it
                                            showEditorDialog = true
                                        },
                                        onPreviewVoice = { viewModel.previewActivityAlarm(it, state.settings) },
                                        onToggleComplete = { viewModel.toggleCompletion(it) }
                                    )
                                }
                                composable(AppSection.WORKOUT.route) {
                                    WorkoutScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        onUpdateExercise = { plan, ex -> viewModel.updateExercise(plan, ex) },
                                        onSaveWorkoutProgress = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) },
                                        onPreviewWorkoutVoice = { text -> viewModel.previewVoice(text, state.settings) }
                                    )
                                }
                                composable(AppSection.QURAN.route) {
                                    QuranScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        onSaveRecord = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) },
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onPreviewVoice = { text -> viewModel.previewVoice(text, state.settings) }
                                    )
                                }
                                composable(AppSection.STUDYING.route) {
                                    StudyingScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        onEditActivity = {
                                            editingActivity = it
                                            showEditorDialog = true
                                        },
                                        onSaveRecord = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) }
                                    )
                                }
                                composable(AppSection.WEB_DEV.route) {
                                    WebDevelopmentScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        onEditActivity = {
                                            editingActivity = it
                                            showEditorDialog = true
                                        },
                                        onSaveRecord = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) }
                                    )
                                }
                                composable(AppSection.YOUTUBE.route) {
                                    YouTubeScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        onSaveRecord = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) },
                                        onPreviewVoice = { text -> viewModel.previewVoice(text, state.settings) }
                                    )
                                }
                                composable(AppSection.PROGRESS.route) {
                                    ProgressScreen(
                                        state = state,
                                        weeklyStats = weeklyStats,
                                        weekOffset = weekOffset,
                                        onShiftWeek = { viewModel.shiftWeekOffset(it) },
                                        onSaveDailyCheck = { rec, msg -> viewModel.saveTrackerRecord(rec, msg) }
                                    )
                                }
                                composable(AppSection.SETTINGS.route) {
                                    SettingsScreen(
                                        settings = state.settings,
                                        availableVoices = availableVoices,
                                        canScheduleExactAlarms = viewModel.canScheduleExactAlarms(),
                                        currentUser = currentUser,
                                        syncError = syncError,
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onPreviewVoice = { text, s -> viewModel.previewVoice(text, s) },
                                        onPreviewSound = { key, vol -> viewModel.previewSound(key, vol) },
                                        onRestoreDefaults = { viewModel.restoreOriginalTimetable() },
                                        onClearHistory = { viewModel.clearHistory() },
                                        onExportJson = { viewModel.exportBackupJson() },
                                        onImportJson = { json -> viewModel.importBackupJson(json) },
                                        onSignOut = {
                                            signOut(credentialManager, scope)
                                        }
                                    )
                                }
                            }

                            if (showEditorDialog) {
                                ActivityEditorDialog(
                                    initialActivity = editingActivity,
                                    onDismiss = {
                                        viewModel.stopSoundPreview()
                                        showEditorDialog = false
                                    },
                                    onSave = { updated ->
                                        viewModel.stopSoundPreview()
                                        viewModel.saveActivity(updated)
                                        showEditorDialog = false
                                    },
                                    onPreviewSound = { soundKey ->
                                        viewModel.previewSound(soundKey, state.settings.alarmVolume)
                                    },
                                    onPreviewVoice = { text ->
                                        viewModel.previewVoice(text, state.settings)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAllSectionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllSectionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "All Assistant Sections",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.height(360.dp)
                ) {
                    items(AppSection.entries) { section ->
                        val selected = currentRoute == section.route
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAllSectionsSheet = false
                                    navigateTo(section)
                                }
                                .testTag("menu_section_${section.route}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = if (selected) SkyAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
