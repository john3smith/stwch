package com.local.stwch

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

/** Visible translucent activity grants a legitimate user-initiated FGS entry point. */
class ShortcutActivity : Activity() {
    private var executed = false
    override fun onResume() {
        super.onResume()
        if (!executed) {
            executed = true
            WatchCommands.parse(intent.action)?.let { WatchCommands.dispatch(this, it) }
            finish()
        }
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
