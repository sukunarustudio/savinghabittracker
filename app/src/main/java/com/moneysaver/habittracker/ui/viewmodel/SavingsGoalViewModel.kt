package com.moneysaver.habittracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneysaver.habittracker.data.repository.UserPreferencesRepository
import com.moneysaver.habittracker.domain.engine.FinancialLedgerEngine
import com.moneysaver.habittracker.domain.exception.InsufficientBalanceException
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.domain.repository.CategoryRepository
import com.moneysaver.habittracker.domain.repository.SavingsGoalRepository
import com.moneysaver.habittracker.domain.repository.TransactionRepository
import com.moneysaver.habittracker.domain.usecase.savings.AllocateSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.CreateSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.DeleteSavingsGoalUseCase
import com.moneysaver.habittracker.domain.usecase.savings.GoalDeletionStrategy
import com.moneysaver.habittracker.domain.usecase.savings.WithdrawSavingsGoalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GoalDeletionChoice {
    REFUND_TO_BALANCE,
    REALIZE_AS_EXPENSE
}

data class SavingsGoalUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val availableBalance: Long = 0L,
    val totalActiveAllocated: Long = 0L,
    val currencySymbol: String = "$",
    val isAddEditOpen: Boolean = false,
    val editingGoal: SavingsGoal? = null,
    val isAllocateOpen: Boolean = false,
    val goalForAllocation: SavingsGoal? = null,
    val isWithdrawOpen: Boolean = false,
    val goalForWithdraw: SavingsGoal? = null,
    val isDeleteDialogOpen: Boolean = false,
    val goalToDelete: SavingsGoal? = null,
    val deletionChoice: GoalDeletionChoice = GoalDeletionChoice.REFUND_TO_BALANCE,
    val selectedRealizeCategoryId: Long? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

