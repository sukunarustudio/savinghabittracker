package com.moneysaver.habittracker

import android.app.Application
import com.moneysaver.habittracker.data.local.MoneyHabitDatabase
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository

class MoneyHabitApplication : Application() {

    val database: MoneyHabitDatabase by lazy {
        MoneyHabitDatabase.getDatabase(this)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MoneyHabitApplication
            private set
    }
}
