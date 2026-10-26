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
import java.util.UUID
import java.security.DigestInputStream
import java.security.MessageDigest
import android.widget.Toast

/** Single process/main-thread writer; atomic file prevents half-written snapshots. */
class WatchStore private constructor(context: Context) {
    private val app = context.applicationContext as Application
    private val file = AtomicFile(File(app.filesDir, "stopwatch.json"))
    private var storageWarningShown = false
    private var writesBlocked = false
    private var recoveredUnreadableFile = false
    val engine = StopwatchEngine(load())

    init {
        engine.restore(now())
        if (recoveredUnreadableFile) Toast.makeText(app, R.string.storage_recovered, Toast.LENGTH_LONG).show()
        save()
    }

    fun now() = Stamp(SystemClock.elapsedRealtime(), System.currentTimeMillis(),
        Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1))

    fun apply(action: WatchAction, origin: WatchOrigin = WatchOrigin.APP): Boolean {
        val changed = engine.apply(action, now(), origin)
        if (changed) save()
        return changed
    }

    fun save(): Boolean {
        if (writesBlocked) {
            if (!storageWarningShown) Toast.makeText(app, R.string.storage_error, Toast.LENGTH_LONG).show()
            storageWarningShown = true
            return false
        }
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

    private fun load(): WatchSnapshot {
        return try {
            if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return WatchSnapshot()
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
        } catch (_: Exception) {
            // Preserve unreadable/unsupported state before initialization can write
            // a fresh snapshot. If preservation fails, leave the original untouched.
            val recovery = AtomicFile(File(app.filesDir, "stopwatch-recovery-${UUID.randomUUID()}.json"))
            var stream: FileOutputStream? = null
            try {
                val output = recovery.startWrite()
                stream = output
                val originalHash = MessageDigest.getInstance("SHA-256")
                DigestInputStream(file.openRead(), originalHash).use { it.copyTo(output) }
                recovery.finishWrite(output)
                stream = null
                val savedHash = MessageDigest.getInstance("SHA-256")
                DigestInputStream(recovery.openRead(), savedHash).use { input ->
                    val buffer = ByteArray(8192)
                    while (input.read(buffer) != -1) { /* Hash the saved bytes without retaining them. */ }
                }
                if (!MessageDigest.isEqual(originalHash.digest(), savedHash.digest())) {
                    throw IOException("Recovery snapshot verification failed")
                }
                recoveredUnreadableFile = true
            } catch (_: Exception) {
                recovery.failWrite(stream)
                writesBlocked = true
            }
            WatchSnapshot()
        }
    }

    companion object {
        private var instance: WatchStore? = null
        fun get(context: Context): WatchStore = instance ?: WatchStore(context).also { instance = it }
    }
}