class SavingsGoalViewModel(
    private val savingsGoalRepository: SavingsGoalRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val ledgerEngine: FinancialLedgerEngine = FinancialLedgerEngine(),
    private val createSavingsGoalUseCase: CreateSavingsGoalUseCase = CreateSavingsGoalUseCase(savingsGoalRepository),
    private val allocateSavingsGoalUseCase: AllocateSavingsGoalUseCase = AllocateSavingsGoalUseCase(
        savingsGoalRepository,
        transactionRepository,
        ledgerEngine
    ),
    private val withdrawSavingsGoalUseCase: WithdrawSavingsGoalUseCase = WithdrawSavingsGoalUseCase(
        savingsGoalRepository,
        transactionRepository
    ),
    private val deleteSavingsGoalUseCase: DeleteSavingsGoalUseCase = DeleteSavingsGoalUseCase(
        savingsGoalRepository,
        transactionRepository
    )
) : ViewModel() {

    private val isAddEditOpenState = MutableStateFlow(false)
    private val editingGoalState = MutableStateFlow<SavingsGoal?>(null)
    private val isAllocateOpenState = MutableStateFlow(false)
    private val goalForAllocationState = MutableStateFlow<SavingsGoal?>(null)
    private val isWithdrawOpenState = MutableStateFlow(false)
    private val goalForWithdrawState = MutableStateFlow<SavingsGoal?>(null)
    private val isDeleteDialogOpenState = MutableStateFlow(false)
    private val goalToDeleteState = MutableStateFlow<SavingsGoal?>(null)
    private val deletionChoiceState = MutableStateFlow(GoalDeletionChoice.REFUND_TO_BALANCE)
    private val selectedRealizeCategoryIdState = MutableStateFlow<Long?>(null)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SavingsGoalUiState> = combine(
        savingsGoalRepository.getGoals(),
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        userPreferencesRepository.userPreferencesFlow
    ) { goals, transactions, categories, userPrefs ->
        val available = ledgerEngine.calculateLifetimeAvailableBalance(transactions, goals)
        val activeAllocated = goals.filter { it.status.isActive }.sumOf { it.savedAmount }
        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }

        SavingsGoalUiState(
            goals = goals,
            expenseCategories = expenseCats,
            availableBalance = available,
            totalActiveAllocated = activeAllocated,
            currencySymbol = userPrefs.currencySymbol,
            isAddEditOpen = isAddEditOpenState.value,
            editingGoal = editingGoalState.value,
            isAllocateOpen = isAllocateOpenState.value,
            goalForAllocation = goalForAllocationState.value,
            isWithdrawOpen = isWithdrawOpenState.value,
            goalForWithdraw = goalForWithdrawState.value,
            isDeleteDialogOpen = isDeleteDialogOpenState.value,
            goalToDelete = goalToDeleteState.value,
            deletionChoice = deletionChoiceState.value,
            selectedRealizeCategoryId = selectedRealizeCategoryIdState.value ?: expenseCats.firstOrNull()?.id,
            error = errorState.value,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsGoalUiState(isLoading = true)
    )

    fun openAddGoal() {
        editingGoalState.value = null
        isAddEditOpenState.value = true
        errorState.value = null
    }

    fun openEditGoal(goal: SavingsGoal) {
        editingGoalState.value = goal
        isAddEditOpenState.value = true
        errorState.value = null
    }

    fun closeAddEditDialog() {
        isAddEditOpenState.value = false
        editingGoalState.value = null
        errorState.value = null
    }

    fun saveGoal(title: String, targetAmount: Long, targetDate: Long?) {
        if (title.isBlank()) {
            errorState.value = "Goal title cannot be empty"
            return
        }
        if (targetAmount <= 0L) {
            errorState.value = "Target amount must be greater than zero"
            return
        }

        viewModelScope.launch {
            try {
                val editing = editingGoalState.value
                if (editing != null) {
                    val status = if (editing.savedAmount >= targetAmount) GoalStatus.ACHIEVED else editing.status
                    val updated = editing.copy(
                        title = title.trim(),
                        targetAmount = targetAmount,
                        targetDate = targetDate,
                        status = status
                    )
                    savingsGoalRepository.updateGoal(updated)
                } else {
                    val newGoal = SavingsGoal(
                        title = title.trim(),
                        targetAmount = targetAmount,
                        targetDate = targetDate,
                        savedAmount = 0L,
                        status = GoalStatus.IN_PROGRESS
                    )
                    createSavingsGoalUseCase(newGoal)
                }
                closeAddEditDialog()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to save savings goal"
            }
        }
    }

    fun openAllocateDialog(goal: SavingsGoal) {
        goalForAllocationState.value = goal
        isAllocateOpenState.value = true
        errorState.value = null
    }

    fun closeAllocateDialog() {
        isAllocateOpenState.value = false
        goalForAllocationState.value = null
        errorState.value = null
    }

    fun allocate(amount: Long, note: String = "") {
        val goal = goalForAllocationState.value ?: return
        if (amount <= 0L) {
            errorState.value = "Allocation amount must be greater than zero"
            return
        }
        if (amount > uiState.value.availableBalance) {
            errorState.value = "Cannot allocate more than available balance"
            return
        }

        viewModelScope.launch {
            try {
                allocateSavingsGoalUseCase(goal.id, amount, note = note)
                closeAllocateDialog()
            } catch (e: InsufficientBalanceException) {
                errorState.value = e.message ?: "Insufficient balance to allocate funds"
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to allocate funds"
            }
        }
    }

    fun openWithdrawDialog(goal: SavingsGoal) {
        goalForWithdrawState.value = goal
        isWithdrawOpenState.value = true
        errorState.value = null
    }

    fun closeWithdrawDialog() {
        isWithdrawOpenState.value = false
        goalForWithdrawState.value = null
        errorState.value = null
    }

    fun withdraw(amount: Long, note: String = "") {
        val goal = goalForWithdrawState.value ?: return
        if (amount <= 0L) {
            errorState.value = "Withdrawal amount must be greater than zero"
            return
        }
        if (amount > goal.savedAmount) {
            errorState.value = "Cannot withdraw more than saved amount"
            return
        }

        viewModelScope.launch {
            try {
                withdrawSavingsGoalUseCase(goal.id, amount, note = note)
                closeWithdrawDialog()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to withdraw funds"
            }
        }
    }

    fun openDeleteDialog(goal: SavingsGoal) {
        goalToDeleteState.value = goal
        deletionChoiceState.value = GoalDeletionChoice.REFUND_TO_BALANCE
        selectedRealizeCategoryIdState.value = uiState.value.expenseCategories.firstOrNull()?.id
        isDeleteDialogOpenState.value = true
        errorState.value = null
    }

    fun closeDeleteDialog() {
        isDeleteDialogOpenState.value = false
        goalToDeleteState.value = null
        errorState.value = null
    }

    fun setDeletionChoice(choice: GoalDeletionChoice) {
        deletionChoiceState.value = choice
    }

    fun setSelectedRealizeCategory(categoryId: Long) {
        selectedRealizeCategoryIdState.value = categoryId
    }

    fun confirmDeleteGoal(realizeNote: String = "") {
        val goal = goalToDeleteState.value ?: return
        val choice = deletionChoiceState.value

        viewModelScope.launch {
            try {
                val strategy = when (choice) {
                    GoalDeletionChoice.REFUND_TO_BALANCE -> GoalDeletionStrategy.RefundToBalance()
                    GoalDeletionChoice.REALIZE_AS_EXPENSE -> {
                        val catId = selectedRealizeCategoryIdState.value
                            ?: uiState.value.expenseCategories.firstOrNull()?.id
                            ?: throw IllegalStateException("An expense category must be selected to realize goal funds as an expense")
                        GoalDeletionStrategy.RealizeAsExpense(
                            categoryId = catId,
                            note = realizeNote
                        )
                    }
                }
                deleteSavingsGoalUseCase(goal.id, strategy)
                closeDeleteDialog()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to delete savings goal"
            }
        }
    }

    fun clearError() {
        errorState.value = null
    }
}
