package com.local.stwch

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : Activity() {
    private val date=SimpleDateFormat("MM.dd HH:mm:ss",Locale.getDefault())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=page()
        root.addView(pill(getString(R.string.back),Palette.mint).apply { setOnClickListener { finish() } },LinearLayout.LayoutParams(-2,-2))
        root.addView(label(getString(R.string.history_title),31f,bold=true).apply { setPadding(0,dp(24),0,dp(7)) })
        root.addView(label(getString(R.string.history_subtitle),13f,Palette.muted).apply { setPadding(0,0,0,dp(20)) })
        val scroll=ScrollView(this); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val list=LinearLayout(this).apply { id=R.id.history_list; orientation=LinearLayout.VERTICAL }; scroll.addView(list)
        val history=WatchStore.get(this).engine.state.history
        if(history.isEmpty()) list.addView(label(getString(R.string.history_empty),15f,Palette.muted))
        history.forEach { record ->
            val card=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=card(android.graphics.Color.WHITE); setPadding(dp(20),dp(18),dp(20),dp(18)) }
            val row=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL }
            row.addView(label(WatchFormat.time(record.segmentMs),28f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
            row.addView(label(getString(when(record.reason) {
                "restart"->R.string.restart_reason; "reset"->R.string.reset_reason
                "reboot"->R.string.reboot_reason; else->R.string.pause_reason
            }),12f,Palette.coral,true))
            card.addView(row)
            card.addView(label(getString(R.string.history_item,date.format(Date(record.startedWallMs)),date.format(Date(record.stoppedWallMs))),13f,Palette.muted).apply { setPadding(0,dp(10),0,0) })
            card.addView(label("누적 "+WatchFormat.time(record.totalMs),12f,Palette.muted).apply { setPadding(0,dp(7),0,0) })
            list.addView(card,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) })
        }
    }
}
