package ca.taplog.app.ui.auth

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.taplog.app.data.AuthApiService
import ca.taplog.app.data.InspectorPreferences
import ca.taplog.app.data.LoginRequest
import ca.taplog.app.data.RefreshRequest
import ca.taplog.app.data.RegisterDeviceRequest
import ca.taplog.app.data.RegisterRequest
import ca.taplog.app.data.ResendCodeRequest
import ca.taplog.app.data.VerifyEmailRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authApiService: AuthApiService,
    private val inspectorPreferences: InspectorPreferences
) : ViewModel() {

    sealed class AuthState {
        object Login : AuthState()
        object Registering : AuthState()
        data class VerifyingEmail(val email: String) : AuthState()
        object LoggingIn : AuthState()
        data class NewDevice(val email: String) : AuthState()
        object Authenticated : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Login)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun showRegistration() {
        _errorMessage.value = null
        _authState.value = AuthState.Registering
    }

    fun showLogin() {
        _errorMessage.value = null
        _authState.value = AuthState.Login
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun register(name: String, email: String, certNumber: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = authApiService.register(
                    RegisterRequest(name = name, email = email, certNumber = certNumber, password = password)
                )
                if (response.isSuccessful) {
                    _authState.value = AuthState.VerifyingEmail(email)
                } else {
                    val detail = response.errorBody()?.string() ?: ""
                    _errorMessage.value = when {
                        detail.contains("cert", ignoreCase = true) -> "Certificate number already registered"
                        detail.contains("email", ignoreCase = true) -> "Email already registered"
                        else -> "Registration failed. Please try again."
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error. Check your network."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val deviceId = inspectorPreferences.ensureDeviceId()
                val response = authApiService.verifyEmail(
                    VerifyEmailRequest(
                        email = email,
                        code = code,
                        deviceId = deviceId,
                        deviceName = Build.MODEL
                    )
                )
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    inspectorPreferences.storeAuthResult(auth.accessToken, auth.refreshToken, auth.inspectorId)
                    _authState.value = AuthState.Authenticated
                } else {
                    _errorMessage.value = "Invalid or expired code — tap Resend to get a new one"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error. Check your network."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _authState.value = AuthState.LoggingIn
            try {
                val deviceId = inspectorPreferences.ensureDeviceId()
                val response = authApiService.login(
                    LoginRequest(
                        email = email,
                        password = password,
                        deviceId = deviceId,
                        deviceName = Build.MODEL
                    )
                )
                when (response.code()) {
                    200 -> {
                        val auth = response.body()!!
                        inspectorPreferences.storeAuthResult(auth.accessToken, auth.refreshToken, auth.inspectorId)
                        _authState.value = AuthState.Authenticated
                    }
                    403 -> {
                        val detail = response.errorBody()?.string() ?: ""
                        if (detail.contains("NEW_DEVICE")) {
                            _authState.value = AuthState.NewDevice(email)
                        } else {
                            _errorMessage.value = "Account not yet verified. Check your email."
                            _authState.value = AuthState.Login
                        }
                    }
                    401 -> {
                        _errorMessage.value = "Incorrect email or password"
                        _authState.value = AuthState.Login
                    }
                    else -> {
                        _errorMessage.value = "Login failed. Please try again."
                        _authState.value = AuthState.Login
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error. Check your network."
                _authState.value = AuthState.Login
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registerDevice(email: String, code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val deviceId = inspectorPreferences.ensureDeviceId()
                val response = authApiService.registerDevice(
                    RegisterDeviceRequest(
                        email = email,
                        code = code,
                        deviceId = deviceId,
                        deviceName = Build.MODEL
                    )
                )
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    inspectorPreferences.storeAuthResult(auth.accessToken, auth.refreshToken, auth.inspectorId)
                    _authState.value = AuthState.Authenticated
                } else {
                    _errorMessage.value = "Invalid or expired code"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Connection error. Check your network."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendCode(email: String, purpose: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                authApiService.resendCode(ResendCodeRequest(email = email, purpose = purpose))
            } catch (_: Exception) { /* silent — user can retry */ }
        }
    }

    class Factory(
        private val authApiService: AuthApiService,
        private val inspectorPreferences: InspectorPreferences
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authApiService, inspectorPreferences) as T
        }
    }
}
