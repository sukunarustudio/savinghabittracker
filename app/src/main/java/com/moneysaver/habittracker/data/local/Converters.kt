package com.moneysaver.habittracker.data.local

import androidx.room.TypeConverter
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.model.habit.FrequencyType

class Converters {

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? {
        return value?.let { CategoryType.valueOf(it) }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let { TransactionType.valueOf(it) }
    }

    @TypeConverter
    fun fromGoalStatus(value: GoalStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toGoalStatus(value: String?): GoalStatus? {
        return value?.let { GoalStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toFrequencyType(value: String?): FrequencyType? {
        return value?.let { FrequencyType.valueOf(it) }
    }
}
