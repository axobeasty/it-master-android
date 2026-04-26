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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingRoute(
    repository: ScheduleRepository,
    onFinished: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val notifyOptions = remember { listOf(5, 10, 15, 30, 45) }
    var notifyMinutes by remember { mutableIntStateOf(15) }

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
            text = "Адрес сервера API задан в приложении: ${BuildConfig.FIXED_API_ORIGIN.trimEnd('/')}. Ниже — только напоминания о парах.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

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

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    repository.completeOnboarding(notifyBeforeMinutes = notifyMinutes)
                    onFinished()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Продолжить")
        }
    }
}
