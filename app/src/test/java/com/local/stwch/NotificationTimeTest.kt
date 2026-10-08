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
}
