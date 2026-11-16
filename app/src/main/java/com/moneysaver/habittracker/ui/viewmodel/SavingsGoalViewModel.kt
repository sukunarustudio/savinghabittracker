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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    val currencySymbol: String = "Rp",
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

private data class SavingsGoalBaseData(
    val goals: List<SavingsGoal>,
    val expenseCategories: List<Category>,
    val availableBalance: Long,
    val totalActiveAllocated: Long,
    val currencySymbol: String
)

private data class SavingsGoalDialogState(
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
    val error: String? = null
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

    private val dialogState = MutableStateFlow(SavingsGoalDialogState())

    private val goalsDataFlow = combine(
        savingsGoalRepository.getGoals(),
        transactionRepository.getTransactions(),
        categoryRepository.getCategories(),
        userPreferencesRepository.userPreferencesFlow
    ) { goals, transactions, categories, userPrefs ->
        val available = ledgerEngine.calculateLifetimeAvailableBalance(transactions, goals)
        val activeAllocated = goals.filter { it.status.isActive }.sumOf { it.savedAmount }
        val expenseCats = categories.filter { it.type == CategoryType.EXPENSE }

        SavingsGoalBaseData(
            goals = goals,
            expenseCategories = expenseCats,
            availableBalance = available,
            totalActiveAllocated = activeAllocated,
            currencySymbol = userPrefs.currencySymbol
        )
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<SavingsGoalUiState> = combine(
        goalsDataFlow,
        dialogState
    ) { data, dialog ->
        SavingsGoalUiState(
            goals = data.goals,
            expenseCategories = data.expenseCategories,
            availableBalance = data.availableBalance,
            totalActiveAllocated = data.totalActiveAllocated,
            currencySymbol = data.currencySymbol,
            isAddEditOpen = dialog.isAddEditOpen,
            editingGoal = dialog.editingGoal,
            isAllocateOpen = dialog.isAllocateOpen,
            goalForAllocation = dialog.goalForAllocation,
            isWithdrawOpen = dialog.isWithdrawOpen,
            goalForWithdraw = dialog.goalForWithdraw,
            isDeleteDialogOpen = dialog.isDeleteDialogOpen,
            goalToDelete = dialog.goalToDelete,
            deletionChoice = dialog.deletionChoice,
            selectedRealizeCategoryId = dialog.selectedRealizeCategoryId ?: data.expenseCategories.firstOrNull()?.id,
            error = dialog.error,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SavingsGoalUiState(isLoading = true)
    )

    fun openAddGoal() {
        dialogState.update { it.copy(isAddEditOpen = true, editingGoal = null, error = null) }
    }

    fun openEditGoal(goal: SavingsGoal) {
        dialogState.update { it.copy(isAddEditOpen = true, editingGoal = goal, error = null) }
    }

    fun closeAddEditDialog() {
        dialogState.update { it.copy(isAddEditOpen = false, editingGoal = null, error = null) }
    }

    fun saveGoal(title: String, targetAmount: Long, targetDate: Long?) {
        if (title.isBlank()) {
            dialogState.update { it.copy(error = "Goal title cannot be empty") }
            return
        }
        if (targetAmount <= 0L) {
            dialogState.update { it.copy(error = "Target amount must be greater than zero") }
            return
        }

        viewModelScope.launch {
            try {
                val editing = dialogState.value.editingGoal
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
                dialogState.update { it.copy(error = e.message ?: "Failed to save savings goal") }
            }
        }
    }

    fun openAllocateDialog(goal: SavingsGoal) {
        dialogState.update { it.copy(isAllocateOpen = true, goalForAllocation = goal, error = null) }
    }

    fun closeAllocateDialog() {
        dialogState.update { it.copy(isAllocateOpen = false, goalForAllocation = null, error = null) }
    }

    fun allocate(amount: Long, note: String = "") {
        val goal = dialogState.value.goalForAllocation ?: return
        if (amount <= 0L) {
            dialogState.update { it.copy(error = "Allocation amount must be greater than zero") }
            return
        }
        if (amount > uiState.value.availableBalance) {
            dialogState.update { it.copy(error = "Cannot allocate more than available balance") }
            return
        }

        viewModelScope.launch {
            try {
                allocateSavingsGoalUseCase(goal.id, amount, note = note)
                closeAllocateDialog()
            } catch (e: InsufficientBalanceException) {
                dialogState.update { it.copy(error = e.message ?: "Insufficient balance to allocate funds") }
            } catch (e: Exception) {
                dialogState.update { it.copy(error = e.message ?: "Failed to allocate funds") }
            }
        }
    }

    fun openWithdrawDialog(goal: SavingsGoal) {
        dialogState.update { it.copy(isWithdrawOpen = true, goalForWithdraw = goal, error = null) }
    }

    fun closeWithdrawDialog() {
        dialogState.update { it.copy(isWithdrawOpen = false, goalForWithdraw = null, error = null) }
    }

    fun withdraw(amount: Long, note: String = "") {
        val goal = dialogState.value.goalForWithdraw ?: return
        if (amount <= 0L) {
            dialogState.update { it.copy(error = "Withdrawal amount must be greater than zero") }
            return
        }
        if (amount > goal.savedAmount) {
            dialogState.update { it.copy(error = "Cannot withdraw more than saved amount") }
            return
        }

        viewModelScope.launch {
            try {
                withdrawSavingsGoalUseCase(goal.id, amount, note = note)
                closeWithdrawDialog()
            } catch (e: Exception) {
                dialogState.update { it.copy(error = e.message ?: "Failed to withdraw funds") }
            }
        }
    }

    fun openDeleteDialog(goal: SavingsGoal) {
        dialogState.update {
            it.copy(
                isDeleteDialogOpen = true,
                goalToDelete = goal,
                deletionChoice = GoalDeletionChoice.REFUND_TO_BALANCE,
                selectedRealizeCategoryId = uiState.value.expenseCategories.firstOrNull()?.id,
                error = null
            )
        }
    }

    fun closeDeleteDialog() {
        dialogState.update {
            it.copy(
                isDeleteDialogOpen = false,
                goalToDelete = null,
                error = null
            )
        }
    }

    fun setDeletionChoice(choice: GoalDeletionChoice) {
        dialogState.update { it.copy(deletionChoice = choice) }
    }

    fun setSelectedRealizeCategory(categoryId: Long) {
        dialogState.update { it.copy(selectedRealizeCategoryId = categoryId) }
    }

    fun confirmDeleteGoal(realizeNote: String = "") {
        val goal = dialogState.value.goalToDelete ?: return
        val choice = dialogState.value.deletionChoice

        viewModelScope.launch {
            try {
                val strategy = when (choice) {
                    GoalDeletionChoice.REFUND_TO_BALANCE -> GoalDeletionStrategy.RefundToBalance()
                    GoalDeletionChoice.REALIZE_AS_EXPENSE -> {
                        val catId = dialogState.value.selectedRealizeCategoryId
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
                dialogState.update { it.copy(error = e.message ?: "Failed to delete savings goal") }
            }
        }
    }

    fun clearError() {
        dialogState.update { it.copy(error = null) }
    }
}
