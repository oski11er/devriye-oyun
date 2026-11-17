package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * TRANSFER SİSTEMİ
 * ---------------------------------------------------------------
 * Akış (kullanıcının oyuncu alması):
 *   1) submitOffer()  → Teklif (Bonservis / Takas / Kiralık / Serbest) PENDING olarak kaydedilir,
 *                       kulüp 1-2 gün içinde yanıt verir.
 *   2) processDaily() → Satıcı kulüp değerlendirir:
 *        - Teklif ≥ istenen bedel           → ACCEPTED_CLUB (oyuncuyla görüşme açılır)
 *        - Teklif ≥ istenen bedelin %70'i  → COUNTERED (karşı teklif; en fazla 3 tur, sabrı azalır)
 *        - Aksi halde                       → REJECTED
 *      Takas: takas oyuncularının değeri, satıcının o pozisyondaki ihtiyacına ve kalitesine göre
 *      %30-100 arası sayılır; nakit fark eklenir. Takas oyuncuları da transferi kabul etmelidir.
 *      Kiralık: oyuncu satıcının ilk 13'ündeyse reddedilir; maaş katkısı ≥ %50, kiralama bedeli, satın alma opsiyonu.
 *   3) negotiateTerms() → Oyuncunun ajanıyla maaş / süre / imza parası / ajan ücreti / rol pazarlığı (3 tur).
 *   4) complete() → Bedel (taksitli olabilir), sonradan satıştan pay, takas oyuncuları ve sözleşmeler işlenir.
 *
 * AI kulüpleri transfer dönemlerinde kendi aralarında ve kullanıcıya teklif yaparak alım-satım yapar.
 */
object TransferMarket {

    private val rnd = Random(System.nanoTime())

    data class TermsResult(val accepted: Boolean, val finalRejection: Boolean, val message: String, val demand: PersonalTerms?)

    fun clubOf(gs: GameState, p: Player): Club? = p.contract?.clubId?.let { gs.clubs[it] }

    /** Kulübün oyuncuya biçtiği önem (1 = kilit oyuncu). */
    fun squadRank(gs: GameState, club: Club, p: Player): Int =
        club.playerIds.mapNotNull { gs.players[it] }.sortedByDescending { it.overall }.indexOfFirst { it.id == p.id } + 1

    /** Satıcı kulübün istediği bedel. */
    fun askingPrice(gs: GameState, p: Player): Long {
        val club = clubOf(gs, p) ?: return 0
        var v = Economy.value(p, gs.season).toDouble()
        val rank = squadRank(gs, club, p)
        v *= when {
            p.transferListed -> 0.85
            rank <= 3 -> 1.6
            rank <= 8 -> 1.35
            rank <= 14 -> 1.15
            else -> 1.0
        }
        if (club.id == gs.userClubId) v = Economy.value(p, gs.season).toDouble()
        val c = p.contract
        if (c != null && c.expiresSeason <= gs.season) v *= 0.6
        c?.releaseClause?.let { v = min(v, it.toDouble()) }
        return Economy.roundNice(v.roundToLong())
    }

    /** Oyuncunun alıcı kulübe gelmeye ilgisi (0-100) ve açıklaması. */
    fun interest(gs: GameState, p: Player, buyer: Club): Pair<Int, String> {
        val current = clubOf(gs, p)
        val curRep = current?.reputation ?: 30
        var score = 60 + (buyer.reputation - curRep) * 2
        val buyerLeague = gs.leagues[buyer.leagueId]
        val curLeague = current?.let { gs.leagues[it.leagueId] }
        if (buyerLeague != null && curLeague != null) score += (buyerLeague.reputation - curLeague.reputation)
        if (p.personality == Personality.AMBITIOUS) score += (buyer.reputation - curRep)
        if (p.personality == Personality.LOYAL && current != null) score -= 15
        if (p.personality == Personality.MERCENARY) score += 10
        if (p.morale < 45) score += 15
        if (p.transferListed) score += 10
        if (gs.competitions[Scheduler.UCL]?.participants?.contains(buyer.id) == true) score += 8
        if (current == null) score += 20
        if (p.overall >= 80 && buyer.reputation < 55) score -= 25
        score = score.coerceIn(0, 100)
        val txt = when {
            score >= 75 -> "Kulübünüze transfer olmaya çok sıcak bakıyor."
            score >= 50 -> "Doğru teklifle ikna edilebilir."
            score >= 30 -> "Kulübünüz konusunda şüpheli; yüksek maaş isteyecektir."
            else -> "Kulübünüze gelmek istemiyor."
        }
        return score to txt
    }

