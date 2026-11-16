package com.moneysaver.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.domain.repository.CategoryRepository
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import com.moneysaver.habittracker.domain.usecase.category.CreateCategoryUseCase
import com.moneysaver.habittracker.domain.usecase.category.DeleteCategoryWithReassignmentUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.CreateTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.DeleteTransactionUseCase
import com.moneysaver.habittracker.domain.usecase.transaction.UpdateTransactionUseCase
import com.moneysaver.habittracker.ui.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableBalance: Long = 0L,
    val filterType: TransactionType? = null,
    val currencySymbol: String = "$",
    val isAddEditOpen: Boolean = false,
    val editingTransaction: Transaction? = null,
    val isCategoryManagementOpen: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val transactionToDelete: Transaction? = null,
    val showOverdraftWarning: Boolean = false,
    val overdraftWarningMessage: String = "",
    val showCategoryDeleteDialog: Boolean = false,
    val categoryToDelete: Category? = null,
    val categoryLinkedCount: Int = 0,
    val error: String? = null,
    val isLoading: Boolean = false
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ledgerEngine: FinancialLedgerEngine = FinancialLedgerEngine(),
    private val createTransactionUseCase: CreateTransactionUseCase = CreateTransactionUseCase(
        transactionRepository,
        savingsGoalRepository,
        ledgerEngine
    ),
    private val updateTransactionUseCase: UpdateTransactionUseCase = UpdateTransactionUseCase(
        transactionRepository,
        savingsGoalRepository,
        ledgerEngine
    ),
    private val deleteTransactionUseCase: DeleteTransactionUseCase = DeleteTransactionUseCase(
        transactionRepository,
        savingsGoalRepository,
        ledgerEngine
    ),
    private val createCategoryUseCase: CreateCategoryUseCase = CreateCategoryUseCase(categoryRepository),
    private val deleteCategoryWithReassignmentUseCase: DeleteCategoryWithReassignmentUseCase = DeleteCategoryWithReassignmentUseCase(
        categoryRepository
    )
) : ViewModel() {

    private val filterTypeState = MutableStateFlow<TransactionType?>(null)
    private val isAddEditOpenState = MutableStateFlow(false)
    private val editingTxState = MutableStateFlow<Transaction?>(null)
    private val isCategoryManagementOpenState = MutableStateFlow(false)
    private val deleteConfirmState = MutableStateFlow<Transaction?>(null)
    private val overdraftWarningState = MutableStateFlow<String?>(null)
    private val categoryToDeleteState = MutableStateFlow<Pair<Category, Int>?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionUiState> = combine(
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        savingsGoalRepository.getGoals(),
        filterTypeState,
        userPreferencesRepository.userPreferencesFlow
    ) { transactions, categories, goals, filter, prefs ->
        val available = ledgerEngine.calculateLifetimeAvailableBalance(transactions, goals)
        val filteredTx = if (filter != null) {
            transactions.filter { it.type == filter }
        } else {
            transactions
        }.sortedByDescending { it.timestamp }

        TransactionUiState(
            transactions = filteredTx,
            categories = categories,
            availableBalance = available,
            filterType = filter,
            currencySymbol = prefs.currencySymbol,
            isAddEditOpen = isAddEditOpenState.value,
            editingTransaction = editingTxState.value,
            isCategoryManagementOpen = isCategoryManagementOpenState.value,
            showDeleteConfirm = deleteConfirmState.value != null,
            transactionToDelete = deleteConfirmState.value,
            showOverdraftWarning = overdraftWarningState.value != null,
            overdraftWarningMessage = overdraftWarningState.value ?: "",
            showCategoryDeleteDialog = categoryToDeleteState.value != null,
            categoryToDelete = categoryToDeleteState.value?.first,
            categoryLinkedCount = categoryToDeleteState.value?.second ?: 0,
            error = errorState.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionUiState(isLoading = true)
    )

    fun setFilter(type: TransactionType?) {
        filterTypeState.value = type
    }

    fun openAddTransaction() {
        editingTxState.value = null
        isAddEditOpenState.value = true
        errorState.value = null
    }

    fun openEditTransaction(transaction: Transaction) {
        editingTxState.value = transaction
        isAddEditOpenState.value = true
        errorState.value = null
    }

    fun closeAddEditDialog() {
        isAddEditOpenState.value = false
        editingTxState.value = null
        errorState.value = null
    }

    fun saveTransaction(
        amount: Long,
        type: TransactionType,
        categoryId: Long?,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0L) {
            errorState.value = "Amount must be greater than zero"
            return
        }

        val currentAvailable = uiState.value.availableBalance
        val editing = editingTxState.value

        // Check overdraft condition for expenses
        if (type == TransactionType.EXPENSE) {
            val netDelta = if (editing != null && editing.type == TransactionType.EXPENSE) {
                amount - editing.amount
            } else {
                amount
            }

            if (netDelta > currentAvailable) {
                val symbol = uiState.value.currencySymbol
                val excess = netDelta - currentAvailable
                overdraftWarningState.value = "Warning: This expense of ${CurrencyFormatter.formatCents(amount, symbol)} " +
                        "exceeds your available balance of ${CurrencyFormatter.formatCents(currentAvailable, symbol)} " +
                        "by ${CurrencyFormatter.formatCents(excess, symbol)}. Non-negative balance invariant strictly forbids this transaction."
                return
            }
        }

        viewModelScope.launch {
            try {
                if (editing != null) {
                    val updated = editing.copy(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        note = note,
                        timestamp = timestamp
                    )
                    updateTransactionUseCase(updated)
                } else {
                    val newTx = Transaction(
                        amount = amount,
                        type = type,
                        categoryId = categoryId,
                        note = note,
                        timestamp = timestamp
                    )
                    createTransactionUseCase(newTx)
                }
                closeAddEditDialog()
            } catch (e: InsufficientBalanceException) {
                overdraftWarningState.value = e.message ?: "Transaction causes overdraft and cannot be completed."
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to save transaction"
            }
        }
    }

    fun promptDeleteTransaction(transaction: Transaction) {
        deleteConfirmState.value = transaction
    }

    fun cancelDeleteTransaction() {
        deleteConfirmState.value = null
    }

    fun confirmDeleteTransaction() {
        val tx = deleteConfirmState.value ?: return
        viewModelScope.launch {
            try {
                deleteTransactionUseCase(tx.id)
                deleteConfirmState.value = null
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to delete transaction"
            }
        }
    }

    fun dismissOverdraftWarning() {
        overdraftWarningState.value = null
    }

    fun openCategoryManagement() {
        isCategoryManagementOpenState.value = true
    }

    fun closeCategoryManagement() {
        isCategoryManagementOpenState.value = false
    }

    fun createCategory(name: String, type: CategoryType, colorHex: String, iconRes: Int = 0) {
        if (name.isBlank()) {
            errorState.value = "Category name cannot be empty"
            return
        }
        viewModelScope.launch {
            try {
                val cat = Category(
                    name = name.trim(),
                    type = type,
                    colorHex = colorHex,
                    iconRes = iconRes,
                    isDefault = false
                )
                createCategoryUseCase(cat)
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to create category"
            }
        }
    }

    fun promptDeleteCategory(category: Category) {
        viewModelScope.launch {
            val count = categoryRepository.countTransactionsForCategory(category.id)
            categoryToDeleteState.value = category to count
        }
    }

    fun cancelDeleteCategory() {
        categoryToDeleteState.value = null
    }

    fun confirmDeleteCategory(targetCategoryId: Long?) {
        val (category, count) = categoryToDeleteState.value ?: return
        viewModelScope.launch {
            try {
                if (count > 0) {
                    if (targetCategoryId == null || targetCategoryId == category.id) {
                        errorState.value = "Please select a different category to reassign linked transactions"
                        return@launch
                    }
                    deleteCategoryWithReassignmentUseCase(category.id, targetCategoryId)
                } else {
                    categoryRepository.deleteCategory(category)
                }
                categoryToDeleteState.value = null
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to delete category"
            }
        }
    }

    fun clearError() {
        errorState.value = null
    }
}
