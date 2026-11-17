package com.futbolmenajer.engine

import com.futbolmenajer.data.db.LeagueCatalog
import com.futbolmenajer.data.db.NameBank
import com.futbolmenajer.data.db.WorldGenerator
import com.futbolmenajer.data.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/** Gün ilerletmenin neden durduğu. */
sealed class Stop {
    data class MatchDay(val fixtureId: Int) : Stop()
    data class Event(val reason: String) : Stop()
    object Press : Stop()
    object NewSeason : Stop()
    object Sacked : Stop()
    object Limit : Stop()
}

/**
 * OYUN MOTORU — sezonun günlük akışını yönetir.
 */
object GameEngine {

    val rnd = Random(System.nanoTime())
    private var importantToday = false

    /* ==================== KARİYER ==================== */

    fun newWorld(seed: Long, progress: (Float, String) -> Unit): GameState {
        val gs = WorldGenerator.generate(seed, progress)
        Scheduler.setupSeason(gs)
        callUpNationalSquads(gs)
        progress(1f, "Hazır!")
        return gs
    }

    fun startCareer(gs: GameState, clubId: Int, managerName: String) {
        val firstTime = gs.userClubId < 0
        gs.userClubId = clubId
        gs.managerName = managerName
        gs.sacked = false
        gs.jobOffers.clear()
        val club = gs.userClub
        if (firstTime) gs.managerReputation = (club.reputation * 0.7).roundToInt().coerceIn(20, 75)
        club.boardConfidence = 70
        club.tactic.lineup.clear(); club.tactic.bench.clear()
        Lineups.ensureUserLineup(gs, club)
        setObjective(gs)
        val league = gs.leagues.getValue(club.leagueId)
        msg(gs, MsgType.BOARD, "${club.name}'e hoş geldiniz!",
            "Sayın $managerName,\n\n${club.name} yönetim kurulu olarak sizi teknik direktörümüz olarak görmekten mutluluk duyuyoruz.\n\n" +
                "Sezon hedefi: ${gs.objective?.description}\n" +
                "Transfer bütçesi: ${Economy.fmt(club.finances.transferBudget)}\n" +
                "Haftalık maaş bütçesi: ${Economy.fmt(club.finances.weeklyWageBudget)}\n\n" +
                "Lig: ${league.name} (${league.tier}. seviye)\nBaşarılar dileriz!")
        msg(gs, MsgType.INFO, "Nasıl oynanır?",
            "• Üstteki ▶ DEVAM düğmesi günleri ilerletir; maç günü veya önemli bir olay olduğunda durur.\n" +
                "• Kadro & Taktik ekranından ilk 11'i, dizilişi ve oyuncu talimatlarını belirleyin.\n" +
                "• Transfer ekranından oyuncu arayın; bonservis, takas, kiralık veya serbest teklif yapın.\n" +
                "• Tesisler ekranından stadyum, antrenman, akademi gibi tesisleri yükseltin.\n" +
                "• Oyunu Menü > Kaydet ile kaydetmeyi unutmayın.")
    }

    fun setObjective(gs: GameState) {
        val club = gs.userClub
        val league = gs.leagues.getValue(club.leagueId)
        val rank = league.clubIds.mapNotNull { gs.clubs[it] }.sortedByDescending { it.reputation }.indexOfFirst { it.id == club.id } + 1
        val n = league.clubIds.size
        val (target, desc) = when {
            rank == 1 -> 1 to "Lig şampiyonluğu"
            rank <= 3 && league.tier == 1 -> 3 to "İlk 3'te bitirmek (Avrupa kupaları)"
            rank <= 6 -> min(6, rank + 2) to "İlk ${min(6, rank + 2)}'da bitirmek"
            league.tier > 1 && rank <= league.promotions + 2 -> max(1, league.promotions) to "Üst lige yükselmek"
            rank <= n / 2 -> (n / 2) to "Ligin ilk yarısında bitirmek"
            else -> (n - league.relegations) to "Ligde kalmak"
        }
        gs.objective = SeasonObjective(desc, target)
    }

    fun msg(gs: GameState, type: MsgType, title: String, body: String, offerId: Int? = null) {
        gs.inbox.add(0, InboxMessage(gs.newId(), gs.day, type, title, body, offerId = offerId))
        if (gs.inbox.size > 250) gs.inbox.subList(250, gs.inbox.size).clear()
        if (type == MsgType.TRANSFER || type == MsgType.BOARD || type == MsgType.INJURY || type == MsgType.YOUTH) importantToday = true
    }

    /* ==================== FİKSTÜR YARDIMCILARI ==================== */

    fun userFixtureToday(gs: GameState): Fixture? =
        gs.fixtures.firstOrNull { it.day == gs.day && !it.played && !it.isNational && (it.homeId == gs.userClubId || it.awayId == gs.userClubId) }

    fun nextUserFixture(gs: GameState): Fixture? =
        gs.fixtures.firstOrNull { !it.played && !it.isNational && (it.homeId == gs.userClubId || it.awayId == gs.userClubId) }

    fun userFixtures(gs: GameState): List<Fixture> =
        gs.fixtures.filter { !it.isNational && (it.homeId == gs.userClubId || it.awayId == gs.userClubId) }

    fun teamName(gs: GameState, id: Int): String = gs.clubName(id)

    /* ==================== GÜN İLERLETME ==================== */

    /** Bir sonraki maç gününe veya önemli olaya kadar ilerler. */
    fun advance(gs: GameState, maxDays: Int = 45): Stop {
        var steps = 0
        while (steps < maxDays) {
            if (gs.sacked) return Stop.Sacked
            userFixtureToday(gs)?.let { return Stop.MatchDay(it.id) }
            if (gs.pendingPress != null) return Stop.Press
            importantToday = false
            val newSeason = finishDay(gs)
            steps++
            if (newSeason) return Stop.NewSeason
            if (gs.sacked) return Stop.Sacked
            if (gs.pendingPress != null) return Stop.Press
            if (importantToday) return Stop.Event("Yeni önemli mesajlarınız var.")
        }
        return Stop.Limit
    }

    /** Günün kalan maçlarını oynatır, günlük işlemleri yapar ve günü bir artırır. true → yeni sezon başladı. */
    fun finishDay(gs: GameState): Boolean {
        val today = gs.fixtures.filter { it.day == gs.day && !it.played }
        for (fx in today) simulateFixture(gs, fx)
        Scheduler.progressCompetitions(gs, rnd) { comp, winner -> onChampion(gs, comp, winner) }
        dailyUpdate(gs)
        if (gs.day >= Scheduler.SEASON_END_DAY) {
            seasonEnd(gs)
            return true
        }
        gs.day++
        // milli ara başlangıcında kadro çağrısı
        if (Scheduler.intlWindows.any { it.first == gs.day }) callUpNationalSquads(gs)
        // maç öncesi basın toplantısı
        val tomorrow = gs.fixtures.firstOrNull { it.day == gs.day && !it.played && !it.isNational && (it.homeId == gs.userClubId || it.awayId == gs.userClubId) }
        if (tomorrow != null && rnd.nextDouble() < 0.30) gs.pendingPress = Press.preMatch(gs, tomorrow, rnd)
        return false
    }

