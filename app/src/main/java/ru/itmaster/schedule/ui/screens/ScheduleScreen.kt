package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.itmaster.schedule.AppPreferences
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.data.api.ScheduleEntryDto
import ru.itmaster.schedule.data.api.ScheduleResponse
import ru.itmaster.schedule.notifications.LessonScheduler
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val ruDate: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoute(
    repository: ScheduleRepository,
) {
    val context = LocalContext.current.applicationContext
    val notifyPrefs by repository.appPreferencesFlow.collectAsState(
        initial = AppPreferences(
            onboardingDone = true,
            notifyBeforeMinutes = 15,
            notificationsEnabled = true,
        ),
    )
    var weekMonday by remember {
        mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }
    var schedule by remember { mutableStateOf<ScheduleResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(weekMonday) {
        loading = true
        error = null
        val sch = repository.loadSchedule(weekMonday.toString())
        sch.onSuccess { schedule = it }
        sch.onFailure { err ->
            error = err.message
            schedule = null
        }
        loading = false
    }

    LaunchedEffect(schedule, weekMonday, notifyPrefs.notifyBeforeMinutes, notifyPrefs.notificationsEnabled) {
        val s = schedule ?: return@LaunchedEffect
        if (weekMonday != LessonScheduler.currentWeekMonday()) return@LaunchedEffect
        LessonScheduler.updateFromSchedule(
            context,
            s,
            weekMonday,
            notifyPrefs.notifyBeforeMinutes,
            notifyPrefs.notificationsEnabled,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Расписание", style = MaterialTheme.typography.titleLarge)
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    weekMonday = weekMonday.minusDays(7)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущая неделя")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Неделя с ${weekMonday.format(ruDate)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    schedule?.group?.let { g ->
                        Text(
                            "Группа: $g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = {
                    weekMonday = weekMonday.plusDays(7)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующая неделя")
                }
            }

            when {
                loading -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.weight(1f))
                }

                error != null && schedule == null -> {
                    Spacer(Modifier.weight(1f))
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.weight(1f))
                }

                else -> {
                    val entries = schedule?.entries.orEmpty()
                    if (entries.isEmpty()) {
                        Spacer(Modifier.weight(1f))
                        Text(
                            "На эту неделю пар нет (или у вас не указана группа).",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Spacer(Modifier.weight(1f))
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(entries, key = { it.id }) { entry ->
                                ScheduleEntryCard(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEntryCard(entry: ScheduleEntryDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                entry.weekdayLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "${entry.startTime.take(5)} — ${entry.endTime.take(5)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            entry.subject?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            val meta = listOfNotNull(
                entry.teacher?.let { "Преподаватель: $it" },
                entry.room?.let { "Ауд. $it" },
                entry.buildingLabel?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
