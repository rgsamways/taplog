package ca.taplog.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ca.taplog.app.data.AppDatabase
import ca.taplog.app.data.AuthInterceptor
import ca.taplog.app.data.EmberRepository
import ca.taplog.app.data.EmberVerticalConfig
import ca.taplog.app.data.InspectorPreferences
import ca.taplog.app.data.RetrofitClient
import ca.taplog.app.data.ReportRepository
import ca.taplog.app.data.SyncRepository
import ca.taplog.app.data.VerticalConfig
import ca.taplog.app.data.VerticalConfigEntity
import ca.taplog.app.data.VerticalRegistry
import ca.taplog.app.ui.auth.AuthViewModel
import ca.taplog.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TapLogApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private val _verticalRegistryReady = MutableStateFlow(false)
    val verticalRegistryReady: StateFlow<Boolean> = _verticalRegistryReady

    override fun onCreate() {
        super.onCreate()
        scheduleSyncIfNeeded(this)
        initVerticalRegistry()
    }

    private fun initVerticalRegistry() {
        appScope.launch {
            try {
                val response = syncApiService.getVerticals()
                if (response.isSuccessful) {
                    val configs = response.body() ?: emptyList()
                    configs.forEach { config ->
                        database.verticalConfigDao().upsert(
                            VerticalConfigEntity(
                                verticalCode = config.vertical.name,
                                configJson = gson.toJson(config)
                            )
                        )
                        VerticalRegistry.register(config)
                    }
                    if (VerticalRegistry.count() > 0) {
                        _verticalRegistryReady.value = true
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w("VerticalRegistry", "Fetch failed, trying cache: ${e.message}")
            }

            // Offline fallback: load from Room cache
            val cached = database.verticalConfigDao().getAll()
            if (cached.isNotEmpty()) {
                cached.forEach { entity ->
                    try {
                        val config = gson.fromJson(entity.configJson, VerticalConfig::class.java)
                        VerticalRegistry.register(config)
                    } catch (e: Exception) {
                        Log.e("VerticalRegistry", "Failed to deserialize ${entity.verticalCode}: ${e.message}")
                    }
                }
            }

            // Static fallback: use bundled Ember config if cache was empty or all failed
            if (VerticalRegistry.count() == 0) {
                VerticalRegistry.register(EmberVerticalConfig.build())
            }

            _verticalRegistryReady.value = true
        }
    }

    val database by lazy { AppDatabase.getDatabase(this) }

    val inspectorPreferences by lazy { InspectorPreferences(this) }

    private val authInterceptor by lazy {
        AuthInterceptor(inspectorPreferences, BuildConfig.BASE_URL)
    }

    private val syncApiService by lazy {
        RetrofitClient.createSyncApiService(authInterceptor)
    }

    val repository by lazy {
        EmberRepository(
            database = database,
            organisationDao = database.organisationDao(),
            siteDao = database.siteDao(),
            assetDao = database.assetDao(),
            inspectionDao = database.inspectionDao(),
            deficiencyDao = database.deficiencyDao(),
            scanEventDao = database.scanEventDao(),
            tagEventDao = database.tagEventDao()
        )
    }

    val syncRepository by lazy {
        SyncRepository(
            organisationDao = database.organisationDao(),
            siteDao = database.siteDao(),
            assetDao = database.assetDao(),
            inspectionDao = database.inspectionDao(),
            deficiencyDao = database.deficiencyDao(),
            scanEventDao = database.scanEventDao(),
            tagEventDao = database.tagEventDao(),
            apiService = syncApiService
        )
    }

    val reportRepository by lazy { ReportRepository(this) }

    val authViewModelFactory: ViewModelProvider.Factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(RetrofitClient.authApiService, inspectorPreferences) as T
            }
        }
    }
}
