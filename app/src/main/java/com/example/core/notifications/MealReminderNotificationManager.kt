package com.example.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.Locale

enum class MealSlotType(
    val id: String,
    val displayName: String,
    val bengaliName: String,
    val notificationId: Int,
    val defaultHour: Int,
    val defaultMinute: Int,
    val emoji: String,
    val defaultPrompt: String
) {
    BREAKFAST(
        id = "Breakfast",
        displayName = "Breakfast",
        bengaliName = "সকালের নাস্তা",
        notificationId = 4101,
        defaultHour = 8,
        defaultMinute = 30,
        emoji = "🍳",
        defaultPrompt = "Good morning! Don't forget to scan or log your breakfast to start your nutrition streak strong."
    ),
    LUNCH(
        id = "Lunch",
        displayName = "Lunch",
        bengaliName = "দুপুরের খাবার",
        notificationId = 4102,
        defaultHour = 13,
        defaultMinute = 15,
        emoji = "🍛",
        defaultPrompt = "It's lunch time! Snap a quick photo or log your midday meal to stay on top of your calorie & macro goals."
    ),
    SNACK(
        id = "Snack",
        displayName = "Evening Snack",
        bengaliName = "বিকেলের নাস্তা",
        notificationId = 4103,
        defaultHour = 17,
        defaultMinute = 0,
        emoji = "🍵",
        defaultPrompt = "Having tea or an evening snack? Log it in NutriScan to keep your daily macros accurate."
    ),
    DINNER(
        id = "Dinner",
        displayName = "Dinner",
        bengaliName = "রাতের খাবার",
        notificationId = 4104,
        defaultHour = 20,
        defaultMinute = 30,
        emoji = "🍲",
        defaultPrompt = "Dinner time! Log your evening meal to complete today's calorie and macronutrient diary."
    );

    companion object {
        fun fromId(id: String): MealSlotType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: BREAKFAST
        }
    }
}

