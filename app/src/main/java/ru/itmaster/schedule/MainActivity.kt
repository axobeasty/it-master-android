package ru.itmaster.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import ru.itmaster.schedule.ui.AppNav
import ru.itmaster.schedule.ui.theme.ItMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ItMasterApp
        setContent {
            ItMasterTheme {
                AppNav(repository = app.repository, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
