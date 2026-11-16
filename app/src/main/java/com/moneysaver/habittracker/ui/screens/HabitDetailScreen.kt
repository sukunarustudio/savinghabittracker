package com.moneysaver.habittracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneysaver.habittracker.domain.model.habit.FrequencyType
import com.moneysaver.habittracker.domain.model.habit.Habit
import com.moneysaver.habittracker.ui.theme.AmberTertiary
import com.moneysaver.habittracker.ui.theme.GreenIncome
import com.moneysaver.habittracker.ui.util.DateFormatter
import com.moneysaver.habittracker.ui.viewmodel.HabitViewModel
import com.moneysaver.habittracker.ui.viewmodel.HeatmapDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habitId: String,
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val detail = state.selectedHabitDetail

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.habit?.name ?: "Habit Detail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = { viewModel.openEditSchedule(detail.habit) }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Schedule")
                        }
                        IconButton(onClick = { viewModel.promptArchiveHabit(detail.habit) }) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive Habit")
                        }
                        IconButton(onClick = { viewModel.promptDeleteHabit(detail.habit) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Habit",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Habit not found or loading...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val habit = detail.habit
            val streak = detail.streakInfo

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    HabitHeaderCard(habit = habit)
                }

                item {
                    StreakStatsRow(
                        currentStreak = streak.currentStreak,
                        longestStreak = streak.longestStreak,
                        totalCompletions = detail.totalCompletions
                    )
                }

                item {
                    HeatmapCard(heatmapDays = detail.heatmapDays)
                }

                item {
                    HabitScheduleInfoCard(habit = habit)
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Edit Schedule Dialog
        if (state.isEditScheduleOpen && state.habitToEdit != null) {
            val habit = state.habitToEdit!!
            var frequencyType by remember { mutableStateOf(habit.schedule.frequencyType) }
            var targetDays by remember { mutableStateOf(habit.schedule.targetDays) }
            var reminderTime by remember { mutableStateOf(habit.schedule.reminderTime) }

            AlertDialog(
                onDismissRequest = { viewModel.closeEditSchedule() },
                title = { Text("Edit Habit Schedule") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Updating schedule applies prospectively from today. Historical streaks are preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = frequencyType == FrequencyType.DAILY,
                                onClick = { frequencyType = FrequencyType.DAILY },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("Daily")
                            }
                            SegmentedButton(
                                selected = frequencyType == FrequencyType.CUSTOM_DAYS,
                                onClick = { frequencyType = FrequencyType.CUSTOM_DAYS },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("Custom Days")
                            }
                        }

                        if (frequencyType == FrequencyType.CUSTOM_DAYS) {
                            Text(text = "Scheduled Days", style = MaterialTheme.typography.labelSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val days = listOf(
                                    DayOfWeek.MONDAY to "M",
                                    DayOfWeek.TUESDAY to "T",
                                    DayOfWeek.WEDNESDAY to "W",
                                    DayOfWeek.THURSDAY to "T",
                                    DayOfWeek.FRIDAY to "F",
                                    DayOfWeek.SATURDAY to "S",
                                    DayOfWeek.SUNDAY to "S"
                                )
                                days.forEach { (day, label) ->
                                    FilterChip(
                                        selected = targetDays.contains(day),
                                        onClick = {
                                            targetDays = if (targetDays.contains(day)) targetDays - day else targetDays + day
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateSchedule(frequencyType, targetDays, reminderTime)
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.closeEditSchedule() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Archive confirmation dialog
        if (state.showArchiveConfirm && state.habitTargetForAction != null) {
            val habit = state.habitTargetForAction!!
            AlertDialog(
                onDismissRequest = { viewModel.cancelArchiveHabit() },
                title = { Text("Archive Habit") },
                text = {
                    Text("Are you sure you want to archive '${habit.name}'? It will no longer appear on future daily checklists.")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmArchiveHabit() }) {
                        Text("Archive")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelArchiveHabit() }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (state.showDeleteConfirm && state.habitTargetForAction != null) {
            val habit = state.habitTargetForAction!!
            AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteHabit() },
                title = { Text("Delete Habit") },
                text = {
                    Text("Are you sure you want to permanently delete '${habit.name}' and all associated completion history?")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDeleteHabit() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDeleteHabit() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun HabitHeaderCard(habit: Habit, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (habit.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = habit.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (habit.schedule.frequencyType == FrequencyType.DAILY) "Daily" else "Custom Days",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                if (habit.schedule.reminderTime != null) {
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            Text(
                                text = DateFormatter.formatTime(habit.schedule.reminderTime),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StreakStatsRow(
    currentStreak: Int,
    longestStreak: Int,
    totalCompletions: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = AmberTertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Current Streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AmberTertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$longestStreak",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Best Streak",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalCompletions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GreenIncome
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total Days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun HeatmapCard(
    heatmapDays: List<HeatmapDay>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Streak Heatmap (Last 10 Weeks)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 10 columns x 7 rows grid
            val columns = heatmapDays.chunked(7)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                columns.forEach { week ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { day ->
                            val color = when {
                                day.isCompleted -> GreenIncome
                                day.isDue -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GreenIncome)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Completed", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Missed", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Rest Day", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun HabitScheduleInfoCard(habit: Habit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Schedule Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Created On: ${DateFormatter.formatLocalDate(habit.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val daysText = if (habit.schedule.frequencyType == FrequencyType.DAILY) {
                "Every day"
            } else {
                habit.schedule.targetDays.joinToString(", ") { it.name.take(3) }
            }
            Text(
                text = "Target Days: $daysText",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (habit.isArchived) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: Archived (on ${habit.archivedAt?.let { DateFormatter.formatLocalDate(it) }})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
