package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.BuildConfig
import ru.itmaster.schedule.ScheduleRepository

private fun defaultServerHost(): String {
    val o = BuildConfig.FIXED_API_ORIGIN.trim().trimEnd('/')
    return o.removePrefix("https://").removePrefix("http://")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingRoute(
    repository: ScheduleRepository,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var serverHost by remember { mutableStateOf(defaultServerHost()) }
    var useHttp by remember {
        mutableStateOf(BuildConfig.FIXED_API_ORIGIN.trim().startsWith("http://", ignoreCase = true))
    }
    val notifyOptions = remember { listOf(5, 10, 15, 30, 45) }
    var notifyMinutes by remember { mutableIntStateOf(15) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "IT-Master",
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Укажите адрес сервера Laravel (тот же домен, что и в браузере). Приложение ходит в API по пути /api.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = serverHost,
            onValueChange = {
                serverHost = it
                error = null
            },
            label = { Text("Домен или IP сервера") },
            placeholder = { Text("example.edu") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                "Использовать HTTP вместо HTTPS",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Включайте только в локальной сети или при проблемах с сертификатом.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = useHttp,
                onCheckedChange = {
                    useHttp = it
                    error = null
                },
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Напоминание о паре до начала (минуты)",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notifyOptions.forEach { m ->
                FilterChip(
                    selected = notifyMinutes == m,
                    onClick = { notifyMinutes = m },
                    label = { Text("$m мин") },
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val host = serverHost.trim()
                if (host.isEmpty()) {
                    error = "Укажите адрес сервера"
                    return@Button
                }
                scope.launch {
                    try {
                        repository.completeOnboarding(
                            serverHostOrUrl = host,
                            useHttp = useHttp,
                            notifyBeforeMinutes = notifyMinutes,
                        )
                        onFinished()
                    } catch (e: IllegalArgumentException) {
                        error = e.message ?: "Некорректный адрес"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Продолжить")
        }
    }
}
