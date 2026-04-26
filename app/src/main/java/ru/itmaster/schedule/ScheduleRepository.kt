package ru.itmaster.schedule

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import ru.itmaster.schedule.data.api.ApiFactory
import ru.itmaster.schedule.data.api.LoginRequest
import ru.itmaster.schedule.data.api.LoginResponse
import ru.itmaster.schedule.data.api.ScheduleResponse
import ru.itmaster.schedule.data.api.SubmitTestResponse
import ru.itmaster.schedule.data.api.TestBeginResponse
import ru.itmaster.schedule.data.api.TestListItemDto
import ru.itmaster.schedule.data.api.UserDto
import ru.itmaster.schedule.notifications.LessonScheduler
import ru.itmaster.schedule.notifications.RefreshAlarmsWorker
import ru.itmaster.schedule.notifications.SERVER_ASSIGN_TEST_TITLE
import ru.itmaster.schedule.notifications.SERVER_TEST_SUBMITTED_TITLE
import ru.itmaster.schedule.notifications.ServerNotificationsWorker
import ru.itmaster.schedule.notifications.showServerPushNotification
import java.io.IOException

class ScheduleRepository(context: Context) {

    private val appContext = context.applicationContext

    private val keyToken = stringPreferencesKey("token")
    private val keyOnboardingDone = booleanPreferencesKey("onboarding_done")
    private val keyNotifyMinutes = intPreferencesKey("notify_before_minutes")
    private val keyNotifyEnabled = booleanPreferencesKey("notifications_enabled")
    private val keyLastNotifId = longPreferencesKey("last_server_notification_id")
    private val keyNotifBootstrapped = booleanPreferencesKey("server_notifications_bootstrapped")
    /** Снимок id тестов из API: новые id по сравнению с прошлым опросом → локальное уведомление. */
    private val keyTestsListBootstrapped = booleanPreferencesKey("tests_list_poll_bootstrapped")
    private val keyKnownTestIds = stringPreferencesKey("known_assigned_test_ids")

    /** Origin API из сборки (`build.gradle.kts` → `FIXED_API_ORIGIN`). */
    fun apiOrigin(): String = BuildConfig.FIXED_API_ORIGIN.trim().trimEnd('/')

    val sessionFlow: Flow<StoredSession> = appContext.sessionDataStore.data.map { prefs ->
        StoredSession(
            baseUrl = apiOrigin(),
            token = prefs[keyToken],
        )
    }

    val appPreferencesFlow: Flow<AppPreferences> = appContext.sessionDataStore.data.map { prefs ->
        AppPreferences(
            onboardingDone = prefs[keyOnboardingDone] == true,
            notifyBeforeMinutes = prefs[keyNotifyMinutes] ?: DEFAULT_NOTIFY_MINUTES,
            notificationsEnabled = prefs[keyNotifyEnabled] != false,
        )
    }

    suspend fun getSession(): StoredSession = sessionFlow.first()

    suspend fun getAppPreferences(): AppPreferences = appPreferencesFlow.first()

    suspend fun getNotifyBeforeMinutes(): Int =
        appContext.sessionDataStore.data.first()[keyNotifyMinutes] ?: DEFAULT_NOTIFY_MINUTES

    suspend fun areNotificationsEnabled(): Boolean =
        appContext.sessionDataStore.data.first()[keyNotifyEnabled] != false

    /** Первый запуск: напоминания о парах. Адрес API задаётся в сборке приложения. */
    suspend fun completeOnboarding(notifyBeforeMinutes: Int) {
        appContext.sessionDataStore.edit {
            it[keyOnboardingDone] = true
            it[keyNotifyMinutes] = notifyBeforeMinutes.coerceIn(1, 120)
            it[keyNotifyEnabled] = true
        }
    }

    suspend fun setNotifyBeforeMinutes(minutes: Int) {
        appContext.sessionDataStore.edit {
            it[keyNotifyMinutes] = minutes.coerceIn(1, 120)
        }
        RefreshAlarmsWorker.enqueue(appContext)
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.sessionDataStore.edit { it[keyNotifyEnabled] = enabled }
        RefreshAlarmsWorker.enqueue(appContext)
    }

    suspend fun saveSession(token: String) {
        appContext.sessionDataStore.edit {
            it[keyToken] = token
            it.remove(keyTestsListBootstrapped)
            it.remove(keyKnownTestIds)
        }
    }

