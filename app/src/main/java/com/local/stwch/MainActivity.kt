package com.local.stwch

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*

class MainActivity : Activity() {
    private lateinit var store: WatchStore
    private lateinit var face: StopwatchFace
    private lateinit var status: TextView
    private lateinit var laps: LinearLayout
    private lateinit var lapButton: Button
    private lateinit var permissionHint: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var rendered: Pair<WatchStatus, List<Lap>>? = null
    private val ticker = object : Runnable {
        override fun run() { render(); handler.postDelayed(this, if (store.engine.state.status == WatchStatus.RUNNING) 50 else 200) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = WatchStore.get(this)
        val root = page()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        root.addView(scroll, LinearLayout.LayoutParams(-1,-1))
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(label("stwch", 39f, bold = true), LinearLayout.LayoutParams(0,-2,1f))
        header.addView(pill(getString(R.string.history), Palette.mint).apply {
            id = R.id.history_button; setOnClickListener { startActivity(Intent(this@MainActivity, HistoryActivity::class.java)) }
        })
        body.addView(header)
        body.addView(label(getString(R.string.tagline), 14f, Palette.muted).apply { setPadding(0,dp(5),0,dp(18)) })
        status = label(getString(R.string.ready), 14f, bold = true).apply {
            id = R.id.status_label; gravity = Gravity.CENTER; background = card(Palette.mint, 20)
            setPadding(dp(14),dp(8),dp(14),dp(8))
        }
        body.addView(status, LinearLayout.LayoutParams(-2,-2).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val width = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val side = dp(minOf(324, (width - 44).toInt()))
        face = StopwatchFace(this).apply { setOnClickListener { command(WatchAction.TOGGLE) } }
        body.addView(face, LinearLayout.LayoutParams(side,side).apply { gravity = Gravity.CENTER_HORIZONTAL; topMargin=dp(16); bottomMargin=dp(14) })
        val controls = LinearLayout(this)
        controls.addView(pill(getString(R.string.reset), Palette.ink).apply {
            id=R.id.reset_button; setTextColor(android.graphics.Color.WHITE)
            setOnClickListener { command(WatchAction.RESET) }
        }, LinearLayout.LayoutParams(0,dp(56),1f).apply { marginEnd=dp(10) })
        lapButton = pill(getString(R.string.lap)).apply { id=R.id.lap_button; setOnClickListener {
            if (store.engine.state.laps.size >= 10) Toast.makeText(this@MainActivity,R.string.lap_max,Toast.LENGTH_SHORT).show()
            else command(WatchAction.LAP)
        } }
        controls.addView(lapButton, LinearLayout.LayoutParams(0,dp(56),1f))
        body.addView(controls)
        body.addView(label(getString(R.string.lap_title), 16f, bold=true).apply { setPadding(0,dp(26),0,dp(12)) })
        laps = LinearLayout(this).apply { id=R.id.lap_list; orientation=LinearLayout.VERTICAL }
        body.addView(laps)
        permissionHint = label(getString(R.string.notification_denied),12f,Palette.coral).apply { setPadding(0,dp(14),0,0) }
        body.addView(permissionHint)
        body.addView(pill(getString(R.string.integration), Palette.background).apply {
            setOnClickListener { integration() }
        }, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(18) })
        render()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            savedInstanceState == null && !getPreferences(MODE_PRIVATE).getBoolean("askedNotifications",false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("askedNotifications",true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),1)
        }
    }

    override fun onResume() {
        super.onResume(); handler.post(ticker)
        // Recover an OS-killed service without resetting an active stopwatch.
        if (store.engine.state.status == WatchStatus.RUNNING) {
            try { androidx.core.content.ContextCompat.startForegroundService(this, Intent(this,StopwatchService::class.java)) }
            catch (_: RuntimeException) { Toast.makeText(this,R.string.service_error,Toast.LENGTH_LONG).show() }
        }
    }
    override fun onPause() { handler.removeCallbacks(ticker); store.save(); super.onPause() }

    private fun command(action: WatchAction) { WatchCommands.dispatch(this,action); handler.postDelayed({ render() },80) }

    private fun render() {
        val s = store.engine.state
        face.elapsed=store.engine.elapsed(store.now()); face.status=s.status; face.invalidate()
        face.contentDescription = getString(when(s.status) { WatchStatus.RUNNING->R.string.tap_pause; WatchStatus.PAUSED->R.string.tap_resume; else->R.string.tap_start })
        permissionHint.visibility = if (getSystemService(NotificationManager::class.java).areNotificationsEnabled()) View.GONE else View.VISIBLE
        lapButton.isEnabled=s.status == WatchStatus.RUNNING; lapButton.alpha=if(lapButton.isEnabled)1f else .45f
        val key=s.status to s.laps
        if (rendered == key) return
        rendered=key
        status.setText(when(s.status) { WatchStatus.RUNNING->R.string.running; WatchStatus.PAUSED->R.string.paused; else->R.string.ready })
        status.background=card(if(s.status==WatchStatus.RUNNING)Palette.peach else Palette.mint,20)
        laps.removeAllViews()
        if (s.laps.isEmpty()) laps.addView(label(getString(R.string.lap_empty),13f,Palette.muted).apply { setPadding(0,0,0,dp(4)) })
        s.laps.asReversed().forEach { lap ->
            val row=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; background=card(android.graphics.Color.WHITE,18); setPadding(dp(16),dp(12),dp(16),dp(12)) }
            row.addView(label("%02d".format(lap.number),15f,Palette.coral,true),LinearLayout.LayoutParams(0,-2,1f))
            row.addView(label("+"+WatchFormat.time(lap.splitMs),13f,Palette.muted),LinearLayout.LayoutParams(0,-2,2f))
            row.addView(label(WatchFormat.time(lap.totalMs),16f,Palette.ink,true))
            laps.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7) })
        }
    }

    private fun integration() {
        AlertDialog.Builder(this).setTitle(R.string.integration_title).setMessage(R.string.integration_body)
            .setPositiveButton(R.string.notification_settings) { _,_->
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
            }.setNeutralButton(R.string.live_settings) { _,_->
                if (Build.VERSION.SDK_INT >= 36) {
                    val settings=Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName)
                    try { startActivity(settings) }
                    catch (_: android.content.ActivityNotFoundException) { Toast.makeText(this,R.string.unsupported_live,Toast.LENGTH_LONG).show() }
                } else Toast.makeText(this,R.string.unsupported_live,Toast.LENGTH_LONG).show()
            }.setNegativeButton(R.string.close,null).show()
    }
}