    /* ==================== MAÇ OYNATMA ==================== */

    fun firstLegScore(gs: GameState, fx: Fixture): Pair<Int, Int>? {
        if (fx.leg != 2) return null
        val f = gs.fixtures.firstOrNull { it.id == fx.firstLegId } ?: return null
        // rövanşın ev sahibi = ilk maçın deplasmanı → (ev sahibinin ilk maç golleri, deplasmanın ilk maç golleri)
        return f.awayGoals to f.homeGoals
    }

    fun buildSim(gs: GameState, fx: Fixture, detailed: Boolean, userLive: Boolean): MatchSim {
        val home: TeamSide; val away: TeamSide
        if (fx.isNational) {
            val h = gs.nationalTeams.values.first { it.id == fx.homeId }
            val a = gs.nationalTeams.values.first { it.id == fx.awayId }
            home = SideFactory.forNation(gs, h); away = SideFactory.forNation(gs, a)
        } else {
            home = SideFactory.forClub(gs, gs.clubs.getValue(fx.homeId), fx.homeId == gs.userClubId)
            away = SideFactory.forClub(gs, gs.clubs.getValue(fx.awayId), fx.awayId == gs.userClubId)
        }
        return MatchSim(gs, fx, home, away, knockout = fx.knockout, neutral = fx.neutral, firstLeg = firstLegScore(gs, fx),
            detailed = detailed, userAutoSubs = !userLive)
    }

    fun simulateFixture(gs: GameState, fx: Fixture) {
        val userInvolved = fx.homeId == gs.userClubId || fx.awayId == gs.userClubId
        val sim = buildSim(gs, fx, detailed = userInvolved, userLive = false)
        sim.runToEnd()
        applyResult(gs, fx, sim)
    }

    private val injuryTypes = listOf(
        Triple("Darbe / ezilme", 2..7, 30), Triple("Ayak bileği burkulması", 7..21, 18), Triple("Hamstring zorlanması", 10..28, 18),
        Triple("Kasık ağrısı", 7..18, 10), Triple("Baldır kas yırtığı", 14..35, 10), Triple("Diz bağı zorlanması", 21..60, 7),
        Triple("Metatars kırığı", 50..90, 4), Triple("Ön çapraz bağ yırtığı", 180..270, 3)
    )

    fun injure(gs: GameState, p: Player, minor: Boolean = false) {
        val list = if (minor) injuryTypes.take(3) else injuryTypes
        val total = list.sumOf { it.third }
        var x = rnd.nextInt(total)
        var chosen = list.first()
        for (t in list) { x -= t.third; if (x < 0) { chosen = t; break } }
        val med = p.contract?.clubId?.let { gs.clubs[it]?.facilities?.medicalLevel } ?: 3
        val days = (chosen.second.random(rnd) * (1.15 - med * 0.05)).roundToInt().coerceAtLeast(1)
        p.injuryDays = max(p.injuryDays, days)
        p.injuryName = chosen.first
        if (p.contract?.clubId == gs.userClubId) {
            msg(gs, MsgType.INJURY, "Sakatlık: ${p.fullName}", "${p.fullName} – ${chosen.first}. Tahmini dönüş: ${days} gün. (Sağlık merkezi seviyesi: $med)")
        }
    }

    fun applyResult(gs: GameState, fx: Fixture, sim: MatchSim) {
        fx.played = true
        fx.homeGoals = sim.homeGoals; fx.awayGoals = sim.awayGoals
        fx.homePens = sim.homePens; fx.awayPens = sim.awayPens; fx.extraTime = sim.extraTime
        val comp = gs.competitions[fx.competitionId]
        val userInvolved = fx.homeId == gs.userClubId || fx.awayId == gs.userClubId
        if (fx.isNational) {
            applyNational(gs, fx, sim); return
        }
        val homeClub = gs.clubs.getValue(fx.homeId)
        val awayClub = gs.clubs.getValue(fx.awayId)
        // seyirci + gelirler
        val imp = when (comp?.type) { CompetitionType.LEAGUE -> 1.0; CompetitionType.DOMESTIC_CUP -> if (fx.roundLabel == "Final") 1.3 else 0.8; CompetitionType.SUPER_CUP -> 1.2; else -> 1.15 }
        fx.attendance = if (fx.neutral) 50_000 else Economy.attendance(homeClub, imp)
        val gate = fx.attendance.toLong() * homeClub.finances.ticketPrice
        if (fx.neutral) {
            Economy.addLedger(gs, homeClub, "Bilet geliri payı", gate / 3); Economy.addLedger(gs, awayClub, "Bilet geliri payı", gate / 3)
        } else Economy.addLedger(gs, homeClub, "Bilet geliri (${fx.attendance} seyirci)", gate)
        if (comp != null) when (comp.type) {
            CompetitionType.LEAGUE -> {
                val lg = gs.leagues[comp.id]
                if (lg != null) {
                    val per = Economy.seasonTv(lg) / max(1, (lg.clubIds.size - 1) * 2)
                    Economy.addLedger(gs, homeClub, "TV yayın geliri", per); Economy.addLedger(gs, awayClub, "TV yayın geliri", per)
                }
                updateTable(gs, comp.id, fx)
            }
            CompetitionType.UCL, CompetitionType.UEL, CompetitionType.UECL -> {
                val mult = when (comp.type) { CompetitionType.UCL -> 1.0; CompetitionType.UEL -> 0.23; else -> 0.13 }
                val winP = (2_100_000 * mult).toLong(); val drawP = (700_000 * mult).toLong()
                when {
                    fx.homeGoals > fx.awayGoals -> Economy.addLedger(gs, homeClub, "${comp.name} galibiyet primi", winP)
                    fx.awayGoals > fx.homeGoals -> Economy.addLedger(gs, awayClub, "${comp.name} galibiyet primi", winP)
                    else -> { Economy.addLedger(gs, homeClub, "${comp.name} beraberlik primi", drawP); Economy.addLedger(gs, awayClub, "${comp.name} beraberlik primi", drawP) }
                }
                if (!fx.knockout) updateTable(gs, comp.id, fx)
                // ülke katsayısı
                val res = fx.homeGoals.compareTo(fx.awayGoals)
                fun addCoef(c: Club, pts: Double) { gs.countryCoefficients[c.country] = (gs.countryCoefficients[c.country] ?: 10.0) + pts }
                when { res > 0 -> addCoef(homeClub, 0.12); res < 0 -> addCoef(awayClub, 0.12); else -> { addCoef(homeClub, 0.05); addCoef(awayClub, 0.05) } }
            }
            else -> {}
        }
        // oyuncu istatistikleri
        for (side in listOf(sim.home, sim.away)) {
            val won = side.goals > (if (side === sim.home) sim.away.goals else sim.home.goals)
            val lost = side.goals < (if (side === sim.home) sim.away.goals else sim.home.goals)
            val conceded = if (side === sim.home) sim.away.goals else sim.home.goals
            for (id in side.played) {
                val p = gs.players[id] ?: continue
                val r = sim.ratings[id] ?: 6.0
                p.stats.apps++; p.careerApps++
                p.stats.ratingSum += r; p.lastRating = r
                val g = sim.goalsBy[id] ?: 0
                p.stats.goals += g; p.careerGoals += g
                p.stats.assists += sim.assistsBy[id] ?: 0
                if (conceded == 0 && p.position == Position.GK && id in side.starting) p.stats.cleanSheets++
                p.form = (p.form * 0.6 + ((r - 4.0) * 20).coerceIn(0.0, 100.0) * 0.4).roundToInt()
                p.fitness = (sim.fitness[id] ?: p.fitness.toDouble()).roundToInt().coerceIn(0, 100)
                p.sharpness = min(100, p.sharpness + 8)
                p.morale = (p.morale + when { won -> 4; lost -> -4; else -> 0 }).coerceIn(5, 100)
            }
            for (id in side.yellow) {
                val p = gs.players[id] ?: continue
                p.stats.yellow++; p.seasonYellows++
                if (id !in sim.secondYellow && p.seasonYellows % 5 == 0) {
                    p.suspendedMatches += 1
                    if (p.contract?.clubId == gs.userClubId) msg(gs, MsgType.INFO, "Cezalı: ${p.fullName}", "${p.fullName} ${p.seasonYellows}. sarı kartını gördü ve 1 maç cezalı.")
                }
            }
            for (id in side.sentOff) {
                val p = gs.players[id] ?: continue
                p.stats.red++
                val ban = if (id in sim.straightRed) 2 + rnd.nextInt(2) else 1
                p.suspendedMatches += ban
                if (p.contract?.clubId == gs.userClubId) msg(gs, MsgType.INFO, "Kırmızı kart cezası: ${p.fullName}", "${p.fullName} disiplin kurulu tarafından $ban maç men cezası aldı.")
            }
            for (id in side.injured) gs.players[id]?.let { injure(gs, it) }
        }
        // ceza düşürme (maçta oynamayan cezalılar)
        for (club in listOf(homeClub, awayClub)) for (id in club.playerIds) {
            val p = gs.players[id] ?: continue
            if (p.suspendedMatches > 0 && id !in sim.home.played && id !in sim.away.played) p.suspendedMatches--
        }
        if (userInvolved) {
            gs.matchReports[fx.id] = sim.report(fx.id)
            if (gs.matchReports.size > 60) gs.matchReports.keys.minOrNull()?.let { gs.matchReports.remove(it) }
            val user = gs.userClub
            val ug = if (fx.homeId == user.id) fx.homeGoals else fx.awayGoals
            val og = if (fx.homeId == user.id) fx.awayGoals else fx.homeGoals
            val oppRep = gs.clubs[if (fx.homeId == user.id) fx.awayId else fx.homeId]?.reputation ?: 50
            val expect = (user.reputation - oppRep) / 10.0
            val delta = when { ug > og -> 3 - expect.coerceIn(-2.0, 2.0); ug < og -> -3 - expect.coerceIn(-2.0, 2.0); else -> -expect }
            user.fanMood = (user.fanMood + delta.roundToInt()).coerceIn(0, 100)
            user.boardConfidence = (user.boardConfidence + (delta / 2).roundToInt()).coerceIn(0, 100)
            gs.managerReputation = (gs.managerReputation + if (ug > og && oppRep > user.reputation) 1 else 0).coerceAtMost(99)
            if (user.boardConfidence < 8 && gs.day > 60) sack(gs, "Yönetim kurulu kötü sonuçlar nedeniyle görevinize son verdi.")
            else if (rnd.nextDouble() < 0.35) gs.pendingPress = Press.postMatch(gs, fx, ug, og, rnd)
        }
    }

