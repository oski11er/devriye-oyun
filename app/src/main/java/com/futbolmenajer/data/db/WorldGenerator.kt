package com.futbolmenajer.data.db

import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.Economy
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * DÜNYA ÜRETİCİ
 * Lig kataloğu + gerçek oyuncu veritabanından tam bir oyun dünyası kurar:
 * ~50 ülke, ~90 lig, ~1450 kulüp, ~33.000 oyuncu, 64 milli takım, 250 menajer (ajan).
 */
object WorldGenerator {

    const val START_SEASON = 2026

    /** Bilinen kulüplerin itibar puanları (1-100). Diğerleri ülke/küme gücünden hesaplanır. */
    val repOverride: Map<String, Int> = mapOf(
        "Real Madrid" to 96, "Manchester City" to 94, "Barcelona" to 94, "Bayern München" to 94,
        "Liverpool" to 93, "Paris Saint-Germain" to 93, "Arsenal" to 92, "Inter" to 90, "Chelsea" to 89,
        "Atlético Madrid" to 88, "Manchester United" to 88, "Juventus" to 87, "Milan" to 86,
        "Borussia Dortmund" to 86, "Napoli" to 85, "Tottenham Hotspur" to 85, "Newcastle United" to 85,
        "Bayer Leverkusen" to 84, "Aston Villa" to 83, "Roma" to 82, "Atalanta" to 82, "RB Leipzig" to 82,
        "Benfica" to 81, "Porto" to 81, "Sporting CP" to 80, "Athletic Club" to 80, "Villarreal" to 79,
        "Real Sociedad" to 78, "Real Betis" to 78, "Lazio" to 78, "Marseille" to 79, "Monaco" to 79,
        "Lyon" to 77, "Lille" to 77, "Brighton" to 79, "Nottingham Forest" to 78, "Crystal Palace" to 77,
        "West Ham United" to 77, "Fulham" to 75, "Bournemouth" to 76, "Brentford" to 75, "Everton" to 75,
        "Wolverhampton" to 74, "Leeds United" to 72, "Sunderland" to 72, "Burnley" to 70,
        "Eintracht Frankfurt" to 79, "VfB Stuttgart" to 78, "SC Freiburg" to 76, "VfL Wolfsburg" to 75,
        "Borussia Mönchengladbach" to 75, "TSG Hoffenheim" to 74, "Werder Bremen" to 73, "1. FSV Mainz 05" to 74,
        "Galatasaray" to 80, "Fenerbahçe" to 79, "Beşiktaş" to 76, "Trabzonspor" to 73, "Başakşehir" to 70,
        "Samsunspor" to 67, "Göztepe" to 66, "Club Brugge" to 77, "PSV" to 78, "Feyenoord" to 77, "Ajax" to 77,
        "AZ" to 74, "FC Twente" to 72, "Celtic" to 75, "Rangers" to 73, "Al-Hilal" to 78, "Al-Nassr" to 78,
        "Al-Ittihad" to 76, "Al-Ahli" to 76, "Al-Qadsiah" to 72, "Flamengo" to 79, "Palmeiras" to 79,
        "Botafogo" to 75, "River Plate" to 77, "Boca Juniors" to 77, "Inter Miami" to 72, "LAFC" to 70,
        "Fiorentina" to 77, "Bologna" to 78, "Como" to 76, "Torino" to 74, "Olympiakos" to 74, "PAOK" to 72,
        "Panathinaikos" to 71, "AEK Atina" to 71, "Salzburg" to 73, "Shakhtar Donetsk" to 72,
        "Dynamo Kyiv" to 69, "Dinamo Zagreb" to 72, "Crvena Zvezda" to 72, "Slavia Praha" to 72,
        "Sparta Praha" to 70, "Viktoria Plzeň" to 69, "Young Boys" to 71, "Basel" to 69, "Sevilla" to 76,
        "Valencia" to 75, "Girona" to 75, "Celta Vigo" to 75, "Osasuna" to 73, "Rennes" to 76, "Nice" to 76,
        "Lens" to 76, "Strasbourg" to 75, "Union SG" to 73, "Anderlecht" to 72, "Genk" to 71,
        "København" to 71, "Midtjylland" to 70, "Bodø/Glimt" to 71, "Zenit" to 75, "Al Ahly" to 72,
        "Pyramids" to 68, "Ferencváros" to 69, "Qarabağ" to 69, "Ludogorets" to 68, "Legia Warszawa" to 68,
        "Lech Poznań" to 68, "Malmö FF" to 68, "Pafos FC" to 66, "Braga" to 75, "Vitória SC" to 70,
        "Sturm Graz" to 69, "Monterrey" to 70, "América" to 70, "Tigres UANL" to 70, "Cruz Azul" to 69,
        "Vissel Kobe" to 66, "Ulsan HD" to 64, "Wydad Casablanca" to 64, "Al-Sadd" to 66,
        // 2026-27 yükselen / güncellenen kulüpler
        "Coventry City" to 70, "Hull City" to 67, "Ipswich Town" to 68, "FC Schalke 04" to 71, "SC Paderborn 07" to 64,
        "SV Elversberg" to 63, "Deportivo" to 67, "Málaga" to 66, "Racing Santander" to 65, "Frosinone" to 64, "Monza" to 67,
        "Venezia" to 65, "Le Mans" to 60, "Troyes" to 62, "Amedspor" to 58, "Çorum FK" to 56, "Erzurumspor" to 57,
        "Paris FC" to 72, "West Ham United" to 74, "Wolverhampton" to 72, "Burnley" to 69, "VfL Wolfsburg" to 72,
        "Girona" to 72, "Mallorca" to 70, "Antalyaspor" to 60, "Kayserispor" to 59, "Fatih Karagümrük" to 58
    )

