package com.futbolmenajer.data.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/* ============================================================
 *  TEMEL VERİ MODELLERİ
 *  Tüm modeller kotlinx.serialization ile JSON'a kaydedilir.
 * ============================================================ */

enum class Line { GK, DEF, MID, ATT }

@Serializable
enum class Position(val tr: String, val line: Line) {
    GK("KL", Line.GK),
    CB("STP", Line.DEF), LB("SLB", Line.DEF), RB("SĞB", Line.DEF),
    DM("DOS", Line.MID), CM("MO", Line.MID), AM("OOS", Line.MID),
    LW("SLK", Line.ATT), RW("SĞK", Line.ATT), ST("SNT", Line.ATT)
}

@Serializable
data class Attributes(
    var pace: Int,        // Hız
    var shooting: Int,    // Şut
    var passing: Int,     // Pas
    var dribbling: Int,   // Top sürme / teknik
    var defending: Int,   // Savunma
    var physical: Int,    // Fizik
    var vision: Int,      // Oyun görüşü (mental)
    var composure: Int,   // Soğukkanlılık (mental)
    var workRate: Int,    // Çalışkanlık (mental)
    var leadership: Int,  // Liderlik (mental)
    var goalkeeping: Int  // Kalecilik
) {
    fun all(): List<Int> = listOf(pace, shooting, passing, dribbling, defending, physical,
        vision, composure, workRate, leadership, goalkeeping)

    /** Pozisyona göre ağırlıklı genel güç (1-99). */
    fun overallFor(pos: Position): Int {
        val v = when (pos) {
            Position.GK -> goalkeeping * 0.70 + composure * 0.10 + physical * 0.10 + passing * 0.05 + leadership * 0.05
            Position.CB -> defending * 0.40 + physical * 0.25 + pace * 0.10 + composure * 0.10 + passing * 0.05 + leadership * 0.10
            Position.LB, Position.RB -> defending * 0.30 + pace * 0.25 + physical * 0.12 + passing * 0.15 + workRate * 0.10 + dribbling * 0.08
            Position.DM -> defending * 0.30 + passing * 0.25 + physical * 0.15 + vision * 0.10 + workRate * 0.10 + composure * 0.10
            Position.CM -> passing * 0.30 + vision * 0.20 + dribbling * 0.12 + defending * 0.10 + workRate * 0.13 + shooting * 0.08 + physical * 0.07
            Position.AM -> passing * 0.25 + vision * 0.22 + dribbling * 0.22 + shooting * 0.18 + composure * 0.13
            Position.LW, Position.RW -> pace * 0.25 + dribbling * 0.28 + shooting * 0.17 + passing * 0.15 + vision * 0.08 + composure * 0.07
            Position.ST -> shooting * 0.40 + composure * 0.17 + pace * 0.15 + dribbling * 0.10 + physical * 0.13 + vision * 0.05
        }
        return v.roundToInt().coerceIn(1, 99)
    }
}

@Serializable
data class Contract(
    var clubId: Int,
    var weeklyWage: Long,          // € / hafta
    var expiresSeason: Int,        // Sözleşmenin bittiği sezon (ör. 2028 → 2027-28 sonu)
    var releaseClause: Long? = null,
    var loanFromClubId: Int? = null,   // Kiralıksa ana kulüp
    var loanUntilSeason: Int? = null,
    var loanWageShare: Int = 100       // Kiralayan kulübün ödediği maaş yüzdesi
)

@Serializable
data class SeasonStats(
    var apps: Int = 0, var goals: Int = 0, var assists: Int = 0,
    var yellow: Int = 0, var red: Int = 0, var ratingSum: Double = 0.0,
    var cleanSheets: Int = 0
) {
    val avgRating: Double get() = if (apps == 0) 0.0 else ratingSum / apps
}

@Serializable
enum class Personality(val tr: String) {
    PROFESSIONAL("Profesyonel"), AMBITIOUS("Hırslı"), LOYAL("Sadık"),
    MERCENARY("Paracı"), TEMPERAMENTAL("Asabi"), BALANCED("Dengeli")
}

