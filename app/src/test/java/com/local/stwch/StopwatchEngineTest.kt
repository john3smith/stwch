package com.local.stwch

import org.junit.Assert.*
import org.junit.Test

class StopwatchEngineTest {
    private fun t(ms:Long,wall:Long=100000+ms,boot:Int=1)=Stamp(ms,wall,boot)
    @Test fun startAndPause() {
        val e=StopwatchEngine(); e.apply(WatchAction.START,t(1000)); e.apply(WatchAction.PAUSE,t(4500))
        assertEquals(WatchStatus.PAUSED,e.state.status); assertEquals(3500,e.elapsed(t(10000)))
        assertEquals(3500,e.state.history.single().segmentMs)
    }
    @Test fun runningStartResetsAndRecordsPrevious() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000));e.apply(WatchAction.LAP,t(2000));e.apply(WatchAction.START,t(4000))
        assertEquals(0,e.elapsed(t(4000))); assertTrue(e.state.laps.isEmpty())
        assertEquals("restart",e.state.history.single().reason);assertEquals(3000,e.state.history.single().totalMs)
    }
    @Test fun pausedStartResumesAndExcludesPausedTime() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000));e.apply(WatchAction.PAUSE,t(3000));e.apply(WatchAction.START,t(10000));e.apply(WatchAction.PAUSE,t(14000))
        assertEquals(6000,e.elapsed(t(20000)));assertEquals(4000,e.state.history.first().segmentMs)
        assertEquals(2,e.state.history.size)
    }
    @Test fun centerToggleNeverRestartsWhileRunning() {
        val e=StopwatchEngine();e.apply(WatchAction.TOGGLE,t(0));e.apply(WatchAction.TOGGLE,t(2000));e.apply(WatchAction.TOGGLE,t(8000))
        assertEquals(3000,e.elapsed(t(9000)))
    }
    @Test fun duplicatePauseIsNoOp() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(0));e.apply(WatchAction.PAUSE,t(1000))
        assertFalse(e.apply(WatchAction.PAUSE,t(3000)));assertEquals(1,e.state.history.size)
    }
    @Test fun resetRunningRecordsAndClearsLaps() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(0));e.apply(WatchAction.LAP,t(1500));e.apply(WatchAction.RESET,t(2000))
        assertEquals(WatchStatus.IDLE,e.state.status);assertEquals(0,e.elapsed(t(4000)));assertTrue(e.state.laps.isEmpty())
        assertEquals("reset",e.state.history.single().reason)
    }
    @Test fun resetPausedDoesNotDuplicateHistory() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(0));e.apply(WatchAction.PAUSE,t(1000));e.apply(WatchAction.RESET,t(2000))
        assertEquals(1,e.state.history.size)
    }
    @Test fun tenLapLimitKeepsFirstTen() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(0));for(i in 1..10) assertTrue(e.apply(WatchAction.LAP,t(i*1000L)))
        assertFalse(e.apply(WatchAction.LAP,t(11000)));assertEquals(10,e.state.laps.size);assertEquals(10000,e.state.laps.last().totalMs)
    }
    @Test fun lapsHaveSplitAndAccumulateAcrossPause() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(0));e.apply(WatchAction.LAP,t(1000));e.apply(WatchAction.PAUSE,t(2000));e.apply(WatchAction.START,t(10000));e.apply(WatchAction.LAP,t(12000))
        assertEquals(4000,e.state.laps.last().totalMs);assertEquals(3000,e.state.laps.last().splitMs)
    }
    @Test fun lapOnlyWhileRunning() { assertFalse(StopwatchEngine().apply(WatchAction.LAP,t(1000))) }
    @Test fun historyKeepsNewestTwenty() {
        val e=StopwatchEngine();for(i in 0..29){e.apply(WatchAction.START,t(i*2000L));e.apply(WatchAction.PAUSE,t(i*2000L+1000))}
        assertEquals(20,e.state.history.size);assertEquals(159000,e.state.history.first().stoppedWallMs)
    }
    @Test fun wallClockChangeDoesNotChangeDuration() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000,10000));assertEquals(4000,e.elapsed(t(5000,1)))
    }
    @Test fun sameBootProcessRestoreKeepsRunning() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000));val restored=StopwatchEngine(e.checkpoint(t(5000)));restored.restore(t(8000))
        assertEquals(WatchStatus.RUNNING,restored.state.status);assertEquals(7000,restored.elapsed(t(8000)))
    }
    @Test fun newBootRestoresPausedAtCheckpointNotWallTime() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000));val restored=StopwatchEngine(e.checkpoint(t(5000)));restored.restore(t(100,900000,2))
        assertEquals(WatchStatus.PAUSED,restored.state.status);assertEquals(4000,restored.elapsed(t(90000,999999,2)))
        assertEquals("reboot",restored.state.history.first().reason)
    }
    @Test fun backwardMonotonicRecoversPaused() {
        val e=StopwatchEngine();e.apply(WatchAction.START,t(1000));e.checkpoint(t(3000));e.restore(t(20))
        assertEquals(WatchStatus.PAUSED,e.state.status);assertEquals(2000,e.state.accumulatedMs)
    }
    @Test fun formattingHoursAndHundredths() {
        assertEquals("01:01.23",WatchFormat.time(61234));assertEquals("1:01:01.23",WatchFormat.time(3661234))
        assertEquals("00:00",WatchFormat.time(-1,false))
    }
}
