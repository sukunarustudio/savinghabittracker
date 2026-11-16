package com.moneysaver.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneysaver.habittracker.domain.model.Category
import com.moneysaver.habittracker.domain.model.CategoryType
import com.moneysaver.habittracker.domain.model.Transaction
import com.moneysaver.habittracker.domain.model.TransactionType
import com.moneysaver.habittracker.ui.components.EmptyStateView
import com.moneysaver.habittracker.ui.components.OverdraftWarningDialog
import com.moneysaver.habittracker.ui.theme.GreenIncome
import com.moneysaver.habittracker.ui.theme.RedExpense
import com.moneysaver.habittracker.ui.util.CurrencyFormatter
import com.moneysaver.habittracker.ui.util.DateFormatter
import com.moneysaver.habittracker.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.openCategoryManagement() }) {
                        Icon(imageVector = Icons.Default.Category, contentDescription = "Manage Categories")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTransaction() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.filterType == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterType == TransactionType.INCOME,
                        onClick = { viewModel.setFilter(TransactionType.INCOME) },
                        label = { Text("Income") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterType == TransactionType.EXPENSE,
                        onClick = { viewModel.setFilter(TransactionType.EXPENSE) },
                        label = { Text("Expense") }
                    )
                }
                item {
                    FilterChip(
                        selected = state.filterType == TransactionType.SAVINGS_DEPOSIT || state.filterType == TransactionType.SAVINGS_WITHDRAWAL,
                        onClick = { viewModel.setFilter(TransactionType.SAVINGS_DEPOSIT) },
                        label = { Text("Transfers") }
                    )
                }
            }

            // Transaction list or empty state
            if (state.transactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No Transactions Found",
                    description = if (state.filterType == null) {
                        "You haven't recorded any transactions yet. Tap the button below to add your first transaction."
                    } else {
                        "No transactions matching the selected filter."
                    },
                    actionButtonText = if (state.filterType == null) "Add Transaction" else null,
                    onActionClick = { viewModel.openAddTransaction() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.transactions, key = { it.id }) { tx ->
                        val category = state.categories.find { it.id == tx.categoryId }
                        TransactionListItem(
                            transaction = tx,
                            category = category,
                            currencySymbol = state.currencySymbol,
                            onEdit = { viewModel.openEditTransaction(tx) },
                            onDelete = { viewModel.promptDeleteTransaction(tx) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Dialogs
        if (state.isAddEditOpen) {
            AddEditTransactionDialog(
                editingTransaction = state.editingTransaction,
                categories = state.categories,
                currencySymbol = state.currencySymbol,
                availableBalance = state.availableBalance,
                onDismiss = { viewModel.closeAddEditDialog() },
                onSave = { amount, type, categoryId, note ->
                    viewModel.saveTransaction(amount, type, categoryId, note)
                }
            )
        }

        if (state.showOverdraftWarning) {
            OverdraftWarningDialog(
                message = state.overdraftWarningMessage,
                onDismiss = { viewModel.dismissOverdraftWarning() }
            )
        }

        if (state.showDeleteConfirm && state.transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteTransaction() },
                title = { Text("Delete Transaction") },
                text = { Text("Are you sure you want to delete this transaction?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDeleteTransaction() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteTransaction() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (state.isCategoryManagementOpen) {
            CategoryManagementDialog(
                categories = state.categories,
                onDismiss = { viewModel.closeCategoryManagement() },
                onCreateCategory = { name, type, colorHex ->
                    viewModel.createCategory(name, type, colorHex)
                },
                onDeleteCategory = { category ->
                    viewModel.promptDeleteCategory(category)
                }
            )
        }

        if (state.showCategoryDeleteDialog && state.categoryToDelete != null) {
            val cat = state.categoryToDelete!!
            val count = state.categoryLinkedCount
            var selectedReassignId by remember {
                mutableStateOf(state.categories.firstOrNull { it.id != cat.id && it.type == cat.type }?.id)
            }

            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteCategory() },
                title = { Text("Delete Category: ${cat.name}") },
                text = {
                    Column {
                        if (count > 0) {
                            Text(
                                text = "This category is linked to $count transaction(s). Choose another category to reassign them to before deleting:"
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val validTargets = state.categories.filter { it.id != cat.id && it.type == cat.type }
                            if (validTargets.isEmpty()) {
                                Text(
                                    text = "No compatible target category available. Create another ${cat.type.name.lowercase()} category first.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                validTargets.forEach { target ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedReassignId = target.id }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.RadioButton(
                                            selected = selectedReassignId == target.id,
                                            onClick = { selectedReassignId = target.id }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(target.name)
                                    }
                                }
                            }
                        } else {
                            Text("Are you sure you want to delete this category?")
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDeleteCategory(selectedReassignId) },
                        enabled = count == 0 || selectedReassignId != null
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteCategory() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionListItem(
    transaction: Transaction,
    category: Category?,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (transaction.type) {
                    TransactionType.INCOME -> Icons.Default.ArrowDownward
                    TransactionType.EXPENSE -> Icons.Default.ArrowUpward
                    TransactionType.SAVINGS_DEPOSIT, TransactionType.SAVINGS_WITHDRAWAL -> Icons.Default.Savings
                }
                val iconColor = when (transaction.type) {
                    TransactionType.INCOME -> GreenIncome
                    TransactionType.EXPENSE -> RedExpense
                    TransactionType.SAVINGS_DEPOSIT, TransactionType.SAVINGS_WITHDRAWAL -> MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = transaction.note.ifBlank {
                            category?.name ?: transaction.type.name.replace("_", " ")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        if (category != null) {
                            Text(
                                text = "${category.name} • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DateFormatter.formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> GreenIncome
                    TransactionType.EXPENSE -> RedExpense
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Text(
                    text = CurrencyFormatter.formatCents(
                        transaction.amount,
                        currencySymbol,
                        includeSign = transaction.type == TransactionType.INCOME
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    editingTransaction: Transaction?,
    categories: List<Category>,
    currencySymbol: String,
    availableBalance: Long,
    onDismiss: () -> Unit,
    onSave: (amount: Long, type: TransactionType, categoryId: Long?, note: String) -> Unit
) {
    var amountText by remember {
        mutableStateOf(
            if (editingTransaction != null) {
                String.format("%.2f", editingTransaction.amount / 100.0)
            } else ""
        )
    }
    var selectedType by remember {
        mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE)
    }
    val filteredCategories = categories.filter { it.type.name == selectedType.name }
    var selectedCategory by remember {
        mutableStateOf(
            editingTransaction?.categoryId?.let { catId -> categories.find { it.id == catId } }
                ?: filteredCategories.firstOrNull()
        )
    }
    var note by remember {
        mutableStateOf(editingTransaction?.note ?: "")
    }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingTransaction == null) "Add Transaction" else "Edit Transaction")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = categories.firstOrNull { it.type == CategoryType.EXPENSE }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense")
                    }
                    SegmentedButton(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = {
                            selectedType = TransactionType.INCOME
                            selectedCategory = categories.firstOrNull { it.type == CategoryType.INCOME }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income")
                    }
                }

                // Amount text field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    prefix = { Text(currencySymbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                val catList = categories.filter {
                    if (selectedType == TransactionType.INCOME) it.type == CategoryType.INCOME else it.type == CategoryType.EXPENSE
                }
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        catList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Note
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
                    val cents = CurrencyFormatter.parseToCents(amountText)
                    if (cents != null && cents > 0L) {
                        onSave(cents, selectedType, selectedCategory?.id, note.trim())
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

@Composable
fun CategoryManagementDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onCreateCategory: (String, CategoryType, String) -> Unit,
    onDeleteCategory: (Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryType by remember { mutableStateOf(CategoryType.EXPENSE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Categories") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add new category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("New Category") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onCreateCategory(newCategoryName, newCategoryType, "#2E7D32")
                                newCategoryName = ""
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                // Existing categories list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cat.name} (${cat.type.name})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { onDeleteCategory(cat) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
