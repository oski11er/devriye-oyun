package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * MAÇ SİMÜLASYON MOTORU
 * ---------------------------------------------------------------
 * Dakika bazlı, olasılık tabanlı simülasyon (Football Manager / Soccer Manager tarzı metin + istatistik).
 *
 *  Her dakika:
 *   1) Orta saha gücüne göre topa sahip olan taraf belirlenir.
 *   2) Hücum gücü / rakip savunma oranına göre pozisyon olasılığı hesaplanır.
 *   3) Pozisyon olursa xG (gol beklentisi) üretilir, şutu çeken oyuncu ağırlıklı seçilir,
 *      şutör bitiricilik + soğukkanlılık vs kaleci kalitesi ile gol olasılığı bulunur.
 *   4) Faul, kart, korner, ofsayt, sakatlık, VAR ve 8 saniye kuralı (2025 IFAB) olayları.
 *   5) Yorgunluk her dakika düşer, güç hesaplamasına yansır.
 *  Kurallar: 5 oyuncu değişikliği / 3 değişiklik penceresi (devre arası sayılmaz),
 *  uzatmada +1 değişiklik, iki ayaklı eşleşmelerde deplasman golü yok, uzatma + penaltılar.
 *
 *  Sınıf adım adım (step) çalışır; arayüz canlı maçta duraklatıp taktik/oyuncu değişikliği yapabilir.
 */

enum class MatchPhase { NOT_STARTED, FIRST_HALF, HALF_TIME, SECOND_HALF, ET_BREAK, ET_FIRST, ET_SECOND, PENALTIES, FINISHED }

class TeamSide(
    val id: Int,
    val name: String,
    val isUser: Boolean,
    var tactic: Tactic,
    val lineup: MutableList<Int>,
    val bench: MutableList<Int>,
    val reputation: Int,
    val clubColor: Long = 0xFF1B5E20
) {
    val slots: MutableList<Position> = tactic.formation.slots.toMutableList()
    val starting: List<Int> = lineup.toList()
    var subsUsed = 0
    var subWindowsUsed = 0
    var subsThisMinute = false
    val subbedOff = mutableSetOf<Int>()
    val subbedOn = mutableListOf<Int>()
    val sentOff = mutableSetOf<Int>()
    val yellow = mutableSetOf<Int>()
    val injured = mutableSetOf<Int>()
    val played = linkedSetOf<Int>().apply { addAll(lineup) }
    val stats = TeamMatchStats()
    var goals = 0
    fun onPitch(): List<Int> = lineup.filter { it >= 0 }
}

