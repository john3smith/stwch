package com.local.stwch

/** Pausing freezes a measurement; only reset ends its foreground session.
 * The paused notification stays ongoing/promotable but needs no timer ticks. */
data class NotificationSession(val ongoing: Boolean, val ticking: Boolean) {
    companion object {
        fun forStatus(status: WatchStatus) = NotificationSession(
            ongoing = status != WatchStatus.IDLE,
            ticking = status == WatchStatus.RUNNING
        )
    }
}
