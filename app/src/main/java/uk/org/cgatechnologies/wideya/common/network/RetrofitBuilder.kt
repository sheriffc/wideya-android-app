package uk.org.cgatechnologies.wideya.common.network

import com.google.gson.GsonBuilder
import com.haroldadmin.cnradapter.NetworkResponseAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.org.cgatechnologies.wideya.BuildConfig
import java.util.concurrent.TimeUnit

object RetrofitBuilder {
    fun getRetrofit(baseUrl: String): Retrofit {

        var networkLoggingEnabled = false

        val gson = GsonBuilder()
//                .setLenient()
            .create()

        when (BuildConfig.BUILD_TYPE) {
            "load", "debug", "dev", "local" -> {
                networkLoggingEnabled = true
            }
            "release", "pilot", "demo" -> {
                networkLoggingEnabled = false
            }
            else -> {
                networkLoggingEnabled = false
            }
        }

        if (networkLoggingEnabled) {

            val httpLoggingInterceptor = HttpLoggingInterceptor()
                .setLevel(HttpLoggingInterceptor.Level.BODY)

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(httpLoggingInterceptor) //for debug
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()

        } else {

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(NetworkResponseAdapterFactory())
                .build()
        }
    }
}