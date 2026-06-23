package com.factory.habitharmony.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.habitharmony.data.entity.HabitCompletionEntity
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.repository.HabitRepository.Companion.daysAgoStart
import com.factory.habitharmony.data.repository.HabitRepository.Companion.todayStart
import com.factory.habitharmony.viewmodel.HabitViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitViewModel,
    habitId: Long,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit,
    onEditHabit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var habit by remember { mutableStateOf<HabitEntity?>(null) }
    var totalCompletions by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val completions by viewModel.getCompletionsForHabit(habitId).collectAsState(initial = emptyList())

    LaunchedEffect(habitId) {
        habit = viewModel.getHabitById(habitId)
    }

    LaunchedEffect(completions) {
        totalCompletions = completions.size
    }

    val h = habit
    if (h == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete \"${h.name}\"? All history will be lost.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            viewModel.deleteHabit(h)
                            onNavigateBack()
                        }
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val habitColor = Color(h.colorHex)

    // Compute current streak from completions
    val completedDates = completions.map { it.completedDate }.toSet()
    val currentStreak = remember(completions) { computeStreakLocal(completedDates, h) }
    val bestStreak = remember(completions) { computeBestStreakLocal(completedDates) }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text(h.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onEditHabit()
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit habit")
                    }
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteDialog = true
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete habit",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Header card ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = habitColor.copy(alpha = 0.12f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(habitColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(h.emoji, fontSize = 30.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            h.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${h.category.emoji} ${h.category.displayName}  •  ${h.frequencyLabel()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                        if (h.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                h.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    icon = { Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFF6B35), modifier = Modifier.size(20.dp)) },
                    label = "Current streak",
                    value = "$currentStreak days",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = { Icon(Icons.Default.Star, null, tint = Color(0xFFFFCA28), modifier = Modifier.size(20.dp)) },
                    label = "Best streak",
                    value = "$bestStreak days",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    icon = { Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
                    label = "Total done",
                    value = "$totalCompletions",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── 28-day grid ───────────────────────────────────────────────────
            Text(
                "Last 28 Days",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            CompletionGrid(
                completedDates = completedDates,
                color = habitColor,
                habit = h
            )

            // ── Recent completions ─────────────────────────────────────────────
            if (completions.isNotEmpty()) {
                Text(
                    "Recent Completions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                completions.take(10).forEach { completion ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(habitColor)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            fmt.format(Date(completion.completedDate)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatPill(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CompletionGrid(
    completedDates: Set<Long>,
    color: Color,
    habit: HabitEntity
) {
    val today = todayStart()
    // Build 4 weeks x 7 days grid, index 0 = 27 days ago
    val cells = (27 downTo 0).map { daysAgo ->
        val date = daysAgoStart(daysAgo)
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val scheduled = habit.isScheduledForDay(dow)
        val completed = completedDates.contains(date)
        Triple(date, scheduled, completed)
    }

    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    // Render as 4 rows of 7
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(4) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(7) { day ->
                    val idx = week * 7 + day
                    val (date, scheduled, completed) = cells[idx]
                    val bgColor = when {
                        completed -> color
                        scheduled -> MaterialTheme.colorScheme.surfaceVariant
                        else -> Color.Transparent
                    }
                    val cellLabel = when {
                        completed -> "Completed ${dateFmt.format(Date(date))}"
                        scheduled -> "Missed ${dateFmt.format(Date(date))}"
                        else -> null
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .then(
                                if (cellLabel != null)
                                    Modifier.semantics { contentDescription = cellLabel }
                                else Modifier
                            )
                    )
                }
            }
        }
        // Day labels
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { lbl ->
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

private fun computeStreakLocal(completedDates: Set<Long>, habit: HabitEntity): Int {
    if (completedDates.isEmpty()) return 0
    val cal = Calendar.getInstance()
    val todayMs = todayStart()
    var streak = 0
    cal.timeInMillis = todayMs
    if (!completedDates.contains(todayMs)) cal.add(Calendar.DAY_OF_YEAR, -1)
    repeat(100) {
        val dayMs = cal.timeInMillis
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        when {
            !habit.isScheduledForDay(dow) -> {}
            completedDates.contains(dayMs) -> streak++
            else -> return streak
        }
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun computeBestStreakLocal(completedDates: Set<Long>): Int {
    if (completedDates.isEmpty()) return 0
    val sorted = completedDates.sorted()
    if (sorted.size == 1) return 1
    var best = 1; var current = 1
    for (i in 1 until sorted.size) {
        if (sorted[i] - sorted[i - 1] == 86_400_000L) {
            current++; if (current > best) best = current
        } else { current = 1 }
    }
    return best
}
