package com.futbolmenajer.engine

import com.futbolmenajer.data.db.LeagueCatalog
import com.futbolmenajer.data.model.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.random.Random

/**
 * SEZON TAKVİMİ
 * Gün 0 = 1 Temmuz. Sezon sonu = gün 340 (≈ 6 Haziran).
 * - Lig maçları: cumartesileri (gerekirse salı ara haftaları)
 * - Şampiyonlar Ligi (salı), Avrupa Ligi / Konferans Ligi (perşembe)
 * - Kupa: çarşamba
 * - Milli ara: Eylül, Ekim, Kasım, Mart
 * - Transfer dönemleri: 1 Tem – 1 Eyl (gün 0-62) ve 1 Oca – 1 Şub (gün 184-215)
 */
object Scheduler {

    const val SEASON_END_DAY = 340
    const val UCL = 30001
    const val UEL = 30002
    const val UECL = 30003
    const val NATIONS = 30020
    const val CUP_BASE = 10000
    const val SUPER_BASE = 20000

    val intlWindows = listOf(62..72, 97..107, 132..142, 264..274)
    val intlMatchDays = listOf(64, 67, 99, 102, 134, 137, 266, 269)
    val uclDays = listOf(77, 91, 112, 126, 147, 161, 203, 211)
    val ueDays = uclDays.map { it + 2 }
    val ueclDays = listOf(79, 93, 114, 128, 149, 163)
    val koDays = listOf(231 to 238, 252 to 259, 280 to 287, 301 to 308)  // play-off, son 16, çeyrek, yarı
    val cupDays = listOf(56, 120, 169, 197, 218, 245, 295, 326)
    const val SUPER_CUP_DAY = 38

    fun date(season: Int, day: Int): LocalDate = LocalDate.of(season, 7, 1).plusDays(day.toLong())

