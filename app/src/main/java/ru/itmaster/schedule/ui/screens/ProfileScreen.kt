package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.data.api.UserDto
import ru.itmaster.schedule.ui.theme.ItMasterCard
import ru.itmaster.schedule.ui.theme.SectionCaption

@Composable
fun ProfileRoute(
    repository: ScheduleRepository,
) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<UserDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        repository.loadMe().fold(
            onSuccess = { user = it },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            "Профиль",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        when {
            loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            error != null -> {
                Text(
                    error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            user != null -> {
                val rows = profileRows(user!!)
                ItMasterCard(modifier = Modifier.weight(1f), expandContent = true) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        items(rows, key = { it.first }) { (label, value) ->
                            Column(Modifier.padding(vertical = 10.dp)) {
                                SectionCaption(label)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    repository.logout()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

private fun profileRows(u: UserDto): List<Pair<String, String>> = buildList {
    add("ФИО" to u.fio)
    add("Логин" to u.login)
    u.email?.takeIf { it.isNotBlank() }?.let { add("Email" to it) }
    u.active?.let { add("Аккаунт активен" to if (it) "да" else "нет") }
    u.roleName?.takeIf { it.isNotBlank() }?.let { add("Роль" to it) }
    u.groupName?.takeIf { it.isNotBlank() }?.let { add("Группа" to it) }
    u.course?.takeIf { it.isNotBlank() }?.let { add("Курс" to it) }
    u.recordBookNumber?.takeIf { it.isNotBlank() }?.let { add("Зачётная книжка" to it) }
    u.enrollmentYear?.takeIf { it.isNotBlank() }?.let { add("Год поступления" to it) }
    u.phone?.takeIf { it.isNotBlank() }?.let { add("Телефон" to it) }
    u.room?.takeIf { it.isNotBlank() }?.let { add("Кабинет" to it) }
    u.birthDate?.takeIf { it.isNotBlank() }?.let { add("Дата рождения" to it) }
    u.citizenship?.takeIf { it.isNotBlank() }?.let { add("Гражданство" to it) }
    u.facultyNote?.takeIf { it.isNotBlank() }?.let { add("Факультет (заметка)" to it) }
    u.facultyName?.takeIf { it.isNotBlank() }?.let { add("Факультет" to it) }
    u.departmentTitle?.takeIf { it.isNotBlank() }?.let { add("Подразделение (справочник)" to it) }
    u.departmentNote?.takeIf { it.isNotBlank() }?.let { add("Подразделение (текст)" to it) }
    u.chairName?.takeIf { it.isNotBlank() }?.let { add("Кафедра" to it) }
    u.emailNotifications?.let { add("Email-уведомления" to if (it) "включены" else "выключены") }
    u.permissions?.let { p ->
        val sched = p.scheduleMy?.let { if (it) "да" else "нет" } ?: "—"
        val tst = p.studentTests?.let { if (it) "да" else "нет" } ?: "—"
        val stats = p.testsStats?.let { if (it) "да" else "нет" } ?: "—"
        val admin = p.testsAdmin?.let { if (it) "да" else "нет" } ?: "—"
        add("Доступ: расписание" to sched)
        add("Доступ: тесты" to tst)
        add("Доступ: статистика тестов" to stats)
        add("Доступ: админ тестов" to admin)
    }
}
