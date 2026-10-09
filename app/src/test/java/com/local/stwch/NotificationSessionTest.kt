package com.local.stwch

import org.junit.Assert.*
import org.junit.Test

class NotificationSessionTest {
    private fun stamp(ms: Long) = Stamp(ms, 100_000 + ms, 1)

    @Test fun runningIsOngoingAndTicks() {
        assertEquals(NotificationSession(true, true), NotificationSession.forStatus(WatchStatus.RUNNING))
    }

    @Test fun pausedStaysOngoingButDoesNotTick() {
        assertEquals(NotificationSession(true, false), NotificationSession.forStatus(WatchStatus.PAUSED))
    }

    @Test fun idleEndsForegroundAndDoesNotTick() {
        assertEquals(NotificationSession(false, false), NotificationSession.forStatus(WatchStatus.IDLE))
    }

    @Test fun pauseKeepsSessionAndFrozenTime() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000), WatchOrigin.APP)
        engine.apply(WatchAction.PAUSE, stamp(4000), WatchOrigin.APP)
        assertTrue(NotificationSession.forStatus(engine.state.status).ongoing)
        assertFalse(NotificationSession.forStatus(engine.state.status).ticking)
        assertEquals(3000L, engine.elapsed(stamp(100_000)))
        assertEquals("00:03", NotificationTime.text(engine.elapsed(stamp(100_000))))
    }

    @Test fun appRepeatedPauseDoesNotRemovePausedSession() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000), WatchOrigin.APP)
        engine.apply(WatchAction.PAUSE, stamp(4000), WatchOrigin.APP)
        assertFalse(engine.apply(WatchAction.PAUSE, stamp(5000), WatchOrigin.APP))
        assertTrue(NotificationSession.forStatus(engine.state.status).ongoing)
    }

    @Test fun resumeKeepsSessionAndAccumulatedTime() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000), WatchOrigin.APP)
        engine.apply(WatchAction.PAUSE, stamp(4000), WatchOrigin.APP)
        engine.apply(WatchAction.START, stamp(10_000), WatchOrigin.APP)
        assertEquals(NotificationSession(true, true), NotificationSession.forStatus(engine.state.status))
        assertEquals(5000L, engine.elapsed(stamp(12_000)))
    }

    @Test fun resetOrRoutineSecondPauseEndsSessionButKeepsHistory() {
        for (action in listOf(WatchAction.RESET, WatchAction.PAUSE)) {
            val engine = StopwatchEngine()
            engine.apply(WatchAction.START, stamp(1000), WatchOrigin.ROUTINE)
            engine.apply(WatchAction.PAUSE, stamp(4000), WatchOrigin.ROUTINE)
            val history = engine.state.history
            engine.apply(action, stamp(5000), WatchOrigin.ROUTINE)
            assertEquals(NotificationSession(false, false), NotificationSession.forStatus(engine.state.status))
            assertEquals(history, engine.state.history)
        }
    }
}
