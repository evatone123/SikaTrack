package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "pocket_ledger_prefs")

data class UserPreferences(
    val defaultCurrency: String = "GH₵",
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val hideBalances: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val weeklySummaryEnabled: Boolean = false,
    val budgetAlertsEnabled: Boolean = true
)

class DataStoreManager(private val context: Context) {

    private object PreferencesKeys {
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_CODE = stringPreferencesKey("pin_code")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val WEEKLY_SUMMARY_ENABLED = booleanPreferencesKey("weekly_summary_enabled")
        val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                defaultCurrency = preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: "GH₵",
                themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
                hideBalances = preferences[PreferencesKeys.HIDE_BALANCES] ?: false,
                pinEnabled = preferences[PreferencesKeys.PIN_ENABLED] ?: false,
                pinCode = preferences[PreferencesKeys.PIN_CODE] ?: "",
                biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
                dailyReminderEnabled = preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: false,
                weeklySummaryEnabled = preferences[PreferencesKeys.WEEKLY_SUMMARY_ENABLED] ?: false,
                budgetAlertsEnabled = preferences[PreferencesKeys.BUDGET_ALERTS_ENABLED] ?: true
            )
        }

    suspend fun updateCurrency(currency: String) {
        try {
            context.dataStore.edit { it[PreferencesKeys.DEFAULT_CURRENCY] = currency }
        } catch (_: Exception) {}
    }

    suspend fun updateThemeMode(mode: String) {
        try {
            context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode }
        } catch (_: Exception) {}
    }

    suspend fun setHideBalances(hide: Boolean) {
        try {
            context.dataStore.edit { it[PreferencesKeys.HIDE_BALANCES] = hide }
        } catch (_: Exception) {}
    }

    suspend fun setPin(enabled: Boolean, code: String) {
        try {
            context.dataStore.edit {
                it[PreferencesKeys.PIN_ENABLED] = enabled
                it[PreferencesKeys.PIN_CODE] = code
            }
        } catch (_: Exception) {}
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_ENABLED] = enabled }
        } catch (_: Exception) {}
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        try {
            context.dataStore.edit { it[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled }
        } catch (_: Exception) {}
    }

    suspend fun setWeeklySummary(enabled: Boolean) {
        try {
            context.dataStore.edit { it[PreferencesKeys.WEEKLY_SUMMARY_ENABLED] = enabled }
        } catch (_: Exception) {}
    }

    suspend fun setBudgetAlerts(enabled: Boolean) {
        try {
            context.dataStore.edit { it[PreferencesKeys.BUDGET_ALERTS_ENABLED] = enabled }
        } catch (_: Exception) {}
    }
}
