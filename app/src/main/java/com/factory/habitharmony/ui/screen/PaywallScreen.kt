package com.factory.habitharmony.ui.screen

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.factory.habitharmony.billing.BillingEvent
import com.factory.habitharmony.billing.BillingManager
import com.factory.habitharmony.billing.InAppProductOffer
import com.factory.habitharmony.billing.PremiumManager
import com.factory.habitharmony.billing.SubscriptionOffer
import com.factory.habitharmony.ui.theme.PremiumGold
import com.factory.habitharmony.ui.theme.PremiumGradientEnd
import com.factory.habitharmony.ui.theme.PremiumGradientStart
import com.factory.habitharmony.ui.theme.Success
import com.factory.habitharmony.ui.theme.TextMuted
import com.factory.habitharmony.ui.theme.TextSecondary

private val TERMS_URL = "https://habitharmony.app/terms"
private val PRIVACY_URL = "https://habitharmony.app/privacy"

private val premiumFeatures = listOf(
    "Unlimited habit tracking",
    "Advanced analytics & insights",
    "Custom habit categories",
    "Habit streaks & milestones",
    "Cloud backup & sync",
    "Custom reminders & scheduling",
    "Export data (CSV/PDF)",
    "Priority support & all future features"
)

// Fallback prices if billing not connected
private val fallbackPrices = mapOf(
    BillingManager.SUB_WEEKLY to "$4.79/week",
    BillingManager.SUB_MONTHLY to "$11.99/month",
    BillingManager.SUB_YEARLY to "$57.60/year",
    BillingManager.SUB_LIFETIME to "$79.99 once",
    BillingManager.IAP_SMALL to "$2.24"
)