    private fun updateTable(gs: GameState, compId: Int, fx: Fixture) {
        val t = gs.tables[compId] ?: return
        val h = t.firstOrNull { it.clubId == fx.homeId } ?: return
        val a = t.firstOrNull { it.clubId == fx.awayId } ?: return
        h.p++; a.p++; h.gf += fx.homeGoals; h.ga += fx.awayGoals; a.gf += fx.awayGoals; a.ga += fx.homeGoals
        when {
            fx.homeGoals > fx.awayGoals -> { h.w++; a.l++ }
            fx.homeGoals < fx.awayGoals -> { a.w++; h.l++ }
            else -> { h.d++; a.d++ }
        }
    }

    private fun applyNational(gs: GameState, fx: Fixture, sim: MatchSim) {
        val h = gs.nationalTeams.values.first { it.id == fx.homeId }
        val a = gs.nationalTeams.values.first { it.id == fx.awayId }
        // Elo benzeri FIFA puanı
        val exp = 1.0 / (1.0 + 10.0.pow((a.fifaPoints - h.fifaPoints) / 600.0))
        val res = when { fx.homeGoals > fx.awayGoals -> 1.0; fx.homeGoals < fx.awayGoals -> 0.0; else -> 0.5 }
        val k = 30.0
        h.fifaPoints += k * (res - exp); a.fifaPoints -= k * (res - exp)
        for (side in listOf(sim.home, sim.away)) for (id in side.played) {
            val p = gs.players[id] ?: continue
            p.internationalCaps++
            p.fitness = (sim.fitness[id] ?: p.fitness.toDouble()).roundToInt()
            if (id in side.injured) injure(gs, p, minor = true)
        }
    }

    /* ==================== GÜNLÜK / HAFTALIK ==================== */

    private fun dailyUpdate(gs: GameState) {
        val user = gs.userClub
        for (p in gs.players.values) {
            if (p.injuryDays > 0) {
                p.injuryDays--
                if (p.injuryDays == 0) {
                    p.injuryName = null
                    if (p.contract?.clubId == gs.userClubId) msg(gs, MsgType.INFO, "İyileşti: ${p.fullName}", "${p.fullName} sakatlığını atlattı ve antrenmanlara döndü.")
                }
            }
            if (p.fitness < 100) {
                val med = 3
                p.fitness = min(100, p.fitness + 7 + med / 2 - (if (p.birthYear < gs.season - 30) 2 else 0))
            }
        }
        // planlı ödemeler
        val due = gs.scheduledPayments.filter { it.season < gs.season || (it.season == gs.season && it.day <= gs.day) }
        for (sp in due) {
            gs.clubs[sp.fromClub]?.let { Economy.addLedger(gs, it, sp.desc, -sp.amount) }
            gs.clubs[sp.toClub]?.let { Economy.addLedger(gs, it, sp.desc, sp.amount) }
        }
        gs.scheduledPayments.removeAll(due)
        // tesis yükseltmesi
        val up = user.facilities.upgradeInProgress
        if (up != null) {
            up.daysLeft--
            if (up.daysLeft <= 0) {
                completeUpgrade(user.facilities, up.type)
                user.facilities.upgradeInProgress = null
                msg(gs, MsgType.BOARD, "Tesis yükseltmesi tamamlandı", "${up.type.tr} seviye ${Economy.facilityLevel(user.facilities, up.type)} oldu!")
            }
        }
        if (gs.day % 7 == 0) weekly(gs)
        TransferMarket.processDaily(gs)
        if (gs.day == 250) youthIntake(gs)
        if (gs.day == 230) contractWarnings(gs)
        if (gs.day % 30 == 15) boardCheck(gs)
    }

