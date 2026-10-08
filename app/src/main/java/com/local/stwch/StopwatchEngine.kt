package com.local.stwch

enum class WatchStatus { IDLE, RUNNING, PAUSED }
enum class WatchAction { START, PAUSE, RESET, LAP, TOGGLE }
enum class WatchOrigin { APP, ROUTINE }
data class Stamp(val monotonicMs: Long, val wallMs: Long, val bootId: Int)
data class Lap(val number: Int, val totalMs: Long, val splitMs: Long)
data class WatchRecord(val startedWallMs: Long, val stoppedWallMs: Long,
    val segmentMs: Long, val totalMs: Long, val reason: String)
data class WatchSnapshot(
    val status: WatchStatus = WatchStatus.IDLE,
    val accumulatedMs: Long = 0,
    val runAtMs: Long = 0,
    val runAtWallMs: Long = 0,
    val sessionAtWallMs: Long = 0,
    val bootId: Int = -1,
    val savedWallMs: Long = 0,
    val savedTotalMs: Long = 0,
    val laps: List<Lap> = emptyList(),
    val history: List<WatchRecord> = emptyList()
)

/** Pure state machine: wall clock is for labels, never for duration. */
class StopwatchEngine(initial: WatchSnapshot = WatchSnapshot()) {
    var state: WatchSnapshot = initial
        private set

    fun elapsed(now: Stamp): Long = if (state.status == WatchStatus.RUNNING) {
        if (now.bootId != state.bootId || now.monotonicMs < state.runAtMs) state.savedTotalMs
        else state.accumulatedMs + (now.monotonicMs - state.runAtMs).coerceAtLeast(0)
    } else state.accumulatedMs

    fun restore(now: Stamp) {
        if (state.status == WatchStatus.RUNNING &&
            (state.bootId != now.bootId || now.monotonicMs < state.runAtMs)) {
            val restored = state.savedTotalMs.coerceAtLeast(state.accumulatedMs)
            addRecord(state.savedWallMs, restored, "reboot")
            state = state.copy(status = WatchStatus.PAUSED, accumulatedMs = restored,
                bootId = now.bootId)
        }
    }

    fun apply(action: WatchAction, now: Stamp, origin: WatchOrigin = WatchOrigin.APP): Boolean {
        restore(now)
        // Routine buttons have distinct double-press semantics. Completed history is retained,
        // but replacing a running measurement never creates an automatic lap/history entry.
        if (origin == WatchOrigin.ROUTINE) {
            if (action == WatchAction.START && state.status == WatchStatus.RUNNING)
                state = WatchSnapshot(history = state.history, bootId = now.bootId)
            if (action == WatchAction.PAUSE && state.status == WatchStatus.PAUSED)
                return apply(WatchAction.RESET, now)
        }
        when (action) {
            WatchAction.TOGGLE -> return apply(if (state.status == WatchStatus.RUNNING)
                WatchAction.PAUSE else WatchAction.START, now)
            WatchAction.START -> {
                if (state.status == WatchStatus.RUNNING) {
                    addRecord(now.wallMs, elapsed(now), "restart")
                    state = WatchSnapshot(history = state.history)
                }
                val session = if (state.status == WatchStatus.IDLE) now.wallMs else state.sessionAtWallMs
                state = state.copy(status = WatchStatus.RUNNING, runAtMs = now.monotonicMs,
                    runAtWallMs = now.wallMs, sessionAtWallMs = session, bootId = now.bootId)
            }
            WatchAction.PAUSE -> {
                if (state.status != WatchStatus.RUNNING) return false
                val total = elapsed(now)
                addRecord(now.wallMs, total, "pause")
                state = state.copy(status = WatchStatus.PAUSED, accumulatedMs = total)
            }
            WatchAction.RESET -> {
                if (state.status == WatchStatus.RUNNING) addRecord(now.wallMs, elapsed(now), "reset")
                state = WatchSnapshot(history = state.history, bootId = now.bootId)
            }
            WatchAction.LAP -> {
                if (state.status != WatchStatus.RUNNING || state.laps.size >= 10) return false
                val total = elapsed(now)
                val previous = state.laps.lastOrNull()?.totalMs ?: 0
                state = state.copy(laps = state.laps + Lap(state.laps.size + 1,
                    total, (total - previous).coerceAtLeast(0)))
            }
        }
        checkpoint(now)
        return true
    }

    fun checkpoint(now: Stamp): WatchSnapshot {
        state = state.copy(savedWallMs = now.wallMs, savedTotalMs = elapsed(now))
        return state
    }

    private fun addRecord(endWall: Long, total: Long, reason: String) {
        val record = WatchRecord(state.runAtWallMs, endWall,
            (total - state.accumulatedMs).coerceAtLeast(0), total, reason)
        state = state.copy(history = (listOf(record) + state.history).take(20))
    }
}

object WatchFormat {
    fun time(ms: Long, hundredths: Boolean = true): String {
        val value = ms.coerceAtLeast(0)
        val seconds = value / 1000
        val hours = seconds / 3600
        val main = if (hours > 0) "%d:%02d:%02d".format(java.util.Locale.ROOT,
            hours, seconds / 60 % 60, seconds % 60)
        else "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
        return if (hundredths) main + ".%02d".format(java.util.Locale.ROOT, value / 10 % 100) else main
    }
}
