package com.factory.habitharmony.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.factory.habitharmony.billing.PremiumManager
import com.factory.habitharmony.ui.components.PremiumFeatureGate
import com.factory.habitharmony.ui.components.ProBadge
import com.factory.habitharmony.ui.theme.PremiumGold
import com.factory.habitharmony.ui.theme.Success
import com.factory.habitharmony.ui.theme.TextSecondary
import com.factory.habitharmony.viewmodel.HabitViewModel

@Composable
fun InsightsScreen(
    viewModel: HabitViewModel,
    premiumManager: PremiumManager,
    paddingValues: PaddingValues,
    onNavigateToPaywall: () -> Unit
) {
    val state by viewModel.insightsState.collectAsState()
    val premiumState by premiumManager.premiumState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            "Insights",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        val hasData = state.weeklyTotal > 0

        if (!hasData) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Insights,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "No insights yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(
                    "Complete some habits to see your progress here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
                )
            }
        }

        // ── Free stats ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightStatCard(
                icon = Icons.Filled.LocalFireDepartment,
                iconTint = Color(0xFFFF6B35),
                label = "Current Streak",
                value = if (state.currentStreak > 0) "${state.currentStreak}d" else "—",
                modifier = Modifier.weight(1f)
            )
            InsightStatCard(
                icon = Icons.Filled.EmojiEvents,
                iconTint = Color(0xFFFFCA28),
                label = "Best Streak",
                value = if (state.bestStreak > 0) "${state.bestStreak}d" else "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InsightStatCard(
                icon = Icons.Filled.BarChart,
                label = "Today",
                value = if (state.weeklyTotal > 0) "${state.completionPercent}%" else "—",
                modifier = Modifier.weight(1f)
            )
            InsightStatCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = "Done Today",
                value = if (state.weeklyTotal > 0) "${state.weeklyCompleted}/${state.weeklyTotal}" else "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Per-habit breakdown ───────────────────────────────────────────────
        if (state.habitStats.isNotEmpty()) {
            Text(
                "Per-Habit Streaks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            state.habitStats.forEach { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.habit.emoji, style = MaterialTheme.typography.titleMedium)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                item.habit.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                item.habit.frequencyLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (item.currentStreak > 0) {
                                Text(
                                    "🔥 ${item.currentStreak}d streak",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFF6B35),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "${item.totalCompletions} total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Premium: Weekly trend ─────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Advanced Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ProBadge(modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(Modifier.height(8.dp))

        PremiumFeatureGate(
            isPremium = premiumState.isPremium,
            onShowPaywall = onNavigateToPaywall
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "7-Day Completion Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    val chartData = if (state.weeklyData.size == 7) state.weeklyData else List(7) { 0f }
                    val days = listOf("6d", "5d", "4d", "3d", "2d", "1d", "Today")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        chartData.forEachIndexed { i, ratio ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Card(
                                    modifier = Modifier
                                        .height((ratio * 72).coerceAtLeast(4f).dp)
                                        .fillMaxWidth(0.085f)
                                        .semantics {
                                            contentDescription = "${days[i]}: ${(ratio * 100).toInt()}% completion"
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (ratio >= 0.8f) Success
                                                         else MaterialTheme.colorScheme.primary
                                    )
                                ) {}
                                Text(
                                    days[i],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Premium: Habit correlations ───────────────────────────────────────
        PremiumFeatureGate(
            isPremium = premiumState.isPremium,
            onShowPaywall = onNavigateToPaywall
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Habit Correlations",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ProBadge(modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.habitStats.size >= 3)
                            "Keep completing habits daily to discover patterns. " +
                            "Correlations appear after tracking multiple habits over time."
                        else
                            "Add at least 3 habits and track them for a few days to unlock " +
                            "personalised correlation insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Premium: Export ───────────────────────────────────────────────────
        PremiumFeatureGate(
            isPremium = premiumState.isPremium,
            onShowPaywall = onNavigateToPaywall
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = PremiumGold)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Export Data",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            ProBadge(modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(
                            "Export your habit data as CSV or PDF",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InsightStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = label, tint = iconTint)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
