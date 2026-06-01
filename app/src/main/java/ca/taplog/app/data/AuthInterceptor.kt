package ca.taplog.app.data

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthInterceptor(
    private val prefs: InspectorPreferences,
    private val baseUrl: String
) : Interceptor {

    private val refreshService: AuthApiService by lazy {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AuthApiService::class.java)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { prefs.getAuthToken() }

        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        if (response.code == 401) {
            response.close()
            val refreshToken = runBlocking { prefs.getRefreshToken() }
            if (!refreshToken.isNullOrBlank()) {
                try {
                    val refreshResponse = runBlocking {
                        refreshService.refresh(RefreshRequest(refreshToken))
                    }
                    if (refreshResponse.isSuccessful) {
                        val auth = refreshResponse.body()!!
                        runBlocking {
                            prefs.storeAuthResult(auth.accessToken, auth.refreshToken, auth.inspectorId)
                        }
                        val retryRequest = chain.request().newBuilder()
                            .header("Authorization", "Bearer ${auth.accessToken}")
                            .build()
                        return chain.proceed(retryRequest)
                    }
                } catch (_: Exception) { /* fall through */ }
            }
            runBlocking { prefs.clearAuth() }
            // Return a synthetic 401 so callers know auth was cleared
            return chain.proceed(chain.request())
        }

        return response
    }
}