    /** Ajanla görüşme metni. */
    fun agentTalk(gs: GameState, p: Player): String {
        val agent = gs.agents[p.agentId]
        val user = gs.userClub
        val (i, txt) = interest(gs, p, user)
        val demand = demandTerms(gs, p, user, SquadRole.FIRST_TEAM)
        val sb = StringBuilder()
        sb.append("Menajer: ${agent?.name ?: "Bilinmiyor"}  (Açgözlülük ${agent?.greed ?: 50}/100, Güvenilirlik ${agent?.reliability ?: 50}/100)\n\n")
        sb.append("\"Müvekkilim ${p.fullName} hakkında konuşalım. $txt\"\n\n")
        if (i >= 30) {
            sb.append("Beklenen haftalık maaş: ${Economy.fmt(demand.weeklyWage)}\n")
            sb.append("Sözleşme süresi: ${demand.years} yıl\n")
            sb.append("Ajan ücreti beklentisi: ${Economy.fmt(demand.agentFee)}\n")
            val c = p.contract
            if (c != null) {
                sb.append("Mevcut sözleşme: ${c.expiresSeason} sonuna kadar, ${Economy.fmt(c.weeklyWage)}/hafta\n")
                c.releaseClause?.let { sb.append("Serbest kalma bedeli: ${Economy.fmt(it)}\n") }
                if (c.expiresSeason <= gs.season) sb.append("Sözleşmesi bu sezon bitiyor — sezon sonunda bedelsiz alınabilir.\n")
            }
        }
        sb.append("\nİlgi seviyesi: $i/100")
        return sb.toString()
    }

    fun preferredYears(age: Int) = when { age <= 24 -> 5; age <= 28 -> 4; age <= 31 -> 3; age <= 33 -> 2; else -> 1 }

    fun demandTerms(gs: GameState, p: Player, buyer: Club, role: SquadRole, fee: Long = 0): PersonalTerms {
        val league = gs.leagues[buyer.leagueId]
        var w = Economy.expectedWage(p.overall, league?.reputation ?: 50).toDouble() * role.wageMult
        w *= when (p.personality) { Personality.MERCENARY -> 1.25; Personality.LOYAL -> 0.95; Personality.AMBITIOUS -> 1.05; else -> 1.0 }
        val cur = clubOf(gs, p)
        if (cur != null && cur.reputation > buyer.reputation) w *= 1 + (cur.reputation - buyer.reputation) / 40.0
        val curWage = p.contract?.weeklyWage ?: 0
        w = max(w, curWage * 1.1)
        val agent = gs.agents[p.agentId]
        val greed = agent?.greed ?: 50
        val agentFee = if (fee > 0) (fee * greed / 1000.0) else (w * 8 * greed / 50.0)
        return PersonalTerms(
            weeklyWage = Economy.roundNice(w.roundToLong()), years = preferredYears(p.age(gs.season)),
            signingBonus = Economy.roundNice((w * 4).roundToLong()), agentFee = Economy.roundNice(agentFee.roundToLong()),
            squadRole = role
        )
    }

    fun windowCheck(gs: GameState, type: OfferType): String? =
        if (type != OfferType.FREE && !Scheduler.transferWindowOpen(gs.day)) "Transfer dönemi kapalı. (Yaz: 1 Tem–1 Eyl, Kış: 1 Oca–1 Şub) Yalnızca serbest oyuncu alınabilir." else null

    /* ---------------- KULLANICI TEKLİFİ ---------------- */