@Serializable
data class Player(
    val id: Int,
    var firstName: String,
    var lastName: String,
    var birthYear: Int,
    var nationality: String,      // ISO-3 kodu (TUR, ENG, ESP ...)
    var position: Position,
    var secondary: List<Position> = emptyList(),
    var attrs: Attributes,
    var potential: Int,
    var contract: Contract?,      // null → serbest oyuncu
    var morale: Int = 70,         // 0-100
    var form: Int = 60,           // 0-100 (son maç performansları)
    var fitness: Int = 100,       // 0-100 (yorgunluk / kondisyon)
    var sharpness: Int = 70,      // maç ritmi
    var injuryDays: Int = 0,
    var injuryName: String? = null,
    var suspendedMatches: Int = 0,
    var seasonYellows: Int = 0,
    var stats: SeasonStats = SeasonStats(),
    var careerGoals: Int = 0,
    var careerApps: Int = 0,
    var personality: Personality = Personality.BALANCED,
    var agentId: Int = 0,
    var isYouth: Boolean = false,
    var transferListed: Boolean = false,
    var loanListed: Boolean = false,
    var photo: String? = null,    // yerel dosya / URL (lisanslı veri yüklenirse)
    var internationalCaps: Int = 0,
    var heightCm: Int = 180,
    var preferredFoot: String = "Sağ",
    var sellOnClubId: Int? = null,
    var sellOnPercent: Int = 0,
    var lastRating: Double = 0.0,
    var isReal: Boolean = false,
    var weeksUnhappy: Int = 0
) {
    val fullName get() = "$firstName $lastName"
    val shortName get() = if (firstName.isEmpty()) lastName else "${firstName.first()}. $lastName"
    fun age(season: Int) = season - birthYear
    val overall: Int get() = attrs.overallFor(position)
    fun ratingAt(pos: Position): Int {
        val base = attrs.overallFor(pos)
        return when {
            pos == position -> base
            pos in secondary -> (base * 0.97).roundToInt()
            pos.line == position.line -> (base * 0.88).roundToInt()
            else -> (base * 0.65).roundToInt()
        }
    }
    val available: Boolean get() = injuryDays == 0 && suspendedMatches == 0
}

/* ----------------------- KULÜP ----------------------- */

@Serializable
data class Facilities(
    var stadiumLevel: Int = 1,     // 1-10
    var stadiumCapacity: Int = 8000,
    var trainingLevel: Int = 1,    // gelişim hızını etkiler
    var youthLevel: Int = 1,       // akademi oyuncu kalitesi
    var medicalLevel: Int = 1,     // sakatlık süresi / riski
    var scoutingLevel: Int = 1,    // gözlemci isabeti
    var commercialLevel: Int = 1,  // sponsorluk ve mağaza geliri
    var upgradeInProgress: FacilityUpgrade? = null
)

@Serializable
enum class FacilityType(val tr: String) {
    STADIUM("Stadyum"), TRAINING("Antrenman Tesisi"), YOUTH("Gençlik Akademisi"),
    MEDICAL("Sağlık Merkezi"), SCOUTING("Scout Ağı"), COMMERCIAL("Ticari Alan / Mağaza")
}

@Serializable
data class FacilityUpgrade(val type: FacilityType, val cost: Long, var daysLeft: Int)

@Serializable
data class Finances(
    var balance: Long,
    var transferBudget: Long,
    var weeklyWageBudget: Long,
    var seasonIncome: Long = 0,
    var seasonExpense: Long = 0,
    var ticketPrice: Int = 25,
    val ledger: MutableList<LedgerEntry> = mutableListOf()
)

@Serializable
data class LedgerEntry(val day: Int, val desc: String, val amount: Long)