    fun completeUpgrade(f: Facilities, t: FacilityType) {
        when (t) {
            FacilityType.STADIUM -> { f.stadiumLevel++; f.stadiumCapacity = (f.stadiumCapacity * 1.12 + 4000).toInt().coerceAtMost(100_000) }
            FacilityType.TRAINING -> f.trainingLevel++
            FacilityType.YOUTH -> f.youthLevel++
            FacilityType.MEDICAL -> f.medicalLevel++
            FacilityType.SCOUTING -> f.scoutingLevel++
            FacilityType.COMMERCIAL -> f.commercialLevel++
        }
    }

    /** Kullanıcının tesis yükseltme isteği. */
    fun requestUpgrade(gs: GameState, t: FacilityType): String {
        val club = gs.userClub
        val f = club.facilities
        if (f.upgradeInProgress != null) return "Zaten devam eden bir yükseltme var: ${f.upgradeInProgress!!.type.tr} (${f.upgradeInProgress!!.daysLeft} gün kaldı)."
        val lvl = Economy.facilityLevel(f, t)
        if (lvl >= 10) return "${t.tr} zaten en üst seviyede."
        val cost = Economy.facilityUpgradeCost(t, lvl)
        if (cost > club.finances.balance * 0.7) return "Yönetim: Kulüp kasası bu yatırım için yeterli değil. (Maliyet ${Economy.fmt(cost)}, kasa ${Economy.fmt(club.finances.balance)})"
        Economy.addLedger(gs, club, "Tesis yatırımı: ${t.tr}", -cost)
        f.upgradeInProgress = FacilityUpgrade(t, cost, Economy.facilityUpgradeDays(t, lvl))
        return "${t.tr} yükseltmesi başladı. Süre: ${Economy.facilityUpgradeDays(t, lvl)} gün, maliyet ${Economy.fmt(cost)}."
    }

    private fun weekly(gs: GameState) {
        training(gs)
        // ücretler (tek geçiş)
        val wages = HashMap<Int, Long>()
        for (p in gs.players.values) {
            val c = p.contract ?: continue
            val lf = c.loanFromClubId
            if (lf != null) {
                wages[c.clubId] = (wages[c.clubId] ?: 0) + c.weeklyWage * c.loanWageShare / 100
                wages[lf] = (wages[lf] ?: 0) + c.weeklyWage * (100 - c.loanWageShare) / 100
            } else wages[c.clubId] = (wages[c.clubId] ?: 0) + c.weeklyWage
        }
        for (club in gs.clubs.values) {
            Economy.addLedger(gs, club, "Haftalık maaşlar", -(wages[club.id] ?: 0))
            Economy.addLedger(gs, club, "Sponsorluk ve mağaza", Economy.weeklyCommercial(club))
            val f = club.facilities
            val maint = (f.stadiumLevel + f.trainingLevel + f.youthLevel + f.medicalLevel + f.scoutingLevel + f.commercialLevel) * 600L * (1 + f.stadiumCapacity / 25000)
            Economy.addLedger(gs, club, "Tesis bakım giderleri", -maint)
            if (club.id != gs.userClubId && club.finances.balance < 0) {
                club.finances.transferBudget = 0
                // borçlu AI kulübü en pahalı oyuncusunu listeye koyar
                club.playerIds.mapNotNull { gs.players[it] }.maxByOrNull { it.contract?.weeklyWage ?: 0 }?.transferListed = true
            }
        }
        // kullanıcı oyuncularının mutluluğu
        val user = gs.userClub
        val squad = user.playerIds.mapNotNull { gs.players[it] }.sortedByDescending { it.overall }
        val playedMatches = gs.fixtures.count { it.played && !it.isNational && (it.homeId == user.id || it.awayId == user.id) }
        squad.forEachIndexed { idx, p ->
            if (playedMatches >= 6 && idx < 14 && p.available && p.stats.apps < playedMatches * 0.35) {
                p.morale = (p.morale - 2).coerceAtLeast(5); p.weeksUnhappy++
                if (p.weeksUnhappy == 8) msg(gs, MsgType.INFO, "Mutsuz oyuncu: ${p.fullName}", "${p.fullName} yeterince süre alamadığı için mutsuz. Durum düzelmezse transfer talep edebilir.")
                if (p.weeksUnhappy == 14) { p.transferListed = true; msg(gs, MsgType.INFO, "Transfer talebi: ${p.fullName}", "${p.fullName} resmi olarak transfer talebinde bulundu ve listeye eklendi.") }
            } else if (p.weeksUnhappy > 0) p.weeksUnhappy--
            val c = p.contract
            if (c != null && c.expiresSeason <= gs.season && p.morale > 40) p.morale--
        }
    }

    private fun growthBase(age: Int): Double = when {
        age <= 18 -> 0.60; age <= 21 -> 0.50; age <= 23 -> 0.38; age <= 26 -> 0.18; age <= 29 -> 0.04
        age <= 31 -> -0.15; age <= 33 -> -0.32; else -> -0.5
    }

    private fun training(gs: GameState) {
        for (club in gs.clubs.values) {
            val isUser = club.id == gs.userClubId
            val plan = if (isUser) gs.training else defaultPlan
            val fac = club.facilities.trainingLevel
            val youthFac = club.facilities.youthLevel
            for (id in club.playerIds + club.youthIds) {
                val p = gs.players[id] ?: continue
                if (p.injuryDays > 0) continue
                val focus = plan.individual[id] ?: plan.focus
                if (isUser) {
                    if (focus == TrainingFocus.RECOVERY) p.fitness = min(100, p.fitness + 15)
                    else p.fitness = max(40, p.fitness - plan.intensity.fatigue)
                    if (rnd.nextDouble() < plan.intensity.injuryRisk * 7) injure(gs, p, minor = true)
                }
                if (focus == TrainingFocus.RECOVERY) continue
                val age = p.age(gs.season)
                val g = growthBase(age)
                val gap = if (g > 0) ((p.potential - p.overall) / 8.0).coerceIn(0.05, 1.6) else 1.0
                val facF = 0.6 + (if (p.isYouth) youthFac else fac) * 0.08
                val attrsIdx = focusAttrs(focus, p.position)
                repeat(2) {
                    val ai = attrsIdx[rnd.nextInt(attrsIdx.size)]
                    val delta = g * plan.intensity.gain * facF * (0.4 + rnd.nextDouble() * 1.2) * gap
                    if (rnd.nextDouble() < kotlin.math.abs(delta)) bump(p.attrs, ai, if (delta > 0) 1 else -1)
                }
            }
        }
    }

    private val defaultPlan = TrainingPlan()