    fun submitOffer(gs: GameState, playerId: Int, type: OfferType, fee: Long, swapIds: List<Int> = emptyList(),
                    loanWageShare: Int = 100, loanFee: Long = 0, buyOption: Long? = null, sellOn: Int = 0, installments: Int = 1): Result<TransferOffer> {
        val p = gs.players[playerId] ?: return Result.failure(Exception("Oyuncu bulunamadı."))
        val user = gs.userClub
        windowCheck(gs, type)?.let { return Result.failure(Exception(it)) }
        val seller = clubOf(gs, p)
        if (seller?.id == user.id) return Result.failure(Exception("Bu oyuncu zaten sizin."))
        if (type == OfferType.FREE && p.contract != null) return Result.failure(Exception("Oyuncu serbest değil."))
        if (type != OfferType.FREE && p.contract == null) return Result.failure(Exception("Oyuncu serbest; doğrudan sözleşme önerin."))
        if (p.contract?.loanFromClubId != null) return Result.failure(Exception("Oyuncu şu an kiralık; transfer edilemez."))
        if (gs.offers.any { it.playerId == playerId && it.fromClubId == user.id && it.status in activeStatuses }) return Result.failure(Exception("Bu oyuncu için zaten aktif bir teklifiniz var."))
        val cash = if (type == OfferType.LOAN) loanFee else fee
        if (cash > user.finances.transferBudget) return Result.failure(Exception("Transfer bütçeniz yetersiz (${Economy.fmt(user.finances.transferBudget)})."))
        if (type == OfferType.SWAP) {
            if (swapIds.isEmpty()) return Result.failure(Exception("Takas için en az bir oyuncu seçin."))
            if (swapIds.any { it !in user.playerIds }) return Result.failure(Exception("Takas oyuncuları kadronuzda olmalı."))
        }
        if (type == OfferType.LOAN && loanWageShare < 0) return Result.failure(Exception("Geçersiz maaş katkısı."))
        val o = TransferOffer(
            id = gs.newId(), playerId = playerId, fromClubId = user.id, toClubId = seller?.id, type = type, fee = fee,
            swapPlayerIds = swapIds, loanWageShare = loanWageShare, loanFeePerSeason = loanFee, loanBuyOption = buyOption,
            sellOnPercent = sellOn, installments = installments.coerceIn(1, 4), createdDay = gs.day,
            responseDay = gs.day + if (type == OfferType.FREE) 0 else 1 + rnd.nextInt(2)
        )
        if (type == OfferType.FREE) {
            o.status = OfferStatus.ACCEPTED_CLUB
            o.message = "Serbest oyuncu – doğrudan sözleşme görüşmesi yapabilirsiniz."
        }
        gs.offers.add(o)
        return Result.success(o)
    }

    val activeStatuses = setOf(OfferStatus.PENDING, OfferStatus.COUNTERED, OfferStatus.ACCEPTED_CLUB)

    /** Kulübün takas oyuncularına biçtiği değer. */
    fun swapValue(gs: GameState, seller: Club, ids: List<Int>): Long {
        val squad = seller.playerIds.mapNotNull { gs.players[it] }
        val avg = if (squad.isEmpty()) 50.0 else squad.sortedByDescending { it.overall }.take(16).map { it.overall }.average()
        var total = 0.0
        for (id in ids) {
            val sp = gs.players[id] ?: continue
            val v = Economy.value(sp, gs.season).toDouble()
            val samePos = squad.count { it.position == sp.position }
            var f = if (samePos >= 3) 0.7 else 1.0
            if (sp.overall < avg - 8) f *= 0.3 else if (sp.overall < avg - 3) f *= 0.7
            if (sp.age(gs.season) >= 31) f *= 0.7
            total += v * f * 0.85
        }
        return total.roundToLong()
    }

    private fun evaluateClub(gs: GameState, o: TransferOffer) {
        val p = gs.players[o.playerId] ?: run { o.status = OfferStatus.WITHDRAWN; return }
        val seller = clubOf(gs, p) ?: run { o.status = OfferStatus.ACCEPTED_CLUB; return }
        if (seller.id != o.toClubId) { o.status = OfferStatus.WITHDRAWN; o.message = "Oyuncu başka bir kulübe gitti."; return }
        val ask = askingPrice(gs, p)
        when (o.type) {
            OfferType.LOAN -> {
                val rank = squadRank(gs, seller, p)
                when {
                    rank <= 13 && !p.loanListed -> { o.status = OfferStatus.REJECTED; o.message = "${seller.name}: \"${p.shortName} planlarımızda önemli, kiralık vermeyiz.\"" }
                    o.loanWageShare < 50 -> { o.status = OfferStatus.COUNTERED; o.loanWageShare = 75; o.counterFee = o.loanFeePerSeason; o.message = "${seller.name} maaşın en az %75'ini karşılamanızı istiyor." }
                    o.loanBuyOption != null && o.loanBuyOption!! < ask * 0.9 -> { o.status = OfferStatus.COUNTERED; o.loanBuyOption = Economy.roundNice((ask * 1.05).toLong()); o.counterFee = o.loanFeePerSeason; o.message = "${seller.name} satın alma opsiyonunun ${Economy.fmt(o.loanBuyOption!!)} olmasını istiyor." }
                    else -> { o.status = OfferStatus.ACCEPTED_CLUB; o.message = "${seller.name} kiralık teklifinizi kabul etti. Şimdi oyuncuyla görüşün." }
                }
            }
            OfferType.TRANSFER, OfferType.SWAP -> {
                val sv = if (o.type == OfferType.SWAP) swapValue(gs, seller, o.swapPlayerIds) else 0
                // takas oyuncularının onayı
                if (o.type == OfferType.SWAP) {
                    for (id in o.swapPlayerIds) {
                        val sp = gs.players[id] ?: continue
                        val (i, _) = interest(gs, sp, seller)
                        if (i < 30) { o.status = OfferStatus.REJECTED; o.message = "${sp.fullName}, ${seller.name}'e gitmeyi reddetti. Takas gerçekleşemez."; return }
                    }
                }
                val sellOnBonus = o.sellOnPercent * ask / 200
                val installmentPenalty = if (o.installments > 1) (o.installments - 1) * 0.04 else 0.0
                val total = ((o.fee + sv + sellOnBonus) * (1 - installmentPenalty)).toLong()
                val patience = 3 - o.negotiationRound
                when {
                    p.contract?.releaseClause != null && o.fee >= p.contract!!.releaseClause!! -> {
                        o.status = OfferStatus.ACCEPTED_CLUB; o.message = "Serbest kalma bedeli ödendi! ${seller.name} transferi engelleyemez."
                    }
                    total >= ask -> { o.status = OfferStatus.ACCEPTED_CLUB; o.message = "${seller.name} teklifinizi kabul etti. Oyuncuyla kişisel şartları görüşün." }
                    total >= ask * 0.70 && patience > 0 -> {
                        o.status = OfferStatus.COUNTERED
                        val needCash = Economy.roundNice(max(0L, ask - sv - sellOnBonus) + if (o.negotiationRound > 0) ask / 20 else 0L)
                        o.counterFee = needCash
                        o.message = if (o.type == OfferType.SWAP) "${seller.name} takasa ek olarak ${Economy.fmt(needCash)} nakit istiyor." else "${seller.name} ${Economy.fmt(needCash)} istiyor."
                    }
                    else -> { o.status = OfferStatus.REJECTED; o.message = "${seller.name} teklifi reddetti. (Beklenti yaklaşık ${Economy.fmt(ask)})" }
                }
            }
            OfferType.FREE -> o.status = OfferStatus.ACCEPTED_CLUB
        }
    }

