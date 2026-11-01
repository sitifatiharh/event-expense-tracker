package com.example.notacara.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_tour_prefs")

class UserTourManager(private val context: Context) {

    companion object {
        val IS_FIRST_TIME_USER = booleanPreferencesKey("is_first_time_user")
        val IS_FIRST_TIME_SCAN = booleanPreferencesKey("is_first_time_scan")
        val IS_FIRST_TIME_EDIT = booleanPreferencesKey("is_first_time_edit")
    }

    val isFirstTimeUser: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_TIME_USER] ?: true
    }

    val isFirstTimeScan: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_TIME_SCAN] ?: true
    }

    val isFirstTimeEdit: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_FIRST_TIME_EDIT] ?: true
    }

    suspend fun setFirstTimeUser(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_USER] = isFirstTime
        }
    }

    suspend fun setFirstTimeScan(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_SCAN] = isFirstTime
        }
    }

    suspend fun setFirstTimeEdit(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_EDIT] = isFirstTime
        }
    }
}