    private fun focusAttrs(f: TrainingFocus, pos: Position): IntArray {
        val base = when (f) {
            TrainingFocus.BALANCED -> intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8)
            TrainingFocus.FITNESS -> intArrayOf(0, 5, 8)
            TrainingFocus.ATTACKING -> intArrayOf(1, 3, 7)
            TrainingFocus.DEFENDING -> intArrayOf(4, 5, 9)
            TrainingFocus.TECHNICAL -> intArrayOf(2, 3, 6)
            TrainingFocus.TACTICAL -> intArrayOf(6, 8, 7)
            TrainingFocus.SET_PIECES -> intArrayOf(2, 1)
            TrainingFocus.RECOVERY -> intArrayOf(5)
        }
        return if (pos == Position.GK) intArrayOf(10, 10, 10, 7, 5, 2) else base
    }

    private fun bump(a: Attributes, i: Int, d: Int) {
        fun c(v: Int) = (v + d).coerceIn(5, 99)
        when (i) {
            0 -> a.pace = c(a.pace); 1 -> a.shooting = c(a.shooting); 2 -> a.passing = c(a.passing)
            3 -> a.dribbling = c(a.dribbling); 4 -> a.defending = c(a.defending); 5 -> a.physical = c(a.physical)
            6 -> a.vision = c(a.vision); 7 -> a.composure = c(a.composure); 8 -> a.workRate = c(a.workRate)
            9 -> a.leadership = c(a.leadership); 10 -> a.goalkeeping = c(a.goalkeeping)
        }
    }

    /* ==================== GENÇLİK AKADEMİSİ ==================== */

    private fun youthIntake(gs: GameState) {
        for (club in gs.clubs.values) {
            val lvl = club.facilities.youthLevel
            val n = 2 + lvl / 3 + rnd.nextInt(0, 3)
            val coef = gs.countryCoefficients[club.country] ?: 20.0
            val created = mutableListOf<Player>()
            repeat(n) {
                val pos = Position.values()[rnd.nextInt(Position.values().size)]
                val ovr = (32 + lvl * 2 + club.reputation * 0.12 + rnd.nextInt(0, 10)).roundToInt().coerceAtMost(70)
                val nat = WorldGenerator.pickNationality(club.country, 6, rnd)
                val p = WorldGenerator.generatedPlayer(gs.newId(), nat, pos, ovr, rnd.nextInt(15, 17), gs.season, rnd)
                p.potential = (45 + lvl * 3.5 + coef * 0.12 + club.reputation * 0.12 + rnd.nextInt(-12, 22)).roundToInt().coerceIn(ovr + 5, 96)
                p.isYouth = true
                p.agentId = rnd.nextInt(1, 251)
                p.contract = Contract(club.id, 150L + rnd.nextInt(0, 300), gs.season + 3)
                gs.players[p.id] = p
                club.youthIds.add(p.id)
                created.add(p)
            }
            if (club.id == gs.userClubId) {
                val lines = created.sortedByDescending { it.potential }.joinToString("\n") { "• ${it.fullName} (${it.position.tr}, ${it.age(gs.season)} yaş) – Güç ${it.overall}, Potansiyel ~${roughPotential(gs, it)}" }
                msg(gs, MsgType.YOUTH, "Gençlik akademisi: yeni oyuncular", "Akademiye bu yıl ${created.size} yeni oyuncu katıldı:\n\n$lines\n\nGençlik Akademisi ekranından A takıma alabilirsiniz.")
            }
        }
    }

    /** Scout seviyesine göre potansiyel tahmini (aralık). */
    fun roughPotential(gs: GameState, p: Player): String {
        val lvl = gs.userClub.facilities.scoutingLevel
        if (p.contract?.clubId == gs.userClubId || lvl >= 9) return "${p.potential}"
        val err = (12 - lvl).coerceAtLeast(2)
        return "${(p.potential - err).coerceAtLeast(p.overall)}-${(p.potential + err).coerceAtMost(99)}"
    }

    fun promoteYouth(gs: GameState, playerId: Int): String {
        val club = gs.userClub
        val p = gs.players[playerId] ?: return "Oyuncu yok."
        if (playerId !in club.youthIds) return "Oyuncu akademide değil."
        club.youthIds.remove(playerId); club.playerIds.add(playerId)
        p.isYouth = false
        val league = gs.leagues.getValue(club.leagueId)
        p.contract = Contract(club.id, Economy.roundNice(Economy.expectedWage(p.overall, league.reputation) * 6 / 10), gs.season + 4)
        return "${p.fullName} A takıma yükseltildi ve profesyonel sözleşme imzaladı."
    }

    private fun contractWarnings(gs: GameState) {
        val exp = gs.userClub.playerIds.mapNotNull { gs.players[it] }.filter { (it.contract?.expiresSeason ?: 9999) <= gs.season && it.contract?.loanFromClubId == null }
        if (exp.isNotEmpty()) msg(gs, MsgType.BOARD, "Sözleşmesi biten oyuncular",
            "Şu oyuncuların sözleşmesi sezon sonunda bitiyor; yenilemezseniz bedelsiz ayrılacaklar:\n\n" + exp.joinToString("\n") { "• ${it.fullName} (${it.overall})" })
    }

    private fun boardCheck(gs: GameState) {
        val club = gs.userClub
        if (club.finances.balance < -5_000_000) {
            club.boardConfidence = (club.boardConfidence - 3).coerceAtLeast(0)
            msg(gs, MsgType.BOARD, "Yönetim: mali uyarı", "Kulüp kasası ${Economy.fmt(club.finances.balance)}. Maaş giderlerini düşürmeniz veya oyuncu satmanız gerekiyor.")
        }
        val obj = gs.objective ?: return
        val pos = leaguePosition(gs, club.id)
        if (gs.day > 120 && pos > obj.targetPosition + 4) {
            club.boardConfidence = (club.boardConfidence - 4).coerceAtLeast(0)
            msg(gs, MsgType.BOARD, "Yönetim: performans endişesi", "Takım şu an $pos. sırada. Hedefimiz: ${obj.description}. Güven: ${club.boardConfidence}/100")
        }
    }

    fun leaguePosition(gs: GameState, clubId: Int): Int {
        val club = gs.clubs[clubId] ?: return 0
        return Scheduler.sortedTable(gs, club.leagueId).indexOfFirst { it.clubId == clubId } + 1
    }

    fun sack(gs: GameState, reason: String) {
        gs.sacked = true
        msg(gs, MsgType.BOARD, "Görevinize son verildi", reason)
        gs.managerReputation = (gs.managerReputation - 8).coerceAtLeast(5)
        val rep = gs.managerReputation
        gs.jobOffers = gs.clubs.values.filter { it.id != gs.userClubId && it.reputation in (rep - 25)..(rep + 3) }
            .shuffled(rnd).take(6).map { it.id }.toMutableList()
        if (gs.jobOffers.isEmpty()) gs.jobOffers = gs.clubs.values.sortedBy { it.reputation }.take(6).map { it.id }.toMutableList()
    }

    /* ==================== ŞAMPİYONLAR ==================== */

    private fun onChampion(gs: GameState, comp: Competition, winner: Int) {
        val name = gs.clubName(winner)
        when (comp.type) {
            CompetitionType.DOMESTIC_CUP -> comp.country?.let { gs.lastCupWinners[it] = winner }
            else -> {}
        }
        val prize = when (comp.type) {
            CompetitionType.UCL -> 25_000_000L; CompetitionType.UEL -> 8_000_000L; CompetitionType.UECL -> 5_000_000L
            CompetitionType.DOMESTIC_CUP -> 2_000_000L; CompetitionType.SUPER_CUP -> 1_000_000L; else -> 0L
        }
        gs.clubs[winner]?.let { c ->
            Economy.addLedger(gs, c, "${comp.name} şampiyonluk ödülü", prize)
            c.reputation = (c.reputation + if (Scheduler.isUclType(comp.type)) 3 else 1).coerceAtMost(99)
        }
        if (winner == gs.userClubId) {
            gs.trophies.add("${comp.name} (${Scheduler.seasonLabel(gs.season)})")
            gs.managerReputation = (gs.managerReputation + if (Scheduler.isUclType(comp.type)) 10 else 4).coerceAtMost(99)
            gs.userClub.boardConfidence = (gs.userClub.boardConfidence + 15).coerceAtMost(100)
            gs.userClub.fanMood = (gs.userClub.fanMood + 15).coerceAtMost(100)
            msg(gs, MsgType.BOARD, "🏆 ŞAMPİYON! ${comp.name}", "Tebrikler! ${name} ${comp.name} kupasını kazandı! Ödül: ${Economy.fmt(prize)}")
        } else if (Scheduler.isUclType(comp.type) || comp.country == gs.userClub.country) {
            msg(gs, MsgType.MEDIA, "${comp.name} şampiyonu", "$name, ${comp.name} şampiyonu oldu.")
        }
    }

    /* ==================== MİLLİ TAKIMLAR ==================== */

    fun callUpNationalSquads(gs: GameState) {
        val byNat = gs.players.values.filter { it.contract != null && !it.isYouth && it.injuryDays < 10 }.groupBy { it.nationality }
        for (nt in gs.nationalTeams.values) {
            val pool = byNat[nt.code] ?: emptyList()
            val gks = pool.filter { it.position == Position.GK }.sortedByDescending { it.overall }.take(3)
            val outs = pool.filter { it.position != Position.GK }.sortedByDescending { it.overall * 0.9 + it.form * 0.1 }.take(23)
            nt.squad = (gks + outs).map { it.id }.toMutableList()
            if (nt.squad.size >= 16) nt.reputation = ((gks + outs).take(16).map { it.overall }.average()).roundToInt()
        }
        if (gs.userClubId >= 0) {
            val mine = gs.nationalTeams.values.flatMap { nt -> nt.squad.mapNotNull { gs.players[it] }.filter { it.contract?.clubId == gs.userClubId }.map { nt.name to it } }
            if (mine.isNotEmpty() && gs.day > 0) msg(gs, MsgType.INFO, "Milli takım davetleri", "Milli takımlara davet edilen oyuncularınız:\n\n" + mine.joinToString("\n") { "• ${it.second.fullName} – ${it.first}" })
        }
    }

    /** Dünya Kupası / Avrupa Şampiyonası simülasyonu (sezon sonu). */
    private fun internationalTournament(gs: GameState, name: String, teamCount: Int, europeOnly: Boolean) {
        callUpNationalSquads(gs)
        val europe = LeagueCatalog.countries.filter { it.uefa }.map { it.code }.toSet() + setOf("WAL", "BIH", "ALB", "GEO", "RUS")
        val teams = gs.nationalTeams.values.filter { !europeOnly || it.code in europe }.filter { it.code != "RUS" }
            .sortedByDescending { it.fifaPoints }.take(teamCount).shuffled(rnd)
        if (teams.size < 8) return
        val sb = StringBuilder("$name ${gs.season + 1}\n\n")
        val groups = teams.chunked(4)
        val firsts = mutableListOf<NationalTeam>(); val seconds = mutableListOf<NationalTeam>(); val thirds = mutableListOf<Pair<NationalTeam, Int>>()
        groups.forEachIndexed { gi, g ->
            val pts = HashMap<Int, Int>(); val gd = HashMap<Int, Int>()
            for (i in g.indices) for (j in i + 1 until g.size) {
                val (hg, ag) = quickNational(gs, g[i], g[j], false)
                pts[g[i].id] = (pts[g[i].id] ?: 0) + when { hg > ag -> 3; hg == ag -> 1; else -> 0 }
                pts[g[j].id] = (pts[g[j].id] ?: 0) + when { ag > hg -> 3; hg == ag -> 1; else -> 0 }
                gd[g[i].id] = (gd[g[i].id] ?: 0) + hg - ag; gd[g[j].id] = (gd[g[j].id] ?: 0) + ag - hg
            }
            val sorted = g.sortedWith(compareByDescending<NationalTeam> { pts[it.id] ?: 0 }.thenByDescending { gd[it.id] ?: 0 })
            sb.append("Grup ${'A' + gi}: ").append(sorted.joinToString(", ") { "${it.name} ${pts[it.id] ?: 0}p" }).append("\n")
            firsts.add(sorted[0]); seconds.add(sorted[1]); if (sorted.size > 2) thirds.add(sorted[2] to ((pts[sorted[2].id] ?: 0) * 100 + (gd[sorted[2].id] ?: 0)))
        }
        val qualified = (firsts + seconds).toMutableList()
        var target = 1; while (target < qualified.size) target *= 2
        qualified += thirds.sortedByDescending { it.second }.take(target - qualified.size).map { it.first }
        var round = qualified.shuffled(rnd)
        sb.append("\n")
        while (round.size > 1) {
            val label = when (round.size) { 2 -> "Final"; 4 -> "Yarı Final"; 8 -> "Çeyrek Final"; 16 -> "Son 16"; else -> "Son ${round.size}" }
            val next = mutableListOf<NationalTeam>()
            val lines = mutableListOf<String>()
            for (i in 0 until round.size / 2) {
                val a = round[2 * i]; val b = round[2 * i + 1]
                val (hg, ag, w) = quickNationalKo(gs, a, b)
                next.add(w); lines.add("${a.name} $hg-$ag ${b.name}" + if (hg == ag) " (pen. ${w.name})" else "")
            }
            if (round.size <= 8) sb.append("$label: ").append(lines.joinToString(" | ")).append("\n")
            round = next
        }
        val champ = round.first()
        sb.append("\n🏆 ŞAMPİYON: ${champ.name}")
        champ.fifaPoints += 50
        gs.history.add("$name ${gs.season + 1}: ${champ.name}")
        msg(gs, MsgType.MEDIA, "$name ${gs.season + 1} sona erdi", sb.toString())
    }

    private fun quickNational(gs: GameState, a: NationalTeam, b: NationalTeam, ko: Boolean): Pair<Int, Int> {
        val sim = MatchSim(gs, null, SideFactory.forNation(gs, a), SideFactory.forNation(gs, b), knockout = ko, neutral = true, detailed = false, rnd = rnd)
        sim.runToEnd()
        for (side in listOf(sim.home, sim.away)) for (id in side.played) gs.players[id]?.let { it.internationalCaps++; it.careerGoals += sim.goalsBy[id] ?: 0 }
        return sim.homeGoals to sim.awayGoals
    }

    private fun quickNationalKo(gs: GameState, a: NationalTeam, b: NationalTeam): Triple<Int, Int, NationalTeam> {
        val sim = MatchSim(gs, null, SideFactory.forNation(gs, a), SideFactory.forNation(gs, b), knockout = true, neutral = true, detailed = false, rnd = rnd)
        sim.runToEnd()
        val w = when {
            sim.homeGoals > sim.awayGoals -> a; sim.awayGoals > sim.homeGoals -> b
            else -> if ((sim.homePens ?: 0) > (sim.awayPens ?: 0)) a else b
        }
        return Triple(sim.homeGoals, sim.awayGoals, w)
    }

    /* ==================== SEZON SONU ==================== */

    fun seasonEnd(gs: GameState) {
        val user = gs.userClub
        val season = gs.season
        val summary = StringBuilder()
        // 1) lig sıralamaları
        val finals = HashMap<Int, List<Int>>()
        for (lg in gs.leagues.values) {
            val order = Scheduler.sortedTable(gs, lg.id).map { it.clubId }
            finals[lg.id] = order
            gs.lastLeagueFinal[lg.id] = order.toMutableList()
            if (lg.tier == 1 && order.isNotEmpty()) {
                gs.lastLeagueChampions[lg.country] = order.first()
                gs.clubs[order.first()]?.let { Economy.addLedger(gs, it, "${lg.name} şampiyonluk ödülü", Economy.seasonTv(lg) / 5) }
            }
        }
        val userLeague = gs.leagues.getValue(user.leagueId)
        val userPos = finals[userLeague.id]?.indexOf(user.id)?.plus(1) ?: 0
        if (userPos == 1) {
            gs.trophies.add("${userLeague.name} (${Scheduler.seasonLabel(season)})")
            gs.managerReputation = (gs.managerReputation + 8).coerceAtMost(99)
        }
        // 2) hedef değerlendirmesi
        val obj = gs.objective
        var note = ""
        if (obj != null) {
            val ok = userPos in 1..obj.targetPosition
            obj.achieved = ok
            val diff = obj.targetPosition - userPos
            user.boardConfidence = (user.boardConfidence + if (ok) 15 + diff * 2 else diff * 5).coerceIn(0, 100)
            gs.managerReputation = (gs.managerReputation + if (ok) 3 else -3).coerceIn(1, 99)
            note = if (ok) "Hedef başarıldı ✅" else "Hedef tutturulamadı ❌"
            summary.append("Lig sıralamanız: $userPos. (${userLeague.name})\nHedef: ${obj.description} → $note\nYönetim güveni: ${user.boardConfidence}/100\n\n")
        }
        gs.seasonRecords.add(SeasonRecord(season, user.name, userLeague.name, userPos, note))
        // 3) küme düşme / yükselme
        val moves = mutableListOf<String>()
        for (cd in LeagueCatalog.countries) {
            val leagues = gs.leagues.values.filter { it.country == cd.code }.sortedBy { it.tier }
            for (i in 0 until leagues.size - 1) {
                val upper = leagues[i]; val lower = leagues[i + 1]
                val n = min(upper.relegations, lower.promotions)
                if (n <= 0) continue
                val upOrder = finals[upper.id] ?: continue
                val lowOrder = finals[lower.id] ?: continue
                val down = upOrder.takeLast(n)
                val up = if (lower.playoffPromotion && n >= 1 && lowOrder.size >= n + 3) {
                    val direct = lowOrder.take(n - 1)
                    val po = lowOrder.subList(n - 1, n + 3)
                    direct + po[rnd.nextInt(po.size)]
                } else lowOrder.take(n)
                for (id in down) { upper.clubIds.remove(id); lower.clubIds.add(id); gs.clubs[id]?.let { it.leagueId = lower.id; it.reputation = (it.reputation - 3).coerceAtLeast(5) } }
                for (id in up) { lower.clubIds.remove(id); upper.clubIds.add(id); gs.clubs[id]?.let { it.leagueId = upper.id; it.reputation = (it.reputation + 3).coerceAtMost(95) } }
                if (user.id in down) moves.add("${user.name} ${lower.name}'e düştü.")
                if (user.id in up) moves.add("${user.name} ${upper.name}'e yükseldi! 🎉")
            }
        }
        if (moves.isNotEmpty()) summary.append(moves.joinToString("\n")).append("\n\n")
        if (moves.any { it.contains("düştü") }) user.boardConfidence = (user.boardConfidence - 20).coerceAtLeast(0)
        // 4) uluslararası turnuva
        val nextYear = season + 1
        if (nextYear % 4 == 2) internationalTournament(gs, "FIFA Dünya Kupası", 48, false)
        if (nextYear % 4 == 0) internationalTournament(gs, "UEFA Avrupa Şampiyonası", 24, true)
        // 5) oyuncular: kiralıklar, sözleşmeler, emeklilik
        val retired = mutableListOf<String>()
        val released = mutableListOf<String>()
        for (p in gs.players.values.toList()) {
            val c = p.contract
            // kiralık dönüşü
            if (c?.loanFromClubId != null && (c.loanUntilSeason ?: season) <= season) {
                val parent = gs.clubs[c.loanFromClubId!!]
                gs.clubs[c.clubId]?.let { it.playerIds.remove(p.id); it.tactic.lineup.remove(p.id); it.tactic.bench.remove(p.id) }
                if (parent != null) { parent.playerIds.add(p.id); p.contract = c.copy(clubId = parent.id, loanFromClubId = null, loanUntilSeason = null, loanWageShare = 100) }
                else p.contract = c.copy(loanFromClubId = null)
            }
            val age = p.age(season + 1)
            val retireP = when { age >= 39 -> 1.0; age >= 37 -> 0.6; age >= 35 -> 0.3 + (if (p.overall < 70) 0.2 else 0.0); age >= 33 && p.overall < 55 -> 0.3; else -> 0.0 }
            if (retireP > 0 && rnd.nextDouble() < retireP) {
                val cl = p.contract?.clubId?.let { gs.clubs[it] }
                if (cl?.id == user.id) retired.add(p.fullName)
                cl?.let { it.playerIds.remove(p.id); it.youthIds.remove(p.id); it.tactic.lineup.remove(p.id); it.tactic.bench.remove(p.id) }
                gs.freeAgents.remove(p.id)
                gs.nationalTeams.values.forEach { it.squad.remove(p.id) }
                gs.players.remove(p.id)
                continue
            }
            val cc = p.contract
            if (cc != null && cc.expiresSeason <= season && cc.loanFromClubId == null) {
                val club = gs.clubs[cc.clubId]
                if (club != null) {
                    val isUser = club.id == user.id
                    val renew = !isUser && (p.isYouth || (rnd.nextDouble() < 0.7 && age <= 32))
                    if (renew) p.contract = cc.copy(expiresSeason = season + TransferMarket.preferredYears(age))
                    else {
                        if (isUser) released.add(p.fullName)
                        club.playerIds.remove(p.id); club.youthIds.remove(p.id); club.tactic.lineup.remove(p.id); club.tactic.bench.remove(p.id)
                        p.contract = null; p.isYouth = false
                        gs.freeAgents.add(p.id)
                    }
                }
            }
            // istatistik sıfırla
            p.stats = SeasonStats(); p.seasonYellows = 0; p.suspendedMatches = 0
            p.fitness = 100; p.morale = ((p.morale + 65) / 2); p.weeksUnhappy = 0
            if (p.isYouth && age >= 19) {
                // akademide yaşı geçen oyuncu: AI terfi eder, kullanıcıya bildirim
                val cl = p.contract?.clubId?.let { gs.clubs[it] }
                if (cl != null && cl.id != user.id) { cl.youthIds.remove(p.id); cl.playerIds.add(p.id); p.isYouth = false }
            }
        }
        if (retired.isNotEmpty()) summary.append("Emekli olan oyuncularınız: ${retired.joinToString()}\n")
        if (released.isNotEmpty()) summary.append("Sözleşmesi biten oyuncularınız ayrıldı: ${released.joinToString()}\n")
        // 6) AI kadrolarını tamamla
        for (club in gs.clubs.values) {
            if (club.id == user.id) continue
            val squad = club.playerIds.mapNotNull { gs.players[it] }
            // en iyi gençleri yükselt
            if (squad.size < 23) club.youthIds.mapNotNull { gs.players[it] }.sortedByDescending { it.overall }.take(23 - squad.size).forEach {
                club.youthIds.remove(it.id); club.playerIds.add(it.id); it.isYouth = false
            }
            val lg = gs.leagues.getValue(club.leagueId)
            var size = club.playerIds.size
            while (size < 20) {
                val pos = listOf(Position.GK, Position.CB, Position.CM, Position.ST, Position.LB, Position.RB, Position.LW, Position.RW, Position.DM, Position.AM)[size % 10]
                val p = WorldGenerator.generatedPlayer(gs.newId(), WorldGenerator.pickNationality(club.country, lg.tier, rnd), pos,
                    (30 + club.reputation * 0.55 + rnd.nextInt(-6, 4)).roundToInt(), rnd.nextInt(18, 24), season + 1, rnd)
                p.agentId = rnd.nextInt(1, 251)
                WorldGenerator.attachContract(gs, p, club, lg, season + 1, rnd)
                gs.players[p.id] = p; club.playerIds.add(p.id); size++
            }
            // bütçe
            club.finances.transferBudget = max(Economy.roundNice((300.0 * Math.exp(0.14 * club.reputation)).toLong()), club.finances.balance * 3 / 10)
            club.finances.weeklyWageBudget = Economy.roundNice((Economy.weeklyWageBill(gs, club, false) * 1.12).toLong())
            club.finances.seasonIncome = 0; club.finances.seasonExpense = 0
            club.tactic.lineup.clear(); club.tactic.bench.clear()
        }
        // 7) serbest oyuncu havuzunu temizle
        val toRemove = gs.freeAgents.mapNotNull { gs.players[it] }.filter { it.age(season + 1) >= 33 || it.overall < 45 }.map { it.id }.toMutableList()
        val keep = gs.freeAgents.filter { it !in toRemove }
        if (keep.size > 600) toRemove += keep.mapNotNull { gs.players[it] }.sortedBy { it.overall }.take(keep.size - 600).map { it.id }
        val rm = toRemove.toHashSet()
        gs.freeAgents.removeAll { it in rm }
        rm.forEach { gs.players.remove(it) }
        // 8) kullanıcı mali durumu
        val bonus = if (obj?.achieved == true) 0.45 else 0.30
        user.finances.transferBudget = max(0L, Economy.roundNice((user.finances.balance * bonus).toLong()))
        user.finances.weeklyWageBudget = max(user.finances.weeklyWageBudget, Economy.roundNice((Economy.weeklyWageBill(gs, user) * 1.08).toLong()))
        summary.append("\nYeni transfer bütçesi: ${Economy.fmt(user.finances.transferBudget)}\nMaaş bütçesi: ${Economy.fmt(user.finances.weeklyWageBudget)}/hafta\n")
        user.finances.seasonIncome = 0; user.finances.seasonExpense = 0
        // 9) katsayılar
        for (cd in LeagueCatalog.countries) gs.countryCoefficients[cd.code] = (gs.countryCoefficients[cd.code] ?: cd.coefficient) * 0.85 + cd.coefficient * 0.15
        // 10) kovulma kontrolü
        if (user.boardConfidence < 20) {
            msg(gs, MsgType.BOARD, "${Scheduler.seasonLabel(season)} sezon değerlendirmesi", summary.toString())
            sack(gs, "Sezon hedeflerinin gerisinde kalındığı için yönetim kurulu yollarınızı ayırma kararı aldı.")
        }
        // 11) yeni sezon
        gs.season++
        gs.offers.removeAll { it.status != OfferStatus.COMPLETED }
        Scheduler.setupSeason(gs)
        callUpNationalSquads(gs)
        if (!gs.sacked) {
            setObjective(gs)
            Lineups.ensureUserLineup(gs, gs.userClub)
            msg(gs, MsgType.BOARD, "${Scheduler.seasonLabel(season)} sezon değerlendirmesi", summary.toString() + "\nYeni sezon hedefi: ${gs.objective?.description}")
        }
        gs.history.add("${Scheduler.seasonLabel(season)}: " + gs.leagues.values.filter { it.tier == 1 }.sortedByDescending { it.reputation }.take(6)
            .joinToString(", ") { "${it.name}: ${gs.clubName(gs.lastLeagueChampions[it.country] ?: -1)}" })
    }

    /** Kovulduktan sonra yeni kulüp kabul. */
    fun acceptJob(gs: GameState, clubId: Int) {
        startCareer(gs, clubId, gs.managerName)
    }

    /* ==================== BÜTÇE ==================== */

    /** Transfer bütçesinden maaş bütçesine aktarım (1 € haftalık = 52 € transfer). */
    fun shiftBudget(gs: GameState, transferAmount: Long): String {
        val f = gs.userClub.finances
        if (transferAmount > 0) {
            if (transferAmount > f.transferBudget) return "Yetersiz transfer bütçesi."
            f.transferBudget -= transferAmount; f.weeklyWageBudget += transferAmount / 52
            return "${Economy.fmt(transferAmount)} transfer bütçesi → +${Economy.fmt(transferAmount / 52)}/hafta maaş bütçesi."
        } else {
            val weekly = -transferAmount / 52
            if (weekly > f.weeklyWageBudget - Economy.weeklyWageBill(gs, gs.userClub)) return "Maaş bütçesinde boşluk yok."
            f.weeklyWageBudget -= weekly; f.transferBudget += -transferAmount
            return "Maaş bütçesinden ${Economy.fmt(weekly)}/hafta → ${Economy.fmt(-transferAmount)} transfer bütçesi."
        }
    }

    fun nationName(code: String) = NameBank.nationByCode[code]?.nameTr ?: code
}
