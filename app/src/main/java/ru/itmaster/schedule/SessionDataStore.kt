package ru.itmaster.schedule

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.sessionDataStore by preferencesDataStore(name = "session")
