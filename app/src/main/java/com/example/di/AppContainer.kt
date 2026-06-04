package com.example.di

import android.util.Log
import com.example.BuildConfig
import com.example.data.remote.MovieApi
import com.example.data.repository.MovieRepositoryImpl
import com.example.domain.repository.MovieRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val movieRepository: MovieRepository
    val isApiKeyValid: Boolean
    val tmdbApiKey: String
}

class AppContainerImpl : AppContainer {

    companion object {
        private const val BASE_URL = "https://api.themoviedb.org/3/"
        private const val TAG = "AppContainer"
    }

    override val tmdbApiKey: String by lazy {
        // Safe check for build configuration keys
        val key = try {
            BuildConfig.TMDB_API_KEY
        } catch (_: Throwable) {
            ""
        }
        if (key == "MY_TMDB_API_KEY" || key.isBlank()) "" else key
    }

    override val isApiKeyValid: Boolean
        get() = tmdbApiKey.isNotBlank()

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val apiInterceptor by lazy {
        Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val url = originalUrl.newBuilder()
                .addQueryParameter("api_key", tmdbApiKey)
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .build()

            chain.proceed(request)
        }
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor { message ->
            Log.d("TMDB_HTTP", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(apiInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    private val movieApi: MovieApi by lazy {
        retrofit.create(MovieApi::class.java)
    }

    override val movieRepository: MovieRepository by lazy {
        MovieRepositoryImpl(movieApi)
    }
}
