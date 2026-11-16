package com.moneysaver.habittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneysaver.habittracker.domain.model.GoalStatus
import com.moneysaver.habittracker.domain.model.SavingsGoal
import com.moneysaver.habittracker.ui.components.EmptyStateView
import com.moneysaver.habittracker.ui.theme.GreenIncome
import com.moneysaver.habittracker.ui.util.CurrencyFormatter
import com.moneysaver.habittracker.ui.util.DateFormatter
import com.moneysaver.habittracker.ui.viewmodel.GoalDeletionChoice
import com.moneysaver.habittracker.ui.viewmodel.SavingsGoalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsGoalViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings Goals",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddGoal() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Goal")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Summary header card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Allocated Savings",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCents(state.totalActiveAllocated, state.currencySymbol),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Available to Allocate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCents(state.availableBalance, state.currencySymbol),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GreenIncome
                        )
                    }
                }
            }

            // Goals list or empty state
            if (state.goals.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Savings,
                    title = "No Savings Goals Yet",
                    description = "Create a savings goal to track funds for emergencies, vacations, or major purchases.",
                    actionButtonText = "Create Goal",
                    onActionClick = { viewModel.openAddGoal() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.goals, key = { it.id }) { goal ->
                        SavingsGoalCard(
                            goal = goal,
                            currencySymbol = state.currencySymbol,
                            onAllocate = { viewModel.openAllocateDialog(goal) },
                            onWithdraw = { viewModel.openWithdrawDialog(goal) },
                            onEdit = { viewModel.openEditGoal(goal) },
                            onDelete = { viewModel.openDeleteDialog(goal) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add/Edit Dialog
        if (state.isAddEditOpen) {
            AddEditGoalDialog(
                editingGoal = state.editingGoal,
                currencySymbol = state.currencySymbol,
                onDismiss = { viewModel.closeAddEditDialog() },
                onSave = { title, targetAmount, targetDate ->
                    viewModel.saveGoal(title, targetAmount, targetDate)
                }
            )
        }

        // Allocate Dialog
        if (state.isAllocateOpen && state.goalForAllocation != null) {
            val goal = state.goalForAllocation!!
            var allocateText by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.closeAllocateDialog() },
                title = { Text("Allocate to: ${goal.title}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Available Balance: ${CurrencyFormatter.formatCents(state.availableBalance, state.currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = allocateText,
                            onValueChange = { allocateText = it },
                            label = { Text("Amount to Allocate") },
                            prefix = { Text(state.currencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cents = CurrencyFormatter.parseToCents(allocateText)
                            if (cents != null && cents > 0L) {
                                viewModel.allocate(cents, note)
                            }
                        }
                    ) {
                        Text("Allocate")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeAllocateDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Withdraw Dialog
        if (state.isWithdrawOpen && state.goalForWithdraw != null) {
            val goal = state.goalForWithdraw!!
            var withdrawText by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { viewModel.closeWithdrawDialog() },
                title = { Text("Withdraw from: ${goal.title}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Currently Saved: ${CurrencyFormatter.formatCents(goal.savedAmount, state.currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = withdrawText,
                            onValueChange = { withdrawText = it },
                            label = { Text("Amount to Withdraw") },
                            prefix = { Text(state.currencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val cents = CurrencyFormatter.parseToCents(withdrawText)
                            if (cents != null && cents > 0L) {
                                viewModel.withdraw(cents, note)
                            }
                        }
                    ) {
                        Text("Withdraw")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeWithdrawDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Dialog with Refund or Realize as Expense selection
        if (state.isDeleteDialogOpen && state.goalToDelete != null) {
            val goal = state.goalToDelete!!
            var realizeNote by remember { mutableStateOf("") }
            var categoryDropdownExpanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { viewModel.closeDeleteDialog() },
                title = { Text("Delete Goal: ${goal.title}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (goal.savedAmount > 0L) {
                            Text(
                                text = "This goal currently holds ${CurrencyFormatter.formatCents(goal.savedAmount, state.currencySymbol)}. How would you like to handle these saved funds?",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            // Option 1: Refund to Available Balance
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDeletionChoice(GoalDeletionChoice.REFUND_TO_BALANCE) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.deletionChoice == GoalDeletionChoice.REFUND_TO_BALANCE,
                                    onClick = { viewModel.setDeletionChoice(GoalDeletionChoice.REFUND_TO_BALANCE) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Refund to Balance", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Returns funds to your net available balance via internal transfer.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Option 2: Realize as Expense
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDeletionChoice(GoalDeletionChoice.REALIZE_AS_EXPENSE) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.deletionChoice == GoalDeletionChoice.REALIZE_AS_EXPENSE,
                                    onClick = { viewModel.setDeletionChoice(GoalDeletionChoice.REALIZE_AS_EXPENSE) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Realize as Expense", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Records funds as spent under an expense category.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // If Realize as Expense selected, show category picker
                            if (state.deletionChoice == GoalDeletionChoice.REALIZE_AS_EXPENSE) {
                                val selectedCategory = state.expenseCategories.find { it.id == state.selectedRealizeCategoryId }
                                    ?: state.expenseCategories.firstOrNull()

                                ExposedDropdownMenuBox(
                                    expanded = categoryDropdownExpanded,
                                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedCategory?.name ?: "Select Expense Category",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Expense Category") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = categoryDropdownExpanded,
                                        onDismissRequest = { categoryDropdownExpanded = false }
                                    ) {
                                        state.expenseCategories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text(cat.name) },
                                                onClick = {
                                                    viewModel.setSelectedRealizeCategory(cat.id)
                                                    categoryDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = realizeNote,
                                    onValueChange = { realizeNote = it },
                                    label = { Text("Expense Note (optional)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text("Are you sure you want to delete this savings goal?")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDeleteGoal(realizeNote) }
                    ) {
                        Text("Delete Goal", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeDeleteDialog() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    currencySymbol: String,
    onAllocate: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.targetDate != null) {
                        Text(
                            text = "Target Date: ${DateFormatter.formatDate(goal.targetDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val statusBadge = when {
                    goal.isAchieved -> "Achieved"
                    goal.status == GoalStatus.CANCELLED -> "Cancelled"
                    goal.status == GoalStatus.OVERDUE -> "Overdue"
                    else -> "In Progress"
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(statusBadge, style = MaterialTheme.typography.labelSmall) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (goal.isAchieved) GreenIncome else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${CurrencyFormatter.formatCents(goal.savedAmount, currencySymbol)} saved",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (goal.isAchieved) GreenIncome else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Goal: ${CurrencyFormatter.formatCents(goal.targetAmount, currencySymbol)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAllocate,
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Allocate")
                    }
                    if (goal.savedAmount > 0L) {
                        OutlinedButton(
                            onClick = onWithdraw,
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Withdraw")
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditGoalDialog(
    editingGoal: SavingsGoal?,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (title: String, targetAmount: Long, targetDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf(editingGoal?.title ?: "") }
    var targetAmountText by remember {
        mutableStateOf(
            if (editingGoal != null) String.format("%.2f", editingGoal.targetAmount / 100.0) else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingGoal == null) "Create Savings Goal" else "Edit Savings Goal") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = { targetAmountText = it },
                    label = { Text("Target Amount") },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = CurrencyFormatter.parseToCents(targetAmountText)
                    if (title.isNotBlank() && cents != null && cents > 0L) {
                        onSave(title.trim(), cents, editingGoal?.targetDate)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
