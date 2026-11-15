package com.moneysaver.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneysaver.habittracker.data.local.dao.CategoryDao
import com.moneysaver.habittracker.data.local.dao.HabitCompletionLogDao
import com.moneysaver.habittracker.data.local.dao.HabitDao
import com.moneysaver.habittracker.data.local.dao.SavingsGoalDao
import com.moneysaver.habittracker.data.local.dao.TransactionDao
import com.moneysaver.habittracker.data.local.entity.CategoryEntity
import com.moneysaver.habittracker.data.local.entity.HabitCompletionLogEntity
import com.moneysaver.habittracker.data.local.entity.HabitEntity
import com.moneysaver.habittracker.data.local.entity.SavingsGoalEntity
import com.moneysaver.habittracker.data.local.entity.TransactionEntity
import com.moneysaver.habittracker.domain.model.CategoryType

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        HabitEntity::class,
        HabitCompletionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MoneyHabitDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionLogDao(): HabitCompletionLogDao

    companion object {
        const val DATABASE_NAME = "money_habit.db"

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(
                name = "Food & Dining",
                iconRes = "restaurant",
                colorHex = "#E53935",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Transportation",
                iconRes = "directions_bus",
                colorHex = "#1E88E5",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Housing",
                iconRes = "home",
                colorHex = "#8E24AA",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Utilities",
                iconRes = "bolt",
                colorHex = "#FB8C00",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Entertainment",
                iconRes = "movie",
                colorHex = "#D81B60",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Shopping",
                iconRes = "shopping_bag",
                colorHex = "#00ACC1",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Health",
                iconRes = "medical_services",
                colorHex = "#43A047",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Education",
                iconRes = "school",
                colorHex = "#3949AB",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Miscellaneous",
                iconRes = "more_horiz",
                colorHex = "#757575",
                type = CategoryType.EXPENSE,
                isDefault = true
            ),
            CategoryEntity(
                name = "Salary",
                iconRes = "payments",
                colorHex = "#2E7D32",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            CategoryEntity(
                name = "Freelance",
                iconRes = "laptop",
                colorHex = "#00897B",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            CategoryEntity(
                name = "Investments",
                iconRes = "trending_up",
                colorHex = "#7CB342",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            CategoryEntity(
                name = "Gifts & Grants",
                iconRes = "card_giftcard",
                colorHex = "#C0CA33",
                type = CategoryType.INCOME,
                isDefault = true
            ),
            CategoryEntity(
                name = "Other Income",
                iconRes = "attach_money",
                colorHex = "#0097A7",
                type = CategoryType.INCOME,
                isDefault = true
            )
        )

        @Volatile
        private var INSTANCE: MoneyHabitDatabase? = null

        fun getDatabase(context: Context): MoneyHabitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MoneyHabitDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                for (category in DEFAULT_CATEGORIES) {
                    db.execSQL(
                        "INSERT INTO categories (name, iconRes, colorHex, type, isDefault) VALUES (?, ?, ?, ?, ?)",
                        arrayOf(
                            category.name,
                            category.iconRes,
                            category.colorHex,
                            category.type.name,
                            if (category.isDefault) 1 else 0
                        )
                    )
                }
            }
        }
    }
}
