package com.example

import android.app.Application
import android.content.Context
import com.example.alarm.AlarmNotificationHelper
import com.example.alarm.AlarmScheduler
import com.example.alarm.AlarmSoundPlayer
import com.example.alarm.VoiceAnnouncementManager
import com.example.data.local.RoutineDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.remote.FirestoreSyncRepository
import com.example.data.repository.RoutineRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private fun ensureFirebaseInitialized() {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            FirebaseApp.initializeApp(appContext)
        }
    }

    val database: RoutineDatabase by lazy { RoutineDatabase.getInstance(appContext) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(appContext) }
    val firebaseAuth: FirebaseAuth by lazy {
        ensureFirebaseInitialized()
        FirebaseAuth.getInstance()
    }
    val firestore: FirebaseFirestore by lazy {
        ensureFirebaseInitialized()
        FirebaseFirestore.getInstance(appContext.getString(R.string.firestore_database_id))
    }
    val firestoreSyncRepository: FirestoreSyncRepository by lazy {
        FirestoreSyncRepository(firestore, firebaseAuth)
    }
    val alarmScheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }
    val alarmSoundPlayer: AlarmSoundPlayer by lazy { AlarmSoundPlayer(appContext) }
    val voiceAnnouncementManager: VoiceAnnouncementManager by lazy {
        VoiceAnnouncementManager(appContext)
    }
    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(
            database = database,
            settingsDataStore = settingsDataStore,
            firestoreSync = firestoreSyncRepository,
            alarmScheduler = alarmScheduler,
            auth = firebaseAuth
        )
    }
}

class DailyLifeApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        container = AppContainer(this)
        AlarmNotificationHelper.ensureChannels(this)
        appScope.launch {
            container.routineRepository.ensureSeededLocally()
        }
    }
}
