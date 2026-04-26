package ru.itmaster.schedule.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.itmaster.schedule.BuildConfig
import java.util.concurrent.TimeUnit

internal object ApiFactory {

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    fun create(baseUrlUserInput: String): ItMasterApi {
        val base = normalizeApiBaseUrl(baseUrlUserInput)
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        val clientBuilder = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(RedirectPolicyInterceptor())
            .addInterceptor(logging)

        if (BuildConfig.ALLOW_INSECURE_SSL) {
            Log.w(
                "ItMasterApi",
                "Включён обход проверки SSL (unsafeSsl в local.properties). Пароли можно перехватить. " +
                    "Выключите после исправления сертификата на сервере.",
            )
            clientBuilder.applyInsecureSslIfNeeded()
        }

        val client = clientBuilder.build()

        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ItMasterApi::class.java)
    }

    /**
     * Собирает origin с учётом переключателя HTTP/HTTPS (host может быть без схемы).
     * Пример: `axobeast.ru`, useHttp=true → `http://axobeast.ru`
     */
    fun resolveServerOrigin(hostOrUrlInput: String, useHttp: Boolean): String {
        var host = hostOrUrlInput.trim().trimEnd('/')
        if (host.isEmpty()) {
            throw IllegalArgumentException("Пустой адрес сервера")
        }
        host = when {
            host.startsWith("https://", ignoreCase = true) -> host.substring(8)
            host.startsWith("http://", ignoreCase = true) -> host.substring(7)
            else -> host
        }.trim().trimEnd('/')
        if (host.isEmpty()) {
            throw IllegalArgumentException("Укажите адрес сервера (домен или IP)")
        }
        val scheme = if (useHttp) "http" else "https"
        return "$scheme://$host"
    }

    /**
     * Origin уже с схемой ([http|https]://host…). Итог для Retrofit: {origin}/api/
     */
    fun normalizeApiBaseUrl(originWithScheme: String): String {
        var s = originWithScheme.trim().trimEnd('/')
        if (s.isEmpty()) {
            throw IllegalArgumentException("Пустой адрес сервера")
        }
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
            s = "https://$s"
        }
        if (!s.endsWith("/api", ignoreCase = true)) {
            s = "$s/api"
        }
        return "$s/"
    }
}
