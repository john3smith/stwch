package com.local.stwch

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object WatchCommands {
    const val PREFIX = "com.local.stwch."
    fun parse(action: String?): WatchAction? = WatchAction.entries.firstOrNull { action == PREFIX + it.name }
    fun intent(context: Context, action: WatchAction) = Intent(context, ShortcutActivity::class.java).setAction(PREFIX + action.name)
    fun dispatch(context: Context, action: WatchAction) {
        try {
            ContextCompat.startForegroundService(context,
                Intent(context, StopwatchService::class.java).setAction(PREFIX + action.name))
        } catch (_: RuntimeException) { Toast.makeText(context, R.string.service_error, Toast.LENGTH_LONG).show() }
    }
}

class StopwatchService : Service() {
    private lateinit var store: WatchStore
    private lateinit var manager: NotificationManager
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { manager.notify(ID, notification()) }
    }
    override fun onCreate() {
        super.onCreate()
        store = WatchStore.get(this)
        manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.channel_description)
            setSound(null, null); enableVibration(false); setShowBadge(false)
        })
        ContextCompat.registerReceiver(this, timeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // All commands, including shortcut actions, are serialized on this main thread.
        val preliminary = notification()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, preliminary, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, preliminary)
        WatchCommands.parse(intent?.action)?.let { store.apply(it) }
        val status = store.engine.state.status
        if (status == WatchStatus.RUNNING) {
            manager.notify(ID, notification())
            return START_STICKY
        }
        stopForeground(if (status == WatchStatus.IDLE) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        if (status == WatchStatus.IDLE) manager.cancel(ID) else manager.notify(ID, notification())
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun pending(action: WatchAction) = PendingIntent.getActivity(this, action.ordinal + 1,
        WatchCommands.intent(this, action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun notification(): Notification {
        val s = store.engine.state
        val running = s.status == WatchStatus.RUNNING
        val elapsed = store.engine.elapsed(store.now())
        val content = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stopwatch).setContentIntent(content)
            .setContentTitle(getString(if (running) R.string.notification_running else R.string.notification_paused))
            .setContentText(if (running) getString(R.string.notification_running_text, s.laps.size)
                else getString(R.string.notification_text, WatchFormat.time(elapsed, false), s.laps.size))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH).setOnlyAlertOnce(true)
            .setOngoing(running).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(android.graphics.Color.rgb(236,118,94))
            .setWhen(System.currentTimeMillis() - elapsed).setShowWhen(running)
            .setUsesChronometer(running).setChronometerCountDown(false)
            .setRequestPromotedOngoing(running)
            .addAction(R.drawable.ic_stopwatch, getString(if (running) R.string.pause else R.string.resume),
                pending(if (running) WatchAction.PAUSE else WatchAction.START))
            .addAction(R.drawable.ic_stopwatch, getString(R.string.reset), pending(WatchAction.RESET))
        if (!running) builder.setShortCriticalText(getString(R.string.pause))
        return builder.build()
    }

    override fun onDestroy() { unregisterReceiver(timeReceiver); store.save(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val CHANNEL = "stopwatch_status"; const val ID = 41 }
}
