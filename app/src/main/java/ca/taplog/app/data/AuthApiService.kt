package ca.taplog.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// ── Request models ───────────────────────────────────────────────────────────

data class RegisterRequest(
    val name: String,
    val email: String,
    val certNumber: String,
    val password: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String,
    val deviceId: String,
    val deviceName: String
)

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceName: String
)

data class RegisterDeviceRequest(
    val email: String,
    val code: String,
    val deviceId: String,
    val deviceName: String
)

data class RefreshRequest(val refreshToken: String)

data class ResendCodeRequest(val email: String, val purpose: String)

// ── Response models ──────────────────────────────────────────────────────────

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val inspectorId: String,
    val name: String,
    val email: String,
    val certNumber: String,
    val organisationId: String
)

data class MessageResponse(val message: String)

// ── Service ──────────────────────────────────────────────────────────────────

interface AuthApiService {

    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<MessageResponse>

    @POST("api/v1/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): Response<AuthResponse>

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/v1/auth/register-device")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): Response<AuthResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("api/v1/auth/resend-code")
    suspend fun resendCode(@Body body: ResendCodeRequest): Response<MessageResponse>
}