    /** Kullanıcı karşı teklifi kabul eder → bedel güncellenir, oyuncu görüşmesi açılır. */
    fun acceptCounter(gs: GameState, offerId: Int): String {
        val o = gs.offers.firstOrNull { it.id == offerId } ?: return "Teklif bulunamadı."
        if (o.status != OfferStatus.COUNTERED) return "Karşı teklif yok."
        if (o.type != OfferType.LOAN) {
            val c = o.counterFee ?: o.fee
            if (c > gs.userClub.finances.transferBudget) return "Bütçe yetersiz."
            o.fee = c
        }
        o.status = OfferStatus.ACCEPTED_CLUB
        o.message = "Kulüpler anlaştı. Oyuncuyla kişisel şartları görüşün."
        return o.message
    }

    /** Teklifi yükselt (yeni pazarlık turu). */
    fun improveOffer(gs: GameState, offerId: Int, newFee: Long): String {
        val o = gs.offers.firstOrNull { it.id == offerId } ?: return "Teklif bulunamadı."
        if (o.status !in setOf(OfferStatus.COUNTERED, OfferStatus.REJECTED)) return "Bu teklif güncellenemez."
        if (o.negotiationRound >= 3) return "Kulüp daha fazla pazarlık yapmak istemiyor."
        if (newFee > gs.userClub.finances.transferBudget) return "Bütçe yetersiz."
        windowCheck(gs, o.type)?.let { return it }
        if (o.type == OfferType.LOAN) o.loanFeePerSeason = newFee else o.fee = newFee
        o.negotiationRound++
        o.status = OfferStatus.PENDING
        o.responseDay = gs.day + 1
        return "Yeni teklif gönderildi. Yanıt yarın gelecek."
    }

    fun withdraw(gs: GameState, offerId: Int) {
        gs.offers.firstOrNull { it.id == offerId }?.let { it.status = OfferStatus.WITHDRAWN }
    }

