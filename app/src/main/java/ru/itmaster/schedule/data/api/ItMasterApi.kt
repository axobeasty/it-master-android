package ru.itmaster.schedule.data.api

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ItMasterApi {
    @POST("mobile/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("mobile/me")
    suspend fun me(@Header("Authorization") authorization: String): MeResponse

    @GET("mobile/schedule")
    suspend fun schedule(
        @Header("Authorization") authorization: String,
        @Query("week") week: String?,
    ): ScheduleResponse

    @GET("mobile/notifications")
    suspend fun notifications(
        @Header("Authorization") authorization: String,
        @Query("since_id") sinceId: Long? = null,
        @Query("bootstrap") bootstrap: Int? = null,
    ): NotificationsResponse

    /** Статистика тестов (права tests_stats / tests_admin на сервере). */
    @GET("mobile/test-stats")
    suspend fun testStats(
        @Header("Authorization") authorization: String,
        @Query("group_id") groupId: Long? = null,
        @Query("page") page: Int? = null,
    ): TestStatsResponse

    /** Тот же контроллер, что и GET /api/mobile/tests (на сервере дублируется для совместимости). */
    @GET("mobile/student-tests")
    suspend fun tests(@Header("Authorization") authorization: String): TestsListResponse

    @POST("mobile/student-tests/{id}/session")
    suspend fun testBegin(
        @Header("Authorization") authorization: String,
        @Path("id") testId: Long,
    ): TestBeginResponse

    @POST("mobile/student-tests/{id}/submit")
    suspend fun testSubmit(
        @Header("Authorization") authorization: String,
        @Path("id") testId: Long,
        @Body body: JsonObject,
    ): SubmitTestResponse

    @POST("mobile/logout")
    suspend fun logout(@Header("Authorization") authorization: String): MessageResponse
}
