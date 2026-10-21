package com.local.stwch

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

/** Transparent user-invoked command entry. Never brings the stopwatch UI forward.
 * Dispatch while resumed so the foreground-service request is user initiated. */
class ShortcutActivity : Activity() {
    private val pending = ArrayDeque<Intent>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CommandDiagnostics.trace(this, "shortcut-create")
        if (savedInstanceState?.getBoolean("handled") != true) pending.addLast(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pending.addLast(intent)
    }
    override fun onResume() {
        super.onResume()
        while (pending.isNotEmpty()) {
            val command = pending.removeFirst()
            val action = WatchCommands.parse(command.action)
            if (action != null) {
                CommandDiagnostics.record(this, action, command)
                CommandDiagnostics.trace(this, "shortcut-background-dispatch")
                WatchCommands.dispatch(this, action,
                    WatchCommands.origin(command, WatchOrigin.ROUTINE))
            }
        }
        // Finish only this transparent entry. Do not launch MainActivity, send
        // HOME, move tasks, or finish the existing stopwatch Activity/task.
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("handled", pending.isEmpty())
        super.onSaveInstanceState(outState)
    }
}

/** Compatible picker for launchers/button tools that use CREATE_SHORTCUT. */
class ShortcutPickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actions = listOf(WatchAction.START, WatchAction.PAUSE, WatchAction.RESET)
        val labels = listOf(R.string.start_shortcut, R.string.pause_shortcut, R.string.reset_shortcut).map(::getString)
        AlertDialog.Builder(this).setTitle(R.string.app_name).setItems(labels.toTypedArray()) { _, index ->
            val result = Intent().putExtra(Intent.EXTRA_SHORTCUT_INTENT, WatchCommands.intent(this, actions[index]))
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, labels[index])
                .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher))
            setResult(RESULT_OK, result); finish()
        }.setOnCancelListener { finish() }.show()
    }
}
