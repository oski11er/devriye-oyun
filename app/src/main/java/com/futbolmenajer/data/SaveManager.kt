package com.futbolmenajer.data

import android.content.Context
import com.futbolmenajer.data.model.GameState
import com.futbolmenajer.engine.Scheduler
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class SaveMeta(val slot: Int, val clubName: String, val manager: String, val dateLabel: String, val savedAt: Long, val version: Int = 1)

/**
 * KAYDET / YÜKLE
 * GameState → JSON → GZIP → files/saves/slotN.sav  (+ slotN.meta özet dosyası)
 * Tamamen çevrimdışı, cihazın uygulama klasöründe tutulur.
 */
class SaveManager(context: Context) {

    private val dir = File(context.filesDir, "saves").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; allowStructuredMapKeys = true }

    fun save(slot: Int, gs: GameState) {
        val tmp = File(dir, "slot$slot.tmp")
        GZIPOutputStream(tmp.outputStream().buffered(1 shl 16)).bufferedWriter(Charsets.UTF_8).use { w ->
            w.write(json.encodeToString(GameState.serializer(), gs))
        }
        val target = File(dir, "slot$slot.sav")
        if (target.exists()) target.delete()
        tmp.renameTo(target)
        val club = gs.clubs[gs.userClubId]?.name ?: "-"
        val meta = SaveMeta(slot, club, gs.managerName, Scheduler.dateStr(gs.season, gs.day), System.currentTimeMillis())
        File(dir, "slot$slot.meta").writeText(json.encodeToString(SaveMeta.serializer(), meta))
    }

    fun load(slot: Int): GameState {
        val f = File(dir, "slot$slot.sav")
        return GZIPInputStream(f.inputStream().buffered(1 shl 16)).bufferedReader(Charsets.UTF_8).use { r ->
            json.decodeFromString(GameState.serializer(), r.readText())
        }
    }

    fun meta(slot: Int): SaveMeta? = runCatching {
        json.decodeFromString(SaveMeta.serializer(), File(dir, "slot$slot.meta").readText())
    }.getOrNull()

    fun delete(slot: Int) {
        File(dir, "slot$slot.sav").delete(); File(dir, "slot$slot.meta").delete()
    }

    companion object { const val SLOTS = 4 }
}
