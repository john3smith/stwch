package com.local.stwch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTimeTest {
    @Test fun showsSecondsNotLapsOrHundredths() {
        assertEquals("00:00", NotificationTime.text(0))
        assertEquals("01:05", NotificationTime.text(65_987))
        assertEquals("1:01:05", NotificationTime.text(3_665_987))
        assertEquals("00:00", NotificationTime.text(-1))
    }
    @Test fun refreshIsAlignedToElapsedSecondsWithoutBusyLoop() {
        assertEquals(1000L, NotificationTime.nextRefreshDelay(0))
        assertEquals(999L, NotificationTime.nextRefreshDelay(1))
        assertEquals(1L, NotificationTime.nextRefreshDelay(999))
        assertEquals(1000L, NotificationTime.nextRefreshDelay(1000))
        assertEquals(1000L, NotificationTime.nextRefreshDelay(-1))
        assertTrue(NotificationTime.nextRefreshDelay(Long.MAX_VALUE) in 1..1000)
    }
    @Test fun pausedDisplayRemainsFrozenAfterTimePasses() {
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, Stamp(1000, 101_000, 1), WatchOrigin.APP)
        engine.apply(WatchAction.PAUSE, Stamp(84_456, 184_456, 1), WatchOrigin.APP)
        val atPause = NotificationTime.text(engine.elapsed(Stamp(84_456, 184_456, 1)))
        val later = NotificationTime.text(engine.elapsed(Stamp(3_684_456, 3_784_456, 1)))
        assertEquals("01:23", atPause)
        assertEquals(atPause, later)
    }
}