@Serializable
data class Club(
    val id: Int,
    var name: String,
    var shortName: String,
    var country: String,
    var leagueId: Int,
    var reputation: Int,          // 1-100
    var finances: Finances,
    var facilities: Facilities,
    var tactic: Tactic = Tactic(),
    val playerIds: MutableList<Int> = mutableListOf(),
    val youthIds: MutableList<Int> = mutableListOf(),
    var primaryColor: Long = 0xFF1B5E20,
    var secondaryColor: Long = 0xFFFFFFFF,
    var fanMood: Int = 60,
    var boardConfidence: Int = 70,
    var stadiumName: String = "",
    var coefficientPoints: Double = 0.0
)

/* ----------------------- TAKTİK ----------------------- */

@Serializable
enum class Formation(val tr: String, val slots: List<Position>) {
    F433("4-3-3", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.LB, Position.DM, Position.CM, Position.CM, Position.RW, Position.ST, Position.LW)),
    F4231("4-2-3-1", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.LB, Position.DM, Position.DM, Position.RW, Position.AM, Position.LW, Position.ST)),
    F442("4-4-2", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.LB, Position.RW, Position.CM, Position.CM, Position.LW, Position.ST, Position.ST)),
    F352("3-5-2", listOf(Position.GK, Position.CB, Position.CB, Position.CB, Position.RB, Position.DM, Position.CM, Position.CM, Position.LB, Position.ST, Position.ST)),
    F343("3-4-3", listOf(Position.GK, Position.CB, Position.CB, Position.CB, Position.RB, Position.CM, Position.CM, Position.LB, Position.RW, Position.ST, Position.LW)),
    F4141("4-1-4-1", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.LB, Position.DM, Position.RW, Position.CM, Position.CM, Position.LW, Position.ST)),
    F532("5-3-2", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.CB, Position.LB, Position.CM, Position.DM, Position.CM, Position.ST, Position.ST)),
    F41212("4-1-2-1-2", listOf(Position.GK, Position.RB, Position.CB, Position.CB, Position.LB, Position.DM, Position.CM, Position.CM, Position.AM, Position.ST, Position.ST))
}

@Serializable enum class Mentality(val tr: String, val atk: Double, val def: Double) {
    VERY_DEFENSIVE("Çok Defansif", 0.80, 1.18), DEFENSIVE("Defansif", 0.90, 1.09),
    BALANCED("Dengeli", 1.0, 1.0), ATTACKING("Hücum", 1.09, 0.92), VERY_ATTACKING("Tam Hücum", 1.18, 0.84)
}
@Serializable enum class Tempo(val tr: String) { SLOW("Yavaş"), NORMAL("Normal"), FAST("Hızlı") }
@Serializable enum class PressIntensity(val tr: String) { LOW("Düşük"), MEDIUM("Orta"), HIGH("Yüksek") }
@Serializable enum class PassingStyle(val tr: String) { SHORT("Kısa Pas"), MIXED("Karışık"), DIRECT("Direkt / Uzun") }
@Serializable enum class DefensiveLine(val tr: String) { DEEP("Derin"), NORMAL("Normal"), HIGH("Önde") }

@Serializable enum class PlayerInstruction(val tr: String) {
    NONE("Yok"), SHOOT_MORE("Daha çok şut çek"), STAY_BACK("Geride kal"),
    GET_FORWARD("İleri çık"), DRIBBLE_MORE("Daha çok top sür"), MARK_TIGHT("Yakın markaj"),
    CROSS_MORE("Daha çok orta aç"), HOLD_BALL("Topu sakla"), TACKLE_HARDER("Sert müdahale")
}

@Serializable
data class Tactic(
    var formation: Formation = Formation.F433,
    var mentality: Mentality = Mentality.BALANCED,
    var tempo: Tempo = Tempo.NORMAL,
    var press: PressIntensity = PressIntensity.MEDIUM,
    var passing: PassingStyle = PassingStyle.MIXED,
    var defLine: DefensiveLine = DefensiveLine.NORMAL,
    /** formation.slots ile aynı sırada 11 oyuncu ID'si */
    var lineup: MutableList<Int> = mutableListOf(),
    var bench: MutableList<Int> = mutableListOf(),
    var instructions: MutableMap<Int, PlayerInstruction> = mutableMapOf(),
    var captainId: Int? = null,
    var penaltyTakerId: Int? = null,
    var freeKickTakerId: Int? = null,
    var autoRotate: Boolean = false   // yorgun oyuncuları otomatik dinlendir
)

