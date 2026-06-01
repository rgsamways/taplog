package ca.taplog.app.data

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "inspector_profile")

data class InspectorClaims(
    val inspectorId: String,
    val name: String,
    val email: String,
    val certNumber: String,
    val organisationId: String
)

class InspectorPreferences(private val context: Context) {

    companion object {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val INSPECTOR_ID = stringPreferencesKey("inspector_id")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val refreshToken: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val inspectorId: Flow<String?> = context.dataStore.data.map { it[INSPECTOR_ID] }

    suspend fun getAuthToken(): String? = authToken.first()
    suspend fun getRefreshToken(): String? = refreshToken.first()

    suspend fun storeAuthResult(token: String, refreshToken: String, inspectorId: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[INSPECTOR_ID] = inspectorId
        }
    }

    suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(INSPECTOR_ID)
            // DEVICE_ID intentionally kept — permanent per device
        }
    }

    suspend fun ensureDeviceId(): String {
        val existing = context.dataStore.data.map { it[DEVICE_ID] }.first()
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs -> prefs[DEVICE_ID] = newId }
        return newId
    }

    fun decodeJwtClaims(token: String?): InspectorClaims? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val paddedPayload = parts[1].let {
                val pad = it.length % 4
                if (pad == 0) it else it + "=".repeat(4 - pad)
            }
            val payload = String(Base64.decode(paddedPayload, Base64.URL_SAFE))
            val json = JSONObject(payload)
            InspectorClaims(
                inspectorId = json.optString("inspectorId"),
                name = json.optString("name"),
                email = json.optString("email"),
                certNumber = json.optString("certNumber"),
                organisationId = json.optString("organisationId")
            )
        } catch (e: Exception) {
            null
        }
    }
}
