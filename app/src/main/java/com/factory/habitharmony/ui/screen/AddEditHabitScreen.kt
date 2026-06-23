package com.factory.habitharmony.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.habitharmony.data.entity.EMOJI_OPTIONS
import com.factory.habitharmony.data.entity.HABIT_COLORS
import com.factory.habitharmony.data.entity.HabitCategory
import com.factory.habitharmony.data.entity.HabitEntity
import com.factory.habitharmony.data.entity.HabitFrequency
import com.factory.habitharmony.viewmodel.HabitViewModel
import kotlinx.coroutines.launch

private val DAY_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditHabitScreen(
    viewModel: HabitViewModel,
    habitId: Long?,
    paddingValues: PaddingValues,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = habitId != null
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val descriptionFocusRequester = remember { FocusRequester() }

    // Form state
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedEmoji by rememberSaveable { mutableStateOf(EMOJI_OPTIONS.first()) }
    var selectedColor by rememberSaveable { mutableStateOf(HABIT_COLORS.first()) }
    var selectedCategory by rememberSaveable { mutableStateOf(HabitCategory.OTHER) }
    var selectedFrequency by rememberSaveable { mutableStateOf(HabitFrequency.DAILY) }
    var selectedDays by rememberSaveable { mutableStateOf(setOf(0, 1, 2, 3, 4, 5, 6)) }
    var nameTouched by rememberSaveable { mutableStateOf(false) }
    val nameError = nameTouched && name.isBlank()

    // Load existing habit if editing
    LaunchedEffect(habitId) {
        if (habitId != null) {
            val habit = viewModel.getHabitById(habitId)
            if (habit != null) {
                name = habit.name
                description = habit.description
                selectedEmoji = habit.emoji
                selectedColor = habit.colorHex
                selectedCategory = habit.category
                selectedFrequency = habit.frequency
                selectedDays = habit.getTargetDaysList().toSet()
            }
        }
    }

    val canSave = name.isNotBlank()

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Habit" else "New Habit",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Name ─────────────────────────────────────────────────────────
            SectionLabel("Habit Name")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameTouched = true },
                placeholder = { Text("e.g. Morning meditation") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Habit name is required") }
                } else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { descriptionFocusRequester.requestFocus() }
                )
            )

            // ── Description ───────────────────────────────────────────────────
            SectionLabel("Description (optional)")
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Why does this habit matter to you?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester),
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            // ── Emoji ─────────────────────────────────────────────────────────
            SectionLabel("Pick an Emoji")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EMOJI_OPTIONS) { emoji ->
                    val isEmojiSelected = emoji == selectedEmoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEmojiSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedEmoji = emoji
                            }
                            .semantics {
                                role = Role.Button
                                contentDescription = "$emoji emoji${if (isEmojiSelected) ", selected" else ""}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }

            // ── Color ─────────────────────────────────────────────────────────
            SectionLabel("Color")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(HABIT_COLORS) { colorLong ->
                    val color = Color(colorLong)
                    val isColorSelected = colorLong == selectedColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isColorSelected)
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedColor = colorLong
                            }
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = "Color option${if (isColorSelected) ", selected" else ""}"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isColorSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Category ──────────────────────────────────────────────────────
            SectionLabel("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = cat == selectedCategory,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategory = cat
                        },
                        label = { Text("${cat.emoji} ${cat.displayName}") }
                    )
                }
            }

            // ── Frequency ─────────────────────────────────────────────────────
            SectionLabel("Frequency")
            val freqOptions = HabitFrequency.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                freqOptions.forEachIndexed { idx, freq ->
                    SegmentedButton(
                        selected = freq == selectedFrequency,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFrequency = freq
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = idx,
                            count = freqOptions.size
                        ),
                        label = { Text(freq.displayName, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            // ── Specific day picker ───────────────────────────────────────────
            AnimatedVisibility(
                visible = selectedFrequency == HabitFrequency.SPECIFIC_DAYS,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    SectionLabel("Which days?")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DAY_LABELS.forEachIndexed { idx, day ->
                            val isSelected = idx in selectedDays
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedDays = if (isSelected) selectedDays - idx else selectedDays + idx
                                    }
                                    .semantics {
                                        role = Role.Button
                                        contentDescription = "$day${if (isSelected) ", selected" else ""}"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Save ──────────────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val targetDaysStr = when (selectedFrequency) {
                            HabitFrequency.DAILY -> "0,1,2,3,4,5,6"
                            HabitFrequency.WEEKDAYS -> "1,2,3,4,5"
                            HabitFrequency.WEEKENDS -> "0,6"
                            HabitFrequency.SPECIFIC_DAYS ->
                                selectedDays.sorted().joinToString(",")
                        }
                        val habit = if (isEditing) {
                            val existing = viewModel.getHabitById(habitId!!)
                            existing?.copy(
                                name = name.trim(),
                                description = description.trim(),
                                emoji = selectedEmoji,
                                colorHex = selectedColor,
                                category = selectedCategory,
                                frequency = selectedFrequency,
                                targetDays = targetDaysStr
                            ) ?: return@launch
                        } else {
                            HabitEntity(
                                name = name.trim(),
                                description = description.trim(),
                                emoji = selectedEmoji,
                                colorHex = selectedColor,
                                category = selectedCategory,
                                frequency = selectedFrequency,
                                targetDays = targetDaysStr
                            )
                        }
                        if (isEditing) viewModel.updateHabit(habit)
                        else viewModel.addHabit(habit)
                        onNavigateBack()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isEditing) "Save Changes" else "Create Habit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}