/* ----------------------- ANTRENMAN ----------------------- */

@Serializable enum class TrainingFocus(val tr: String) {
    BALANCED("Dengeli"), FITNESS("Kondisyon"), ATTACKING("Hücum"), DEFENDING("Savunma"),
    TECHNICAL("Teknik"), TACTICAL("Taktik"), SET_PIECES("Duran Toplar"), RECOVERY("Dinlenme")
}
@Serializable enum class TrainingIntensity(val tr: String, val gain: Double, val fatigue: Int, val injuryRisk: Double) {
    LIGHT("Hafif", 0.6, 2, 0.0005), NORMAL("Normal", 1.0, 5, 0.0012), HEAVY("Yoğun", 1.4, 9, 0.0030)
}

@Serializable
data class TrainingPlan(
    var focus: TrainingFocus = TrainingFocus.BALANCED,
    var intensity: TrainingIntensity = TrainingIntensity.NORMAL,
    var individual: MutableMap<Int, TrainingFocus> = mutableMapOf()
)

/* ----------------------- LİG / TURNUVA ----------------------- */

@Serializable enum class CompetitionType { LEAGUE, DOMESTIC_CUP, SUPER_CUP, UCL, UEL, UECL, WORLD_CUP, NATIONS_FRIENDLY, CLUB_WORLD_CUP }

@Serializable
data class League(
    val id: Int,
    val name: String,
    val country: String,
    val tier: Int,
    val clubIds: MutableList<Int> = mutableListOf(),
    val promotions: Int = 3,
    val relegations: Int = 3,
    val playoffPromotion: Boolean = false,
    val reputation: Int = 50
)

@Serializable
data class Competition(
    val id: Int,
    val name: String,
    val type: CompetitionType,
    val country: String? = null,
    val leagueId: Int? = null,
    var participants: MutableList<Int> = mutableListOf(),   // kulüp ya da milli takım ID'leri
    var stage: String = "",
    var finished: Boolean = false,
    var winnerId: Int? = null,
    /** grup / lig aşaması tabloları: grup adı → takım ID listesi */
    var groups: MutableMap<String, MutableList<Int>> = mutableMapOf(),
    /** eleme turları için: tur indeksi (0 = lig/grup aşaması) */
    var round: Int = 0,
    val roundNames: List<String> = emptyList()
)

@Serializable
data class Fixture(
    val id: Int,
    val competitionId: Int,
    val day: Int,
    val homeId: Int,
    val awayId: Int,
    val roundLabel: String,
    val isNational: Boolean = false,
    val knockout: Boolean = false,
    /** iki ayaklı eşleşmede ilk maçın ID'si (rövanş ise) */
    val firstLegId: Int? = null,
    val groupName: String? = null,
    var played: Boolean = false,
    var homeGoals: Int = 0,
    var awayGoals: Int = 0,
    var homePens: Int? = null,
    var awayPens: Int? = null,
    var extraTime: Boolean = false,
    var attendance: Int = 0,
    /** 0 = tek maç, 1 = ilk ayak, 2 = rövanş */
    val leg: Int = 0,
    val neutral: Boolean = false
) {
    fun winnerId(): Int? = when {
        !played -> null
        homePens != null && awayPens != null -> if (homePens!! > awayPens!!) homeId else awayId
        homeGoals > awayGoals -> homeId
        awayGoals > homeGoals -> awayId
        else -> null
    }
}

@Serializable
data class TableRow(
    val clubId: Int, var p: Int = 0, var w: Int = 0, var d: Int = 0, var l: Int = 0,
    var gf: Int = 0, var ga: Int = 0
) {
    val pts get() = w * 3 + d
    val gd get() = gf - ga
}

/* ----------------------- MİLLİ TAKIM ----------------------- */

