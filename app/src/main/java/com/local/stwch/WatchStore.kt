package com.local.stwch

import android.content.Context
import android.app.Application
import android.os.SystemClock
import android.provider.Settings
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.widget.Toast

/** Single process/main-thread writer; atomic file prevents half-written snapshots. */
class WatchStore private constructor(context: Context) {
    private val app = context.applicationContext as Application
    private val file = AtomicFile(File(app.filesDir, "stopwatch.json"))
    val engine = StopwatchEngine(load())
    private var storageWarningShown = false

    init { engine.restore(now()); save() }

    fun now() = Stamp(SystemClock.elapsedRealtime(), System.currentTimeMillis(),
        Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1))

    fun apply(action: WatchAction): Boolean {
        val changed = engine.apply(action, now())
        if (changed) save()
        return changed
    }

    fun save(): Boolean {
        val s = engine.checkpoint(now())
        val json = JSONObject().put("schema", 1).put("status", s.status.name)
            .put("accumulated", s.accumulatedMs).put("runAt", s.runAtMs)
            .put("runWall", s.runAtWallMs).put("sessionWall", s.sessionAtWallMs)
            .put("boot", s.bootId).put("savedWall", s.savedWallMs).put("savedTotal", s.savedTotalMs)
        json.put("laps", JSONArray().apply { s.laps.forEach {
            put(JSONObject().put("number", it.number).put("total", it.totalMs).put("split", it.splitMs))
        } })
        json.put("history", JSONArray().apply { s.history.forEach {
            put(JSONObject().put("start", it.startedWallMs).put("stop", it.stoppedWallMs)
                .put("segment", it.segmentMs).put("total", it.totalMs).put("reason", it.reason))
        } })
        var stream: FileOutputStream? = null
        return try {
            stream = file.startWrite()
            stream.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(stream)
            storageWarningShown = false
            true
        } catch (_: IOException) {
            file.failWrite(stream)
            if (!storageWarningShown) Toast.makeText(app, R.string.storage_error, Toast.LENGTH_LONG).show()
            storageWarningShown = true
            false
        }
    }

    private fun load(): WatchSnapshot = try {
        val text = file.openRead().use { it.readBytes().toString(Charsets.UTF_8) }
        val j = JSONObject(text)
        require(j.getInt("schema") == 1)
        val laps = j.optJSONArray("laps") ?: JSONArray()
        val records = j.optJSONArray("history") ?: JSONArray()
        WatchSnapshot(WatchStatus.valueOf(j.getString("status")), j.getLong("accumulated").coerceAtLeast(0),
            j.getLong("runAt"), j.getLong("runWall"), j.getLong("sessionWall"), j.getInt("boot"),
            j.getLong("savedWall"), j.getLong("savedTotal").coerceAtLeast(0),
            (0 until minOf(laps.length(), 10)).map { i -> laps.getJSONObject(i).let {
                Lap(i + 1, it.getLong("total").coerceAtLeast(0), it.getLong("split").coerceAtLeast(0)) } },
            (0 until minOf(records.length(), 20)).map { i -> records.getJSONObject(i).let {
                WatchRecord(it.getLong("start"), it.getLong("stop"), it.getLong("segment").coerceAtLeast(0),
                    it.getLong("total").coerceAtLeast(0), it.getString("reason")) } })
    } catch (_: Exception) { WatchSnapshot() }

    companion object {
        private var instance: WatchStore? = null
        fun get(context: Context): WatchStore = instance ?: WatchStore(context).also { instance = it }
    }
}
