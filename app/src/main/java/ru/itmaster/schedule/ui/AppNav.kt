package ru.itmaster.schedule.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.itmaster.schedule.ScheduleRepository
import ru.itmaster.schedule.notifications.RefreshAlarmsWorker
import ru.itmaster.schedule.notifications.ServerNotificationsWorker
import ru.itmaster.schedule.ui.screens.LoginRoute
import ru.itmaster.schedule.ui.screens.OnboardingRoute
import ru.itmaster.schedule.ui.screens.ProfileRoute
import ru.itmaster.schedule.ui.screens.ScheduleRoute
import ru.itmaster.schedule.ui.screens.SettingsRoute
import ru.itmaster.schedule.ui.screens.TestsRoute

private const val RouteOnboarding = "onboarding"
private const val RouteLogin = "login"
private const val RouteMain = "main"

@Composable
fun AppNav(
    repository: ScheduleRepository,
    modifier: Modifier = Modifier,
    openTestIdFromNotification: Long? = null,
    onOpenTestNotificationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current.applicationContext
    var start by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val prefs = repository.getAppPreferences()
        val session = repository.getSession()
        start = when {
            !prefs.onboardingDone -> RouteOnboarding
            !session.isLoggedIn -> RouteLogin
            else -> RouteMain
        }
    }

    if (start == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    NavHost(
        navController = navController,
        startDestination = start!!,
        modifier = modifier,
    ) {
        composable(RouteOnboarding) {
            OnboardingRoute(
                repository = repository,
                onFinished = {
                    navController.navigate(RouteLogin) {
                        popUpTo(RouteOnboarding) { inclusive = true }
                    }
                },
            )
        }
        composable(RouteLogin) {
            LoginRoute(
                repository = repository,
                onLoggedIn = {
                    RefreshAlarmsWorker.enqueue(context)
                    ServerNotificationsWorker.enqueuePeriodic(context)
                    ServerNotificationsWorker.enqueueOnce(context)
                    navController.navigate(RouteMain) {
                        popUpTo(RouteLogin) { inclusive = true }
                    }
                },
            )
        }
        composable(RouteMain) {
            MainShell(
                repository = repository,
                onRequireLogin = {
                    navController.navigate(RouteLogin) {
                        popUpTo(RouteMain) { inclusive = true }
                    }
                },
                openTestIdFromNotification = openTestIdFromNotification,
                onOpenTestNotificationHandled = onOpenTestNotificationHandled,
            )
        }
    }
}

@Composable
private fun MainShell(
    repository: ScheduleRepository,
    onRequireLogin: () -> Unit,
    openTestIdFromNotification: Long?,
    onOpenTestNotificationHandled: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var canAccessSchedule by remember { mutableStateOf(true) }
    var canAccessTests by remember { mutableStateOf(true) }
    var canAccessTestStats by remember { mutableStateOf(false) }
    var externalOpenTestId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        repository.loadMe().fold(
            onSuccess = { u ->
                canAccessSchedule = u.permissions?.scheduleMy != false
                canAccessTests = u.permissions?.studentTests != false
                canAccessTestStats = u.permissions?.testsStats == true ||
                    u.permissions?.testsAdmin == true
            },
            onFailure = {
                canAccessSchedule = true
                canAccessTests = true
                canAccessTestStats = false
            },
        )
    }

    LaunchedEffect(Unit) {
        var first = true
        repository.sessionFlow.collect { s ->
            if (first) {
                first = false
                return@collect
            }
            if (!s.isLoggedIn) {
                onRequireLogin()
            }
        }
    }

    LaunchedEffect(openTestIdFromNotification, canAccessTests) {
        val id = openTestIdFromNotification ?: return@LaunchedEffect
        if (id <= 0L || !canAccessTests) return@LaunchedEffect
        tab = 1
        externalOpenTestId = id
        onOpenTestNotificationHandled()
    }

    LaunchedEffect(canAccessSchedule, canAccessTests, canAccessTestStats) {
        if (!canAccessSchedule && (canAccessTests || canAccessTestStats)) {
            tab = 1
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text("Расписание") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        Icon(
                            if (canAccessTestStats && !canAccessTests) {
                                Icons.Filled.BarChart
                            } else {
                                Icons.AutoMirrored.Filled.ViewList
                            },
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            if (canAccessTestStats && !canAccessTests) {
                                "Статистика"
                            } else {
                                "Тесты"
                            },
                        )
                    },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Профиль") },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("Настройки") },
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when (tab) {
                0 -> ScheduleRoute(
                    repository = repository,
                    canAccessSchedule = canAccessSchedule,
                )
                1 -> TestsRoute(
                    repository = repository,
                    canAccessTests = canAccessTests,
                    canAccessTestStats = canAccessTestStats,
                    externalOpenTestId = externalOpenTestId,
                    onExternalOpenHandled = { externalOpenTestId = 0L },
                )
                2 -> ProfileRoute(repository = repository)
                3 -> SettingsRoute(repository = repository)
            }
        }
    }
}
