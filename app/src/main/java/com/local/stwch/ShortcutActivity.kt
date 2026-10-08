package com.local.stwch

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

/** Translucent user-initiated FGS entry point. No affinity with the app UI and
 * no forced task removal/reordering: finish only this command's Activity. */
class ShortcutActivity : Activity() {
    private val pending = ArrayDeque<Intent>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            WatchCommands.parse(command.action)?.let {
                CommandDiagnostics.record(this, it, command)
                WatchCommands.dispatch(this, it, WatchCommands.origin(command, WatchOrigin.ROUTINE))
            }
        }
        // Removing a singleInstance task can expose Home instead of the caller on
        // OEM shortcut launchers. Never finishAndRemoveTask/finishAffinity or
        // moveTaskToBack here; each command closes only its own Activity.
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
