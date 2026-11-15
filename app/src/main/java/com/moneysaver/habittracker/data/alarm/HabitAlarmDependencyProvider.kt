package com.moneysaver.habittracker.data.alarm

import android.content.Context
import com.moneysaver.habittracker.domain.repository.HabitRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dependency locator and provider for broadcast receivers (HabitAlarmReceiver and SystemEventReceiver).
 * Enables clean decoupling and dependency injection / mocking in tests.
 */
object HabitAlarmDependencyProvider {
    var repositoryProvider: ((Context) -> HabitRepository?)? = null
    var schedulerProvider: ((Context) -> AlarmScheduler)? = null
    var coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO

    /**
     * Resolves the HabitRepository if registered.
     */
    fun getRepository(context: Context): HabitRepository? {
        return repositoryProvider?.invoke(context)
    }

    /**
     * Resolves the AlarmScheduler, defaulting to HabitAlarmScheduler(context).
     */
    fun getScheduler(context: Context): AlarmScheduler {
        return schedulerProvider?.invoke(context) ?: HabitAlarmScheduler(context)
    }

    /**
     * Resets custom providers to defaults (useful for test teardown).
     */
    fun reset() {
        repositoryProvider = null
        schedulerProvider = null
        coroutineDispatcher = Dispatchers.IO
    }
}
