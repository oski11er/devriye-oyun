package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/** Para, piyasa değeri ve maaş hesapları. Tüm tutarlar Euro. */
object Economy {

    /** Piyasa değeri: güç, yaş, potansiyel ve kalan sözleşmeye göre. */
    fun value(p: Player, season: Int): Long {
        val ovr = p.overall
        var v = 600.0 * exp(0.135 * ovr)
        val age = p.age(season)
        v *= when {
            age <= 19 -> 1.35
            age <= 21 -> 1.30
            age <= 24 -> 1.15
            age <= 27 -> 1.05
            age <= 29 -> 0.90
            age <= 31 -> 0.65
            age <= 33 -> 0.40
            else -> 0.22
        }
        if (age < 25 && p.potential > ovr) v *= 1.0 + (p.potential - ovr) * 0.035
        val c = p.contract
        if (c != null) {
            val left = c.expiresSeason - season
            v *= when {
                left <= 0 -> 0.35
                left == 1 -> 0.75
                else -> 1.0
            }
        }
        if (p.injuryDays > 30) v *= 0.8
        return roundNice(max(5_000.0, v).roundToLong())
    }

    /** Oyuncunun beklediği haftalık maaş (lig itibarına göre). */
    fun expectedWage(ovr: Int, leagueRep: Int): Long {
        val base = 15.0 * exp(0.105 * ovr)
        val leagueFactor = 0.30 + 0.75 * (leagueRep / 100.0)
        return roundNice(max(300.0, base * leagueFactor).roundToLong())
    }

    fun roundNice(x: Long): Long {
        val a = abs(x)
        val step = when {
            a >= 10_000_000 -> 100_000L
            a >= 1_000_000 -> 50_000L
            a >= 100_000 -> 5_000L
            a >= 10_000 -> 500L
            a >= 1_000 -> 50L
            else -> 10L
        }
        return (x / step) * step
    }

    fun fmt(amount: Long): String {
        val a = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            a >= 1_000_000_000 -> sign + "€" + String.format(Locale.US, "%.2f", a / 1e9) + " Mr"
            a >= 1_000_000 -> sign + "€" + String.format(Locale.US, "%.1f", a / 1e6) + " M"
            a >= 1_000 -> sign + "€" + String.format(Locale.US, "%.0f", a / 1e3) + " B"
            else -> "$sign€$a"
        }
    }

    fun clubLeagueRep(gs: GameState, clubId: Int?): Int {
        val c = clubId?.let { gs.clubs[it] } ?: return 45
        return gs.leagues[c.leagueId]?.reputation ?: 45
    }

    /** Toplam haftalık maaş yükü (kiralık maaş paylaşımı dahil). */
    fun weeklyWageBill(gs: GameState, club: Club, includeLoanedOut: Boolean = true): Long {
        var sum = 0L
        for (id in club.playerIds) {
            val p = gs.players[id] ?: continue
            val c = p.contract ?: continue
            sum += if (c.loanFromClubId != null) c.weeklyWage * c.loanWageShare / 100 else c.weeklyWage
        }
        // başka kulübe kiraladığımız oyuncuların maaşının bizde kalan kısmı
        if (includeLoanedOut) for (p in gs.players.values) {
            val c = p.contract ?: continue
            if (c.loanFromClubId == club.id) sum += c.weeklyWage * (100 - c.loanWageShare) / 100
        }
        return sum
    }

    fun addLedger(gs: GameState, club: Club, desc: String, amount: Long) {
        club.finances.balance += amount
        if (amount >= 0) club.finances.seasonIncome += amount else club.finances.seasonExpense += -amount
        if (club.id == gs.userClubId) {
            club.finances.ledger.add(0, LedgerEntry(gs.day, desc, amount))
            if (club.finances.ledger.size > 120) club.finances.ledger.removeAt(club.finances.ledger.size - 1)
        }
    }

    fun facilityUpgradeCost(type: FacilityType, level: Int): Long {
        val base = when (type) {
            FacilityType.STADIUM -> 12_000_000.0
            FacilityType.TRAINING -> 6_000_000.0
            FacilityType.YOUTH -> 4_000_000.0
            FacilityType.MEDICAL -> 3_000_000.0
            FacilityType.SCOUTING -> 2_000_000.0
            FacilityType.COMMERCIAL -> 5_000_000.0
        }
        return roundNice((base * Math.pow(level.toDouble(), 1.5)).roundToLong())
    }

    fun facilityUpgradeDays(type: FacilityType, level: Int): Int =
        if (type == FacilityType.STADIUM) 45 + 25 * level else 20 + 12 * level

    fun facilityLevel(f: Facilities, t: FacilityType): Int = when (t) {
        FacilityType.STADIUM -> f.stadiumLevel
        FacilityType.TRAINING -> f.trainingLevel
        FacilityType.YOUTH -> f.youthLevel
        FacilityType.MEDICAL -> f.medicalLevel
        FacilityType.SCOUTING -> f.scoutingLevel
        FacilityType.COMMERCIAL -> f.commercialLevel
    }

    fun fairTicketPrice(rep: Int): Int = 10 + (rep * 0.6).toInt()

    fun attendance(club: Club, importance: Double): Int {
        val fair = fairTicketPrice(club.reputation)
        val priceEffect = (club.finances.ticketPrice - fair).toDouble() / fair * 0.5
        val ratio = (0.35 + club.reputation / 150.0 + club.fanMood / 300.0 - priceEffect) * importance
        return (club.facilities.stadiumCapacity * min(1.0, max(0.15, ratio))).toInt()
    }

    /** Lig sezonluk TV geliri (kulüp başına). */
    fun seasonTv(league: League): Long {
        val r = league.reputation.toDouble()
        return (2.0 * r * r * r * r * Math.pow(0.35, (league.tier - 1).toDouble())).roundToLong()
    }

    /** Haftalık sponsorluk/mağaza geliri. */
    fun weeklyCommercial(club: Club): Long {
        val r = club.reputation.toDouble()
        return (r * r * r * club.facilities.commercialLevel * 0.9 * (0.8 + club.fanMood / 250.0)).roundToLong()
    }
}