    private val colorOverride: Map<String, Pair<Long, Long>> = mapOf(
        "Galatasaray" to (0xFFA90432 to 0xFFFDB912), "Fenerbahçe" to (0xFF0A1E5E to 0xFFFFED00),
        "Beşiktaş" to (0xFF000000 to 0xFFFFFFFF), "Trabzonspor" to (0xFF7B1C2E to 0xFF6CB4E4),
        "Real Madrid" to (0xFFFFFFFF to 0xFF1C2C5B), "Barcelona" to (0xFFA50044 to 0xFF004D98),
        "Manchester City" to (0xFF6CABDD to 0xFFFFFFFF), "Liverpool" to (0xFFC8102E to 0xFFFFFFFF),
        "Arsenal" to (0xFFEF0107 to 0xFFFFFFFF), "Chelsea" to (0xFF034694 to 0xFFFFFFFF),
        "Manchester United" to (0xFFDA291C to 0xFF000000), "Bayern München" to (0xFFDC052D to 0xFFFFFFFF),
        "Borussia Dortmund" to (0xFFFDE100 to 0xFF000000), "Juventus" to (0xFF000000 to 0xFFFFFFFF),
        "Inter" to (0xFF010E80 to 0xFF000000), "Milan" to (0xFFFB090B to 0xFF000000),
        "Paris Saint-Germain" to (0xFF004170 to 0xFFDA291C), "Atlético Madrid" to (0xFFCB3524 to 0xFFFFFFFF)
    )

    private val palette = listOf(0xFF1B5E20, 0xFFB71C1C, 0xFF0D47A1, 0xFFF9A825, 0xFF4A148C, 0xFF263238,
        0xFFE65100, 0xFF006064, 0xFF880E4F, 0xFF1A237E, 0xFF33691E, 0xFF3E2723)

    /** Kadro şablonu (pozisyon → sayı). */
    private val bigTemplate = listOf(Position.GK to 3, Position.CB to 4, Position.LB to 2, Position.RB to 2,
        Position.DM to 2, Position.CM to 3, Position.AM to 2, Position.LW to 2, Position.RW to 2, Position.ST to 3)
    private val smallTemplate = listOf(Position.GK to 2, Position.CB to 4, Position.LB to 1, Position.RB to 1,
        Position.DM to 2, Position.CM to 3, Position.AM to 1, Position.LW to 2, Position.RW to 2, Position.ST to 3)

