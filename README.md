# HabitHarmony

A modern Android habit tracker built with Jetpack Compose and Material 3. Track daily habits, build streaks, gain insights, and unlock premium analytics — all with a polished, accessible UI.

## Features

### Core (Free)
- **Daily Habit Tracking** — One-tap completion with emoji identifiers, animated row state, and streak counters
- **Habit Management** — Create, edit, archive, and delete habits; set frequency (daily, weekdays, weekends, specific days) and category
- **Streak & Stats** — Current streak, best streak, and 28-day completion grid per habit
- **Insights Dashboard** — Completion rate, today's progress, and per-habit streak breakdown
- **Free Tier** — Up to 5 habits (premium unlocks unlimited)
- **Material You Theming** — Dynamic color on Android 12+; full dark/light mode with in-app toggle

### Premium (In-App Purchase)
- Unlimited habits
- Advanced analytics: 7-day completion trend chart and habit correlations
- Cloud backup & sync across devices
- Custom reminders & scheduling per habit
- Data export (CSV/PDF)
- Custom app themes
- Priority support & all future features

### Monetization
- Google Play Billing 7.1.1 (subscriptions + one-time purchases)
- Plans: Weekly, Monthly, Yearly (best value — 60% off), Lifetime
- Tip Jar one-time IAP
- Restore purchases support
- Graceful offline fallback with retry

### Accessibility
- Content descriptions and semantic roles (`Role.Button`, `Role.RadioButton`, `Role.Switch`) on all interactive elements
- Full-row toggleable touch targets for settings switches
- 28-day completion grid with per-cell screen-reader labels (completed / missed)
- Weekly chart bars with percentage descriptions for TalkBack
- Paywall tier cards fully described for screen readers
- Empty states with descriptive messaging on all screens
- Inline name validation error on the Add/Edit habit form

### Polish
- Haptic feedback on all interactive elements — `LongPress` for primary actions, `TextHandleMove` for selections and navigation
- Keyboard IME actions in the habit form: Next moves focus to description, Done dismisses the keyboard
- Edge-to-edge display with proper system bar handling via `WindowInsets`
- Animated card selection states and color-to-completed transitions
- Loading indicators: splash screen on launch, `CircularProgressIndicator` while habit detail loads
- Animated visibility for the specific-days picker

## Requirements

| | |
|---|---|
| Min Android | 7.0 (API 24) |
| Target / Compile SDK | 34 (Android 14) |
| Android Studio | Hedgehog (2023.1.1) or later |
| JDK | 11 or later |
| Kotlin | 2.0.0 |
| AGP | 8.2.2 |

## Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/HabitHarmony.git
   cd HabitHarmony
   ```

2. Open the project in Android Studio and let Gradle sync.

3. Build a debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

4. Install on a connected device or running emulator:
   ```bash
   ./gradlew installDebug
   ```

5. Build a release APK (requires a signing config in `app/build.gradle.kts`):
   ```bash
   ./gradlew assembleRelease
   ```

> **Google Play Billing**: The app uses real product IDs defined in `BillingManager`. For local testing without Play Store setup, the billing UI renders with fallback prices and a graceful offline error state.

## Project Structure

```
app/src/main/java/com/factory/habitharmony/
├── HabitHarmonyApp.kt              # Application class; initialises Room, BillingManager, PremiumManager
├── MainActivity.kt                 # Single activity; edge-to-edge, splash screen, notification permission, dark mode state
├── billing/
│   ├── BillingManager.kt           # Google Play Billing — subscriptions + IAP, event channel
│   └── PremiumManager.kt           # Premium state backed by SharedPreferences
├── data/
│   ├── dao/HabitDao.kt             # Room DAO — habits and completions queries
│   ├── database/HabitDatabase.kt   # Room database definition (v1)
│   ├── entity/
│   │   ├── HabitEntity.kt          # Habit row; emoji, color, category, frequency, targetDays
│   │   └── HabitCompletionEntity.kt# Completion record; FK → HabitEntity with cascade delete
│   └── repository/HabitRepository.kt # Single source of truth; streak computation
├── navigation/
│   └── AppNavigation.kt            # Bottom nav + NavHost; routes: habits, insights, settings, paywall, add/edit/detail
├── ui/
│   ├── components/
│   │   └── PremiumComponents.kt    # ProBadge, PremiumFeatureGate overlay, PremiumUpgradeCard
│   ├── screen/
│   │   ├── HabitsScreen.kt         # Today's habits list, progress card, empty state, FAB
│   │   ├── AddEditHabitScreen.kt   # Habit form — name, emoji, color, category, frequency, days picker
│   │   ├── HabitDetailScreen.kt    # Streak stats, 28-day completion grid, recent completions
│   │   ├── InsightsScreen.kt       # Stats dashboard; free overview + premium analytics gate
│   │   ├── PaywallScreen.kt        # Subscription/purchase flow with tier selection
│   │   └── SettingsScreen.kt       # Notifications, dark mode toggle, premium features, about
│   └── theme/
│       ├── Color.kt                # Full colour palette — primary indigo, premium gold, dark surfaces
│       ├── Theme.kt                # Material 3 theme with dynamic colour (API 31+)
│       └── Type.kt                 # Typography (Material 3 defaults)
└── viewmodel/
    └── HabitViewModel.kt           # HabitsUiState + InsightsUiState; toggleCompletion, CRUD operations
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose with Material 3 |
| Navigation | Navigation Compose 2.7.7 |
| Database | Room 2.6.1 with KSP |
| Preferences | SharedPreferences (settings) |
| Billing | Google Play Billing KTX 7.1.1 |
| Async | Kotlin Coroutines + Flow |
| State | ViewModel + StateFlow |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 34 (Android 14) |

## License

All rights reserved.
