package ru.itmaster.schedule.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.data.api.TestDetailDto
import ru.itmaster.schedule.data.api.TestListItemDto
import ru.itmaster.schedule.data.api.TestQuestionDto

@Composable
fun TestsRoute(
    repository: ScheduleRepository,
    canAccessTests: Boolean,
    externalOpenTestId: Long = 0L,
    onExternalOpenHandled: () -> Unit = {},
) {
    var openTestId by remember { mutableLongStateOf(0L) }
    var listRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(externalOpenTestId) {
        if (externalOpenTestId > 0L) {
            openTestId = externalOpenTestId
            onExternalOpenHandled()
        }
    }

    if (!canAccessTests) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "У вас нет доступа к разделу тестирования.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    if (openTestId > 0L) {
        TestTakeRoute(
            testId = openTestId,
            repository = repository,
            onClose = {
                openTestId = 0L
                listRefresh++
            },
        )
    } else {
        TestsListRoute(
            repository = repository,
            refreshSignal = listRefresh,
            onOpenTest = { openTestId = it },
        )
    }
}

@Composable
private fun TestsListRoute(
    repository: ScheduleRepository,
    refreshSignal: Int,
    onOpenTest: (Long) -> Unit,
) {
    var items by remember { mutableStateOf<List<TestListItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshSignal, retryTick) {
        loading = true
        error = null
        repository.loadTestsList().fold(
            onSuccess = {
                items = it
                error = null
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Тестирование", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))

        when {
            loading -> {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.weight(1f))
            }

            error != null -> {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { retryTick++ }) { Text("Повторить") }
            }

            items.isEmpty() -> {
                Text(
                    "Нет доступных тестов для вашей группы.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { t ->
                        TestListCard(t, onOpen = { onOpenTest(t.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TestListCard(item: TestListItemDto, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val meta = buildList {
                add("${item.questionsCount} вопр.")
                item.timeLimitMinutes?.takeIf { it > 0 }?.let { add("лимит $it мин") }
                if (item.attemptsLimit > 0) {
                    add("попытки ${item.attemptsUsed}/${item.attemptsLimit}")
                } else {
                    add("попытки без лимита")
                }
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            item.lastAttempt?.let { a ->
                Text(
                    "Последняя попытка: ${a.score}/${a.maxScore} (${a.percentage}%), ${a.grade} (${a.gradeLabel})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpen,
                enabled = item.canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (item.canStart) "Начать" else "Лимит попыток")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestTakeRoute(
    testId: Long,
    repository: ScheduleRepository,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TestDetailDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resultText by remember { mutableStateOf<String?>(null) }

    val singleAnswers = remember { mutableStateMapOf<Long, Int>() }
    val multiAnswers = remember { mutableStateMapOf<Long, Set<Int>>() }
    val matchAnswers = remember { mutableStateMapOf<Long, Map<String, String>>() }
    val wordAnswers = remember { mutableStateMapOf<Long, String>() }

    var remainingSec by remember { mutableIntStateOf(0) }
    val finished = remember(testId) { AtomicBoolean(false) }

    LaunchedEffect(testId) {
        finished.set(false)
        loading = true
        error = null
        resultText = null
        singleAnswers.clear()
        multiAnswers.clear()
        matchAnswers.clear()
        wordAnswers.clear()
        repository.beginTestSession(testId).fold(
            onSuccess = { res ->
                detail = res.test
                val limitMin = res.test.timeLimitMinutes ?: 0
                remainingSec = if (limitMin > 0) limitMin * 60 else 0
            },
            onFailure = { error = it.message },
        )
        loading = false
    }

    LaunchedEffect(detail) {
        val testLocal = detail ?: return@LaunchedEffect
        val limitMin = testLocal.timeLimitMinutes ?: 0
        if (limitMin <= 0) return@LaunchedEffect
        var sec = limitMin * 60
        remainingSec = sec
        while (sec > 0) {
            delay(1000)
            if (finished.get()) return@LaunchedEffect
            sec -= 1
            remainingSec = sec
        }
        if (!finished.compareAndSet(false, true)) return@LaunchedEffect
        submitting = true
        val body = buildSubmitJson(
            testLocal,
            singleAnswers,
            multiAnswers,
            matchAnswers,
            wordAnswers,
            autoSubmitted = true,
        )
        repository.submitTest(testId, body).fold(
            onSuccess = { resultText = it.message + "\n" + formatAttempt(it.attempt) },
            onFailure = {
                resultText = it.message
                finished.set(false)
            },
        )
        submitting = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.title ?: "Тест") },
                navigationIcon = {
                    IconButton(onClick = onClose, enabled = !submitting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            when {
                loading -> {
                    Spacer(Modifier.weight(1f))
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.weight(1f))
                }

                error != null -> {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = onClose) { Text("Закрыть") }
                }

                resultText != null -> {
                    Text(resultText!!, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onClose) { Text("К списку тестов") }
                }

                detail != null -> {
                    val test = detail!!
                    if (remainingSec > 0 && (test.timeLimitMinutes ?: 0) > 0) {
                        val m = remainingSec / 60
                        val s = remainingSec % 60
                        Text(
                            "Осталось: ${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    test.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        test.questions.sortedBy { it.sortOrder }.forEachIndexed { idx, q ->
                            QuestionBlock(
                                index = idx + 1,
                                q = q,
                                singleSelected = singleAnswers[q.id],
                                onSingle = { singleAnswers[q.id] = it },
                                multiSelected = multiAnswers[q.id] ?: emptySet(),
                                onMultiToggle = { i ->
                                    val cur = multiAnswers[q.id] ?: emptySet()
                                    multiAnswers[q.id] = if (i in cur) cur - i else cur + i
                                },
                                matchSelected = matchAnswers[q.id] ?: emptyMap(),
                                onMatchChange = { left, right ->
                                    val cur = matchAnswers[q.id]?.toMutableMap() ?: mutableMapOf()
                                    cur[left] = right
                                    matchAnswers[q.id] = cur
                                },
                                word = wordAnswers[q.id] ?: "",
                                onWord = { wordAnswers[q.id] = it },
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (submitting || !finished.compareAndSet(false, true)) return@Button
                            submitting = true
                            scope.launch {
                                val body = buildSubmitJson(test, singleAnswers, multiAnswers, matchAnswers, wordAnswers, autoSubmitted = false)
                                repository.submitTest(testId, body).fold(
                                    onSuccess = { resultText = it.message + "\n" + formatAttempt(it.attempt) },
                                    onFailure = {
                                        error = it.message
                                        finished.set(false)
                                    },
                                )
                                submitting = false
                            }
                        },
                        enabled = !submitting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Отправить")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionBlock(
    index: Int,
    q: TestQuestionDto,
    singleSelected: Int?,
    onSingle: (Int) -> Unit,
    multiSelected: Set<Int>,
    onMultiToggle: (Int) -> Unit,
    matchSelected: Map<String, String>,
    onMatchChange: (String, String) -> Unit,
    word: String,
    onWord: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "$index. ${q.questionText} (${q.points} б.)",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            when (q.type) {
                "single" -> {
                    Column(Modifier.selectableGroup()) {
                        q.options?.forEachIndexed { idx, label ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = singleSelected == idx,
                                        onClick = { onSingle(idx) },
                                        role = Role.RadioButton,
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = singleSelected == idx, onClick = null)
                                Text(label, Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }

                "multiple" -> {
                    q.options?.forEachIndexed { idx, label ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = idx in multiSelected,
                                onCheckedChange = { onMultiToggle(idx) },
                            )
                            Text(label, Modifier.padding(start = 8.dp))
                        }
                    }
                }

                "match" -> {
                    val rights = q.right.orEmpty()
                    q.left?.forEach { left ->
                        MatchRow(
                            left = left,
                            rights = rights,
                            selected = matchSelected[left].orEmpty(),
                            onSelect = { onMatchChange(left, it) },
                        )
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = word,
                        onValueChange = onWord,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ответ") },
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchRow(
    left: String,
    rights: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(left, style = MaterialTheme.typography.bodyMedium)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            rights.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { r ->
                        FilterChip(
                            selected = selected == r,
                            onClick = { onSelect(r) },
                            label = { Text(r, maxLines = 2) },
                        )
                    }
                }
            }
        }
    }
}

private fun buildSubmitJson(
    test: TestDetailDto,
    single: Map<Long, Int>,
    multi: Map<Long, Set<Int>>,
    match: Map<Long, Map<String, String>>,
    word: Map<Long, String>,
    autoSubmitted: Boolean,
): JsonObject {
    val answersObj = JsonObject()
    for (q in test.questions) {
        when (q.type) {
            "single" -> {
                val idx = single[q.id] ?: continue
                answersObj.addProperty(q.id.toString(), idx)
            }

            "multiple" -> {
                val set = multi[q.id] ?: continue
                if (set.isEmpty()) continue
                val arr = JsonArray()
                set.sorted().forEach { arr.add(JsonPrimitive(it)) }
                answersObj.add(q.id.toString(), arr)
            }

            "match" -> {
                val pairs = match[q.id] ?: continue
                if (pairs.isEmpty()) continue
                val mo = JsonObject()
                pairs.forEach { (l, r) -> mo.addProperty(l, r) }
                answersObj.add(q.id.toString(), mo)
            }

            else -> {
                val w = word[q.id]?.trim().orEmpty()
                if (w.isEmpty()) continue
                answersObj.addProperty(q.id.toString(), w)
            }
        }
    }
    return JsonObject().apply {
        add("answers", answersObj)
        addProperty("auto_submitted", autoSubmitted)
    }
}

private fun formatAttempt(a: ru.itmaster.schedule.data.api.SubmitTestAttemptDto?): String {
    if (a == null) return ""
    return "Баллы: ${a.score}/${a.maxScore} (${a.percentage}%). Оценка: ${a.grade} (${a.gradeLabel})."
}