    fun generate(seed: Long, progress: (Float, String) -> Unit = { _, _ -> }): GameState {
        val rnd = Random(seed)
        val gs = GameState(season = START_SEASON, rngSeed = seed)
        progress(0.02f, "Menajerler (ajanlar) oluşturuluyor…")
        val groups = NameGroup.values()
        for (i in 1..250) {
            val (f, l) = NameBank.randomName(groups[rnd.nextInt(groups.size)], rnd)
            gs.agents[i] = Agent(i, "$f $l", greed = rnd.nextInt(20, 91), reliability = rnd.nextInt(20, 96))
        }

        var leagueId = 1
        var clubId = 1
        val countries = LeagueCatalog.countries
        countries.forEachIndexed { ci, cd ->
            progress(0.05f + 0.75f * ci / countries.size, "${cd.nameTr} ligleri hazırlanıyor…")
            gs.countryCoefficients[cd.code] = cd.coefficient
            val used = HashSet<String>()
            val cityPool = ArrayDeque(cd.cities)
            var genericCounter = 1
            fun nextName(): String {
                while (cityPool.isNotEmpty()) {
                    val n = cityPool.removeFirst()
                    if (used.add(n)) return n
                }
                val suffixes = LeagueCatalog.clubSuffix(cd.code)
                while (true) {
                    val base = cd.cities.getOrNull(rnd.nextInt(max(1, cd.cities.size))) ?: cd.nameTr
                    val n = base + suffixes[rnd.nextInt(suffixes.size)] + if (genericCounter > 40) " ${genericCounter}" else ""
                    genericCounter++
                    if (used.add(n)) return n
                }
            }
            cd.leagues.forEachIndexed { ti, ld ->
                val tier = ti + 1
                val leagueRep = (30 + cd.coefficient * 0.65 - (tier - 1) * 13).roundToInt().coerceIn(10, 99)
                val league = League(leagueId++, ld.name, cd.code, tier, promotions = ld.promo, relegations = ld.releg,
                    playoffPromotion = ld.playoff, reputation = leagueRep)
                val names = mutableListOf<String>()
                ld.clubs.forEach { if (used.add(it)) names.add(it) }
                while (names.size < ld.size) names.add(nextName())
                names.forEachIndexed { idx, name ->
                    val baseRep = 30 + cd.coefficient * 0.45 - (tier - 1) * 9
                    // listelenmemiş ligde sıra = güç sırası (katalogdaki şehir/kulüp listeleri güç sırasına yakındır)
                    val orderBonus = if (ld.clubs.isEmpty()) (ld.size / 2 - idx) * 0.6 else 0.0
                    val rep = (repOverride[name] ?: (baseRep + orderBonus + rnd.nextInt(-6, 7)).roundToInt()).coerceIn(5, 99)
                    val club = makeClub(clubId++, name, cd.code, league.id, rep, rnd)
                    gs.clubs[club.id] = club
                    league.clubIds.add(club.id)
                }
                gs.leagues[league.id] = league
            }
        }

        progress(0.82f, "Oyuncular oluşturuluyor…")
        var pid = 1
        val season = START_SEASON
        for (club in gs.clubs.values.sortedBy { it.id }) {
            val league = gs.leagues.getValue(club.leagueId)
            val template = if (league.tier <= 2) bigTemplate else smallTemplate
            val need = HashMap<Position, Int>().apply { template.forEach { (p, n) -> put(p, n) } }
            // 1) gerçek oyuncular
            for (e in RealPlayers.parse(club.name)) {
                val pos = runCatching { Position.valueOf(e.pos) }.getOrDefault(Position.CM)
                val p = makePlayer(pid++, e.first, e.last, e.birth, e.nat, pos, e.ovr, e.pot, rnd)
                p.isReal = true
                attachContract(gs, p, club, league, season, rnd)
                gs.players[p.id] = p
                club.playerIds.add(p.id)
                need[pos] = (need[pos] ?: 0) - 1
            }
            // 2) üretilen oyuncular
            val mean = 33 + club.reputation * 0.55
            for ((pos, n) in template) {
                val left = need[pos] ?: 0
                for (k in 0 until left) {
                    val starter = k == 0 && (need[pos] ?: 0) == template.first { it.first == pos }.second
                    val ovr = (mean + (if (starter) 2 else -3) + rnd.nextInt(-6, 6)).roundToInt().coerceIn(30, 88)
                    val nat = pickNationality(club.country, league.tier, rnd)
                    val age = randomAge(rnd)
                    val p = generatedPlayer(pid++, nat, pos, ovr, age, season, rnd)
                    attachContract(gs, p, club, league, season, rnd)
                    gs.players[p.id] = p
                    club.playerIds.add(p.id)
                }
            }
            // 3) gençlik akademisi
            repeat(4) {
                val pos = Position.values()[rnd.nextInt(Position.values().size)]
                val ovr = (28 + club.facilities.youthLevel * 2 + club.reputation * 0.15 + rnd.nextInt(0, 10)).roundToInt()
                val p = generatedPlayer(pid++, pickNationality(club.country, 5, rnd), pos, ovr, rnd.nextInt(15, 19), season, rnd)
                p.isYouth = true
                p.potential = (ovr + rnd.nextInt(6, 24) + club.facilities.youthLevel + if (rnd.nextInt(40) == 0) 12 else 0).coerceAtMost(92)
                p.contract = Contract(club.id, 200L + rnd.nextInt(0, 400), season + 3)
                gs.players[p.id] = p
                club.youthIds.add(p.id)
            }
            club.finances.weeklyWageBudget = Economy.roundNice((Economy.weeklyWageBill(gs, club, includeLoanedOut = false) * 1.12).toLong())
        }

        progress(0.90f, "Milli takımlar kuruluyor…")
        // Milli takımlar için yeterli oyuncu havuzu
        val byNat = gs.players.values.groupingBy { it.nationality }.eachCount()
        val hostLeagues = gs.leagues.values.filter { it.tier <= 2 && it.country in setOf("FRA", "BEL", "TUR", "POR", "ENG", "ESP", "GER", "ITA", "NED", "SUI", "AUT", "KSA") }
        NameBank.nations.forEachIndexed { i, n ->
            val have = byNat[n.code] ?: 0
            for (k in have until 30) {
                val league = hostLeagues[rnd.nextInt(hostLeagues.size)]
                val club = gs.clubs.getValue(league.clubIds[rnd.nextInt(league.clubIds.size)])
                val pos = Position.values()[rnd.nextInt(Position.values().size)]
                val ovr = (n.strength - 9 + rnd.nextInt(-8, 7)).coerceIn(45, 86)
                val p = generatedPlayer(pid++, n.code, pos, ovr, randomAge(rnd), season, rnd)
                attachContract(gs, p, club, league, season, rnd)
                gs.players[p.id] = p
                club.playerIds.add(p.id)
            }
            gs.nationalTeams[n.code] = NationalTeam(100_000 + i, n.code, n.nameTr, n.strength,
                fifaPoints = 1000.0 + n.strength * 10.0)
        }

        progress(0.93f, "Teknik ekipler…")
        for (club in gs.clubs.values) {
            val hc = RealPlayers2026.headCoaches[club.name]
            if (hc != null) { club.headCoach = hc.first; club.headCoachNat = hc.second }
            else { val (f, l) = NameBank.randomName(NameBank.groupFor(club.country), rnd); club.headCoach = "$f $l"; club.headCoachNat = club.country }
            club.coachQuality = (club.reputation * 0.8 + rnd.nextInt(5, 20)).roundToInt().coerceIn(20, 95)
            for (role in StaffRole.values()) club.staff.add(makeStaff(gs, role, club.country, (club.reputation * 0.75 + rnd.nextInt(-8, 12)).roundToInt(), rnd))
        }
        repeat(60) {
            val role = StaffRole.values()[rnd.nextInt(StaffRole.values().size)]
            val nat = NameBank.nations[rnd.nextInt(NameBank.nations.size)].code
            gs.staffPool.add(makeStaff(gs, role, nat, rnd.nextInt(35, 92), rnd))
        }

        progress(0.94f, "Serbest oyuncular…")
        repeat(320) {
            val pos = Position.values()[rnd.nextInt(Position.values().size)]
            val nat = NameBank.nations[rnd.nextInt(NameBank.nations.size)].code
            val p = generatedPlayer(pid++, nat, pos, rnd.nextInt(45, 76), rnd.nextInt(19, 35), season, rnd)
            p.contract = null
            gs.players[p.id] = p
            gs.freeAgents.add(p.id)
        }
        gs.players.values.forEach { it.agentId = rnd.nextInt(1, 251) }
        gs.nextId = maxOf(gs.nextId, pid + 10)
        progress(0.97f, "Sezon takvimi…")
        return gs
    }

