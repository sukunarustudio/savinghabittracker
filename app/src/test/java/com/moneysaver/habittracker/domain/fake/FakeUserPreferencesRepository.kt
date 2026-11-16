package com.moneysaver.habittracker.domain.fake

import com.moneysaver.habittracker.data.repository.UserPreferences
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository
import com.moneysaver.habittracker.domain.model.PeriodFilter
import com.moneysaver.habittracker.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeUserPreferencesRepository(
    initialPreferences: UserPreferences = UserPreferences()
) : UserPreferencesRepository {

    private val _prefs = MutableStateFlow(initialPreferences)

    override val userPreferencesFlow: Flow<UserPreferences> = _prefs.asStateFlow()
    override val userPreferences: StateFlow<UserPreferences> = _prefs.asStateFlow()

    override suspend fun setCurrencySymbol(symbol: String) {
        _prefs.value = _prefs.value.copy(currencySymbol = symbol)
    }

    override suspend fun updateCurrencySymbol(symbol: String) = setCurrencySymbol(symbol)

    override suspend fun setThemeMode(mode: ThemeMode) {
        _prefs.value = _prefs.value.copy(themeMode = mode)
    }

    override suspend fun updateThemeMode(mode: ThemeMode) = setThemeMode(mode)

    override suspend fun setPeriodFilter(period: PeriodFilter) {
        _prefs.value = _prefs.value.copy(periodFilter = period)
    }

    override suspend fun updatePeriodFilter(period: PeriodFilter) = setPeriodFilter(period)

    override suspend fun setLanguage(language: String) {
        _prefs.value = _prefs.value.copy(language = language)
    }

    override suspend fun updateLanguage(language: String) = setLanguage(language)

    override suspend fun clear() {
        _prefs.value = UserPreferences()
    }
}
