package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.MainActivity
import ru.itmaster.schedule.BuildConfig
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.ui.theme.ItMasterCard
import ru.itmaster.schedule.ui.theme.SectionCaption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(repository: ScheduleRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        if (!loaded) {
            return@Column
        }

        ItMasterCard {
            SectionCaption("Уведомления о парах")
            Spacer(Modifier.height(4.dp))
            Text(
                "Напоминание за выбранное число минут до начала пары. На Android 13+ нужно разрешение на уведомления.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "О новых тестах приложение опрашивает сервер на экране и в фоне (~каждые 15 мин).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text("Включить напоминания", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = notifyEnabled,
                onCheckedChange = { v ->
                    notifyEnabled = v
                    scope.launch { repository.setNotificationsEnabled(v) }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text("За сколько минут до начала", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
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
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        ItMasterCard {
            SectionCaption("О приложении")
            Spacer(Modifier.height(4.dp))
            Text(
                "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { (context as? MainActivity)?.checkForUpdatesManually() },
            ) {
                Text("Проверить обновления")
            }
        }
    }
}