    fun makeClub(id: Int, name: String, country: String, leagueId: Int, rep: Int, rnd: Random): Club {
        val tb = Economy.roundNice((300.0 * Math.exp(0.14 * rep)).toLong())
        val fac = (1 + rep / 11).coerceIn(1, 9)
        val colors = colorOverride[name] ?: (palette[rnd.nextInt(palette.size)] to (if (rnd.nextBoolean()) 0xFFFFFFFF else palette[rnd.nextInt(palette.size)]))
        val short = name.replace("FC ", "").replace(" FC", "").let { if (it.length <= 12) it else it.take(11) + "." }
        return Club(
            id = id, name = name, shortName = short, country = country, leagueId = leagueId, reputation = rep,
            finances = Finances(balance = tb * 2 + 500_000, transferBudget = tb, weeklyWageBudget = 0,
                ticketPrice = Economy.fairTicketPrice(rep)),
            facilities = Facilities(stadiumLevel = fac, stadiumCapacity = (3000 + rep * rep * 8).coerceAtMost(85000),
                trainingLevel = fac, youthLevel = (fac + rnd.nextInt(-1, 2)).coerceIn(1, 10), medicalLevel = fac,
                scoutingLevel = (fac - 1).coerceAtLeast(1), commercialLevel = fac),
            primaryColor = colors.first, secondaryColor = colors.second,
            stadiumName = "$name Stadyumu"
        )
    }

