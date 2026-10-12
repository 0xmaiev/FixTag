package com.example.fixtag.api

import android.content.Context
import android.util.Log
import com.example.fixtag.data.Database
import com.example.fixtag.data.model.Auth
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class AuthInterceptor(private val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // We use runBlocking because OkHttp interceptors run synchronously on a background thread.
        // This safely queries Room before completing the network payload execution.
        val auth = runBlocking {
            Database.getDatabase(context).getAuthDao().get()
        }

        val requestBuilder = originalRequest.newBuilder()

        // Automatically inject the refresh token as a header if it exists in Room
        if (auth != null) {
            requestBuilder.addHeader("Authorization", "Bearer ${auth.accessToken}")
        }

        return chain.proceed(requestBuilder.build())
    }
}

class TokenAuthenticator(private val apiService: ApiService): Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        TODO("Not yet implemented")
    }
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:3000"

    private var refreshApi: ApiService? = null

    fun getApiService(context: Context) : ApiService {

        // Build the Authenticator
        val tokenAuthenticator = object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                synchronized(this) {
                    // If we've already tried this request, and it still failed, stop to avoid loops
                    if (response.priorResponse != null) return null

                    // Synchronously get the old refresh token from Room
                    val db = Database.getDatabase(context)
                    val auth = runBlocking { db.getAuthDao().get() }
                    val currentRefreshToken = auth?.refreshToken ?: return null

                    // LAZY INITIALIZATION TRICK:
                    // If our refresh endpoint hasn't been built yet, initialize it right now.
                    // It will use a separate, basic OkHttpClient that doesn't have an Authenticator attached.
                    if (refreshApi == null) {
                        val basicClient = OkHttpClient.Builder()
                            .addInterceptor(AuthInterceptor(context))
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .build()
                        val basicRetrofit = Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(basicClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()

                        refreshApi = basicRetrofit.create(ApiService::class.java)
                    }

                    try {
                        // before forcing a refresh, check if another request already refreshed
                        
                        val sent = response.request.header("Authorization")
                            ?.removePrefix("Bearer ")
                        if (sent != auth.accessToken) {
                            return response.request.newBuilder()
                                    .header("Authorization", "Bearer ${auth.accessToken}")
                                    .build()
                        }


                        // Execute the synchronous refresh network call
                        val refreshResponse = runBlocking {
                            refreshApi!!.refresh(RefreshRequest(currentRefreshToken))
                        }

                        if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                            val newTokens = refreshResponse.body()!!
                            Log.d("TokenAuthenticator", "Successfully refreshed access token")

                            // Save the newly minted tokens into Room
                            runBlocking {
                                db.getAuthDao().save(
                                    Auth(
                                        userId = auth.userId,
                                        accessToken = newTokens.accessToken,
                                        refreshToken = newTokens.refreshToken
                                    )
                                )
                            }

                            // Retry the original failed request with the new token
                            return response.request.newBuilder()
                                .header("Authorization", "Bearer ${newTokens.accessToken}")
                                .build()
                        }

                    } catch (e: Exception) {
                        Log.e("TokenAuthenticator", e.toString())
                    }

                    // If refreshing the token failed (e.g. user session fully expired),
                    // return null to stop retrying. You can also trigger a logout broadcast here.
                    return null
                }
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(context))
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(
                    GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create()
                )
            )
            .build()
            .create(ApiService::class.java)
    }
}