    /** Oyuncu (ajanı) ile kişisel şart görüşmesi. */
    fun negotiateTerms(gs: GameState, offerId: Int, terms: PersonalTerms): TermsResult {
        val o = gs.offers.firstOrNull { it.id == offerId } ?: return TermsResult(false, true, "Teklif yok.", null)
        val p = gs.players[o.playerId] ?: return TermsResult(false, true, "Oyuncu yok.", null)
        val buyer = gs.clubs.getValue(o.fromClubId)
        val (interestScore, itxt) = interest(gs, p, buyer)
        if (interestScore < 25 && o.type != OfferType.LOAN) {
            o.status = OfferStatus.PLAYER_REJECTED
            o.message = "${p.fullName}: $itxt"
            return TermsResult(false, true, o.message, null)
        }
        val demand = demandTerms(gs, p, buyer, terms.squadRole, o.fee)
        if (o.type == OfferType.LOAN) {
            // kiralıkta maaş yapısı aynı kalır, oyuncu oynama süresi ister
            val rankAtBuyer = buyer.playerIds.mapNotNull { gs.players[it] }.count { it.overall > p.overall } + 1
            return if (rankAtBuyer <= 16 || interestScore >= 50) {
                complete(gs, o, null)
                TermsResult(true, false, "${p.fullName} kiralık olarak kadronuza katıldı!", null)
            } else {
                o.status = OfferStatus.PLAYER_REJECTED; o.message = "${p.fullName} kulübünüzde yeterli süre alamayacağını düşünüyor."
                TermsResult(false, true, o.message, null)
            }
        }
        val agent = gs.agents[p.agentId]
        val flex = 0.97 - (agent?.reliability ?: 50) / 2000.0  // güvenilir ajan küçük indirim yapar
        val wageOk = terms.weeklyWage + terms.signingBonus / 52 / max(1, terms.years) >= demand.weeklyWage * flex
        val agentOk = terms.agentFee >= demand.agentFee * 0.9
        val yearsOk = kotlin.math.abs(terms.years - demand.years) <= 1 || p.age(gs.season) < 30
        val wageBill = Economy.weeklyWageBill(gs, buyer)
        if (wageBill + terms.weeklyWage > buyer.finances.weeklyWageBudget * 1.0 && buyer.id == gs.userClubId) {
            return TermsResult(false, false, "Yönetim: Haftalık maaş bütçesi aşılıyor! (Mevcut ${Economy.fmt(wageBill)} / Bütçe ${Economy.fmt(buyer.finances.weeklyWageBudget)}) Finans ekranından transfer bütçesini maaşa aktarabilirsiniz.", demand)
        }
        if (wageOk && agentOk && yearsOk) {
            complete(gs, o, terms)
            return TermsResult(true, false, "Anlaşma tamam! ${p.fullName} ${buyer.name}'e imza attı.", null)
        }
        o.negotiationRound++
        if (o.negotiationRound >= 4 || terms.weeklyWage < demand.weeklyWage * 0.75) {
            o.status = OfferStatus.PLAYER_REJECTED
            o.message = "${agent?.name ?: "Ajan"}: \"Teklifiniz müvekkilimin değerinin çok altında. Görüşmeler sona erdi.\""
            return TermsResult(false, true, o.message, demand)
        }
        val parts = mutableListOf<String>()
        if (!wageOk) parts.add("haftalık ${Economy.fmt(demand.weeklyWage)} maaş")
        if (!agentOk) parts.add("${Economy.fmt(demand.agentFee)} ajan ücreti")
        if (!yearsOk) parts.add("${demand.years} yıllık sözleşme")
        return TermsResult(false, false, "${agent?.name ?: "Ajan"}: \"Müvekkilim ${parts.joinToString(", ")} istiyor.\" (Kalan tur: ${4 - o.negotiationRound})", demand)
    }

    /* ---------------- TRANSFERİ TAMAMLA ---------------- */