    fun makeStaff(gs: GameState, role: StaffRole, nat: String, quality: Int, rnd: Random): Staff {
        val (f, l) = NameBank.randomName(NameBank.groupFor(nat), rnd)
        val q = quality.coerceIn(20, 95)
        return Staff(gs.nextId++, "$f $l", nat, role, q, rnd.nextInt(32, 66), Economy.roundNice((60.0 * Math.exp(0.07 * q)).toLong()))
    }

    fun randomAge(rnd: Random): Int {
        val a = (rnd.nextInt(17, 36) + rnd.nextInt(17, 36) + rnd.nextInt(18, 34)) / 3
        return a.coerceIn(17, 36)
    }

    fun pickNationality(country: String, tier: Int, rnd: Random): String {
        val domesticShare = when (country) {
            "ENG" -> 0.40; "KSA", "QAT" -> 0.55; "TUR" -> 0.60; "ITA", "GER", "BEL", "POR" -> 0.55
            "USA" -> 0.55; else -> 0.70
        } + (tier - 1) * 0.12
        val code = when (country) { "QAT" -> "QAT"; else -> country }
        if (rnd.nextDouble() < domesticShare && NameBank.nationByCode.containsKey(code)) return code
        val pool = NameBank.foreignPool(country)
        return pool[rnd.nextInt(pool.size)]
    }

    fun generatedPlayer(id: Int, nat: String, pos: Position, ovr: Int, age: Int, season: Int, rnd: Random): Player {
        val (f, l) = NameBank.randomName(NameBank.groupFor(nat), rnd)
        val pot = when {
            age <= 20 -> ovr + rnd.nextInt(3, 17) + if (rnd.nextInt(60) == 0) 10 else 0
            age <= 23 -> ovr + rnd.nextInt(1, 10)
            age <= 26 -> ovr + rnd.nextInt(0, 6)
            else -> ovr
        }.coerceAtMost(95)
        return makePlayer(id, f, l, season - age, nat, pos, ovr, pot, rnd)
    }

