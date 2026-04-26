package ru.itmaster.schedule.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Без автоматического следования редиректам: иначе запрос по HTTP часто уходит на HTTPS,
 * и снова возникает ошибка сертификата, хотя пользователь выбрал HTTP.
 */
internal class RedirectPolicyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val code = response.code
        if (code !in REDIRECT_CODES) {
            return response
        }
        val location = response.header("Location").orEmpty()
        response.close()
        val message = if (request.url.scheme.equals("http", ignoreCase = true) &&
            location.startsWith("https:", ignoreCase = true)
        ) {
            "Сервер принудительно перенаправляет с HTTP на HTTPS ($location). " +
                "По факту соединение всё равно идёт по HTTPS — исправьте цепочку сертификата (fullchain) на хостинге. " +
                "Либо отключите редирект HTTP→HTTPS, если сайт должен работать по HTTP."
        } else {
            "Сервер вернул перенаправление $code на «$location». Укажите в приложении конечный адрес без редиректа или настройте сервер."
        }
        throw IOException(message)
    }

    private companion object {
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