class MatchSim(
    private val gs: GameState,
    val fixture: Fixture?,
    val home: TeamSide,
    val away: TeamSide,
    private val knockout: Boolean,
    private val neutral: Boolean,
    /** rövanş maçında ilk maç skorları (ev sahibinin golleri, deplasmanın golleri) */
    private val firstLeg: Pair<Int, Int>? = null,
    private val detailed: Boolean = true,
    private val userAutoSubs: Boolean = true,
    private val rnd: Random = Random(System.nanoTime())
) {
    var phase = MatchPhase.NOT_STARTED; private set
    var minute = 0; private set
    var added = 0; private set
    val events = mutableListOf<MatchEvent>()
    val fitness = HashMap<Int, Double>()
    val ratings = HashMap<Int, Double>()
    val goalsBy = HashMap<Int, Int>()
    val assistsBy = HashMap<Int, Int>()
    val straightRed = mutableSetOf<Int>()
    val secondYellow = mutableSetOf<Int>()
    var homePens: Int? = null; private set
    var awayPens: Int? = null; private set
    var extraTime = false; private set
    private var stoppage1 = 1 + rnd.nextInt(4)
    private var stoppage2 = 2 + rnd.nextInt(6)
    private var dirty = true
    private var strH = Str(); private var strA = Str()
    private val maxSubs get() = if (extraTime) 6 else 5
    private val maxWindows get() = if (extraTime) 4 else 3

    data class Str(var gk: Double = 60.0, var def: Double = 60.0, var mid: Double = 60.0, var att: Double = 60.0, var freq: Double = 1.0)

    init {
        for (side in listOf(home, away)) for (id in side.lineup + side.bench) {
            val p = gs.players[id] ?: continue
            fitness[id] = p.fitness.toDouble()
        }
        for (id in home.lineup + away.lineup) ratings[id] = 6.0
    }

    val finished get() = phase == MatchPhase.FINISHED
    val homeGoals get() = home.goals
    val awayGoals get() = away.goals

    fun minuteLabel(): String = when {
        phase == MatchPhase.NOT_STARTED -> "0'"
        added > 0 -> "$minute+$added'"
        else -> "$minute'"
    }

    private fun p(id: Int): Player? = gs.players[id]
    private fun other(s: TeamSide) = if (s === home) away else home
    private fun ev(type: EventType, side: TeamSide?, text: String, pid: Int? = null, pid2: Int? = null) {
        val e = MatchEvent(minute, type, side?.id, pid, pid2, text, added)
        events.add(e); stepEvents.add(e)
    }
    private val stepEvents = mutableListOf<MatchEvent>()
    private fun name(id: Int?) = id?.let { p(it)?.shortName } ?: "?"

    /** Tüm maçı tek seferde oynat. */
    fun runToEnd() { while (!finished) step() }

    /** Bir dakika ilerlet; bu adımda oluşan olayları döndürür. */
    fun step(): List<MatchEvent> {
        stepEvents.clear()
        home.subsThisMinute = false; away.subsThisMinute = false
        when (phase) {
            MatchPhase.NOT_STARTED -> {
                phase = MatchPhase.FIRST_HALF
                ev(EventType.KICKOFF, null, "Hakemin düdüğüyle ${home.name} – ${away.name} maçı başladı!")
            }
            MatchPhase.FIRST_HALF -> {
                if (minute < 45) minute++ else added++
                simulateMinute()
                if (minute == 45 && added >= stoppage1) {
                    ev(EventType.HALF_TIME, null, "İlk yarı sona erdi: ${home.name} ${home.goals} - ${away.goals} ${away.name}")
                    phase = MatchPhase.HALF_TIME
                }
            }
            MatchPhase.HALF_TIME -> {
                added = 0; minute = 45
                aiHalfTime(home); aiHalfTime(away)
                phase = MatchPhase.SECOND_HALF
                ev(EventType.INFO, null, "İkinci yarı başladı.")
            }
            MatchPhase.SECOND_HALF -> {
                if (minute < 90) minute++ else added++
                simulateMinute()
                aiManage(home); aiManage(away)
                if (minute == 90 && added >= stoppage2) endOfRegulation()
            }
            MatchPhase.ET_BREAK -> {
                added = 0; minute = 90; extraTime = true
                phase = MatchPhase.ET_FIRST
                ev(EventType.EXTRA_TIME, null, "Uzatma devresi başlıyor! (her takıma +1 değişiklik hakkı)")
            }
            MatchPhase.ET_FIRST -> {
                if (minute < 105) minute++ else added++
                simulateMinute()
                if (minute == 105 && added >= 1) { added = 0; phase = MatchPhase.ET_SECOND; ev(EventType.INFO, null, "Uzatmanın ilk yarısı bitti.") }
            }
            MatchPhase.ET_SECOND -> {
                if (minute < 120) minute++ else added++
                simulateMinute()
                aiManage(home); aiManage(away)
                if (minute == 120 && added >= 2) {
                    if (tied()) { phase = MatchPhase.PENALTIES; ev(EventType.INFO, null, "Uzatmalarda da eşitlik bozulmadı. Seri penaltı atışları!") }
                    else finish()
                }
            }
            MatchPhase.PENALTIES -> { shootout(); finish() }
            MatchPhase.FINISHED -> {}
        }
        return stepEvents.toList()
    }

    private fun tied(): Boolean {
        if (firstLeg != null) {
            val homeAgg = home.goals + firstLeg.second
            val awayAgg = away.goals + firstLeg.first
            return homeAgg == awayAgg
        }
        return home.goals == away.goals
    }

    private fun endOfRegulation() {
        if (knockout && (fixture?.leg != 1) && tied()) {
            phase = MatchPhase.ET_BREAK
            ev(EventType.INFO, null, "Normal süre ${home.goals}-${away.goals} bitti" + (firstLeg?.let { " (toplam ${home.goals + it.second}-${away.goals + it.first})" } ?: "") + ". Uzatmalara gidiliyor.")
        } else finish()
    }

    private fun finish() {
        phase = MatchPhase.FINISHED
        finalizeRatings()
        val pens = if (homePens != null) " (Penaltılar: $homePens-$awayPens)" else ""
        ev(EventType.FULL_TIME, null, "Maç sona erdi: ${home.name} ${home.goals} - ${away.goals} ${away.name}$pens")
    }

    /* ---------------- GÜÇ HESABI ---------------- */

    private fun computeStr(side: TeamSide): Str {
        var gk = 0.0; var def = 0.0; var defW = 0.0; var mid = 0.0; var midW = 0.0; var att = 0.0; var attW = 0.0
        var count = 0
        side.lineup.forEachIndexed { i, id ->
            if (id < 0) return@forEachIndexed
            val pl = p(id) ?: return@forEachIndexed
            count++
            val pos = side.slots[i]
            val fit = fitness[id] ?: 100.0
            val cond = 0.70 + 0.30 * fit / 100.0
            val r = pl.ratingAt(pos) * cond * (0.95 + pl.morale / 1000.0) * (0.97 + pl.form / 1666.0)
            val ins = side.tactic.instructions[id]
            var wD: Double; var wM: Double; var wA: Double
            when (pos) {
                Position.GK -> { gk = r; return@forEachIndexed }
                Position.CB -> { wD = 1.0; wM = 0.0; wA = 0.0 }
                Position.LB, Position.RB -> { wD = 0.8; wM = 0.15; wA = 0.05 }
                Position.DM -> { wD = 0.5; wM = 0.5; wA = 0.0 }
                Position.CM -> { wD = 0.15; wM = 0.75; wA = 0.10 }
                Position.AM -> { wD = 0.0; wM = 0.5; wA = 0.5 }
                Position.LW, Position.RW -> { wD = 0.0; wM = 0.2; wA = 0.8 }
                Position.ST -> { wD = 0.0; wM = 0.05; wA = 0.95 }
            }
            when (ins) {
                PlayerInstruction.GET_FORWARD -> { wA += 0.15; wD = max(0.0, wD - 0.15) }
                PlayerInstruction.STAY_BACK -> { wD += 0.15; wA = max(0.0, wA - 0.15) }
                else -> {}
            }
            def += r * wD; defW += wD; mid += r * wM; midW += wM; att += r * wA; attW += wA
        }
        val s = Str(
            gk = if (gk > 0) gk else 25.0,
            def = if (defW > 0) def / defW else 35.0,
            mid = if (midW > 0) mid / midW else 35.0,
            att = if (attW > 0) att / attW else 35.0
        )
        val t = side.tactic
        when (t.press) { PressIntensity.HIGH -> { s.mid *= 1.04; s.def *= 0.985 }; PressIntensity.LOW -> { s.mid *= 0.97; s.def *= 1.02 }; else -> {} }
        when (t.passing) { PassingStyle.SHORT -> { s.mid *= 1.03; s.att *= 0.985 }; PassingStyle.DIRECT -> { s.att *= 1.03; s.mid *= 0.97 }; else -> {} }
        when (t.tempo) { Tempo.FAST -> { s.freq = 1.08; s.att *= 1.01 }; Tempo.SLOW -> { s.freq = 0.92; s.def *= 1.02 }; else -> {} }
        when (t.defLine) { DefensiveLine.HIGH -> { s.mid *= 1.02; s.def *= 0.98 }; DefensiveLine.DEEP -> { s.def *= 1.03; s.mid *= 0.98 }; else -> {} }
        val numF = (count / 11.0).pow(1.3)
        s.def *= numF; s.mid *= numF; s.att *= numF
        if (!neutral && side === home) { s.def *= 1.03; s.mid *= 1.03; s.att *= 1.03 }
        return s
    }

    private fun ensureStr() {
        if (dirty || minute % 5 == 0) { strH = computeStr(home); strA = computeStr(away); dirty = false }
    }

    /* ---------------- DAKİKA SİMÜLASYONU ---------------- */

    private fun simulateMinute() {
        ensureStr()
        drainFitness(home); drainFitness(away)
        val k = 2.2
        val pHome = strH.mid.pow(k) / (strH.mid.pow(k) + strA.mid.pow(k))
        val attSide = if (rnd.nextDouble() < pHome) home else away
        val defSide = other(attSide)
        val aStr = if (attSide === home) strH else strA
        val dStr = if (attSide === home) strA else strH
        attSide.stats.possessionTicks++
        attSide.stats.passes += 4 + rnd.nextInt(6)

        val ratio = (aStr.att / dStr.def).coerceIn(0.4, 2.5)
        val chanceP = 0.165 * ratio.pow(1.8) * attSide.tactic.mentality.atk / defSide.tactic.mentality.def * aStr.freq
        val r = rnd.nextDouble()
        when {
            r < chanceP -> chance(attSide, defSide, ratio, aStr, dStr)
            r < chanceP + 0.045 -> corner(attSide, defSide, ratio)
            r < chanceP + 0.067 -> { attSide.stats.offsides++ }
        }
        // fauller (her iki takım)
        for (s in listOf(home, away)) {
            var fp = 0.11
            if (s.tactic.press == PressIntensity.HIGH) fp += 0.02
            if (rnd.nextDouble() < fp) foul(s)
        }
        // sakatlık
        if (rnd.nextDouble() < 0.0038) injury(if (rnd.nextBoolean()) home else away)
        // 2025 kuralı: kaleci topu 8 saniyeden fazla tutarsa rakibe korner
        if (rnd.nextDouble() < 0.0007) {
            val gkId = defSide.lineup.getOrNull(0)?.takeIf { it >= 0 }
            ev(EventType.GK_8_SECONDS, attSide, "8 saniye kuralı! ${name(gkId)} topu çok uzun tuttu, hakem ${attSide.name} lehine korner verdi.", gkId)
            attSide.stats.corners++
        }
        // oyun içi performans dalgalanması
        for (s in listOf(home, away)) for (id in s.onPitch()) if (rnd.nextInt(30) == 0) ratings[id] = (ratings[id] ?: 6.0) + (rnd.nextDouble() - 0.48) * 0.12
    }

    private fun drainFitness(s: TeamSide) {
        val press = when (s.tactic.press) { PressIntensity.LOW -> 0.9; PressIntensity.MEDIUM -> 1.0; PressIntensity.HIGH -> 1.22 }
        val tempo = when (s.tactic.tempo) { Tempo.SLOW -> 0.92; Tempo.NORMAL -> 1.0; Tempo.FAST -> 1.1 }
        for (id in s.onPitch()) {
            val pl = p(id) ?: continue
            val gkF = if (pl.position == Position.GK) 0.35 else 1.0
            val d = 0.22 * (1.35 - pl.attrs.physical / 100.0 * 0.7) * press * tempo * gkF
            fitness[id] = max(0.0, (fitness[id] ?: 100.0) - d)
        }
    }

    private fun pickShooter(s: TeamSide, header: Boolean = false): Int? {
        val cands = s.lineup.mapIndexedNotNull { i, id -> if (id < 0) null else Triple(id, s.slots[i], p(id)) }
        if (cands.isEmpty()) return null
        val weights = cands.map { (id, pos, pl) ->
            pl ?: return@map 0.0
            var w = when (pos) {
                Position.ST -> 3.0; Position.LW, Position.RW -> 2.3; Position.AM -> 2.3
                Position.CM -> 1.3; Position.DM -> 0.6; Position.LB, Position.RB -> 0.5
                Position.CB -> if (header) 2.2 else 0.35; Position.GK -> 0.0
            }
            if (s.tactic.instructions[id] == PlayerInstruction.SHOOT_MORE) w *= 1.6
            w * (if (header) pl.heightCm / 180.0 * pl.attrs.physical / 70.0 else 0.6 + pl.attrs.shooting / 175.0)
        }
        return weightedPick(cands.map { it.first }, weights)
    }

    private fun pickAssister(s: TeamSide, exclude: Int): Int? {
        val cands = s.lineup.mapIndexedNotNull { i, id -> if (id < 0 || id == exclude) null else id to s.slots[i] }
        if (cands.isEmpty()) return null
        val weights = cands.map { (id, pos) ->
            val pl = p(id) ?: return@map 0.0
            val w = when (pos) {
                Position.AM -> 3.0; Position.LW, Position.RW -> 2.6; Position.CM -> 2.0; Position.ST -> 1.6
                Position.LB, Position.RB -> 1.3; Position.DM -> 0.8; Position.CB -> 0.3; Position.GK -> 0.05
            }
            val c = if (s.tactic.instructions[id] == PlayerInstruction.CROSS_MORE) 1.3 else 1.0
            w * c * (pl.attrs.passing + pl.attrs.vision) / 120.0
        }
        return weightedPick(cands.map { it.first }, weights)
    }

    private fun <T> weightedPick(items: List<T>, w: List<Double>): T? {
        val total = w.sum()
        if (total <= 0 || items.isEmpty()) return items.firstOrNull()
        var x = rnd.nextDouble() * total
        for (i in items.indices) { x -= w[i]; if (x <= 0) return items[i] }
        return items.last()
    }

    private fun gkOf(s: TeamSide): Player? = s.lineup.getOrNull(0)?.takeIf { it >= 0 }?.let { p(it) }

    private fun chance(att: TeamSide, def: TeamSide, ratio: Double, aStr: Str, dStr: Str) {
        // %3: penaltı
        if (rnd.nextDouble() < 0.03) { penalty(att, def); return }
        val roll = rnd.nextDouble()
        val (kind, baseXg) = when {
            roll < 0.10 -> "big" to (0.30 + rnd.nextDouble() * 0.25)
            roll < 0.78 -> "normal" to (0.05 + rnd.nextDouble() * 0.11)
            else -> "long" to (0.02 + rnd.nextDouble() * 0.05)
        }
        var xg = baseXg * ratio.pow(0.35)
        if (att.tactic.passing == PassingStyle.DIRECT) xg *= 0.95
        val shooterId = pickShooter(att) ?: return
        shot(att, def, shooterId, xg.coerceIn(0.01, 0.75), kind, header = false)
    }

    private fun shot(att: TeamSide, def: TeamSide, shooterId: Int, xg: Double, kind: String, header: Boolean, assisted: Boolean = true) {
        val shooter = p(shooterId) ?: return
        val gk = gkOf(def)
        att.stats.shots++; att.stats.xg += xg
        val finish = 1.0 + (shooter.attrs.shooting - 70) / 100.0 + (shooter.attrs.composure - 70) / 200.0
        val keeper = if (gk == null) 0.5 else 1.0 + (gk.attrs.goalkeeping * (0.7 + 0.3 * (fitness[gk.id] ?: 100.0) / 100) - 70) / 120.0
        val goalP = (xg * finish / keeper).coerceIn(0.01, 0.85)
        val r = rnd.nextDouble()
        if (r < goalP) {
            att.stats.onTarget++
            // VAR kontrolü
            if (rnd.nextDouble() < 0.05) {
                ev(EventType.VAR, att, "VAR incelemesi… ${shooter.shortName}'in golü ofsayt gerekçesiyle İPTAL edildi!", shooterId)
                return
            }
            val assister = if (assisted && rnd.nextDouble() < 0.72) pickAssister(att, shooterId) else null
            scoreGoal(att, def, shooterId, assister, kind, header)
        } else if (r < goalP + (1 - goalP) * 0.32) {
            att.stats.onTarget++
            def.stats.saves++
            gk?.let { ratings[it.id] = (ratings[it.id] ?: 6.0) + if (kind == "big") 0.35 else 0.15 }
            ratings[shooterId] = (ratings[shooterId] ?: 6.0) + 0.08
            if (detailed && (kind == "big" || rnd.nextInt(3) == 0)) ev(EventType.SAVE, def, Commentary.save(shooter.shortName, gk?.shortName ?: "kaleci", att.name, rnd), gk?.id, shooterId)
        } else {
            ratings[shooterId] = (ratings[shooterId] ?: 6.0) - if (kind == "big") 0.25 else 0.04
            if (detailed && kind == "big") ev(EventType.CHANCE, att, Commentary.miss(shooter.shortName, att.name, rnd), shooterId)
        }
    }

    private fun scoreGoal(att: TeamSide, def: TeamSide, scorer: Int, assister: Int?, kind: String, header: Boolean, type: EventType = EventType.GOAL) {
        att.goals++; att.stats.goals++
        goalsBy[scorer] = (goalsBy[scorer] ?: 0) + 1
        ratings[scorer] = (ratings[scorer] ?: 6.0) + 1.0
        assister?.let { assistsBy[it] = (assistsBy[it] ?: 0) + 1; ratings[it] = (ratings[it] ?: 6.0) + 0.6 }
        def.lineup.forEachIndexed { i, id ->
            if (id < 0) return@forEachIndexed
            val pen = when (def.slots[i]) { Position.GK -> 0.35; Position.CB, Position.LB, Position.RB -> 0.18; Position.DM -> 0.08; else -> 0.0 }
            ratings[id] = (ratings[id] ?: 6.0) - pen
        }
        val score = "${home.goals}-${away.goals}"
        val text = if (type == EventType.PENALTY_GOAL) "GOOOL! ${name(scorer)} penaltıyı gole çevirdi! ($score)"
        else Commentary.goal(name(scorer), assister?.let { name(it) }, att.name, header, kind, score, rnd)
        ev(type, att, text, scorer, assister)
        dirty = true
    }

    private fun penalty(att: TeamSide, def: TeamSide) {
        val takerId = att.tactic.penaltyTakerId?.takeIf { it in att.onPitch() }
            ?: att.onPitch().filter { p(it)?.position != Position.GK }.maxByOrNull { (p(it)?.attrs?.shooting ?: 0) + (p(it)?.attrs?.composure ?: 0) } ?: return
        val taker = p(takerId) ?: return
        if (rnd.nextBoolean()) ev(EventType.VAR, att, "VAR incelemesi sonrası hakem beyaz noktayı gösterdi: ${att.name} lehine PENALTI!")
        else ev(EventType.INFO, att, "Ceza sahasında faul! ${att.name} penaltı kazandı.")
        att.stats.shots++; att.stats.xg += 0.76
        val gk = gkOf(def)
        val conv = (0.76 * (1 + (taker.attrs.shooting + taker.attrs.composure - 140) / 300.0) / (1 + ((gk?.attrs?.goalkeeping ?: 50) - 70) / 300.0)).coerceIn(0.45, 0.93)
        if (rnd.nextDouble() < conv) {
            att.stats.onTarget++
            scoreGoal(att, def, takerId, null, "pen", false, EventType.PENALTY_GOAL)
        } else {
            val saved = rnd.nextBoolean()
            if (saved) { att.stats.onTarget++; def.stats.saves++; gk?.let { ratings[it.id] = (ratings[it.id] ?: 6.0) + 0.8 } }
            ratings[takerId] = (ratings[takerId] ?: 6.0) - 0.6
            ev(EventType.PENALTY_MISS, att, if (saved) "${gk?.shortName ?: "Kaleci"} penaltıda ${taker.shortName}'i durdurdu! Müthiş kurtarış!" else "${taker.shortName} penaltıda topu auta gönderdi!", takerId)
        }
    }

    private fun corner(att: TeamSide, def: TeamSide, ratio: Double) {
        att.stats.corners++
        if (rnd.nextDouble() < 0.24) {
            val shooter = pickShooter(att, header = true) ?: return
            shot(att, def, shooter, (0.04 + rnd.nextDouble() * 0.10) * ratio.pow(0.3), "corner", header = true)
        } else if (detailed && rnd.nextInt(5) == 0) ev(EventType.CORNER, att, "${att.name} korner kullandı, savunma uzaklaştırdı.")
    }

    private fun foul(s: TeamSide) {
        val cands = s.lineup.mapIndexedNotNull { i, id -> if (id < 0 || s.slots[i] == Position.GK) null else id to s.slots[i] }
        if (cands.isEmpty()) return
        val w = cands.map { (id, pos) ->
            val base = when (pos) { Position.CB, Position.DM -> 2.0; Position.LB, Position.RB, Position.CM -> 1.5; else -> 0.9 }
            base * (if (s.tactic.instructions[id] == PlayerInstruction.TACKLE_HARDER) 1.6 else 1.0)
        }
        val fouler = weightedPick(cands.map { it.first }, w) ?: return
        s.stats.fouls++
        val pl = p(fouler) ?: return
        var yp = 0.14
        if (s.tactic.instructions[fouler] == PlayerInstruction.TACKLE_HARDER) yp += 0.08
        if (s.tactic.press == PressIntensity.HIGH) yp += 0.03
        if (pl.personality == Personality.TEMPERAMENTAL) yp += 0.05
        val r = rnd.nextDouble()
        when {
            r < 0.0055 -> sendOff(s, fouler, straight = true)
            r < yp -> {
                if (fouler in s.yellow) {
                    secondYellow.add(fouler)
                    s.stats.yellows++
                    ev(EventType.SECOND_YELLOW, s, "${pl.shortName} ikinci sarı karttan kırmızı kart gördü! ${s.name} 10 kişi kaldı.", fouler)
                    removeFromPitch(s, fouler); s.sentOff.add(fouler)
                    ratings[fouler] = (ratings[fouler] ?: 6.0) - 1.2
                    s.stats.reds++
                } else {
                    s.yellow.add(fouler); s.stats.yellows++
                    ratings[fouler] = (ratings[fouler] ?: 6.0) - 0.3
                    ev(EventType.YELLOW, s, Commentary.yellow(pl.shortName, rnd), fouler)
                }
            }
            r < yp + 0.06 -> {
                // tehlikeli serbest vuruş
                val att = other(s)
                val taker = att.tactic.freeKickTakerId?.takeIf { it in att.onPitch() }
                    ?: att.onPitch().maxByOrNull { (p(it)?.attrs?.shooting ?: 0) + (p(it)?.attrs?.passing ?: 0) }
                if (taker != null) shot(att, s, taker, 0.05 + rnd.nextDouble() * 0.04, "fk", header = false, assisted = false)
            }
        }
    }

    private fun sendOff(s: TeamSide, id: Int, straight: Boolean) {
        val pl = p(id) ?: return
        straightRed.add(id)
        s.stats.reds++
        ratings[id] = (ratings[id] ?: 6.0) - 1.5
        ev(EventType.RED, s, "DİREKT KIRMIZI KART! ${pl.shortName} sert müdahalesi sonrası oyundan atıldı. (VAR onayladı)", id)
        removeFromPitch(s, id); s.sentOff.add(id)
    }

    private fun removeFromPitch(s: TeamSide, id: Int) {
        val i = s.lineup.indexOf(id)
        if (i >= 0) {
            s.lineup[i] = -1
            // kaleci atıldıysa: bir oyuncuyu çıkarıp yedek kaleci sok (AI)
            if (i == 0 && (!s.isUser || userAutoSubs)) {
                val gkSub = s.bench.firstOrNull { p(it)?.position == Position.GK }
                val outId = s.lineup.withIndex().filter { it.value >= 0 && it.index != 0 }.maxByOrNull { it.index }?.value
                if (gkSub != null && outId != null && canSub(s)) { makeSub(s, outId, gkSub, forced = true); val j = s.lineup.indexOf(gkSub); if (j > 0) { s.lineup[j] = -1; s.lineup[0] = gkSub } }
            }
        }
        dirty = true
    }

    private fun injury(s: TeamSide) {
        val on = s.onPitch()
        if (on.isEmpty()) return
        val w = on.map { 1.0 + (100 - (fitness[it] ?: 100.0)) / 25.0 }
        val id = weightedPick(on, w) ?: return
        s.injured.add(id)
        ev(EventType.INJURY, s, "${name(id)} sakatlandı ve oyuna devam edemiyor.", id)
        if ((!s.isUser || userAutoSubs) && canSub(s)) {
            val i = s.lineup.indexOf(id)
            val pos = s.slots.getOrElse(i) { Position.CM }
            val best = s.bench.filter { it !in s.subbedOn }.maxByOrNull { p(it)?.ratingAt(pos) ?: 0 }
            if (best != null) { makeSub(s, id, best, forced = true); return }
        }
        if (!s.isUser || userAutoSubs || !canSub(s)) removeFromPitch(s, id)
        // kullanıcı canlı oynuyorsa sakat oyuncu sahada (verimsiz) kalır, değişiklik bekler
        if (s.isUser && !userAutoSubs && canSub(s)) fitness[id] = 10.0
    }

    /* ---------------- DEĞİŞİKLİK ---------------- */

    fun canSub(s: TeamSide): Boolean = s.subsUsed < maxSubs && (s.subWindowsUsed < maxWindows || s.subsThisMinute || phase == MatchPhase.HALF_TIME || phase == MatchPhase.ET_BREAK)

    /** Oyuncu değişikliği. Başarılıysa null, değilse hata mesajı döner. */
    fun makeSub(s: TeamSide, outId: Int, inId: Int, forced: Boolean = false): String? {
        if (finished) return "Maç bitti."
        if (!canSub(s)) return "Değişiklik hakkınız kalmadı (5 oyuncu / 3 pencere)."
        val idx = s.lineup.indexOf(outId)
        if (idx < 0) return "Çıkacak oyuncu sahada değil."
        if (inId !in s.bench || inId in s.subbedOn) return "Giren oyuncu yedeklerde değil."
        s.lineup[idx] = inId
        s.bench.remove(inId)
        s.subbedOff.add(outId); s.subbedOn.add(inId); s.played.add(inId)
        s.subsUsed++
        val freeWindow = phase == MatchPhase.HALF_TIME || phase == MatchPhase.ET_BREAK || phase == MatchPhase.NOT_STARTED
        if (!s.subsThisMinute && !freeWindow) s.subWindowsUsed++
        s.subsThisMinute = true
        ratings[inId] = 6.0
        if (!fitness.containsKey(inId)) fitness[inId] = (p(inId)?.fitness ?: 100).toDouble()
        ev(EventType.SUB, s, "Oyuncu değişikliği (${s.name}): ${name(inId)} oyuna girdi, ${name(outId)} çıktı.", inId, outId)
        dirty = true
        return null
    }

    fun changeTactic(s: TeamSide, t: Tactic) {
        val formationChanged = t.formation != s.tactic.formation
        s.tactic = t
        if (formationChanged) {
            s.slots.clear(); s.slots.addAll(t.formation.slots)
            ev(EventType.INFO, s, "${s.name} dizilişini ${t.formation.tr} olarak değiştirdi.")
        } else ev(EventType.INFO, s, "${s.name} taktik değişikliğine gitti: ${t.mentality.tr} oyun.")
        dirty = true
    }

    private fun aiHalfTime(s: TeamSide) {
        if (s.isUser && !userAutoSubs) return
        // devre arasında en yorgun / kart görmüş riskli oyuncuyu değiştir
        val diff = s.goals - other(s).goals
        val worst = s.onPitch().filter { it != s.lineup.getOrNull(0) }.minByOrNull { (ratings[it] ?: 6.0) + (fitness[it] ?: 100.0) / 100.0 - (if (it in s.yellow) 0.6 else 0.0) }
        if (worst != null && (ratings[worst] ?: 6.0) < 6.0 && rnd.nextInt(2) == 0) aiSubFor(s, worst)
        if (diff <= -2 && s.tactic.mentality.ordinal < Mentality.ATTACKING.ordinal) s.tactic = s.tactic.copy(mentality = Mentality.ATTACKING)
    }

    private fun aiManage(s: TeamSide) {
        if (s.isUser && !userAutoSubs) return
        if (minute in listOf(60, 70, 80, 110) && added == 0 && canSub(s)) {
            val tired = s.onPitch().filter { it != s.lineup.getOrNull(0) }
                .sortedBy { (fitness[it] ?: 100.0) / 20.0 + (ratings[it] ?: 6.0) - (if (it in s.yellow) 0.8 else 0.0) }
            var made = 0
            for (id in tired) {
                if (made >= 2 || !canSub(s)) break
                if ((fitness[id] ?: 100.0) < 72 || (ratings[id] ?: 6.0) < 6.0 || (minute >= 80 && made == 0 && rnd.nextBoolean())) {
                    if (aiSubFor(s, id)) made++
                }
            }
        }
        if (minute == 72 && added == 0) {
            val diff = s.goals - other(s).goals
            if (diff < 0 && s.tactic.mentality.ordinal < Mentality.VERY_ATTACKING.ordinal) {
                s.tactic = s.tactic.copy(mentality = Mentality.values()[s.tactic.mentality.ordinal + 1]); dirty = true
                if (detailed) ev(EventType.INFO, s, "${s.name} skoru değiştirmek için daha hücumcu oynamaya başladı.")
            } else if (diff == 1 && minute >= 72 && s.tactic.mentality.ordinal > Mentality.DEFENSIVE.ordinal) {
                s.tactic = s.tactic.copy(mentality = Mentality.DEFENSIVE); dirty = true
                if (detailed) ev(EventType.INFO, s, "${s.name} skoru korumak için geri çekildi.")
            }
        }
    }

    private fun aiSubFor(s: TeamSide, outId: Int): Boolean {
        val i = s.lineup.indexOf(outId)
        if (i < 0) return false
        val pos = s.slots[i]
        val best = s.bench.filter { it !in s.subbedOn && p(it)?.position != Position.GK }
            .maxByOrNull { (p(it)?.ratingAt(pos) ?: 0) * (0.7 + 0.3 * (fitness[it] ?: 100.0) / 100.0) } ?: return false
        return makeSub(s, outId, best) == null
    }

    /* ---------------- PENALTILAR ---------------- */

    private fun shootout() {
        fun takers(s: TeamSide) = s.onPitch().filter { p(it)?.position != Position.GK }
            .sortedByDescending { (p(it)?.attrs?.shooting ?: 0) + (p(it)?.attrs?.composure ?: 0) }.ifEmpty { s.onPitch() }
        val th = takers(home); val ta = takers(away)
        var hp = 0; var ap = 0; var round = 0
        fun kick(s: TeamSide, list: List<Int>, def: TeamSide): Boolean {
            val id = list[round % list.size]; val t = p(id)!!
            val gk = gkOf(def)
            val conv = (0.75 * (1 + (t.attrs.shooting + t.attrs.composure - 140) / 300.0) / (1 + ((gk?.attrs?.goalkeeping ?: 50) - 70) / 300.0)).coerceIn(0.5, 0.92)
            val ok = rnd.nextDouble() < conv
            ev(EventType.SHOOTOUT, s, if (ok) "${t.shortName} penaltıyı attı." else "${t.shortName} penaltıyı kaçırdı!", id)
            return ok
        }
        while (true) {
            if (kick(home, th, away)) hp++
            if (round < 5 && hp > ap + (5 - round)) break
            if (kick(away, ta, home)) ap++
            round++
            if (round < 5) {
                if (hp > ap + (5 - round) || ap > hp + (5 - round)) break
            } else if (hp != ap) break
            if (round > 30) { hp++; break }
        }
        homePens = hp; awayPens = ap
        val w = if (hp > ap) home else away
        ev(EventType.INFO, w, "${w.name} seri penaltılarda $hp-$ap kazandı!")
    }

    /* ---------------- REYTİNG ---------------- */

    private fun finalizeRatings() {
        for (s in listOf(home, away)) {
            val diff = s.goals - other(s).goals
            val conceded = other(s).goals
            for (id in s.played) {
                var r = ratings[id] ?: 6.0
                r += when { diff > 0 -> 0.3; diff < 0 -> -0.2; else -> 0.0 }
                val pl = p(id)
                if (conceded == 0 && pl != null && (pl.position == Position.GK || pl.position.line == Line.DEF) && id in s.starting) r += 0.6
                r += (rnd.nextDouble() - 0.5) * 0.6
                ratings[id] = r.coerceIn(3.0, 10.0)
            }
        }
    }

    fun motm(): Int? = ratings.maxByOrNull { it.value }?.key

    fun report(fixtureId: Int): MatchReport = MatchReport(
        fixtureId, home.id, away.id, events.toList(), home.stats, away.stats, ratings.toMap(),
        home.starting, away.starting, homePens, awayPens, motm()
    )

    fun possessionHome(): Int {
        val t = home.stats.possessionTicks + away.stats.possessionTicks
        return if (t == 0) 50 else (home.stats.possessionTicks * 100 / t)
    }
}