    fun complete(gs: GameState, o: TransferOffer, terms: PersonalTerms?) {
        val p = gs.players[o.playerId] ?: return
        val buyer = gs.clubs.getValue(o.fromClubId)
        val seller = clubOf(gs, p)
        val season = gs.season
        when (o.type) {
            OfferType.LOAN -> {
                val c = p.contract ?: return
                seller?.let { it.playerIds.remove(p.id); Economy.addLedger(gs, it, "Kiralık geliri: ${p.shortName}", o.loanFeePerSeason) }
                Economy.addLedger(gs, buyer, "Kiralık bedeli: ${p.shortName}", -o.loanFeePerSeason)
                buyer.finances.transferBudget -= o.loanFeePerSeason
                p.contract = c.copy(clubId = buyer.id, loanFromClubId = seller?.id, loanUntilSeason = season, loanWageShare = o.loanWageShare)
                buyer.playerIds.add(p.id)
                buyer.tactic.lineup.remove(p.id)
                gs.transferHistory.add(0, "${Scheduler.dateStr(season, gs.day)}: ${p.fullName} ${seller?.name} → ${buyer.name} (Kiralık)")
            }
            else -> {
                val fee = o.fee
                if (seller != null) {
                    seller.playerIds.remove(p.id); seller.youthIds.remove(p.id)
                    seller.tactic.lineup.remove(p.id); seller.tactic.bench.remove(p.id)
                    // sonradan satıştan pay
                    var sellerGets = fee
                    val so = p.sellOnClubId
                    if (so != null && p.sellOnPercent > 0 && fee > 0) {
                        val cut = fee * p.sellOnPercent / 100
                        sellerGets -= cut
                        gs.clubs[so]?.let { Economy.addLedger(gs, it, "Sonradan satış payı: ${p.shortName}", cut) }
                    }
                    if (o.installments <= 1) {
                        Economy.addLedger(gs, seller, "Satış: ${p.shortName}", sellerGets)
                        Economy.addLedger(gs, buyer, "Transfer: ${p.shortName}", -fee)
                    } else {
                        val part = fee / o.installments
                        Economy.addLedger(gs, seller, "Satış (1. taksit): ${p.shortName}", sellerGets / o.installments)
                        Economy.addLedger(gs, buyer, "Transfer (1. taksit): ${p.shortName}", -part)
                        for (k in 1 until o.installments) gs.scheduledPayments.add(ScheduledPayment(gs.day + 120 * k, season + (gs.day + 120 * k) / 340, buyer.id, seller.id, part, "Taksit: ${p.shortName}"))
                    }
                    buyer.finances.transferBudget -= if (o.installments <= 1) fee else fee / o.installments
                    seller.finances.transferBudget += (sellerGets * 0.7).toLong()
                    // takas oyuncuları
                    for (sid in o.swapPlayerIds) {
                        val sp = gs.players[sid] ?: continue
                        buyer.playerIds.remove(sid); buyer.tactic.lineup.remove(sid); buyer.tactic.bench.remove(sid)
                        seller.playerIds.add(sid)
                        sp.contract = sp.contract?.copy(clubId = seller.id) ?: Contract(seller.id, Economy.expectedWage(sp.overall, 50), season + 2)
                        sp.transferListed = false
                        gs.transferHistory.add(0, "${Scheduler.dateStr(season, gs.day)}: ${sp.fullName} ${buyer.name} → ${seller.name} (Takas)")
                    }
                } else {
                    gs.freeAgents.remove(p.id)
                }
                val t = terms ?: demandTerms(gs, p, buyer, SquadRole.ROTATION, fee)
                if (t.signingBonus > 0) Economy.addLedger(gs, buyer, "İmza parası: ${p.shortName}", -t.signingBonus)
                if (t.agentFee > 0) Economy.addLedger(gs, buyer, "Ajan ücreti: ${p.shortName}", -t.agentFee)
                p.contract = Contract(buyer.id, t.weeklyWage, season + t.years, t.releaseClause)
                p.sellOnClubId = if (o.sellOnPercent > 0) seller?.id else null
                p.sellOnPercent = o.sellOnPercent
                p.transferListed = false; p.loanListed = false; p.isYouth = false
                p.morale = (p.morale + 12).coerceAtMost(100)
                buyer.playerIds.add(p.id)
                val label = when (o.type) { OfferType.SWAP -> "Takas + ${Economy.fmt(fee)}"; OfferType.FREE -> "Bedelsiz"; else -> Economy.fmt(fee) }
                gs.transferHistory.add(0, "${Scheduler.dateStr(season, gs.day)}: ${p.fullName} ${seller?.name ?: "Serbest"} → ${buyer.name} ($label)")
            }
        }
        o.status = OfferStatus.COMPLETED
        if (gs.transferHistory.size > 300) gs.transferHistory.subList(300, gs.transferHistory.size).clear()
        if (buyer.id == gs.userClubId || seller?.id == gs.userClubId) {
            GameEngine.msg(gs, MsgType.TRANSFER, "Transfer tamamlandı: ${p.fullName}", gs.transferHistory.first())
        }
    }

    /* ---------------- GÜNLÜK İŞLEM ---------------- */

    fun processDaily(gs: GameState) {
        for (o in gs.offers) {
            if (o.status == OfferStatus.PENDING && o.responseDay <= gs.day) {
                if (o.fromClubId == gs.userClubId) {
                    evaluateClub(gs, o)
                    val p = gs.players[o.playerId]
                    GameEngine.msg(gs, MsgType.TRANSFER, "Teklif yanıtı: ${p?.fullName ?: ""}", "${o.status.tr}. ${o.message}", o.id)
                }
            }
        }
        // AI teklifleri (kullanıcı oyuncularına)
        if (Scheduler.transferWindowOpen(gs.day)) {
            aiOffersForUser(gs)
            aiTransfers(gs)
        }
        // eski teklifleri temizle
        gs.offers.removeAll { it.status !in activeStatuses && gs.day - it.createdDay > 30 }
        if (gs.offers.size > 200) gs.offers.subList(0, gs.offers.size - 200).clear()
    }

