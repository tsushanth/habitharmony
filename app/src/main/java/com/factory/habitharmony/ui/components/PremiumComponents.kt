package com.factory.habitharmony.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.habitharmony.ui.theme.PremiumGold
import com.factory.habitharmony.ui.theme.PremiumGoldDark
import com.factory.habitharmony.ui.theme.PremiumGradientEnd
import com.factory.habitharmony.ui.theme.PremiumGradientStart
import com.factory.habitharmony.ui.theme.ProBadgeBackground
import com.factory.habitharmony.ui.theme.ProBadgeBorder

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ProBadgeBackground)
            .border(1.dp, ProBadgeBorder, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .semantics { contentDescription = "Premium feature" }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = "PRO",
                color = PremiumGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun PremiumFeatureGate(
    isPremium: Boolean,
    onShowPaywall: () -> Unit,
    modifier: Modifier = Modifier,
    lockedContent: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    if (isPremium) {
        content()
    } else {
        Box(
            modifier = modifier.clickable(role = Role.Button) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShowPaywall()
            }
        ) {
            if (lockedContent != null) {
                lockedContent()
            } else {
                content()
            }
            // Lock overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Premium feature",
                        tint = PremiumGold,
                        modifier = Modifier.size(28.dp)
                    )
                    ProBadge(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
fun PremiumUpgradeCard(
    isPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isPremium) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Premium active",
                    tint = PremiumGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Premium Active",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    } else {
        val hapticFeedback = LocalHapticFeedback.current
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            colors = CardDefaults.cardColors(containerColor = PremiumGoldDark.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                PremiumGradientStart.copy(alpha = 0.1f),
                                PremiumGradientEnd.copy(alpha = 0.1f)
                            )
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Upgrade to premium",
                    tint = PremiumGold,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        text = "Upgrade to Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                    Text(
                        text = "Unlock all features",
                        style = MaterialTheme.typography.bodySmall,
                        color = PremiumGoldDark
                    )
                }
                ProBadge()
            }
        }
    }
}
