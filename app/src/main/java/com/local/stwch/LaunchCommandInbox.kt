package com.local.stwch

data class LaunchCommand(val action: WatchAction, val origin: WatchOrigin)

/** Main-thread, pending commands only. Rotation restores undelivered commands;
 * completed commands are not replayed by resume or Activity recreation. */
class LaunchCommandInbox(saved: List<String> = emptyList()) {
    private val pending = ArrayDeque<LaunchCommand>()

    init {
        saved.forEach { encoded ->
            val parts = encoded.split(':')
            if (parts.size == 2) {
                val action = WatchAction.entries.firstOrNull { it.name == parts[0] }
                val origin = WatchOrigin.entries.firstOrNull { it.name == parts[1] }
                if (action != null && origin != null) enqueue(action, origin)
            }
        }
    }

    fun enqueue(action: WatchAction, origin: WatchOrigin) { pending.addLast(LaunchCommand(action, origin)) }
    fun snapshot(): ArrayList<String> = ArrayList(pending.map { "${it.action.name}:${it.origin.name}" })
    fun drain(dispatch: (LaunchCommand) -> Unit) {
        while (pending.isNotEmpty()) dispatch(pending.removeFirst())
    }
}