    suspend fun clearSession() {
        appContext.sessionDataStore.edit {
            it.remove(keyToken)
        }
    }

    suspend fun logout() {
        LessonScheduler.cancelAll(appContext)
        ServerNotificationsWorker.cancelAll(appContext)
        val token = getSession().token
        if (token != null) {
            runCatching {
                val api = ApiFactory.create(apiOrigin())
                api.logout("Bearer $token")
            }
        }
        appContext.sessionDataStore.edit {
            it.remove(keyToken)
            it.remove(keyLastNotifId)
            it.remove(keyNotifBootstrapped)
            it.remove(keyTestsListBootstrapped)
            it.remove(keyKnownTestIds)
        }
    }

    suspend fun login(login: String, password: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val api = ApiFactory.create(apiOrigin())
                val response = api.login(LoginRequest(login = login, password = password))
                saveSession(response.token)
                Result.success(response)
            } catch (e: HttpException) {
                val msg = e.response()?.errorBody()?.string()?.let { parseErrorMessage(it) }
                    ?: "Ошибка сервера (${e.code()})"
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            } catch (e: Exception) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    suspend fun loadSchedule(weekIsoDate: String?): Result<ScheduleResponse> =
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext Result.failure(Exception("Не авторизован"))
            try {
                val api = ApiFactory.create(apiOrigin())
                val data = api.schedule("Bearer $token", weekIsoDate)
                Result.success(data)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    appContext.sessionDataStore.edit { it.remove(keyToken) }
                }
                val msg = e.response()?.errorBody()?.string()?.let { parseErrorMessage(it) }
                    ?: "Ошибка (${e.code()})"
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    suspend fun loadMe(): Result<UserDto> =
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext Result.failure(Exception("Не авторизован"))
            try {
                val api = ApiFactory.create(apiOrigin())
                Result.success(api.me("Bearer $token").user)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    appContext.sessionDataStore.edit { it.remove(keyToken) }
                }
                val msg = e.response()?.errorBody()?.string()?.let { parseErrorMessage(it) }
                    ?: "Ошибка (${e.code()})"
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    suspend fun pollServerNotifications() {
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext
            try {
                val api = ApiFactory.create(apiOrigin())
                val auth = "Bearer $token"
                val snapshot = appContext.sessionDataStore.data.first()
                val bootstrapped = snapshot[keyNotifBootstrapped] == true
                val lastId = snapshot[keyLastNotifId] ?: 0L

                if (!bootstrapped) {
                    val res = api.notifications(authorization = auth, sinceId = null, bootstrap = 1)
                    appContext.sessionDataStore.edit {
                        it[keyNotifBootstrapped] = true
                        it[keyLastNotifId] = res.maxId
                    }
                    return@withContext
                }

                val res = api.notifications(authorization = auth, sinceId = lastId, bootstrap = null)
                if (res.items.isEmpty()) return@withContext

                var maxSeen = lastId
                for (item in res.items) {
                    maxSeen = maxOf(maxSeen, item.id)
                    if (item.title == SERVER_ASSIGN_TEST_TITLE || item.title == SERVER_TEST_SUBMITTED_TITLE) {
                        val nid = (2_000_000 + (item.id % 500_000L)).toInt()
                        showServerPushNotification(appContext, item.title, item.message, nid)
                    }
                }
                if (maxSeen > lastId) {
                    appContext.sessionDataStore.edit { it[keyLastNotifId] = maxSeen }
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Фоновая проверка: GET список назначенных тестов и сравнение с прошлым снимком id.
     * Не зависит от таблицы notifs на сервере. Первый успешный ответ только запоминает id без уведомлений.
     */
    suspend fun pollAssignedTestsSnapshot() {
        withContext(Dispatchers.IO) {
            if (getSession().token == null) return@withContext
            val tests = loadTestsList().getOrNull() ?: return@withContext
            val currentIds = tests.map { it.id }.toSet()
            val snap = appContext.sessionDataStore.data.first()
            val bootstrapped = snap[keyTestsListBootstrapped] == true
            val prevIds = snap[keyKnownTestIds]
                ?.split(',')
                ?.mapNotNull { it.trim().toLongOrNull() }
                ?.toSet()
                ?: emptySet()

            if (!bootstrapped) {
                appContext.sessionDataStore.edit {
                    it[keyTestsListBootstrapped] = true
                    it[keyKnownTestIds] = currentIds.sorted().joinToString(",")
                }
                return@withContext
            }

            val added = currentIds - prevIds
            if (added.isNotEmpty()) {
                for (id in added.sorted()) {
                    val t = tests.find { it.id == id }
                    val body = t?.title?.takeIf { it.isNotBlank() }
                        ?.let { title -> "«$title»" }
                        ?: "Вам доступен новый тест"
                    val nid = (3_000_000 + (id % 500_000L)).toInt()
                    showServerPushNotification(appContext, "Новый тест", body, nid, openTestId = id)
                }
            }

            appContext.sessionDataStore.edit {
                it[keyKnownTestIds] = currentIds.sorted().joinToString(",")
            }
        }
    }

    suspend fun loadTestsList(): Result<List<TestListItemDto>> =
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext Result.failure(Exception("Не авторизован"))
            try {
                val api = ApiFactory.create(apiOrigin())
                Result.success(api.tests("Bearer $token").tests)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    appContext.sessionDataStore.edit { it.remove(keyToken) }
                }
                val msg = httpExceptionMessage(
                    e,
                    notFoundDetail = "На сервере нет API тестов. Обновите бэкенд и выполните: php artisan route:clear",
                )
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    suspend fun beginTestSession(testId: Long): Result<TestBeginResponse> =
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext Result.failure(Exception("Не авторизован"))
            try {
                val api = ApiFactory.create(apiOrigin())
                Result.success(api.testBegin("Bearer $token", testId))
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    appContext.sessionDataStore.edit { it.remove(keyToken) }
                }
                val msg = httpExceptionMessage(
                    e,
                    notFoundDetail = "На сервере нет API тестов. Обновите бэкенд и выполните: php artisan route:clear",
                )
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    suspend fun submitTest(testId: Long, body: JsonObject): Result<SubmitTestResponse> =
        withContext(Dispatchers.IO) {
            val token = getSession().token ?: return@withContext Result.failure(Exception("Не авторизован"))
            try {
                val api = ApiFactory.create(apiOrigin())
                Result.success(api.testSubmit("Bearer $token", testId, body))
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    appContext.sessionDataStore.edit { it.remove(keyToken) }
                }
                val msg = httpExceptionMessage(
                    e,
                    notFoundDetail = "На сервере нет API тестов. Обновите бэкенд и выполните: php artisan route:clear",
                )
                Result.failure(Exception(msg))
            } catch (e: IOException) {
                Result.failure(Exception(networkFailureMessage(e)))
            }
        }

    private fun httpExceptionMessage(e: HttpException, notFoundDetail: String? = null): String {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        parseErrorMessage(raw)?.let { return it }
        if (e.code() == 404) {
            return notFoundDetail
                ?: "Не найдено (404). Проверьте адрес API или обновление сервера."
        }
        if (raw.isNotBlank() && !raw.trimStart().startsWith("{")) {
            return "Ошибка сервера (${e.code()}). Ответ не JSON — смотрите логи веб-сервера и Laravel."
        }
        return "Ошибка сервера (${e.code()}). На стороне Laravel см. storage/logs/laravel.log и выполните миграции."
    }

    private fun networkFailureMessage(e: Throwable): String {
        val text = buildString {
            var c: Throwable? = e
            val seen = HashSet<Throwable>()
            while (c != null && seen.add(c)) {
                append(' ')
                append(c.message ?: "")
                c = c.cause
            }
        }
        if (text.contains("Trust anchor", ignoreCase = true) ||
            text.contains("CertPathValidatorException", ignoreCase = true) ||
            text.contains("unable to find valid certification path", ignoreCase = true)
        ) {
            return "Проблема HTTPS: Android не принимает сертификат. Исправьте сертификат на сервере или пересоберите приложение с http:// в FIXED_API_ORIGIN (только debug/внутренняя сеть)."
        }
        return "Нет сети или сервер недоступен: ${e.message ?: e.javaClass.simpleName}"
    }

    private fun parseErrorMessage(json: String): String? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            org.json.JSONObject(trimmed).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val DEFAULT_NOTIFY_MINUTES = 15
    }
}

data class StoredSession(
    val baseUrl: String?,
    val token: String?,
) {
    val isLoggedIn: Boolean get() = !token.isNullOrBlank()
}

data class AppPreferences(
    val onboardingDone: Boolean,
    val notifyBeforeMinutes: Int,
    val notificationsEnabled: Boolean,
)
