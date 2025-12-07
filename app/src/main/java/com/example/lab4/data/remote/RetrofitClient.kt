package com.example.lab4.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    internal const val BASE_URL = "http://10.137.157.147:8080/"
    // Placeholder for the Gemini API base URL
    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // Separate OkHttp client for Gemini so we can inject the API key header.
    private val geminiOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                // Read GEMINI_API_KEY via reflection to avoid compile-time dependency on generated BuildConfig
                val key = try {
                    val cls = Class.forName("com.example.lab4.BuildConfig")
                    val field = cls.getDeclaredField("GEMINI_API_KEY")
                    field.isAccessible = true
                    (field.get(null) as? String) ?: ""
                } catch (e: Exception) {
                    ""
                }
                if (key.isNotBlank()) {
                    // Use Authorization: Bearer <KEY> when available
                    builder.addHeader("Authorization", "Bearer $key")
                }
                chain.proceed(builder.build())
            }
            .build()
    }

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val geminiInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(geminiOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // No-op initializer to mirror usage in Activities. Keeps backward compatibility.
    fun init(context: Context) {
        // Currently no initialization required; method present so callers can safely call it early.
    }

    fun <T> createService(serviceClass: Class<T>): T {
        return instance.create(serviceClass)
    }

    fun createGeminiService(): GeminiService {
        return geminiInstance.create(GeminiService::class.java)
    }

    val authService: AuthService by lazy {
        instance.create(AuthService::class.java)
    }
}
