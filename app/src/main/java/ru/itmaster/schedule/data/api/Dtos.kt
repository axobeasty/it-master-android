package ru.itmaster.schedule.data.api

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val login: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: UserDto,
)

data class MeResponse(
    val user: UserDto,
)

data class UserPermissionsDto(
    @SerializedName("schedule_my") val scheduleMy: Boolean? = null,
    @SerializedName("student_tests") val studentTests: Boolean? = null,
    @SerializedName("tests_admin") val testsAdmin: Boolean? = null,
    @SerializedName("tests_stats") val testsStats: Boolean? = null,
)

data class UserDto(
    val id: Long,
    val login: String,
    val fio: String,
    val email: String?,
    val active: Boolean? = null,
    val room: String? = null,
    val phone: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    val citizenship: String? = null,
    val course: String? = null,
    @SerializedName("record_book_number") val recordBookNumber: String? = null,
    @SerializedName("enrollment_year") val enrollmentYear: String? = null,
    @SerializedName("faculty_note") val facultyNote: String? = null,
    @SerializedName("department_note") val departmentNote: String? = null,
    @SerializedName("group_id") val groupId: Long? = null,
    @SerializedName("group_name") val groupName: String? = null,
    @SerializedName("role_id") val roleId: Long? = null,
    @SerializedName("role_name") val roleName: String? = null,
    @SerializedName("department_id") val departmentId: Long? = null,
    @SerializedName("department_title") val departmentTitle: String? = null,
    @SerializedName("faculty_id") val facultyId: Long? = null,
    @SerializedName("faculty_name") val facultyName: String? = null,
    @SerializedName("chair_id") val chairId: Long? = null,
    @SerializedName("chair_name") val chairName: String? = null,
    @SerializedName("email_notifications") val emailNotifications: Boolean? = null,
    val permissions: UserPermissionsDto? = null,
)

data class ScheduleResponse(
    @SerializedName("week_start") val weekStart: String,
    val group: String?,
    val entries: List<ScheduleEntryDto>,
)

data class ScheduleEntryDto(
    val id: Long,
    val weekday: Int,
    @SerializedName("weekday_label") val weekdayLabel: String,
    @SerializedName("lesson_slot") val lessonSlot: Int?,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    val subject: String?,
    val teacher: String?,
    val room: String?,
    val building: String?,
    @SerializedName("building_label") val buildingLabel: String?,
)

data class MessageResponse(
    val message: String?,
)

data class NotificationsResponse(
    val items: List<NotificationItemDto> = emptyList(),
    @SerializedName("max_id") val maxId: Long = 0,
)

data class NotificationItemDto(
    val id: Long,
    val title: String,
    val message: String,
    @SerializedName("is_read") val isRead: Boolean? = null,
    @SerializedName("created_at") val createdAt: String? = null,
)

data class TestsListResponse(
    val tests: List<TestListItemDto> = emptyList(),
)

data class TestListItemDto(
    val id: Long,
    val title: String,
    val description: String? = null,
    @SerializedName("time_limit_minutes") val timeLimitMinutes: Int? = null,
    @SerializedName("attempts_limit") val attemptsLimit: Int = 0,
    @SerializedName("attempts_used") val attemptsUsed: Int = 0,
    @SerializedName("can_start") val canStart: Boolean = true,
    @SerializedName("questions_count") val questionsCount: Int = 0,
    @SerializedName("last_attempt") val lastAttempt: TestLastAttemptDto? = null,
)

data class TestLastAttemptDto(
    val id: Long,
    val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    val percentage: Double,
    val grade: String,
    @SerializedName("grade_label") val gradeLabel: String,
    @SerializedName("submitted_at") val submittedAt: String? = null,
)

data class TestBeginResponse(
    val test: TestDetailDto,
    @SerializedName("started_at") val startedAt: String,
)

data class TestDetailDto(
    val id: Long,
    val title: String,
    val description: String? = null,
    @SerializedName("time_limit_minutes") val timeLimitMinutes: Int? = null,
    @SerializedName("attempts_limit") val attemptsLimit: Int = 0,
    val questions: List<TestQuestionDto> = emptyList(),
)

data class TestQuestionDto(
    val id: Long,
    val type: String,
    @SerializedName("question_text") val questionText: String,
    val points: Int,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    val options: List<String>? = null,
    val left: List<String>? = null,
    val right: List<String>? = null,
)

data class SubmitTestResponse(
    val message: String? = null,
    val attempt: SubmitTestAttemptDto? = null,
)

data class SubmitTestAttemptDto(
    val id: Long,
    val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    val percentage: Double,
    val grade: String,
    @SerializedName("grade_label") val gradeLabel: String,
    @SerializedName("submitted_at") val submittedAt: String? = null,
)

data class TestStatsFilterDto(
    @SerializedName("group_id") val groupId: Long,
    val label: String,
)

data class TestStatsGroupOptionDto(
    val id: Long,
    val name: String,
)

data class GroupTestStatsSummaryDto(
    val count: Int,
    val avg: Double,
    val min: Double,
    val max: Double,
)

data class TestStatsAttemptsPageDto(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int,
    @SerializedName("last_page") val lastPage: Int,
    val data: List<TestStatsAttemptRowDto> = emptyList(),
)

data class TestStatsAttemptRowDto(
    val id: Long,
    @SerializedName("student_fio") val studentFio: String?,
    @SerializedName("group_name") val groupName: String?,
    @SerializedName("test_title") val testTitle: String?,
    val score: Int,
    @SerializedName("max_score") val maxScore: Int,
    val percentage: Double,
    val grade: String,
    @SerializedName("grade_label") val gradeLabel: String,
    @SerializedName("submitted_at") val submittedAt: String? = null,
)

data class TestStatsResponse(
    val filter: TestStatsFilterDto,
    val groups: List<TestStatsGroupOptionDto> = emptyList(),
    @SerializedName("stats_by_group") val statsByGroup: Map<String, GroupTestStatsSummaryDto> = emptyMap(),
    val attempts: TestStatsAttemptsPageDto,
)
