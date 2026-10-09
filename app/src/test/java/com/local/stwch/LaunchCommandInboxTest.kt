package com.local.stwch

import org.junit.Assert.*
import org.junit.Test

class LaunchCommandInboxTest {
    @Test fun commandsExecuteOnceAndInOrder() {
        val inbox = LaunchCommandInbox()
        inbox.enqueue(WatchAction.START, WatchOrigin.ROUTINE)
        inbox.enqueue(WatchAction.PAUSE, WatchOrigin.ROUTINE)
        val received = mutableListOf<LaunchCommand>()
        inbox.drain(received::add)
        inbox.drain(received::add)
        assertEquals(listOf(LaunchCommand(WatchAction.START, WatchOrigin.ROUTINE),
            LaunchCommand(WatchAction.PAUSE, WatchOrigin.ROUTINE)), received)
    }
    @Test fun deliveredCommandDoesNotReplayAfterRecreation() {
        val inbox = LaunchCommandInbox(listOf("START:ROUTINE"))
        inbox.drain { }
        var calls = 0
        LaunchCommandInbox(inbox.snapshot()).drain { calls++ }
        assertEquals(0, calls)
    }
    @Test fun pendingCommandSurvivesRecreation() {
        val inbox = LaunchCommandInbox()
        inbox.enqueue(WatchAction.PAUSE, WatchOrigin.ROUTINE)
        val received = mutableListOf<LaunchCommand>()
        LaunchCommandInbox(inbox.snapshot()).drain(received::add)
        assertEquals(listOf(LaunchCommand(WatchAction.PAUSE, WatchOrigin.ROUTINE)), received)
    }
    @Test fun malformedSavedCommandsAreIgnored() {
        val received = mutableListOf<LaunchCommand>()
        LaunchCommandInbox(listOf("unknown", "START:OTHER", "OTHER:APP", "PAUSE:APP:extra"))
            .drain(received::add)
        assertTrue(received.isEmpty())
    }
    @Test fun repeatedIntentStillRepresentsARealSecondRoutinePress() {
        val inbox = LaunchCommandInbox()
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, Stamp(1000, 1000, 1), WatchOrigin.ROUTINE)
        inbox.enqueue(WatchAction.PAUSE, WatchOrigin.ROUTINE)
        inbox.enqueue(WatchAction.PAUSE, WatchOrigin.ROUTINE)
        inbox.drain { engine.apply(it.action, Stamp(5000, 5000, 1), it.origin) }
        assertEquals(WatchStatus.IDLE, engine.state.status)
    }
    @Test fun rotationOfPausedCommandDoesNotBecomeDoublePauseReset() {
        val inbox = LaunchCommandInbox(listOf("PAUSE:ROUTINE"))
        val engine = StopwatchEngine()
        engine.apply(WatchAction.START, Stamp(1000, 1000, 1), WatchOrigin.ROUTINE)
        inbox.drain { engine.apply(it.action, Stamp(5000, 5000, 1), it.origin) }
        LaunchCommandInbox(inbox.snapshot()).drain { engine.apply(it.action, Stamp(8000, 8000, 1), it.origin) }
        assertEquals(WatchStatus.PAUSED, engine.state.status)
        assertEquals(4000, engine.elapsed(Stamp(9000, 9000, 1)))
    }
}