    fun makePlayer(id: Int, first: String, last: String, birth: Int, nat: String, pos: Position, ovr: Int, pot: Int, rnd: Random): Player {
        val attrs = makeAttributes(pos, ovr, rnd)
        val secondary = when (pos) {
            Position.CB -> if (rnd.nextInt(4) == 0) listOf(Position.DM) else emptyList()
            Position.LB -> if (rnd.nextBoolean()) listOf(Position.LW) else emptyList()
            Position.RB -> if (rnd.nextBoolean()) listOf(Position.RW) else emptyList()
            Position.DM -> listOf(Position.CM)
            Position.CM -> if (rnd.nextBoolean()) listOf(Position.DM) else listOf(Position.AM)
            Position.AM -> if (rnd.nextBoolean()) listOf(Position.CM) else listOf(Position.ST)
            Position.LW -> listOf(Position.RW)
            Position.RW -> listOf(Position.LW)
            Position.ST -> if (rnd.nextInt(3) == 0) listOf(Position.AM) else emptyList()
            Position.GK -> emptyList()
        }
        return Player(
            id = id, firstName = first, lastName = last, birthYear = birth, nationality = nat,
            position = pos, secondary = secondary, attrs = attrs, potential = max(pot, ovr), contract = null,
            morale = rnd.nextInt(55, 85), form = rnd.nextInt(45, 75), fitness = 100,
            personality = Personality.values()[rnd.nextInt(Personality.values().size)],
            heightCm = if (pos == Position.GK || pos == Position.CB) rnd.nextInt(183, 199) else rnd.nextInt(168, 192),
            preferredFoot = if (pos == Position.LB || pos == Position.LW) (if (rnd.nextInt(4) == 0) "Sağ" else "Sol") else (if (rnd.nextInt(5) == 0) "Sol" else "Sağ")
        )
    }

    /** Pozisyona özgü profil + güç hedefine kalibrasyon. */
    fun makeAttributes(pos: Position, ovr: Int, rnd: Random): Attributes {
        // sırası: pace, shooting, passing, dribbling, defending, physical, vision, composure, workRate, leadership, goalkeeping
        val off = when (pos) {
            Position.GK -> intArrayOf(-30, -45, -20, -35, -40, -10, -15, 0, -15, -5, 0)
            Position.CB -> intArrayOf(-8, -30, -10, -18, 3, 2, -15, -2, -5, 0, -60)
            Position.LB, Position.RB -> intArrayOf(4, -22, -4, -4, -2, -4, -10, -6, 2, -8, -60)
            Position.DM -> intArrayOf(-8, -14, 0, -6, 0, 0, -3, 0, 2, -2, -60)
            Position.CM -> intArrayOf(-4, -8, 3, 0, -12, -6, 2, 0, 0, -4, -60)
            Position.AM -> intArrayOf(0, -2, 2, 3, -30, -12, 3, 0, -6, -6, -60)
            Position.LW, Position.RW -> intArrayOf(5, -3, -3, 4, -32, -10, -4, -3, -4, -10, -60)
            Position.ST -> intArrayOf(0, 3, -10, -2, -38, 0, -8, 1, -6, -6, -60)
        }
        val v = IntArray(11) { (ovr + off[it] + rnd.nextInt(-5, 6)).coerceIn(8, 99) }
        fun build() = Attributes(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9], v[10])
        repeat(3) {
            val diff = ovr - build().overallFor(pos)
            if (diff != 0) for (i in 0 until 11) {
                val isGkAttr = i == 10
                if (pos == Position.GK || !isGkAttr) v[i] = (v[i] + diff).coerceIn(8, 99)
            }
        }
        return build()
    }

    fun attachContract(gs: GameState, p: Player, club: Club, league: League, season: Int, rnd: Random) {
        val wage = Economy.roundNice((Economy.expectedWage(p.overall, league.reputation) * (0.8 + rnd.nextDouble() * 0.4)).toLong())
        val years = rnd.nextInt(1, 6)
        val v = Economy.value(p.copy(contract = Contract(club.id, wage, season + years)), season)
        p.contract = Contract(club.id, wage, season + years,
            releaseClause = if (club.country == "ESP") v * 3 else if (rnd.nextInt(6) == 0) (v * 2.2).toLong() else null)
    }

    fun minOf(a: Int, b: Int) = min(a, b)
}
