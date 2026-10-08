package com.local.stwch

/** Notification text deliberately has no lap information. Do not count ticks:
 * delayed callbacks, sleep and clock changes must not alter stopwatch elapsed time. */
object NotificationTime {
    fun text(elapsedMs: Long): String = WatchFormat.time(elapsedMs, false)
    fun nextRefreshDelay(elapsedMs: Long): Long = 1000 - elapsedMs.coerceAtLeast(0) % 1000
}