data class MealSlotSchedule(
    val slot: MealSlotType,
    val enabled: Boolean,
    val hourOfDay: Int,
    val minute: Int
) {
    fun formatted12Hour(): String {
        val amPm = if (hourOfDay < 12) "AM" else "PM"
        val hour12 = when {
            hourOfDay == 0 -> 12
            hourOfDay > 12 -> hourOfDay - 12
            else -> hourOfDay
        }
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    fun formatted24Hour(): String {
        return String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
    }
}

data class MealReminderSchedule(
    val masterEnabled: Boolean = true,
    val breakfastEnabled: Boolean = true,
    val breakfastHour: Int = 8,
    val breakfastMinute: Int = 30,
    val lunchEnabled: Boolean = true,
    val lunchHour: Int = 13,
    val lunchMinute: Int = 15,
    val snackEnabled: Boolean = false,
    val snackHour: Int = 17,
    val snackMinute: Int = 0,
    val dinnerEnabled: Boolean = true,
    val dinnerHour: Int = 20,
    val dinnerMinute: Int = 30,
    val smartSkipLoggedMeals: Boolean = true
) {
    fun allSlots(): List<MealSlotSchedule> = listOf(
        MealSlotSchedule(MealSlotType.BREAKFAST, breakfastEnabled, breakfastHour, breakfastMinute),
        MealSlotSchedule(MealSlotType.LUNCH, lunchEnabled, lunchHour, lunchMinute),
        MealSlotSchedule(MealSlotType.SNACK, snackEnabled, snackHour, snackMinute),
        MealSlotSchedule(MealSlotType.DINNER, dinnerEnabled, dinnerHour, dinnerMinute)
    )

    fun updateSlot(
        slot: MealSlotType,
        enabled: Boolean? = null,
        hourOfDay: Int? = null,
        minute: Int? = null
    ): MealReminderSchedule {
        return when (slot) {
            MealSlotType.BREAKFAST -> copy(
                breakfastEnabled = enabled ?: breakfastEnabled,
                breakfastHour = (hourOfDay ?: breakfastHour).coerceIn(0, 23),
                breakfastMinute = (minute ?: breakfastMinute).coerceIn(0, 59)
            )
            MealSlotType.LUNCH -> copy(
                lunchEnabled = enabled ?: lunchEnabled,
                lunchHour = (hourOfDay ?: lunchHour).coerceIn(0, 23),
                lunchMinute = (minute ?: lunchMinute).coerceIn(0, 59)
            )
            MealSlotType.SNACK -> copy(
                snackEnabled = enabled ?: snackEnabled,
                snackHour = (hourOfDay ?: snackHour).coerceIn(0, 23),
                snackMinute = (minute ?: snackMinute).coerceIn(0, 59)
            )
            MealSlotType.DINNER -> copy(
                dinnerEnabled = enabled ?: dinnerEnabled,
                dinnerHour = (hourOfDay ?: dinnerHour).coerceIn(0, 23),
                dinnerMinute = (minute ?: dinnerMinute).coerceIn(0, 59)
            )
        }
    }

    /**
     * Computes the next upcoming enabled meal reminder relative to the current time.
     */
    fun nextUpcomingSlot(now: Calendar = Calendar.getInstance()): Pair<MealSlotSchedule, String>? {
        if (!masterEnabled) return null
        val enabledSlots = allSlots().filter { it.enabled }
        if (enabledSlots.isEmpty()) return null

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayUpcoming = enabledSlots
            .map { slot -> slot to (slot.hourOfDay * 60 + slot.minute) }
            .filter { (_, slotMinutes) -> slotMinutes > nowMinutes }
            .minByOrNull { (_, slotMinutes) -> slotMinutes }

        if (todayUpcoming != null) {
            val diffMin = todayUpcoming.second - nowMinutes
            val hours = diffMin / 60
            val mins = diffMin % 60
            val relativeText = if (hours > 0) "in ${hours}h ${mins}m" else "in ${mins}m"
            return todayUpcoming.first to "Today at ${todayUpcoming.first.formatted12Hour()} ($relativeText)"
        }

        // Otherwise, first enabled slot tomorrow morning
        val tomorrowFirst = enabledSlots.minByOrNull { it.hourOfDay * 60 + it.minute } ?: return null
        return tomorrowFirst to "Tomorrow at ${tomorrowFirst.formatted12Hour()}"
    }
}

object MealReminderNotificationManager {
    const val CHANNEL_ID = "nutriscan_meal_reminders_channel"
    private const val PREFS_NAME = "nutriscan_meal_reminders_prefs"
    const val ACTION_TRIGGER_MEAL_REMINDER = "com.example.nutriscan.ACTION_TRIGGER_MEAL_REMINDER"
    const val EXTRA_MEAL_SLOT_ID = "extra_meal_slot_id"
    const val EXTRA_OPEN_MEAL_SLOT = "extra_open_meal_slot"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Meal Logging Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to scan or log Breakfast, Lunch, Snacks, and Dinner according to your saved schedule."
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun loadSchedule(context: Context): MealReminderSchedule {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MealReminderSchedule(
            masterEnabled = prefs.getBoolean("master_enabled", true),
            breakfastEnabled = prefs.getBoolean("breakfast_enabled", true),
            breakfastHour = prefs.getInt("breakfast_hour", MealSlotType.BREAKFAST.defaultHour),
            breakfastMinute = prefs.getInt("breakfast_minute", MealSlotType.BREAKFAST.defaultMinute),
            lunchEnabled = prefs.getBoolean("lunch_enabled", true),
            lunchHour = prefs.getInt("lunch_hour", MealSlotType.LUNCH.defaultHour),
            lunchMinute = prefs.getInt("lunch_minute", MealSlotType.LUNCH.defaultMinute),
            snackEnabled = prefs.getBoolean("snack_enabled", false),
            snackHour = prefs.getInt("snack_hour", MealSlotType.SNACK.defaultHour),
            snackMinute = prefs.getInt("snack_minute", MealSlotType.SNACK.defaultMinute),
            dinnerEnabled = prefs.getBoolean("dinner_enabled", true),
            dinnerHour = prefs.getInt("dinner_hour", MealSlotType.DINNER.defaultHour),
            dinnerMinute = prefs.getInt("dinner_minute", MealSlotType.DINNER.defaultMinute),
            smartSkipLoggedMeals = prefs.getBoolean("smart_skip_logged_meals", true)
        )
    }

    fun saveAndSyncSchedule(context: Context, schedule: MealReminderSchedule) {
        val appCtx = context.applicationContext
        appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("master_enabled", schedule.masterEnabled)
            .putBoolean("breakfast_enabled", schedule.breakfastEnabled)
            .putInt("breakfast_hour", schedule.breakfastHour)
            .putInt("breakfast_minute", schedule.breakfastMinute)
            .putBoolean("lunch_enabled", schedule.lunchEnabled)
            .putInt("lunch_hour", schedule.lunchHour)
            .putInt("lunch_minute", schedule.lunchMinute)
            .putBoolean("snack_enabled", schedule.snackEnabled)
            .putInt("snack_hour", schedule.snackHour)
            .putInt("snack_minute", schedule.snackMinute)
            .putBoolean("dinner_enabled", schedule.dinnerEnabled)
            .putInt("dinner_hour", schedule.dinnerHour)
            .putInt("dinner_minute", schedule.dinnerMinute)
            .putBoolean("smart_skip_logged_meals", schedule.smartSkipLoggedMeals)
            .putLong("updated_at_ms", System.currentTimeMillis())
            .apply()

        syncAllAlarms(appCtx, schedule)
    }

    /**
     * Records that a meal type was logged for a specific dateKey ("yyyy-MM-dd") so smart reminders
     * can know whether the user has already logged that meal today.
     */
    fun markMealLoggedForDate(context: Context, dateKey: String, mealType: String) {
        val normalized = mealType.trim().lowercase(Locale.US)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("logged_${dateKey}_$normalized", true)
            .apply()
    }

    fun isMealAlreadyLoggedToday(context: Context, dateKey: String, slot: MealSlotType): Boolean {
        val normalized = slot.id.lowercase(Locale.US)
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("logged_${dateKey}_$normalized", false)
    }

    /**
     * Schedules or cancels daily alarms using AlarmManager.setWindow / setInexactRepeating
     * (compliant with Google Play policy — no SCHEDULE_EXACT_ALARM permission required).
     */
    fun syncAllAlarms(context: Context, schedule: MealReminderSchedule = loadSchedule(context)) {
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        schedule.allSlots().forEach { slotSchedule ->
            val pendingIntent = buildAlarmPendingIntent(context, slotSchedule.slot)
            alarmManager.cancel(pendingIntent)

            if (schedule.masterEnabled && slotSchedule.enabled) {
                val triggerAtMillis = computeNextTriggerTimeMillis(
                    hourOfDay = slotSchedule.hourOfDay,
                    minute = slotSchedule.minute
                )
                // Use setWindow (10-minute window) for reliable daily delivery without exact-alarm permission
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    10 * 60 * 1000L,
                    pendingIntent
                )
            }
        }
    }

    fun computeNextTriggerTimeMillis(
        hourOfDay: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= nowMillis + 5_000L) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private fun buildAlarmPendingIntent(context: Context, slot: MealSlotType): PendingIntent {
        val intent = Intent(context, MealReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_MEAL_REMINDER
            putExtra(EXTRA_MEAL_SLOT_ID, slot.id)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Posts the push notification to the Android system status bar.
     * Returns true if the notification was posted, or false if permission is not yet granted.
     */
    fun showMealReminderNotification(
        context: Context,
        slot: MealSlotType,
        isTestNotification: Boolean = false
    ): Boolean {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) {
            return false
        }

        val schedule = loadSchedule(context)
        val slotSchedule = schedule.allSlots().firstOrNull { it.slot == slot }
        val timeFormatted = slotSchedule?.formatted12Hour() ?: ""

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MEAL_SLOT, slot.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            slot.notificationId + 100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isTestNotification) {
            "${slot.emoji} ${slot.displayName} Reminder (${timeFormatted.ifBlank { "Scheduled" }})"
        } else {
            "${slot.emoji} Time to Log ${slot.displayName}! ($timeFormatted)"
        }

        val bodyText = slot.defaultPrompt

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$bodyText\nTap to scan your plate with AI or log from the food database.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_camera,
                "Scan ${slot.displayName}",
                contentPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_edit,
                "Log in Diary",
                contentPendingIntent
            )
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(slot.notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

/**
 * BroadcastReceiver invoked by AlarmManager when a scheduled meal reminder time arrives.
 */
class MealReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val slotId = intent?.getStringExtra(MealReminderNotificationManager.EXTRA_MEAL_SLOT_ID) ?: return
        val slot = MealSlotType.fromId(slotId)
        val schedule = MealReminderNotificationManager.loadSchedule(context)

        if (schedule.masterEnabled) {
            val todayKey = com.example.model.todayDateKey()
            val alreadyLogged = MealReminderNotificationManager.isMealAlreadyLoggedToday(context, todayKey, slot)
            if (!(schedule.smartSkipLoggedMeals && alreadyLogged)) {
                MealReminderNotificationManager.showMealReminderNotification(
                    context = context,
                    slot = slot,
                    isTestNotification = false
                )
            }
        }

        // Re-schedule tomorrow's window alarm for all active slots
        MealReminderNotificationManager.syncAllAlarms(context, schedule)
    }
}

/**
 * Restores scheduled meal reminders automatically when the device boots up.
 */
class MealReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val schedule = MealReminderNotificationManager.loadSchedule(context)
            MealReminderNotificationManager.syncAllAlarms(context, schedule)
        }
    }
}
