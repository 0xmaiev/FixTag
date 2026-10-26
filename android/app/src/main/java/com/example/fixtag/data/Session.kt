package com.example.fixtag.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// declared once at top level: two DataStores on the same file throw at runtime
val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")