    private fun aiOffersForUser(gs: GameState) {
        val user = gs.userClub
        for (id in user.playerIds.toList()) {
            val p = gs.players[id] ?: continue
            if (p.contract?.loanFromClubId != null) continue
            val chance = when { p.transferListed -> 0.07; p.loanListed -> 0.05; p.overall >= 75 -> 0.012; else -> 0.004 }
            if (rnd.nextDouble() > chance) continue
            if (gs.offers.any { it.playerId == id && it.status in activeStatuses }) continue
            val value = Economy.value(p, gs.season)
            val buyers = gs.clubs.values.filter { it.id != user.id && it.reputation >= p.overall - 18 && it.finances.transferBudget >= value * 0.8 }
            if (buyers.isEmpty()) continue
            val buyer = buyers[rnd.nextInt(buyers.size)]
            val loan = p.loanListed && !p.transferListed
            val fee = if (loan) 0 else Economy.roundNice((value * (0.8 + rnd.nextDouble() * 0.45)).toLong())
            val o = TransferOffer(gs.newId(), id, buyer.id, user.id, if (loan) OfferType.LOAN else OfferType.TRANSFER, fee,
                loanWageShare = if (loan) 50 + rnd.nextInt(51) else 100, loanFeePerSeason = if (loan) value / 20 else 0,
                createdDay = gs.day, responseDay = gs.day + 5)
            gs.offers.add(o)
            msg(gs, buyer, p, o)
        }
    }

    private fun msg(gs: GameState, buyer: Club, p: Player, o: TransferOffer) {
        val body = if (o.type == OfferType.LOAN) "${buyer.name}, ${p.fullName} için kiralık teklif yaptı. Maaşın %${o.loanWageShare}'ini ödeyecekler, kiralama bedeli ${Economy.fmt(o.loanFeePerSeason)}."
        else "${buyer.name}, ${p.fullName} için ${Economy.fmt(o.fee)} teklif etti. (Piyasa değeri: ${Economy.fmt(Economy.value(p, gs.season))})"
        GameEngine.msg(gs, MsgType.TRANSFER, "Gelen teklif: ${p.fullName}", body, o.id)
    }

    /** Kullanıcı gelen teklife yanıt verir. counter != null → karşı teklif. */
    fun respondIncoming(gs: GameState, offerId: Int, accept: Boolean, counter: Long? = null): String {
        val o = gs.offers.firstOrNull { it.id == offerId } ?: return "Teklif bulunamadı."
        val p = gs.players[o.playerId] ?: return "Oyuncu bulunamadı."
        if (o.status !in activeStatuses) return "Teklif artık geçerli değil."
        if (counter != null) {
            val v = Economy.value(p, gs.season)
            return if (counter <= v * 1.35 && rnd.nextDouble() < 0.75 - (counter - o.fee).toDouble() / max(1L, v) ) {
                o.fee = counter
                if (o.toClubId == gs.userClubId) { complete(gs, o, null); "${gs.clubName(o.fromClubId)} karşı teklifinizi kabul etti! ${p.fullName} satıldı (${Economy.fmt(counter)})." }
                else "Kabul edildi."
            } else { o.status = OfferStatus.REJECTED; "${gs.clubName(o.fromClubId)} karşı teklifinizi reddetti ve görüşmelerden çekildi." }
        }
        return if (accept) {
            val (i, _) = interest(gs, p, gs.clubs.getValue(o.fromClubId))
            if (i < 20 && o.type != OfferType.LOAN) { o.status = OfferStatus.PLAYER_REJECTED; "${p.fullName} bu kulübe gitmeyi reddetti." }
            else { complete(gs, o, null); "${p.fullName}, ${gs.clubName(o.fromClubId)} kulübüne ${if (o.type == OfferType.LOAN) "kiralandı" else "satıldı"}." }
        } else { o.status = OfferStatus.REJECTED; "Teklif reddedildi." }
    }

    /* ---------------- AI ↔ AI TRANSFERLERİ ---------------- */

    private val needTemplate = mapOf(Position.GK to 2, Position.CB to 4, Position.LB to 1, Position.RB to 1, Position.DM to 1,
        Position.CM to 3, Position.AM to 1, Position.LW to 1, Position.RW to 1, Position.ST to 2)

