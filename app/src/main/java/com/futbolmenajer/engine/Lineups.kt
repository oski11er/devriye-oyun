package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*

/** Otomatik kadro seçimi ve kadro yardımcıları. */
object Lineups {

    /** Maç için seçilebilir mi? */
    fun selectable(p: Player): Boolean = p.injuryDays == 0 && p.suspendedMatches == 0

    private fun pickScore(p: Player, pos: Position, rotate: Boolean): Double {
        var s = p.ratingAt(pos).toDouble()
        val fit = p.fitness
        s *= if (rotate) (0.55 + 0.45 * fit / 100.0) else (0.80 + 0.20 * fit / 100.0)
        s *= 0.97 + p.form / 100.0 * 0.06
        return s
    }

    /** Diziliş için en iyi 11 + yedekler. */
    fun autoPick(players: List<Player>, formation: Formation, rotate: Boolean = true, benchSize: Int = 9): Pair<MutableList<Int>, MutableList<Int>> {
        val pool = players.filter { selectable(it) }.toMutableList()
        val lineup = MutableList(formation.slots.size) { -1 }
        // önce kaleci ve kıt pozisyonlar: slotları "uzmana ihtiyaç" sırasına göre doldur
        val order = formation.slots.indices.sortedBy { if (formation.slots[it] == Position.GK) 0 else 1 }
        for (i in order) {
            val pos = formation.slots[i]
            val best = pool.maxByOrNull { pickScore(it, pos, rotate) } ?: continue
            lineup[i] = best.id
            pool.remove(best)
        }
        // yedekler: 1 kaleci + en iyi diğerleri
        val bench = mutableListOf<Int>()
        pool.filter { it.position == Position.GK }.maxByOrNull { it.overall }?.let { bench.add(it.id); pool.remove(it) }
        pool.sortedByDescending { it.overall * (0.7 + 0.3 * it.fitness / 100.0) }.take(benchSize - bench.size).forEach { bench.add(it.id) }
        return lineup.filter { it >= 0 }.toMutableList().let { l ->
            if (l.size == lineup.size) lineup to bench else {
                // eksik oyuncu varsa yedeklerden doldur
                val filled = lineup.toMutableList()
                for (i in filled.indices) if (filled[i] < 0 && bench.isNotEmpty()) filled[i] = bench.removeAt(bench.size - 1)
                filled.filter { it >= 0 }.toMutableList() to bench
            }
        }
    }

    fun squadOf(gs: GameState, club: Club): List<Player> = club.playerIds.mapNotNull { gs.players[it] }

    /** Kullanıcının taktiğindeki ilk 11'i doğrular; geçersizse otomatik tamamlar. */
    fun ensureUserLineup(gs: GameState, club: Club): List<String> {
        val notes = mutableListOf<String>()
        val t = club.tactic
        val squad = squadOf(gs, club)
        val valid = t.lineup.size == t.formation.slots.size && t.lineup.all { id ->
            id in club.playerIds && gs.players[id]?.let { selectable(it) } == true
        } && t.lineup.toSet().size == t.lineup.size
        if (!valid) {
            val keep = t.lineup.filter { id -> id in club.playerIds && gs.players[id]?.let { selectable(it) } == true }
            val (auto, bench) = autoPick(squad, t.formation, rotate = t.autoRotate)
            if (t.lineup.isNotEmpty()) notes.add("İlk 11'de oynayamayacak oyuncular vardı; kadro otomatik tamamlandı.")
            // mümkün olduğunca kullanıcı seçimini koru
            val result = MutableList(t.formation.slots.size) { -1 }
            for (i in result.indices) {
                val prev = t.lineup.getOrNull(i)
                if (prev != null && prev in keep && prev !in result) result[i] = prev
            }
            val pool = (auto + bench).filter { it !in result }.toMutableList()
            for (i in result.indices) if (result[i] < 0) {
                val pos = t.formation.slots[i]
                val best = pool.mapNotNull { gs.players[it] }.maxByOrNull { it.ratingAt(pos) }
                if (best != null) { result[i] = best.id; pool.remove(best.id) }
            }
            t.lineup = result.filter { it >= 0 }.toMutableList()
            t.bench = pool.take(9).toMutableList()
        }
        t.bench = t.bench.filter { id -> id in club.playerIds && id !in t.lineup && gs.players[id]?.let { selectable(it) } == true }.toMutableList()
        if (t.bench.size < 9) {
            squad.filter { selectable(it) && it.id !in t.lineup && it.id !in t.bench }
                .sortedByDescending { it.overall }.take(9 - t.bench.size).forEach { t.bench.add(it.id) }
        }
        return notes
    }

    /** Takımın ortalama gücü (ilk 11). */
    fun teamStrength(gs: GameState, club: Club): Int {
        val squad = squadOf(gs, club)
        if (squad.isEmpty()) return 40
        val (l, _) = autoPick(squad, club.tactic.formation, rotate = false, benchSize = 0)
        val r = l.mapIndexedNotNull { i, id -> gs.players[id]?.ratingAt(club.tactic.formation.slots.getOrElse(i) { Position.CM }) }
        return if (r.isEmpty()) 40 else r.average().toInt()
    }
}