    fun dateStr(season: Int, day: Int): String {
        val d = date(season, day)
        val m = d.month.getDisplayName(TextStyle.FULL, Locale("tr"))
        val w = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("tr"))
        return "${d.dayOfMonth} $m ${d.year}, $w"
    }

    fun seasonLabel(season: Int) = "$season-${(season + 1) % 100}"

    fun transferWindowOpen(day: Int) = day in 0..62 || day in 184..215

    fun clubIdsInCountry(gs: GameState, country: String): List<Int> =
        gs.leagues.values.filter { it.country == country }.sortedBy { it.tier }.flatMap { it.clubIds }

    /* ============================================================ */

    fun setupSeason(gs: GameState, rnd: Random = Random(gs.rngSeed + gs.season)) {
        gs.fixtures.clear(); gs.competitions.clear(); gs.tables.clear(); gs.matchReports.clear()
        gs.day = 0
        // --- Ligler
        val saturdays = (0..330).filter { date(gs.season, it).dayOfWeek == DayOfWeek.SATURDAY && it >= 44 && it <= 318 }
            .filter { d -> intlWindows.none { d in it } }
        val tuesdays = (0..330).filter { date(gs.season, it).dayOfWeek == DayOfWeek.TUESDAY && it in 50..315 }
            .filter { d -> intlWindows.none { d in it } && d !in uclDays && d !in koDays.flatMap { listOf(it.first, it.second) } && d !in cupDays }
        for (lg in gs.leagues.values) {
            val comp = Competition(lg.id, lg.name, CompetitionType.LEAGUE, lg.country, lg.id, lg.clubIds.toMutableList())
            gs.competitions[comp.id] = comp
            gs.tables[comp.id] = lg.clubIds.map { TableRow(it) }.toMutableList()
            val rounds = roundRobin(lg.clubIds.shuffled(rnd), double = true)
            val days = pickDays(rounds.size, saturdays, tuesdays)
            rounds.forEachIndexed { r, pairs ->
                for ((h, a) in pairs) gs.fixtures.add(Fixture(gs.newId(), comp.id, days[r], h, a, "${r + 1}. Hafta"))
            }
        }
        // --- Yerel kupalar ve süper kupalar
        LeagueCatalog.countries.forEachIndexed { ci, cd ->
            val ids = clubIdsInCountry(gs, cd.code)
            if (ids.size < 8) return@forEachIndexed
            val size = when { ids.size >= 64 -> 64; ids.size >= 32 -> 32; ids.size >= 16 -> 16; else -> 8 }
            val entrants = ids.take(size).toMutableList()
            val names = koRoundNames(size)
            val cup = Competition(CUP_BASE + ci, cd.cupName, CompetitionType.DOMESTIC_CUP, cd.code, null, entrants, roundNames = names)
            gs.competitions[cup.id] = cup
            val firstDay = cupDays[cupDays.size - names.size]
            createKoRound(gs, cup, entrants.shuffled(rnd), firstDay, null, names[0], rnd)
            // süper kupa: geçen sezon lig şampiyonu vs kupa şampiyonu
            val topLeague = gs.leagues.values.firstOrNull { it.country == cd.code && it.tier == 1 } ?: return@forEachIndexed
            val champ = gs.lastLeagueChampions[cd.code] ?: topLeague.clubIds.maxByOrNull { gs.clubs.getValue(it).reputation }!!
            var cupW = gs.lastCupWinners[cd.code] ?: topLeague.clubIds.filter { it != champ }.maxByOrNull { gs.clubs.getValue(it).reputation }!!
            if (cupW == champ) cupW = gs.lastLeagueFinal[topLeague.id]?.getOrNull(1) ?: topLeague.clubIds.first { it != champ }
            val sc = Competition(SUPER_BASE + ci, "${cd.nameTr} Süper Kupası", CompetitionType.SUPER_CUP, cd.code, null, mutableListOf(champ, cupW), roundNames = listOf("Final"))
            gs.competitions[sc.id] = sc
            gs.fixtures.add(Fixture(gs.newId(), sc.id, SUPER_CUP_DAY, champ, cupW, "Final", knockout = true, neutral = true))
        }
        // --- Avrupa kupaları
        setupEurope(gs, rnd)
        // --- Milli maçlar
        val nat = Competition(NATIONS, "Milli Maçlar (Uluslar Ligi / Eleme / Hazırlık)", CompetitionType.NATIONS_FRIENDLY,
            participants = gs.nationalTeams.values.map { it.id }.toMutableList())
        gs.competitions[nat.id] = nat
        for (d in intlMatchDays) {
            // benzer güçteki takımları eşleştir
            val teams = gs.nationalTeams.values.map { it to it.fifaPoints + rnd.nextInt(-80, 80) }.sortedByDescending { it.second }.map { it.first }
            var i = 0
            while (i + 1 < teams.size) {
                val (h, a) = if (rnd.nextBoolean()) teams[i] to teams[i + 1] else teams[i + 1] to teams[i]
                gs.fixtures.add(Fixture(gs.newId(), NATIONS, d, h.id, a.id, "Milli Maç", isNational = true))
                i += 2
            }
        }
        gs.fixtures.sortBy { it.day }
    }

    private fun pickDays(n: Int, sats: List<Int>, tues: List<Int>): List<Int> {
        if (n <= sats.size) {
            // eşit aralıklı seç
            return (0 until n).map { sats[(it * (sats.size - 1).toDouble() / (n - 1).coerceAtLeast(1)).toInt()] }
        }
        val all = (sats + tues.take(n - sats.size)).sorted()
        return all.take(n)
    }

    /** Daire yöntemiyle fikstür. double = true → rövanşlı. */
    fun roundRobin(teams: List<Int>, double: Boolean): List<List<Pair<Int, Int>>> {
        val list = teams.toMutableList()
        if (list.size % 2 == 1) list.add(-1)
        val n = list.size
        val rounds = mutableListOf<List<Pair<Int, Int>>>()
        val arr = list.toMutableList()
        for (r in 0 until n - 1) {
            val pairs = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until n / 2) {
                val a = arr[i]; val b = arr[n - 1 - i]
                if (a == -1 || b == -1) continue
                pairs.add(if ((r + i) % 2 == 0) a to b else b to a)
            }
            rounds.add(pairs)
            // rotasyon (ilk eleman sabit)
            val last = arr.removeAt(n - 1)
            arr.add(1, last)
        }
        if (double) {
            val second = rounds.map { rd -> rd.map { (h, a) -> a to h } }
            return rounds + second
        }
        return rounds
    }

    fun koRoundNames(size: Int): List<String> {
        val all = listOf("1. Tur", "2. Tur", "Son 32", "Son 16", "Çeyrek Final", "Yarı Final", "Final")
        val rounds = Integer.numberOfTrailingZeros(size) // 64 → 6 tur
        return when (rounds) {
            6 -> listOf("1. Tur", "Son 32", "Son 16", "Çeyrek Final", "Yarı Final", "Final")
            5 -> listOf("Son 32", "Son 16", "Çeyrek Final", "Yarı Final", "Final")
            4 -> listOf("Son 16", "Çeyrek Final", "Yarı Final", "Final")
            else -> all.takeLast(rounds)
        }
    }

    /** Eleme turu oluşturur. secondLegDay != null → iki ayaklı. */
    fun createKoRound(gs: GameState, comp: Competition, teams: List<Int>, day: Int, secondLegDay: Int?, label: String, rnd: Random, seeded: Boolean = false) {
        val list = if (seeded) teams else teams.shuffled(rnd)
        val half = list.size / 2
        for (i in 0 until half) {
            // seeded: ilk yarı güçlü takımlar, ikinci ayağı evinde oynar
            val strong = if (seeded) list[i] else list[2 * i]
            val weak = if (seeded) list[list.size - 1 - i] else list[2 * i + 1]
            if (secondLegDay == null) {
                gs.fixtures.add(Fixture(gs.newId(), comp.id, day, strong, weak, label, knockout = true, neutral = label == "Final" && comp.type != CompetitionType.DOMESTIC_CUP))
            } else {
                val l1 = Fixture(gs.newId(), comp.id, day, weak, strong, "$label (1. maç)", knockout = true, leg = 1)
                gs.fixtures.add(l1)
                gs.fixtures.add(Fixture(gs.newId(), comp.id, secondLegDay, strong, weak, "$label (rövanş)", knockout = true, firstLegId = l1.id, leg = 2))
            }
        }
        comp.stage = label
        gs.fixtures.sortBy { it.day }
    }

    /* -------------------- AVRUPA -------------------- */

    private fun slots(rank: Int): Triple<Int, Int, Int> = when (rank) {
        in 1..4 -> Triple(4, 2, 1)
        5 -> Triple(3, 2, 1)
        6 -> Triple(2, 2, 1)
        in 7..10 -> Triple(2, 1, 1)
        in 11..15 -> Triple(1, 1, 2)
        in 16..25 -> Triple(1, 1, 1)
        else -> Triple(1, 0, 1)
    }

    fun setupEurope(gs: GameState, rnd: Random) {
        val uefa = LeagueCatalog.countries.filter { it.uefa }.sortedByDescending { gs.countryCoefficients[it.code] ?: it.coefficient }
        val uclCand = mutableListOf<Int>(); val uelCand = mutableListOf<Int>(); val ueclCand = mutableListOf<Int>()
        uefa.forEachIndexed { idx, cd ->
            val top = gs.leagues.values.firstOrNull { it.country == cd.code && it.tier == 1 } ?: return@forEachIndexed
            val order = gs.lastLeagueFinal[top.id]?.filter { it in top.clubIds }?.takeIf { it.size >= 6 }
                ?: top.clubIds.sortedByDescending { gs.clubs.getValue(it).reputation }
            val (a, b, c) = slots(idx + 1)
            uclCand += order.take(a); uelCand += order.drop(a).take(b); ueclCand += order.drop(a + b).take(c)
        }
        val coef = { id: Int -> val c = gs.clubs.getValue(id); c.reputation + (gs.countryCoefficients[c.country] ?: 10.0) * 0.12 }
        val ucl = uclCand.sortedByDescending(coef).take(36)
        val uel = (uclCand - ucl.toSet() + uelCand).sortedByDescending(coef).take(36)
        val uecl = (uclCand + uelCand - ucl.toSet() - uel.toSet() + ueclCand).sortedByDescending(coef).take(36)
        makeLeaguePhase(gs, UCL, "UEFA Şampiyonlar Ligi", CompetitionType.UCL, ucl, uclDays, rnd)
        makeLeaguePhase(gs, UEL, "UEFA Avrupa Ligi", CompetitionType.UEL, uel, ueDays, rnd)
        makeLeaguePhase(gs, UECL, "UEFA Konferans Ligi", CompetitionType.UECL, uecl, ueclDays, rnd)
    }

    private fun makeLeaguePhase(gs: GameState, id: Int, name: String, type: CompetitionType, teams: List<Int>, days: List<Int>, rnd: Random) {
        if (teams.size < 8) return
        val comp = Competition(id, name, type, participants = teams.toMutableList(), stage = "Lig Aşaması",
            roundNames = listOf("Lig Aşaması", "Play-off", "Son 16", "Çeyrek Final", "Yarı Final", "Final"))
        gs.competitions[id] = comp
        gs.tables[id] = teams.map { TableRow(it) }.toMutableList()
        // torbalara göre dizip daire yöntemi → her takım farklı rakiplerle oynar
        val pots = teams.chunked(9)
        val interleaved = mutableListOf<Int>()
        for (i in 0 until 9) pots.forEach { pot -> pot.getOrNull(i)?.let { interleaved.add(it) } }
        val rounds = roundRobin(interleaved.shuffled(rnd), double = false)
        days.forEachIndexed { r, d ->
            rounds.getOrNull(r * 3 % rounds.size)?.forEach { (h, a) ->
                gs.fixtures.add(Fixture(gs.newId(), id, d, h, a, "Lig Aşaması ${r + 1}. Maç"))
            }
        }
    }

    /* -------------------- ELEME İLERLEMESİ -------------------- */

    /** İki ayaklı / tek maçlık eşleşmenin galibi. */
    fun tieWinner(gs: GameState, fx: Fixture): Int? {
        if (!fx.played) return null
        if (fx.leg == 1) return null
        if (fx.leg == 2) {
            val first = gs.fixtures.firstOrNull { it.id == fx.firstLegId } ?: return fx.winnerId()
            if (fx.homePens != null && fx.awayPens != null) return if (fx.homePens!! > fx.awayPens!!) fx.homeId else fx.awayId
            val homeAgg = fx.homeGoals + first.awayGoals
            val awayAgg = fx.awayGoals + first.homeGoals
            return when { homeAgg > awayAgg -> fx.homeId; awayAgg > homeAgg -> fx.awayId; else -> fx.homeId }
        }
        return fx.winnerId() ?: fx.homeId
    }

    /** Gün sonunda turnuvaları ilerletir (yeni tur kurası, şampiyon belirleme). */
    fun progressCompetitions(gs: GameState, rnd: Random, onChampion: (Competition, Int) -> Unit) {
        for (comp in gs.competitions.values.toList()) {
            if (comp.finished) continue
            when (comp.type) {
                CompetitionType.DOMESTIC_CUP, CompetitionType.SUPER_CUP -> progressKo(gs, comp, rnd, onChampion) { idx -> cupDays.getOrNull(cupDays.size - comp.roundNames.size + idx)?.let { it to null } }
                CompetitionType.UCL, CompetitionType.UEL, CompetitionType.UECL -> progressEurope(gs, comp, rnd, onChampion)
                else -> {}
            }
        }
    }

    private fun currentRoundFixtures(gs: GameState, comp: Competition): List<Fixture> =
        gs.fixtures.filter { it.competitionId == comp.id && it.knockout && it.roundLabel.startsWith(comp.stage) }

    private fun progressKo(gs: GameState, comp: Competition, rnd: Random, onChampion: (Competition, Int) -> Unit, dayFor: (Int) -> Pair<Int, Int?>?) {
        val fx = currentRoundFixtures(gs, comp)
        if (fx.isEmpty() || fx.any { !it.played }) return
        val winners = fx.filter { it.leg != 1 }.mapNotNull { tieWinner(gs, it) }
        if (winners.size == 1 || comp.stage == "Final") {
            comp.finished = true; comp.winnerId = winners.firstOrNull(); comp.stage = "Tamamlandı"
            winners.firstOrNull()?.let { onChampion(comp, it) }
            return
        }
        comp.round++
        val label = comp.roundNames.getOrElse(comp.round) { "Tur ${comp.round + 1}" }
        val days = dayFor(comp.round) ?: return
        createKoRound(gs, comp, winners, days.first.coerceAtLeast(gs.day + 1), days.second, label, rnd)
    }

    private fun progressEurope(gs: GameState, comp: Competition, rnd: Random, onChampion: (Competition, Int) -> Unit) {
        if (comp.round == 0) {
            val league = gs.fixtures.filter { it.competitionId == comp.id && !it.knockout }
            if (league.isEmpty() || league.any { !it.played }) return
            val table = sortedTable(gs, comp.id)
            val top8 = table.take(8).map { it.clubId }
            val po = table.drop(8).take(16).map { it.clubId }
            comp.groups["top8"] = top8.toMutableList()
            comp.round = 1
            // play-off: 9-24 → 9v24, 10v23 ...
            createKoRound(gs, comp, po, koDays[0].first, koDays[0].second, "Play-off", rnd, seeded = true)
            return
        }
        val fx = currentRoundFixtures(gs, comp)
        if (fx.isEmpty() || fx.any { !it.played }) return
        val winners = fx.filter { it.leg != 1 }.mapNotNull { tieWinner(gs, it) }
        if (comp.stage == "Final") {
            comp.finished = true; comp.winnerId = winners.firstOrNull(); comp.stage = "Tamamlandı"
            winners.firstOrNull()?.let { onChampion(comp, it) }
            return
        }
        comp.round++
        when (comp.round) {
            2 -> { // son 16: ilk 8 + play-off galipleri
                val top8 = comp.groups["top8"] ?: mutableListOf()
                val list = top8 + winners.shuffled(rnd)
                createKoRound(gs, comp, list, koDays[1].first, koDays[1].second, "Son 16", rnd, seeded = true)
            }
            3 -> createKoRound(gs, comp, winners, koDays[2].first, koDays[2].second, "Çeyrek Final", rnd)
            4 -> createKoRound(gs, comp, winners, koDays[3].first, koDays[3].second, "Yarı Final", rnd)
            5 -> {
                val day = when (comp.id) { UCL -> 333; UEL -> 323; else -> 330 }
                createKoRound(gs, comp, winners, day, null, "Final", rnd)
            }
        }
    }

    fun sortedTable(gs: GameState, compId: Int): List<TableRow> =
        (gs.tables[compId] ?: mutableListOf()).sortedWith(compareByDescending<TableRow> { it.pts }.thenByDescending { it.gd }.thenByDescending { it.gf }.thenBy { gs.clubs[it.clubId]?.name ?: "" })

    fun isUclType(t: CompetitionType) = t == CompetitionType.UCL || t == CompetitionType.UEL || t == CompetitionType.UECL
}