/** Türkçe maç anlatımı şablonları. */
object Commentary {
    fun goal(scorer: String, assister: String?, team: String, header: Boolean, kind: String, score: String, rnd: Random): String {
        val base = when {
            header -> listOf("GOOOL! $scorer kafayla ağları sarstı!", "GOL! Korner sonrası $scorer'in kafa vuruşu filelerde!", "$scorer yükseldi ve kafayla golü buldu!")
            kind == "long" -> listOf("MUHTEŞEM GOL! $scorer ceza sahası dışından füze gibi bir şut!", "İnanılmaz! $scorer uzaktan köşeye bıraktı!")
            kind == "fk" -> listOf("FRİKİK GOLÜ! $scorer topu barajın üzerinden ağlara gönderdi!", "Harika serbest vuruş! $scorer doksana taktı!")
            kind == "big" -> listOf("GOOOL! $scorer karşı karşıya pozisyonda hata yapmadı!", "GOL! $scorer boş pozisyonda topu ağlara yolladı!", "$scorer kaleciyi de geçip golü attı!")
            else -> listOf("GOOOL! $scorer harika vurdu!", "GOL! $team öne geçmek için bastırıyordu, $scorer affetmedi!", "$scorer ceza sahasında döndü ve vurdu: GOL!", "GOL! $scorer'in şutu kaleciden sekip ağlarla buluştu!")
        }
        val a = assister?.let { listOf(" Asist: $it.", " Pas $it'den.", " $it'nin harika pasında.").random(rnd) } ?: ""
        return base.random(rnd) + a + " ($score)"
    }
    fun save(shooter: String, gk: String, team: String, rnd: Random) = listOf(
        "$shooter vurdu, $gk müthiş bir refleksle çıkardı!", "$gk, $shooter'in şutunda gole izin vermedi.",
        "$team tehlikeli geldi: $shooter'in şutunda $gk yerinde!", "Kurtarış! $gk köşeden çeldi."
    ).random(rnd)
    fun miss(shooter: String, team: String, rnd: Random) = listOf(
        "İnanılmaz kaçan gol! $shooter boş kaleye atamadı!", "$shooter karşı karşıya kaldı ama top direğin yanından auta gitti!",
        "$team büyük fırsat kaçırdı, $shooter'in vuruşu üstten dışarıda!", "Direk! $shooter'in şutu direkten döndü!"
    ).random(rnd)
    fun yellow(name: String, rnd: Random) = listOf(
        "$name sert müdahalesi nedeniyle sarı kart gördü.", "Hakem $name'e sarı kartını gösterdi.",
        "$name taktik faul yaptı, sarı kart.", "$name itirazı nedeniyle sarı kart gördü (yalnızca kaptan hakemle konuşabilir)."
    ).random(rnd)
}

/** Takım tarafı oluşturucuları (kulüp ve milli takım). */
object SideFactory {
    fun forClub(gs: GameState, club: Club, isUser: Boolean): TeamSide {
        val tactic: Tactic
        val lineup: MutableList<Int>
        val bench: MutableList<Int>
        if (isUser) {
            Lineups.ensureUserLineup(gs, club)
            tactic = club.tactic.copy(instructions = club.tactic.instructions.toMutableMap())
            lineup = club.tactic.lineup.toMutableList()
            bench = club.tactic.bench.toMutableList()
        } else {
            val (l, b) = Lineups.autoPick(Lineups.squadOf(gs, club), club.tactic.formation, rotate = true)
            tactic = club.tactic.copy()
            lineup = l; bench = b
        }
        return TeamSide(club.id, club.name, isUser, tactic, lineup, bench, club.reputation, club.primaryColor)
    }

    fun forNation(gs: GameState, nt: NationalTeam): TeamSide {
        val squad = nt.squad.mapNotNull { gs.players[it] }
        val formation = Formation.F4231
        val (l, b) = Lineups.autoPick(squad, formation, rotate = false)
        return TeamSide(nt.id, nt.name, false, Tactic(formation = formation), l, b, nt.reputation)
    }
}
