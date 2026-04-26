package ru.itmaster.schedule.data.api

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Отключает проверку цепочки сертификата и имя хоста. Удобно только пока на сервере чинят fullchain.
 * Никогда не включайте это в release: пароль и токен можно перехватить (MITM).
 */
@SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
internal fun OkHttpClient.Builder.applyInsecureSslIfNeeded(): OkHttpClient.Builder {
    val trustAll = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        },
    )
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, trustAll, SecureRandom())
    val tm = trustAll[0] as X509TrustManager
    sslSocketFactory(ctx.socketFactory, tm)
    hostnameVerifier { _, _ -> true }
    return this
}
