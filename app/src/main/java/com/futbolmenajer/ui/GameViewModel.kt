package com.futbolmenajer.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.futbolmenajer.data.SaveManager
import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen(val title: String) {
    MENU("Futbol Menajer"), CLUB_SELECT("Kulüp Seç"), HOME("Ana Sayfa"), INBOX("Gelen Kutusu"), SQUAD("Kadro"),
    TACTICS("Taktik"), TRAINING("Antrenman"), TRANSFERS("Transfer"), FIXTURES("Fikstür"), TABLES("Puan Durumu"),
    COMPETITIONS("Turnuvalar"), YOUTH("Gençlik Akademisi"), FACILITIES("Tesisler"), FINANCE("Finans"),
    CLUB("Kulüp & Hedefler"), NATIONAL("Milli Takımlar"), PRE_MATCH("Maç Önü"), LIVE_MATCH("Canlı Maç"),
    MATCH_REPORT("Maç Raporu"), PLAYER("Oyuncu"), CLUB_VIEW("Kulüp"), SAVE_LOAD("Kaydet / Yükle"), SACKED("İş Teklifleri")
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    val saves = SaveManager(app)
    var gs: GameState? by mutableStateOf(null); private set
    var version by mutableIntStateOf(0); private set
    var busy by mutableStateOf(false); private set
    var busyText by mutableStateOf(""); private set
    var progress by mutableStateOf(0f); private set
    var toast by mutableStateOf<String?>(null)

    private val stack = ArrayDeque<Screen>().apply { add(Screen.MENU) }
    var screen by mutableStateOf(Screen.MENU); private set
    var selectedPlayerId by mutableIntStateOf(-1)
    var selectedClubId by mutableIntStateOf(-1)
    var selectedFixtureId by mutableIntStateOf(-1)
    var currentSlot by mutableIntStateOf(1)

    // canlı maç
    var sim: MatchSim? by mutableStateOf(null); private set
    var liveFixtureId = -1; private set

    fun bump() { version++ }
    fun say(s: String) { toast = s }

    /* ------------ navigasyon ------------ */
    fun go(s: Screen, clear: Boolean = false) {
        if (clear) stack.clear()
        if (stack.lastOrNull() != s) stack.addLast(s)
        screen = s
    }
    fun back(): Boolean {
        if (screen == Screen.LIVE_MATCH || screen == Screen.SACKED) return true
        if (stack.size <= 1) return false
        stack.removeLast(); screen = stack.last(); return true
    }
    fun openPlayer(id: Int) { selectedPlayerId = id; go(Screen.PLAYER) }
    fun openClub(id: Int) { selectedClubId = id; go(Screen.CLUB_VIEW) }
    fun openReport(fixtureId: Int) { selectedFixtureId = fixtureId; go(Screen.MATCH_REPORT) }

    private fun work(text: String, block: suspend () -> Unit) {
        if (busy) return
        busy = true; busyText = text; progress = 0f
        viewModelScope.launch {
            try { block() } catch (e: Throwable) { toast = "Hata: ${e.message}"; e.printStackTrace() }
            finally { busy = false; bump() }
        }
    }

    /* ------------ yeni oyun / kayıt ------------ */
    fun newWorld() = work("Dünya oluşturuluyor…") {
        val world = withContext(Dispatchers.Default) {
            GameEngine.newWorld(System.currentTimeMillis()) { p, t -> progress = p; busyText = t }
        }
        gs = world
        go(Screen.CLUB_SELECT, clear = true)
    }

    fun startCareer(clubId: Int, name: String) {
        val g = gs ?: return
        GameEngine.startCareer(g, clubId, name.ifBlank { "Menajer" })
        go(Screen.HOME, clear = true)
        bump()
    }

    fun save(slot: Int) = work("Kaydediliyor…") {
        val g = gs ?: return@work
        withContext(Dispatchers.IO) { saves.save(slot, g) }
        currentSlot = slot
        toast = "Oyun $slot. yuvaya kaydedildi."
    }

    fun load(slot: Int) = work("Yükleniyor…") {
        val g = withContext(Dispatchers.IO) { saves.load(slot) }
        gs = g; currentSlot = slot
        go(if (g.sacked) Screen.SACKED else Screen.HOME, clear = true)
    }

    fun quitToMenu() { gs = null; sim = null; go(Screen.MENU, clear = true) }

    /* ------------ gün ilerletme ------------ */
    fun advance() = work("Günler ilerliyor…") {
        val g = gs ?: return@work
        val stop = withContext(Dispatchers.Default) { GameEngine.advance(g) }
        when (stop) {
            is Stop.MatchDay -> { selectedFixtureId = stop.fixtureId; go(Screen.PRE_MATCH) }
            is Stop.Event -> toast = stop.reason
            is Stop.NewSeason -> { toast = "Yeni sezon başladı: ${Scheduler.seasonLabel(g.season)}"; go(Screen.INBOX) }
            is Stop.Sacked -> go(Screen.SACKED, clear = true)
            else -> {}
        }
    }

    fun startLive(fixtureId: Int) {
        val g = gs ?: return
        val fx = g.fixtures.firstOrNull { it.id == fixtureId } ?: return
        sim = GameEngine.buildSim(g, fx, detailed = true, userLive = true)
        liveFixtureId = fixtureId
        go(Screen.LIVE_MATCH)
    }

    fun quickResult(fixtureId: Int) = work("Maç oynanıyor…") {
        val g = gs ?: return@work
        val fx = g.fixtures.firstOrNull { it.id == fixtureId } ?: return@work
        withContext(Dispatchers.Default) {
            val s = GameEngine.buildSim(g, fx, detailed = true, userLive = false)
            s.runToEnd()
            GameEngine.applyResult(g, fx, s)
            GameEngine.finishDay(g)
        }
        stack.removeLastOrNull()
        selectedFixtureId = fixtureId
        go(Screen.MATCH_REPORT)
    }

    fun finishLive() = work("Maç sonuçları işleniyor…") {
        val g = gs ?: return@work
        val s = sim ?: return@work
        val fx = g.fixtures.firstOrNull { it.id == liveFixtureId } ?: return@work
        withContext(Dispatchers.Default) {
            if (!s.finished) s.runToEnd()
            GameEngine.applyResult(g, fx, s)
            GameEngine.finishDay(g)
        }
        sim = null
        // canlı maç ve maç önü ekranlarını yığından çıkar
        while (stack.lastOrNull() == Screen.LIVE_MATCH || stack.lastOrNull() == Screen.PRE_MATCH) stack.removeLast()
        selectedFixtureId = fx.id
        go(Screen.MATCH_REPORT)
        if (g.sacked) go(Screen.SACKED, clear = true)
    }

    fun answerPress(o: PressOption) {
        val g = gs ?: return
        toast = Press.answer(g, o); bump()
    }

    fun acceptJob(clubId: Int) {
        val g = gs ?: return
        GameEngine.acceptJob(g, clubId)
        go(Screen.HOME, clear = true); bump()
    }
}
