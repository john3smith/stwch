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
    const val EXTRA_ORIGIN = PREFIX + "ORIGIN"
    fun parse(action: String?): WatchAction? = WatchAction.entries.firstOrNull { action == PREFIX + it.name }
    fun origin(intent: Intent?, fallback: WatchOrigin = WatchOrigin.APP): WatchOrigin =
        WatchOrigin.entries.firstOrNull { it.name == intent?.getStringExtra(EXTRA_ORIGIN) } ?: fallback
    fun intent(context: Context, action: WatchAction, origin: WatchOrigin = WatchOrigin.ROUTINE) =
        Intent(context, ShortcutActivity::class.java).setAction(PREFIX + action.name)
            .putExtra(EXTRA_ORIGIN, origin.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
    fun dispatch(context: Context, action: WatchAction, origin: WatchOrigin = WatchOrigin.APP) {
        try {
            ContextCompat.startForegroundService(context,
                Intent(context, StopwatchService::class.java).setAction(PREFIX + action.name)
                    .putExtra(EXTRA_ORIGIN, origin.name))
        } catch (_: RuntimeException) { Toast.makeText(context, R.string.service_error, Toast.LENGTH_LONG).show() }
    }
}

class StopwatchService : Service() {
    private lateinit var store: WatchStore
    private lateinit var manager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            if (store.engine.state.status != WatchStatus.RUNNING) return
            manager.notify(ID, notification())
            handler.postDelayed(this, NotificationTime.nextRefreshDelay(store.engine.elapsed(store.now())))
        }
    }
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshRunning() }
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
            addAction(Intent.ACTION_SCREEN_ON)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // All commands, including shortcut actions, are serialized on this main thread.
        ForegroundCommandSequence.run(store.engine, WatchCommands.parse(intent?.action),
            store.now(), WatchCommands.origin(intent), publish = {
                val initial = notification()
                if (Build.VERSION.SDK_INT >= 34) startForeground(ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                else startForeground(ID, initial)
                android.util.Log.d("stwch.FGS", "Initial notification state=${store.engine.state.status}"
                    + " ongoing=${initial.flags and Notification.FLAG_ONGOING_EVENT != 0}"
                    + " promoteRequested=${initial.extras.getBoolean("android.requestPromotedOngoing")}")
            }, persist = { store.save() })
        val status = store.engine.state.status
        if (status == WatchStatus.RUNNING) {
            refreshRunning()
            return START_STICKY
        }
        handler.removeCallbacks(ticker)
        stopForeground(if (status == WatchStatus.IDLE) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        if (status == WatchStatus.IDLE) manager.cancel(ID) else manager.notify(ID, notification())
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun refreshRunning() {
        handler.removeCallbacks(ticker)
        if (store.engine.state.status == WatchStatus.RUNNING) ticker.run()
    }

    // A notification action is itself a documented user-initiated FGS entry.
    // Do not start a translucent command Activity or change tasks for these buttons.
    private fun pending(action: WatchAction) = PendingIntent.getForegroundService(this, action.ordinal + 1,
        Intent(this, StopwatchService::class.java).setAction(WatchCommands.PREFIX + action.name)
            .putExtra(WatchCommands.EXTRA_ORIGIN, WatchOrigin.APP.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
            .setContentText(getString(if (running) R.string.notification_time else R.string.notification_paused_time,
                NotificationTime.text(elapsed)))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH).setOnlyAlertOnce(true)
            .setOngoing(running).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(android.graphics.Color.rgb(236,118,94))
            .setWhen(System.currentTimeMillis() - elapsed).setShowWhen(running)
            .setUsesChronometer(running).setChronometerCountDown(false)
            .setRequestPromotedOngoing(running)
            .addAction(R.drawable.ic_stopwatch, getString(if (running) R.string.pause else R.string.resume),
                pending(if (running) WatchAction.PAUSE else WatchAction.START))
            .addAction(R.drawable.ic_stopwatch, getString(R.string.reset), pending(WatchAction.RESET))
        // A stopwatch has no predetermined finish percentage. Android 16's
        // public live-update style represents an active session without inventing
        // a percent or using Samsung's private notification/media APIs.
        if (running && Build.VERSION.SDK_INT >= 36) {
            builder.setStyle(NotificationCompat.ProgressStyle().setProgressIndeterminate(true))
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(getString(
                if (running) R.string.notification_time else R.string.notification_paused_time,
                NotificationTime.text(elapsed))))
        }
        if (!running) builder.setShortCriticalText(getString(R.string.pause))
        return builder.build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        unregisterReceiver(timeReceiver); store.save(); super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val CHANNEL = "stopwatch_status"; const val ID = 41 }
}
