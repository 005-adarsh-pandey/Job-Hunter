package com.example.jobhunter.data.api

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "job_hunter_prefs"
    private const val KEY_ENDPOINT_URL = "key_endpoint_url"

    // The real backend endpoint provided by user
    const val DEFAULT_ENDPOINT = "https://resume-analyser-1-j7wk.onrender.com/match-jobs"
    private const val BASE_URL = "https://resume-analyser-1-j7wk.onrender.com/"

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        // Render free tier instances may take 30-60 seconds on initial cold start
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: JobHunterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(JobHunterApiService::class.java)
    }

    fun getGson(): Gson = gson

    fun getEndpointUrl(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_ENDPOINT_URL, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    }

    fun setEndpointUrl(context: Context, url: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_ENDPOINT_URL, url.trim()).apply()
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