@Serializable
data class NationalTeam(
    val id: Int,                 // 100000 + index (kulüp ID'leriyle çakışmaz)
    val code: String,
    val name: String,
    var reputation: Int,
    var squad: MutableList<Int> = mutableListOf(),
    var fifaPoints: Double = 1200.0
)

/* ----------------------- TRANSFER ----------------------- */

@Serializable enum class OfferType(val tr: String) { TRANSFER("Bonservis"), LOAN("Kiralık"), SWAP("Takas"), FREE("Serbest") }
@Serializable enum class OfferStatus(val tr: String) {
    PENDING("Beklemede"), COUNTERED("Karşı teklif"), ACCEPTED_CLUB("Kulüp kabul etti – oyuncuyla görüş"),
    AGREED("Anlaşma sağlandı"), REJECTED("Reddedildi"), PLAYER_REJECTED("Oyuncu reddetti"),
    WITHDRAWN("Geri çekildi"), COMPLETED("Tamamlandı")
}

@Serializable
data class TransferOffer(
    val id: Int,
    val playerId: Int,
    val fromClubId: Int,          // teklifi yapan (alıcı)
    val toClubId: Int?,           // oyuncunun kulübü (serbestse null)
    val type: OfferType,
    var fee: Long = 0,
    val swapPlayerIds: List<Int> = emptyList(),
    var loanWageShare: Int = 100,
    var loanFeePerSeason: Long = 0,
    var loanBuyOption: Long? = null,
    var sellOnPercent: Int = 0,
    var installments: Int = 1,    // taksit sayısı
    var status: OfferStatus = OfferStatus.PENDING,
    var counterFee: Long? = null,
    var counterSwapCash: Long? = null,
    var negotiationRound: Int = 0,
    val createdDay: Int,
    var responseDay: Int,
    var personalTerms: PersonalTerms? = null,
    var message: String = ""
)

@Serializable
data class PersonalTerms(
    var weeklyWage: Long,
    var years: Int,
    var signingBonus: Long = 0,
    var agentFee: Long = 0,
    var releaseClause: Long? = null,
    var squadRole: SquadRole = SquadRole.ROTATION
)

@Serializable enum class SquadRole(val tr: String, val wageMult: Double) {
    STAR("Yıldız oyuncu", 1.25), FIRST_TEAM("İlk 11", 1.10), ROTATION("Rotasyon", 1.0),
    BACKUP("Yedek", 0.9), PROSPECT("Gelecek vaat eden", 0.8)
}

@Serializable
data class Agent(val id: Int, val name: String, val greed: Int, val reliability: Int)

/* ----------------------- MESAJLAR / OLAYLAR ----------------------- */

@Serializable enum class MsgType { INFO, TRANSFER, BOARD, INJURY, MEDIA, YOUTH, MATCH, FINANCE }

@Serializable
data class InboxMessage(
    val id: Int, val day: Int, val type: MsgType,
    val title: String, val body: String, var read: Boolean = false,
    val offerId: Int? = null
)

@Serializable
data class SeasonObjective(
    val description: String,
    val targetPosition: Int,          // lig sıralama hedefi
    val cupTargetRound: String? = null,
    var achieved: Boolean? = null
)

/* ----------------------- OYUN DURUMU ----------------------- */