@Composable
fun PaywallScreen(
    billingManager: BillingManager,
    premiumManager: PremiumManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val hapticFeedback = LocalHapticFeedback.current
    val activity = context as? Activity

    val subscriptionOffers by billingManager.subscriptionOffers.collectAsState()
    val iapOffers by billingManager.iapOffers.collectAsState()
    val isConnected by billingManager.isConnected.collectAsState()
    val isLoading by billingManager.isLoading.collectAsState()
    val premiumState by premiumManager.premiumState.collectAsState()

    var selectedOffer by remember { mutableStateOf<Any?>(null) }

    // Auto-select yearly as default best value
    LaunchedEffect(subscriptionOffers) {
        if (selectedOffer == null && subscriptionOffers.isNotEmpty()) {
            selectedOffer = subscriptionOffers.find { it.productId == BillingManager.SUB_YEARLY }
                ?: subscriptionOffers.firstOrNull()
        }
    }

    // Handle billing events
    LaunchedEffect(Unit) {
        billingManager.billingEvents.collect { event ->
            when (event) {
                is BillingEvent.PurchaseComplete -> {
                    Toast.makeText(context, "Purchase successful! Welcome to Premium.", Toast.LENGTH_LONG).show()
                    onClose()
                }
                is BillingEvent.PurchaseError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is BillingEvent.PurchaseCancelled -> {
                    Toast.makeText(context, "Purchase cancelled.", Toast.LENGTH_SHORT).show()
                }
                is BillingEvent.PurchasePending -> {
                    Toast.makeText(context, "Purchase pending. You'll get access once payment completes.", Toast.LENGTH_LONG).show()
                }
                is BillingEvent.NoActiveSubscriptions -> { /* no-op in UI */ }
            }
        }
    }

    // If already premium, show active state
    if (premiumState.isPremium) {
        PremiumActiveScreen(premiumState.activeSubscriptionId, onClose)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close button
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClose()
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            }

            // Header
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Premium",
                tint = PremiumGold,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Unlock HabitHarmony PRO",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Build better habits with premium features",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Feature highlights
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    premiumFeatures.forEach { feature ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Included",
                                tint = Success,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Loading state
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp),
                    color = PremiumGold
                )
            } else if (!isConnected && subscriptionOffers.isEmpty()) {
                // Offline / no connection - show fallback UI
                Text(
                    text = "Unable to load prices. Please check your internet connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        billingManager.startConnection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumGold)
                ) {
                    Text("Retry", color = Color.Black)
                }
            } else {
                // Subscription tiers
                Text(
                    text = "Choose your plan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subscription options (weekly / monthly / yearly)
                subscriptionOffers.forEach { offer ->
                    SubscriptionTierCard(
                        offer = offer,
                        isSelected = selectedOffer == offer,
                        isBestValue = offer.productId == BillingManager.SUB_YEARLY,
                        fallbackPrice = fallbackPrices[offer.productId],
                        onClick = { selectedOffer = offer }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Lifetime as a tier card in the same section (it's an INAPP purchase)
                val lifetimeOffer = iapOffers.find { it.productId == BillingManager.SUB_LIFETIME }
                lifetimeOffer?.let { lifetime ->
                    LifetimeTierCard(
                        offer = lifetime,
                        isSelected = selectedOffer == lifetime,
                        fallbackPrice = fallbackPrices[lifetime.productId],
                        onClick = { selectedOffer = lifetime }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Remaining IAPs (tip jar etc.)
                val otherIapOffers = iapOffers.filter { it.productId != BillingManager.SUB_LIFETIME }
                if (otherIapOffers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Support the App",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    otherIapOffers.forEach { offer ->
                        IapCard(
                            offer = offer,
                            isSelected = selectedOffer == offer,
                            fallbackPrice = fallbackPrices[offer.productId],
                            onClick = { selectedOffer = offer }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Subscribe button
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (activity == null) return@Button
                        when (val selected = selectedOffer) {
                            is SubscriptionOffer -> billingManager.launchSubscriptionFlow(activity, selected)
                            is InAppProductOffer -> billingManager.launchInAppPurchaseFlow(activity, selected)
                            else -> { /* No offer selected; button should be disabled, no-op */ }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    enabled = selectedOffer != null
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PremiumGradientStart, PremiumGradientEnd)
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedOffer) {
                                is SubscriptionOffer -> "Subscribe Now"
                                is InAppProductOffer -> "Purchase"
                                else -> "Select a Plan"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Restore purchases
            TextButton(onClick = { billingManager.restorePurchases() }) {
                Text(
                    text = "Restore Purchases",
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auto-renewal disclaimer
            Text(
                text = "Subscriptions auto-renew unless cancelled at least 24 hours before the end of the current period. " +
                        "Manage subscriptions in Google Play Store settings.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Terms & Privacy
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Terms of Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(TERMS_URL) }
                )
                Text(
                    text = "  •  ",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(role = Role.Button) { uriHandler.openUri(PRIVACY_URL) }
                )
            }
        }
    }
}

@Composable
private fun SubscriptionTierCard(
    offer: SubscriptionOffer,
    isSelected: Boolean,
    isBestValue: Boolean,
    fallbackPrice: String?,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PremiumGold else Color.Transparent,
        label = "borderColor"
    )

    val displayName = when (offer.productId) {
        BillingManager.SUB_WEEKLY -> "Weekly"
        BillingManager.SUB_MONTHLY -> "Monthly"
        BillingManager.SUB_YEARLY -> "Yearly"
        BillingManager.SUB_LIFETIME -> "Lifetime"
        else -> offer.name
    }

    val periodLabel = when (offer.productId) {
        BillingManager.SUB_WEEKLY -> "/week"
        BillingManager.SUB_MONTHLY -> "/month"
        BillingManager.SUB_YEARLY -> "/year"
        BillingManager.SUB_LIFETIME -> " one-time"
        else -> ""
    }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(role = Role.RadioButton) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .semantics { contentDescription = "$displayName plan, ${offer.price.ifEmpty { fallbackPrice ?: "" }}$periodLabel${if (isSelected) ", selected" else ""}${if (isBestValue) ", best value" else ""}" },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    PremiumGold.copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection indicator
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            if (isSelected) PremiumGold else TextMuted,
                            CircleShape
                        )
                        .then(
                            if (isSelected) Modifier.background(PremiumGold, CircleShape)
                            else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Black, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (offer.productId == BillingManager.SUB_YEARLY) {
                        Text(
                            text = "Save 60%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Success,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = "${offer.price.ifEmpty { fallbackPrice ?: "" }}$periodLabel",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PremiumGold else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Best value badge
        if (isBestValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp)
                    .background(PremiumGold, RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "BEST VALUE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun LifetimeTierCard(
    offer: InAppProductOffer,
    isSelected: Boolean,
    fallbackPrice: String?,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PremiumGold else Color.Transparent,
        label = "borderColor"
    )

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable(role = Role.RadioButton) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .semantics {
                    contentDescription = "Lifetime plan, ${offer.price.ifEmpty { fallbackPrice ?: "" }} one-time${if (isSelected) ", selected" else ""}"
                },
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected)
                    PremiumGold.copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isSelected) PremiumGold else TextMuted, CircleShape)
                        .then(
                            if (isSelected) Modifier.background(PremiumGold, CircleShape) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Black, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Lifetime",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "One-time, never pay again",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${offer.price.ifEmpty { fallbackPrice ?: "" }} once",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PremiumGold else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Lifetime badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp)
                .background(
                    Brush.horizontalGradient(listOf(PremiumGradientStart, PremiumGradientEnd)),
                    RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = "LIFETIME",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun IapCard(
    offer: InAppProductOffer,
    isSelected: Boolean,
    fallbackPrice: String?,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PremiumGold else Color.Transparent,
        label = "borderColor"
    )

    val displayName = when (offer.productId) {
        BillingManager.IAP_SMALL -> "Tip Jar"
        else -> offer.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(role = Role.RadioButton) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "$displayName, ${offer.price.ifEmpty { fallbackPrice ?: "" }}${if (isSelected) ", selected" else ""}" },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                PremiumGold.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        if (isSelected) PremiumGold else TextMuted,
                        CircleShape
                    )
                    .then(
                        if (isSelected) Modifier.background(PremiumGold, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.Black, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "One-time purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = offer.price.ifEmpty { fallbackPrice ?: "" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PremiumGold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PremiumActiveScreen(
    activeSubscriptionId: String?,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Premium active",
                tint = PremiumGold,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You're a PRO member!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All premium features are unlocked.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (activeSubscriptionId != null) {
                val planName = when (activeSubscriptionId) {
                    BillingManager.SUB_WEEKLY -> "Weekly"
                    BillingManager.SUB_MONTHLY -> "Monthly"
                    BillingManager.SUB_YEARLY -> "Yearly"
                    BillingManager.SUB_LIFETIME -> "Lifetime"
                    else -> "Active"
                }
                Text(
                    text = "Plan: $planName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumGold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onClose) {
                Text("Continue")
            }
        }
    }
}
