package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.BuildConfig
import ru.itmaster.schedule.ScheduleRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(repository: ScheduleRepository) {
    val scope = rememberCoroutineScope()
    var notifyEnabled by remember { mutableStateOf(true) }
    var notifyMinutes by remember { mutableStateOf(15) }
    var loaded by remember { mutableStateOf(false) }
    val notifyOptions = remember { listOf(5, 10, 15, 30, 45) }

    LaunchedEffect(Unit) {
        notifyMinutes = repository.getNotifyBeforeMinutes()
        notifyEnabled = repository.areNotificationsEnabled()
        loaded = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        if (!loaded) {
            return@Column
        }

        Text("Уведомления о парах", style = MaterialTheme.typography.titleMedium)
        Text(
            "Напоминание приходит за выбранное число минут до начала пары по текущей неделе расписания (будильник в статус-баре). Нужно разрешение на уведомления (Android 13+).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "О новых тестах: приложение периодически запрашивает список назначенных тестов (и уведомления) — при возврате на экран и примерно каждые 15 минут в фоне.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column {
            Text("Включить напоминания", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = notifyEnabled,
                onCheckedChange = { v ->
                    notifyEnabled = v
                    scope.launch { repository.setNotificationsEnabled(v) }
                },
            )
        }
        Text("За сколько минут до начала", style = MaterialTheme.typography.bodySmall)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notifyOptions.forEach { m ->
                FilterChip(
                    selected = notifyMinutes == m,
                    onClick = {
                        notifyMinutes = m
                        scope.launch { repository.setNotifyBeforeMinutes(m) }
                    },
                    label = { Text("$m мин") },
                )
            }
        }

        Spacer(Modifier.padding(4.dp))
        Text("О приложении", style = MaterialTheme.typography.titleMedium)
        Text(
            "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