@Serializable
data class GameState(
    var saveName: String = "Kariyer",
    var managerName: String = "Menajer",
    var season: Int = 2026,           // 2026 → 2025-26 değil, 2026-27 sezonu başlangıcı
    var day: Int = 0,                 // sezon içi gün (0 = 1 Temmuz)
    var userClubId: Int = -1,
    val players: HashMap<Int, Player> = HashMap(),
    val clubs: HashMap<Int, Club> = HashMap(),
    val leagues: HashMap<Int, League> = HashMap(),
    val competitions: HashMap<Int, Competition> = HashMap(),
    val fixtures: MutableList<Fixture> = mutableListOf(),
    val nationalTeams: HashMap<String, NationalTeam> = HashMap(),
    val agents: HashMap<Int, Agent> = HashMap(),
    val offers: MutableList<TransferOffer> = mutableListOf(),
    val inbox: MutableList<InboxMessage> = mutableListOf(),
    val freeAgents: MutableList<Int> = mutableListOf(),
    var training: TrainingPlan = TrainingPlan(),
    var objective: SeasonObjective? = null,
    var managerReputation: Int = 40,
    var nextId: Int = 1_000_000,
    val history: MutableList<String> = mutableListOf(),
    val countryCoefficients: HashMap<String, Double> = HashMap(),
    var pendingPress: PressConference? = null,
    var rngSeed: Long = 42L,
    val scheduledPayments: MutableList<ScheduledPayment> = mutableListOf(),
    val transferHistory: MutableList<String> = mutableListOf(),
    val matchReports: HashMap<Int, MatchReport> = HashMap(),
    var sacked: Boolean = false,
    var lastCupWinners: HashMap<String, Int> = HashMap(),       // ülke → kupa şampiyonu
    var lastLeagueChampions: HashMap<String, Int> = HashMap(),  // ülke → lig şampiyonu
    /** competitionId → puan tablosu (lig ve Avrupa lig aşaması) */
    val tables: HashMap<Int, MutableList<TableRow>> = HashMap(),
    val seasonRecords: MutableList<SeasonRecord> = mutableListOf(),
    val trophies: MutableList<String> = mutableListOf(),
    var pendingUserFixtureId: Int? = null,
    var lastTrainingDay: Int = 0,
    var scoutedPlayerIds: MutableList<Int> = mutableListOf(),
    var shortlist: MutableList<Int> = mutableListOf(),
    /** leagueId → geçen sezonun final sıralaması */
    val lastLeagueFinal: HashMap<Int, MutableList<Int>> = HashMap(),
    var jobOffers: MutableList<Int> = mutableListOf(),
    var version: Int = 1
) {
    fun newId(): Int = nextId++
    val userClub: Club get() = clubs.getValue(userClubId)
    fun clubName(id: Int): String = clubs[id]?.name ?: nationalTeams.values.firstOrNull { it.id == id }?.name ?: "?"
}

@Serializable
data class PressConference(val question: String, val options: List<PressOption>, val context: String)
@Serializable
data class PressOption(val text: String, val moraleDelta: Int, val fanDelta: Int, val boardDelta: Int, val mediaNote: String)

@Serializable
data class ScheduledPayment(val day: Int, val season: Int, val fromClub: Int, val toClub: Int, val amount: Long, val desc: String)

/* ----------------------- MAÇ RAPORU ----------------------- */

@Serializable enum class EventType { KICKOFF, GOAL, PENALTY_GOAL, PENALTY_MISS, OWN_GOAL, CHANCE, SAVE, YELLOW, SECOND_YELLOW, RED, INJURY, SUB, VAR, CORNER, GK_8_SECONDS, HALF_TIME, FULL_TIME, EXTRA_TIME, SHOOTOUT, INFO }

@Serializable
data class MatchEvent(
    val minute: Int, val type: EventType, val teamId: Int?,
    val playerId: Int? = null, val secondPlayerId: Int? = null, val text: String,
    val added: Int = 0
)

@Serializable
data class TeamMatchStats(
    var goals: Int = 0, var shots: Int = 0, var onTarget: Int = 0, var xg: Double = 0.0,
    var possessionTicks: Int = 0, var corners: Int = 0, var fouls: Int = 0,
    var yellows: Int = 0, var reds: Int = 0, var offsides: Int = 0, var passes: Int = 0,
    var saves: Int = 0
)

@Serializable
data class MatchReport(
    val fixtureId: Int, val homeId: Int, val awayId: Int,
    val events: List<MatchEvent>, val home: TeamMatchStats, val away: TeamMatchStats,
    val ratings: Map<Int, Double>, val homeLineup: List<Int>, val awayLineup: List<Int>,
    val homePens: Int? = null, val awayPens: Int? = null, val motmId: Int? = null
)

@Serializable
data class SeasonRecord(val season: Int, val clubName: String, val leagueName: String, val position: Int, val note: String)
