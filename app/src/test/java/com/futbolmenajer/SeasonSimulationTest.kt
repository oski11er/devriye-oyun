package com.futbolmenajer

import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.*
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/** Motoru uçtan uca test eder: dünya → 2 sezon → kaydet/yükle → transfer akışı. */
class SeasonSimulationTest {

    @Test
    fun fullSeasonRuns() {
        val t0 = System.currentTimeMillis()
        val gs = GameEngine.newWorld(42L) { _, _ -> }
        println("Dünya: ${gs.clubs.size} kulüp, ${gs.players.size} oyuncu, ${gs.leagues.size} lig, ${gs.fixtures.size} maç, ${System.currentTimeMillis() - t0} ms")
        val salah = gs.players.values.first { it.lastName == "Salah" && it.isReal }
        println("Salah kulübü: " + gs.clubName(salah.contract!!.clubId) + " | Wonderkid sayısı: " + gs.players.values.count { it.age(gs.season) <= 21 && it.potential >= 82 } +
            " | Veteran: " + gs.players.values.count { it.age(gs.season) >= 33 && it.overall >= 72 } + " | Teknik ekip havuzu: " + gs.staffPool.size)
        println("Premier League: " + gs.leagues.values.first { it.name == "Premier League" }.clubIds.joinToString { gs.clubName(it) })
        println("Real Madrid hocası: " + gs.clubs.values.first { it.name == "Real Madrid" }.headCoach)
        val gsId = gs.clubs.values.first { it.name == "Galatasaray" }.id
        GameEngine.startCareer(gs, gsId, "Test")

        // transfer akışı: serbest oyuncu
        val fa = gs.freeAgents.mapNotNull { gs.players[it] }.maxByOrNull { it.overall }!!
        val o = TransferMarket.submitOffer(gs, fa.id, OfferType.FREE, 0).getOrThrow()
        val d = TransferMarket.demandTerms(gs, fa, gs.userClub, SquadRole.ROTATION)
        println("Serbest: " + TransferMarket.negotiateTerms(gs, o.id, d.copy(weeklyWage = d.weeklyWage * 2, agentFee = d.agentFee * 2)).message)
        // bonservis teklifi
        val target = gs.clubs.values.first { it.name == "Trabzonspor" }.playerIds.mapNotNull { gs.players[it] }.maxByOrNull { it.overall }!!
        println("Bonservis: " + TransferMarket.submitOffer(gs, target.id, OfferType.TRANSFER, minOf(gs.userClub.finances.transferBudget, TransferMarket.askingPrice(gs, target))).fold({ "gönderildi" }, { it.message }))
        // takas teklifi
        val swapTarget = gs.clubs.values.first { it.name == "Beşiktaş" }.playerIds.mapNotNull { gs.players[it] }.first()
        TransferMarket.submitOffer(gs, swapTarget.id, OfferType.SWAP, 1_000_000, swapIds = listOf(gs.userClub.playerIds.last()))

        val startSeason = gs.season
        var matches = 0
        var guard = 0
        val t1 = System.currentTimeMillis()
        var printedGoals = false
        while (gs.season < startSeason + 2 && guard++ < 5000) {
            if (!printedGoals && gs.day > 300) {
                printedGoals = true
                val lf = gs.fixtures.filter { it.played && gs.competitions[it.competitionId]?.type == CompetitionType.LEAGUE }
                println("Lig maçı başı gol: " + lf.map { it.homeGoals + it.awayGoals }.average() + " | ev galibiyeti %" + (lf.count { it.homeGoals > it.awayGoals } * 100 / lf.size) + " beraberlik %" + (lf.count { it.homeGoals == it.awayGoals } * 100 / lf.size))
                val top = gs.players.values.filter { it.stats.goals > 0 }.sortedByDescending { it.stats.goals }.take(5)
                println("Gol krallığı: " + top.joinToString { "${it.fullName} ${it.stats.goals}" })
                val gt = Scheduler.sortedTable(gs, gs.userClub.leagueId).take(5)
                println("Süper Lig ilk 5: " + gt.joinToString { "${gs.clubName(it.clubId)} ${it.pts}" })
                val pl = gs.leagues.values.first { it.name == "Premier League" }
                println("PL ilk 5: " + Scheduler.sortedTable(gs, pl.id).take(5).joinToString { "${gs.clubName(it.clubId)} ${it.pts}" })
                println("Kullanıcı kasası: " + Economy.fmt(gs.userClub.finances.balance))
            }
            when (val s = GameEngine.advance(gs)) {
                is Stop.MatchDay -> {
                    val fx = gs.fixtures.first { it.id == s.fixtureId }
                    val sim = GameEngine.buildSim(gs, fx, detailed = true, userLive = true)
                    // canlı maç gibi: adım adım + değişiklik
                    while (!sim.finished) {
                        sim.step()
                        val side = if (sim.home.isUser) sim.home else sim.away
                        if (sim.minute == 65 && side.bench.isNotEmpty() && side.onPitch().size > 1) sim.makeSub(side, side.onPitch().last(), side.bench.first())
                    }
                    GameEngine.applyResult(gs, fx, sim)
                    GameEngine.finishDay(gs)
                    matches++
                    // kabul edilen teklif varsa kişisel şartlar
                    gs.offers.filter { it.fromClubId == gs.userClubId && it.status == OfferStatus.ACCEPTED_CLUB }.forEach { of ->
                        val p = gs.players[of.playerId]!!
                        val dd = TransferMarket.demandTerms(gs, p, gs.userClub, SquadRole.FIRST_TEAM, of.fee)
                        gs.userClub.finances.weeklyWageBudget += dd.weeklyWage * 2
                        println("Şartlar: " + TransferMarket.negotiateTerms(gs, of.id, dd.copy(weeklyWage = dd.weeklyWage * 2, agentFee = dd.agentFee * 2)).message)
                    }
                    gs.offers.filter { it.status == OfferStatus.COUNTERED && it.fromClubId == gs.userClubId }.forEach { TransferMarket.acceptCounter(gs, it.id) }
                }
                is Stop.Press -> Press.answer(gs, gs.pendingPress!!.options.first())
                is Stop.Sacked -> GameEngine.acceptJob(gs, gs.jobOffers.first())
                else -> {}
            }
        }
        println("2 sezon: $matches kullanıcı maçı, ${System.currentTimeMillis() - t1} ms, sezon=${gs.season}, gün=${gs.day}")
        assertTrue(gs.season == startSeason + 2)
        val played = gs.fixtures.filter { it.played && !it.isNational }
        if (played.isNotEmpty()) println("Maç başı gol (yeni sezon): " + played.map { it.homeGoals + it.awayGoals }.average())
        gs.seasonRecords.forEach { println("Kayıt: $it") }
        gs.history.forEach { println("Tarih: $it") }
        gs.trophies.forEach { println("Kupa: $it") }
        println("Transferler (ilk 10): " + gs.transferHistory.take(10))

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = false; allowStructuredMapKeys = true }
        val t2 = System.currentTimeMillis()
        val str = json.encodeToString(GameState.serializer(), gs)
        val back = json.decodeFromString(GameState.serializer(), str)
        println("Kayıt boyutu: ${str.length / 1024} KB, kaydet+yükle ${System.currentTimeMillis() - t2} ms")
        assertTrue(back.players.size == gs.players.size)
        // yüklenen oyun devam edebilmeli
        repeat(10) { GameEngine.advance(back).let { s -> if (s is Stop.MatchDay) { back.fixtures.first { it.id == s.fixtureId }.let { fx -> val sm = GameEngine.buildSim(back, fx, true, false); sm.runToEnd(); GameEngine.applyResult(back, fx, sm); GameEngine.finishDay(back) } } else if (s is Stop.Press) Press.answer(back, back.pendingPress!!.options[1]) } }
    }
}
