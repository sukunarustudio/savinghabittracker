package com.moneysaver.habittracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.moneysaver.habittracker.domain.model.PeriodFilter
import com.moneysaver.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val currencySymbol: String = "$",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val periodFilter: PeriodFilter = PeriodFilter.MONTHLY
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    constructor(
        context: Context,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ) : this(
        context.applicationContext.userDataStore,
        scope
    )

    companion object {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PERIOD_FILTER = stringPreferencesKey("period_filter")
    }

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val currencySymbol = preferences[CURRENCY_SYMBOL] ?: "$"
            val themeMode = preferences[THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM
            val periodFilter = preferences[PERIOD_FILTER]?.let {
                runCatching { PeriodFilter.valueOf(it) }.getOrDefault(PeriodFilter.MONTHLY)
            } ?: PeriodFilter.MONTHLY

            UserPreferences(
                currencySymbol = currencySymbol,
                themeMode = themeMode,
                periodFilter = periodFilter
            )
        }

    val userPreferences: StateFlow<UserPreferences> = userPreferencesFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    suspend fun setCurrencySymbol(symbol: String) {
        dataStore.edit { preferences ->
            preferences[CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun updateCurrencySymbol(symbol: String) = setCurrencySymbol(symbol)

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) = setThemeMode(mode)

    suspend fun setPeriodFilter(period: PeriodFilter) {
        dataStore.edit { preferences ->
            preferences[PERIOD_FILTER] = period.name
        }
    }

    suspend fun updatePeriodFilter(period: PeriodFilter) = setPeriodFilter(period)

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
