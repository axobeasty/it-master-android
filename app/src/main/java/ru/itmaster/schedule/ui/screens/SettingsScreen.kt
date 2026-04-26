package ru.itmaster.schedule.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.BuildConfig
import ru.itmaster.schedule.ScheduleRepository

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(repository: ScheduleRepository) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var serverHost by remember { mutableStateOf("") }
    var useHttp by remember { mutableStateOf(false) }
    var notifyEnabled by remember { mutableStateOf(true) }
    var notifyMinutes by remember { mutableStateOf(15) }
    var loaded by remember { mutableStateOf(false) }
    val notifyOptions = remember { listOf(5, 10, 15, 30, 45) }

    LaunchedEffect(Unit) {
        val origin = repository.apiOrigin()
        useHttp = origin.startsWith("http://", ignoreCase = true)
        serverHost = origin.removePrefix("https://").removePrefix("http://").trimEnd('/')
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

        Text(
            "Сервер API",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "После смены адреса сессия сбрасывается — войдите снова.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = serverHost,
            onValueChange = { serverHost = it },
            label = { Text("Домен или IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Column {
            Text("HTTP вместо HTTPS", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = useHttp, onCheckedChange = { useHttp = it })
        }
        Button(
            onClick = {
                val h = serverHost.trim()
                if (h.isEmpty()) {
                    Toast.makeText(context, "Укажите адрес сервера", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    try {
                        repository.updateServerEndpoint(h, useHttp)
                        Toast.makeText(
                            context,
                            "Сервер сохранён. Выполните вход снова.",
                            Toast.LENGTH_LONG,
                        ).show()
                    } catch (e: IllegalArgumentException) {
                        Toast.makeText(context, e.message ?: "Ошибка", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Применить адрес сервера")
        }

        Spacer(Modifier.padding(4.dp))
        Text("Уведомления о парах", style = MaterialTheme.typography.titleMedium)
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