    private fun aiTransfers(gs: GameState) {
        val clubs = gs.clubs.values.filter { it.id != gs.userClubId }
        val attempts = max(3, clubs.size / 45)
        repeat(attempts) {
            val club = clubs[rnd.nextInt(clubs.size)]
            val squad = club.playerIds.mapNotNull { gs.players[it] }
            if (squad.size > 30) {
                // fazla oyuncuyu serbest bırak / transfer listesine koy
                squad.minByOrNull { it.overall }?.let { w -> w.transferListed = true; if (squad.size > 33) release(gs, club, w) }
                return@repeat
            }
            val need = needTemplate.entries.firstOrNull { (pos, n) -> squad.count { it.position == pos } < n }?.key
                ?: squad.groupBy { it.position }.minByOrNull { (_, l) -> l.maxOf { it.overall } }?.key ?: return@repeat
            val currentBest = squad.filter { it.position == need }.maxOfOrNull { it.overall } ?: 0
            val budget = club.finances.transferBudget
            val target = findTarget(gs, club, need, currentBest, budget) ?: return@repeat
            val seller = clubOf(gs, target)
            if (seller?.id == gs.userClubId) return@repeat
            val fee = if (seller == null) 0 else askingPrice(gs, target)
            if (fee > budget) return@repeat
            val (i, _) = interest(gs, target, club)
            if (i < 35) return@repeat
            val o = TransferOffer(gs.newId(), target.id, club.id, seller?.id, if (seller == null) OfferType.FREE else OfferType.TRANSFER, fee, createdDay = gs.day, responseDay = gs.day)
            complete(gs, o, null)
            if (fee >= 25_000_000 || seller?.country == gs.userClub.country && fee >= 3_000_000) {
                GameEngine.msg(gs, MsgType.MEDIA, "Transfer haberi", "${target.fullName}, ${seller?.name ?: "serbest"} → ${club.name} (${Economy.fmt(fee)})")
            }
        }
    }

    private fun findTarget(gs: GameState, club: Club, pos: Position, currentBest: Int, budget: Long): Player? {
        // serbest oyuncular
        val free = gs.freeAgents.mapNotNull { gs.players[it] }.filter { it.position == pos && it.overall >= currentBest - 3 && it.overall <= club.reputation + 5 }
            .maxByOrNull { it.overall }
        if (free != null && rnd.nextBoolean()) return free
        // diğer kulüplerden (aynı ülke veya benzer itibar)
        val domestic = gs.clubs.values.filter { it.country == club.country }
        val all = gs.clubs.values.toList()
        repeat(25) {
            val c = if (rnd.nextDouble() < 0.65 && domestic.isNotEmpty()) domestic[rnd.nextInt(domestic.size)] else all[rnd.nextInt(all.size)]
            if (c.id == club.id || c.reputation > club.reputation + 3) return@repeat
            val cand = c.playerIds.mapNotNull { gs.players[it] }.filter {
                it.position == pos && it.overall > currentBest && it.contract?.loanFromClubId == null &&
                    Economy.value(it, gs.season) <= budget && it.age(gs.season) <= 31
            }.maxByOrNull { it.overall }
            if (cand != null) return cand
        }
        return free
    }

    fun release(gs: GameState, club: Club, p: Player) {
        val c = p.contract ?: return
        val left = max(0, c.expiresSeason - gs.season)
        val payoff = c.weeklyWage * 52L * left / 2
        if (payoff > 0) Economy.addLedger(gs, club, "Sözleşme feshi: ${p.shortName}", -payoff)
        club.playerIds.remove(p.id); club.youthIds.remove(p.id)
        club.tactic.lineup.remove(p.id); club.tactic.bench.remove(p.id)
        p.contract = null; p.transferListed = false; p.loanListed = false
        gs.freeAgents.add(p.id)
    }

    /** Sözleşme yenileme. */
    fun renewDemand(gs: GameState, p: Player): PersonalTerms {
        val club = clubOf(gs, p) ?: gs.userClub
        val d = demandTerms(gs, p, club, if (squadRank(gs, club, p) <= 5) SquadRole.STAR else SquadRole.FIRST_TEAM)
        return d.copy(weeklyWage = max(d.weeklyWage, (p.contract?.weeklyWage ?: 0) * 105 / 100), agentFee = d.agentFee / 2)
    }

    fun renew(gs: GameState, p: Player, wage: Long, years: Int): String {
        val d = renewDemand(gs, p)
        val club = clubOf(gs, p) ?: return "Oyuncu kulüpte değil."
        if (Economy.weeklyWageBill(gs, club) - (p.contract?.weeklyWage ?: 0) + wage > club.finances.weeklyWageBudget) return "Maaş bütçesi yetersiz."
        return if (wage >= d.weeklyWage * 0.95) {
            p.contract = p.contract!!.copy(weeklyWage = wage, expiresSeason = gs.season + years)
            Economy.addLedger(gs, club, "Ajan ücreti (yenileme): ${p.shortName}", -d.agentFee)
            p.morale = (p.morale + 8).coerceAtMost(100)
            "${p.fullName} sözleşmesini ${gs.season + years} sonuna kadar uzattı (${Economy.fmt(wage)}/hafta)."
        } else "${p.fullName} teklifi yetersiz buldu. Beklenti: ${Economy.fmt(d.weeklyWage)}/hafta."
    }
}
