package com.local.stwch

import org.junit.Assert.*
import org.junit.Test

class ForegroundCommandSequenceTest {
    private fun stamp(ms: Long) = Stamp(ms, 100_000 + ms, 1)

    @Test fun firstStartPublishesRunningBeforeSaving() {
        val engine = StopwatchEngine()
        val events = mutableListOf<String>()
        ForegroundCommandSequence.run(engine, WatchAction.START, stamp(1000), WatchOrigin.APP,
            publish = {
                assertEquals(WatchStatus.RUNNING, engine.state.status)
                assertEquals(0L, engine.elapsed(stamp(1000)))
                events.add("publish")
            }, persist = { events.add("persist") })
        assertEquals(listOf("publish", "persist"), events)
    }

    @Test fun resumePublishesRunningWithAccumulatedTime() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000))
        engine.apply(WatchAction.PAUSE, stamp(3500))
        ForegroundCommandSequence.run(engine, WatchAction.START, stamp(10_000), WatchOrigin.ROUTINE,
            publish = {
                assertEquals(WatchStatus.RUNNING, engine.state.status)
                assertEquals(2500L, engine.elapsed(stamp(10_000)))
            }, persist = {})
    }

    @Test fun pausePublishesPausedNotStaleRunning() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000))
        ForegroundCommandSequence.run(engine, WatchAction.PAUSE, stamp(4000), WatchOrigin.APP,
            publish = {
                assertEquals(WatchStatus.PAUSED, engine.state.status)
                assertEquals(3000L, engine.elapsed(stamp(5000)))
            }, persist = {})
    }

    @Test fun serviceRecoveryDoesNotRestartOrWriteBeforePublication() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000))
        ForegroundCommandSequence.run(engine, null, stamp(5000), WatchOrigin.APP,
            publish = {
                assertEquals(WatchStatus.RUNNING, engine.state.status)
                assertEquals(4000L, engine.elapsed(stamp(5000)))
            }, persist = { fail("No command: no extra persistence") })
    }

    @Test fun routineSecondPausePublishesIdleAndPreservesHistory() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, stamp(1000), WatchOrigin.ROUTINE)
        engine.apply(WatchAction.PAUSE, stamp(3500), WatchOrigin.ROUTINE)
        val records = engine.state.history
        ForegroundCommandSequence.run(engine, WatchAction.PAUSE, stamp(4000), WatchOrigin.ROUTINE,
            publish = {
                assertEquals(WatchStatus.IDLE, engine.state.status)
                assertEquals(records, engine.state.history)
            }, persist = {})
    }
}
