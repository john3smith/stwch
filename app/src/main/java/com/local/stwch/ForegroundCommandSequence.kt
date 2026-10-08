package com.local.stwch

/** Apply only in-memory work before the first FGS notification. Disk I/O follows
 * publication, so the startup deadline is not delayed by AtomicFile/fsync.
 * The initial notification must represent the new command, not the old state. */
object ForegroundCommandSequence {
    fun run(engine: StopwatchEngine, action: WatchAction?, now: Stamp,
            origin: WatchOrigin, publish: () -> Unit, persist: () -> Unit) {
        val changed = action?.let { engine.apply(it, now, origin) } ?: false
        publish()
        if (changed) persist()
    }
}
