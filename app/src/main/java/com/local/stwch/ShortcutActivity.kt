package com.local.stwch

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

/** Translucent, isolated command task grants a legitimate user-initiated FGS entry point.
 * It never launches MainActivity or removes the UI/caller task. */
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
                WatchCommands.dispatch(this, it, WatchCommands.origin(command, WatchOrigin.ROUTINE))
            }
        }
        // singleInstance + separate affinity ensure this is only the command's task.
        if (isTaskRoot) finishAndRemoveTask() else finish()
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
