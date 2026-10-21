package com.local.stwch

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.util.Log

/** Static XML shortcuts receive CLEAR_TASK | TASK_ON_HOME in Android's parser.
 * Dynamic shortcuts keep our background-only intent flags. New IDs deliberately
 * avoid immutable legacy/pinned shortcuts; old Samsung mappings need reselection.
 * No foreground-app inspection or task restoration is needed. */
object RoutineShortcuts {
    fun register(context: Context): Boolean {
        return try {
            val manager = context.getSystemService(ShortcutManager::class.java) ?: return false
            if (!context.getSystemService(android.os.UserManager::class.java).isUserUnlocked) return false
            val definitions = listOf(
                Triple("background_start", WatchAction.START, R.string.start_shortcut),
                Triple("background_pause", WatchAction.PAUSE, R.string.pause_shortcut),
                Triple("background_reset", WatchAction.RESET, R.string.reset_shortcut)
            )
            val desired = definitions.mapIndexed { rank, (id, action, label) ->
                ShortcutInfo.Builder(context, id)
                    .setActivity(ComponentName(context, MainActivity::class.java))
                    .setShortLabel(context.getString(label))
                    .setLongLabel(context.getString(label))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_launcher))
                    .setRank(rank)
                    .setIntent(WatchCommands.intent(context, action))
                    .build()
            }
            // Avoid repeated writes/rate limiting on every process or UI start.
            val existing = manager.dynamicShortcuts.associateBy { it.id }
            val unchanged = desired.all { wanted ->
                val old = existing[wanted.id]
                old != null && old.isEnabled && old.activity == wanted.activity &&
                    old.shortLabel.toString() == wanted.shortLabel.toString() &&
                    old.rank == wanted.rank && old.intent?.component == wanted.intent?.component &&
                    old.intent?.action == wanted.intent?.action && old.intent?.flags == wanted.intent?.flags
            }
            if (unchanged) true else manager.setDynamicShortcuts(desired).also {
                Log.d("stwch.Shortcuts", "Background shortcut registration success=$it")
            }
        } catch (error: RuntimeException) {
            Log.w("stwch.Shortcuts", "Registration failed: ${error.javaClass.simpleName}")
            false
        }
    }
}

class StwchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RoutineShortcuts.register(this)
    }
}

/** Refresh after an upgrade without launching a UI or starting the timer. */
class ShortcutUpgradeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) RoutineShortcuts.register(context)
    }
}
