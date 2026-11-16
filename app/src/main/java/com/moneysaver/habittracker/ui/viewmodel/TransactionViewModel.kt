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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableBalance: Long = 0L,
    val filterType: TransactionType? = null,
    val currencySymbol: String = "Rp",
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

private data class TransactionBaseData(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val availableBalance: Long,
    val filterType: TransactionType?,
    val currencySymbol: String
)

private data class TransactionDialogState(
    val isAddEditOpen: Boolean = false,
    val editingTransaction: Transaction? = null,
    val isCategoryManagementOpen: Boolean = false,
    val transactionToDelete: Transaction? = null,
    val overdraftWarningMessage: String? = null,
    val categoryToDelete: Pair<Category, Int>? = null,
    val error: String? = null
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
    private val dialogState = MutableStateFlow(TransactionDialogState())

    private val transactionDataFlow = combine(
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

        TransactionBaseData(
            transactions = filteredTx,
            categories = categories,
            availableBalance = available,
            filterType = filter,
            currencySymbol = prefs.currencySymbol
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<TransactionUiState> = combine(
        transactionDataFlow,
        dialogState
    ) { data, dialog ->
        TransactionUiState(
            transactions = data.transactions,
            categories = data.categories,
            availableBalance = data.availableBalance,
            filterType = data.filterType,
            currencySymbol = data.currencySymbol,
            isAddEditOpen = dialog.isAddEditOpen,
            editingTransaction = dialog.editingTransaction,
            isCategoryManagementOpen = dialog.isCategoryManagementOpen,
            showDeleteConfirm = dialog.transactionToDelete != null,
            transactionToDelete = dialog.transactionToDelete,
            showOverdraftWarning = dialog.overdraftWarningMessage != null,
            overdraftWarningMessage = dialog.overdraftWarningMessage ?: "",
            showCategoryDeleteDialog = dialog.categoryToDelete != null,
            categoryToDelete = dialog.categoryToDelete?.first,
            categoryLinkedCount = dialog.categoryToDelete?.second ?: 0,
            error = dialog.error,
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
        dialogState.update { it.copy(isAddEditOpen = true, editingTransaction = null, error = null) }
    }

    fun openEditTransaction(transaction: Transaction) {
        dialogState.update { it.copy(isAddEditOpen = true, editingTransaction = transaction, error = null) }
    }

    fun closeAddEditDialog() {
        dialogState.update { it.copy(isAddEditOpen = false, editingTransaction = null, error = null) }
    }

    fun saveTransaction(
        amount: Long,
        type: TransactionType,
        categoryId: Long?,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (amount <= 0L) {
            dialogState.update { it.copy(error = "Amount must be greater than zero") }
            return
        }

        val currentAvailable = uiState.value.availableBalance
        val editing = dialogState.value.editingTransaction

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
                dialogState.update {
                    it.copy(
                        overdraftWarningMessage = "Warning: This expense of ${CurrencyFormatter.formatCents(amount, symbol)} " +
                                "exceeds your available balance of ${CurrencyFormatter.formatCents(currentAvailable, symbol)} " +
                                "by ${CurrencyFormatter.formatCents(excess, symbol)}. Non-negative balance invariant strictly forbids this transaction."
                    )
                }
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
                dialogState.update {
                    it.copy(overdraftWarningMessage = e.message ?: "Transaction causes overdraft and cannot be completed.")
                }
            } catch (e: Exception) {
                dialogState.update { it.copy(error = e.message ?: "Failed to save transaction") }
            }
        }
    }

    fun promptDeleteTransaction(transaction: Transaction) {
        dialogState.update { it.copy(transactionToDelete = transaction) }
    }

    fun cancelDeleteTransaction() {
        dialogState.update { it.copy(transactionToDelete = null) }
    }

    fun confirmDeleteTransaction() {
        val tx = dialogState.value.transactionToDelete ?: return
        viewModelScope.launch {
            try {
                deleteTransactionUseCase(tx.id)
                dialogState.update { it.copy(transactionToDelete = null) }
            } catch (e: Exception) {
                dialogState.update { it.copy(error = e.message ?: "Failed to delete transaction") }
            }
        }
    }

    fun dismissOverdraftWarning() {
        dialogState.update { it.copy(overdraftWarningMessage = null) }
    }

    fun openCategoryManagement() {
        dialogState.update { it.copy(isCategoryManagementOpen = true) }
    }

    fun closeCategoryManagement() {
        dialogState.update { it.copy(isCategoryManagementOpen = false) }
    }

    fun createCategory(name: String, type: CategoryType, colorHex: String, iconRes: Int = 0) {
        if (name.isBlank()) {
            dialogState.update { it.copy(error = "Category name cannot be empty") }
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
                dialogState.update { it.copy(error = e.message ?: "Failed to create category") }
            }
        }
    }

    fun promptDeleteCategory(category: Category) {
        viewModelScope.launch {
            val count = categoryRepository.countTransactionsForCategory(category.id)
            dialogState.update { it.copy(categoryToDelete = category to count) }
        }
    }

    fun cancelDeleteCategory() {
        dialogState.update { it.copy(categoryToDelete = null) }
    }

    fun confirmDeleteCategory(targetCategoryId: Long?) {
        val (category, count) = dialogState.value.categoryToDelete ?: return
        viewModelScope.launch {
            try {
                if (count > 0) {
                    if (targetCategoryId == null || targetCategoryId == category.id) {
                        dialogState.update { it.copy(error = "Please select a different category to reassign linked transactions") }
                        return@launch
                    }
                    deleteCategoryWithReassignmentUseCase(category.id, targetCategoryId)
                } else {
                    categoryRepository.deleteCategory(category)
                }
                dialogState.update { it.copy(categoryToDelete = null) }
            } catch (e: Exception) {
                dialogState.update { it.copy(error = e.message ?: "Failed to delete category") }
            }
        }
    }

    fun clearError() {
        dialogState.update { it.copy(error = null) }
    }
}
