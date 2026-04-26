package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.itmaster.schedule.BuildConfig
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.ui.theme.GradientPrimaryButton
import ru.itmaster.schedule.ui.theme.ItBackground
import ru.itmaster.schedule.ui.theme.LoginGradientBrush
import ru.itmaster.schedule.ui.theme.SectionCaption

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
            .background(ItBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.32f)
                .background(LoginGradientBrush)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "IT-Master",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Первый запуск",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.68f),
            shape = MaterialTheme.shapes.extraLarge,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text(
                    text = "API: ${BuildConfig.FIXED_API_ORIGIN.trimEnd('/')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                SectionCaption("Напоминание о паре")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "За сколько минут до начала показывать уведомление",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    notifyOptions.forEach { m ->
                        FilterChip(
                            selected = notifyMinutes == m,
                            onClick = { notifyMinutes = m },
                            label = { Text("$m мин") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                GradientPrimaryButton(
                    text = "Продолжить",
                    onClick = {
                        scope.launch {
                            repository.completeOnboarding(notifyBeforeMinutes = notifyMinutes)
                            onFinished()
                        }
                    },
                )
            }
        }
    }
}
