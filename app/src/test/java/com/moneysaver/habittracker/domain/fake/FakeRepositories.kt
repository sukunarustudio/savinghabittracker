package com.moneysaver.habittracker.domain.fake

import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.repository.CategoryRepository
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeTransactionRepository : TransactionRepository {
    private val transactions = mutableListOf<Transaction>()
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    private fun emit() {
        transactionsFlow.value = transactions.toList()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val assignedId = if (transaction.id > 0L) transaction.id else nextId++
        val saved = transaction.copy(id = assignedId)
        transactions.add(saved)
        emit()
        return assignedId
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index != -1) {
            transactions[index] = transaction
            emit()
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactions.removeAll { it.id == transaction.id }
        emit()
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactions.find { it.id == id }
    }

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionsFlow.asStateFlow()
    }

    override suspend fun getAllTransactions(): List<Transaction> {
        return transactions.toList()
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionsFlow.map { list ->
            list.filter { it.timestamp in startDate..endDate }
        }
    }

    override suspend fun getTransactionsByGoalId(goalId: Long): List<Transaction> {
        return transactions.filter { it.savingsGoalId == goalId }
    }

    override suspend fun deleteTransactionsByGoalId(goalId: Long) {
        transactions.removeAll { it.savingsGoalId == goalId }
        emit()
    }
}

class FakeSavingsGoalRepository : SavingsGoalRepository {
    private val goals = mutableListOf<SavingsGoal>()
    private val goalsFlow = MutableStateFlow<List<SavingsGoal>>(emptyList())
    private var nextId = 1L

    private fun emit() {
        goalsFlow.value = goals.toList()
    }

    override suspend fun insertGoal(goal: SavingsGoal): Long {
        val assignedId = if (goal.id > 0L) goal.id else nextId++
        val saved = goal.copy(id = assignedId)
        goals.add(saved)
        emit()
        return assignedId
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        val index = goals.indexOfFirst { it.id == goal.id }
        if (index != -1) {
            goals[index] = goal
            emit()
        }
    }

    override suspend fun deleteGoal(goal: SavingsGoal) {
        goals.removeAll { it.id == goal.id }
        emit()
    }

    override suspend fun getGoalById(id: Long): SavingsGoal? {
        return goals.find { it.id == id }
    }

    override fun getGoals(): Flow<List<SavingsGoal>> {
        return goalsFlow.asStateFlow()
    }

    override suspend fun getAllGoals(): List<SavingsGoal> {
        return goals.toList()
    }

    override suspend fun updateSavedAmount(goalId: Long, newSavedAmount: Long) {
        val index = goals.indexOfFirst { it.id == goalId }
        if (index != -1) {
            goals[index] = goals[index].copy(savedAmount = newSavedAmount)
            emit()
        }
    }
}

class FakeCategoryRepository : CategoryRepository {
    private val categories = mutableListOf<Category>()
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L
    val reassignedEvents = mutableListOf<Pair<Long, Long>>()

    private fun emit() {
        categoriesFlow.value = categories.toList()
    }

    override suspend fun insertCategory(category: Category): Long {
        val assignedId = if (category.id > 0L) category.id else nextId++
        val saved = category.copy(id = assignedId)
        categories.add(saved)
        emit()
        return assignedId
    }

    override suspend fun updateCategory(category: Category) {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index != -1) {
            categories[index] = category
            emit()
        }
    }

    override suspend fun deleteCategory(category: Category) {
        categories.removeAll { it.id == category.id }
        emit()
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categories.find { it.id == id }
    }

    override fun getCategories(): Flow<List<Category>> {
        return categoriesFlow.asStateFlow()
    }

    override suspend fun getAllCategories(): List<Category> {
        return categories.toList()
    }

    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) {
        reassignedEvents.add(oldCategoryId to newCategoryId)
    }

    override suspend fun countTransactionsForCategory(categoryId: Long): Int {
        return 0
    }
}
