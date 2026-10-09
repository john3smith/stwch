package com.local.stwch

import android.content.Context
import android.content.Intent

/** One local command receipt only; no caller package, URL, screen content or
 * usage history is collected. Helps distinguish OEM launch flags from app logic. */
object CommandDiagnostics {
    /** Bounded local logcat diagnostics: no arbitrary Intent extras/caller data. */
    fun trace(activity: android.app.Activity, event: String) {
        android.util.Log.d("stwch.Activity", "$event component=${activity.javaClass.simpleName}"
            + " instance=${System.identityHashCode(activity)} task=${activity.taskId}"
            + " root=${activity.isTaskRoot} finishing=${activity.isFinishing}"
            + " flags=0x${Integer.toHexString(activity.intent.flags)}"
            + " command=${WatchCommands.parse(activity.intent.action)?.name ?: "OPEN"}")
    }
    fun record(context: Context, action: WatchAction, intent: Intent) {
        context.getSharedPreferences("command_diagnostics", Context.MODE_PRIVATE).edit()
            .putString("action", action.name).putInt("flags", intent.flags)
            .putLong("time", System.currentTimeMillis()).apply()
    }
    fun describe(context: Context): String {
        val prefs = context.getSharedPreferences("command_diagnostics", Context.MODE_PRIVATE)
        val action = prefs.getString("action", null) ?: return context.getString(R.string.command_diagnostics_empty)
        val flags = prefs.getInt("flags", 0)
        val time = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.ROOT)
            .format(java.util.Date(prefs.getLong("time", 0)))
        return context.getString(R.string.command_diagnostics_detail, action, time,
            "0x" + Integer.toHexString(flags), context.getString(
                if (flags and Intent.FLAG_ACTIVITY_TASK_ON_HOME != 0) R.string.command_home_requested
                else R.string.command_home_not_requested))
    }
}
