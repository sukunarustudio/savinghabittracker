# Money Saver & Habit Tracker (Android)

A modern, offline-first native Android application combining financial cash flow tracking & savings goals with daily routine checklists and habit streak analytics.

## Tech Stack
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
- **Local Persistence**: Room Database (SQLite) + DataStore Preferences
- **Asynchronous & Reactive**: Kotlin Coroutines & Flow (`StateFlow` / `SharedFlow`)
- **System Services**: Android AlarmManager (`setExactAndAllowWhileIdle` with graceful fallback) & NotificationManager

## Key Features
1. **Unified Dashboard**: Live net available balance, periodic income/expense cash flow, and today's scheduled habit completion progress.
2. **Money Saver**:
   - Minor unit (`Long`, cents) precision math preventing floating point rounding errors.
   - Non-negative available balance & running balance overdraft prevention.
   - Authoritative linked ledger for internal transfers (`SAVINGS_DEPOSIT`, `SAVINGS_WITHDRAWAL`).
   - Category management with safe reassignment before deletion.
   - Savings goals with target deadlines, progress tracking, and balance refund / realized expense handling upon deletion.
3. **Habit Tracker**:
   - Daily and custom-day recurring schedules with prospective schedule modification.
   - Exact & idle-safe reminder alarms with boot / timezone rescheduling receivers.
   - Habit checklist with untoggle support, idempotent completion logging, and streak calculation engine.
