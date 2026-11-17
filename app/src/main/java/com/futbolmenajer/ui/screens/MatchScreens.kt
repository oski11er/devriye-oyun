@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.*
import com.futbolmenajer.ui.*
import kotlinx.coroutines.delay

/* ======================= MAÇ ÖNÜ ======================= */

@Composable
fun PreMatchScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val fx = gs.fixtures.firstOrNull { it.id == vm.selectedFixtureId } ?: return
    val user = gs.userClub
    val oppId = if (fx.homeId == user.id) fx.awayId else fx.homeId
    val opp = gs.clubs[oppId]
    val notes = remember(v) { Lineups.ensureUserLineup(gs, user) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        InfoCard {
            Text("${gs.competitions[fx.competitionId]?.name ?: ""} · ${fx.roundLabel}", color = Gold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(gs.clubName(fx.homeId), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                Text("vs", color = Muted)
                Text(gs.clubName(fx.awayId), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
            }
            GameEngine.firstLegScore(gs, fx)?.let { (h, a) -> Text("İlk maç: ${gs.clubName(fx.awayId)} $a - $h ${gs.clubName(fx.homeId)} (deplasman golü kuralı yok)", fontSize = 12.sp, color = Muted) }
            if (fx.neutral) Text("Tarafsız saha", color = Muted, fontSize = 12.sp)
        }
        if (opp != null) InfoCard {
            Text("Rakip analizi: ${opp.name}", fontWeight = FontWeight.Bold)
            KV("Takım gücü", "${Lineups.teamStrength(gs, opp)} (bizim: ${Lineups.teamStrength(gs, user)})")
            KV("Lig sırası", "${GameEngine.leaguePosition(gs, opp.id)}.")
            KV("Diziliş", opp.tactic.formation.tr)
            val form = gs.fixtures.filter { it.played && !it.isNational && (it.homeId == opp.id || it.awayId == opp.id) }.takeLast(5).joinToString(" ") { f ->
                val g = if (f.homeId == opp.id) f.homeGoals - f.awayGoals else f.awayGoals - f.homeGoals
                if (g > 0) "G" else if (g < 0) "M" else "B"
            }
            KV("Son form", form.ifBlank { "-" })
            val best = Lineups.squadOf(gs, opp).sortedByDescending { it.overall }.take(3)
            Text("Yıldızlar: " + best.joinToString { "${it.shortName} (${it.overall})" }, fontSize = 12.sp, color = Muted)
        }
        notes.forEach { Text("⚠ $it", color = Warn, fontSize = 12.sp) }
        InfoCard {
            Text("İlk 11 – ${user.tactic.formation.tr} · ${user.tactic.mentality.tr}", fontWeight = FontWeight.Bold)
            user.tactic.lineup.forEachIndexed { i, id ->
                val p = gs.players[id] ?: return@forEachIndexed
                Text("${user.tactic.formation.slots.getOrElse(i) { Position.CM }.tr}  ${p.shortName}  (${p.ratingAt(user.tactic.formation.slots.getOrElse(i) { Position.CM })}) · %${p.fitness}", fontSize = 13.sp)
            }
            TextButton(onClick = { vm.go(Screen.TACTICS) }) { Text("Kadroyu / taktiği düzenle") }
        }
        Button(onClick = { vm.startLive(fx.id) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("▶ MAÇI CANLI İZLE", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.quickResult(fx.id) }, modifier = Modifier.fillMaxWidth()) { Text("Hızlı sonuç") }
    }
}

/* ======================= CANLI MAÇ ======================= */

@Composable
fun LiveMatchScreen(vm: GameViewModel) {
    val sim = vm.sim ?: return
    val gs = vm.gs ?: return
    var tick by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var speed by remember { mutableIntStateOf(1) }
    var tab by remember { mutableIntStateOf(0) }
    var subOut by remember { mutableStateOf<Int?>(null) }
    val userSide = if (sim.home.isUser) sim.home else sim.away
    LaunchedEffect(running, speed) {
        while (running && !sim.finished) {
            delay(when (speed) { 1 -> 650L; 2 -> 250L; 3 -> 70L; else -> 10L })
            val evs = sim.step()
            tick++
            // önemli olaylarda yavaş modda kısa duraklama
            if (speed == 1 && evs.any { it.type == EventType.GOAL || it.type == EventType.PENALTY_GOAL || it.type == EventType.RED }) delay(1200)
            if (sim.phase == MatchPhase.HALF_TIME && speed < 4) { running = false }
        }
    }
    val t = tick // yeniden çizim
    Column(Modifier.fillMaxSize()) {
        // skor tabelası
        Column(Modifier.fillMaxWidth().background(PitchDark).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sim.home.name, Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Text("  ${sim.homeGoals} - ${sim.awayGoals}  ", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Gold)
                Text(sim.away.name, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            }
            val phaseTxt = when (sim.phase) {
                MatchPhase.HALF_TIME -> "Devre arası"; MatchPhase.FINISHED -> "Maç sonu"; MatchPhase.ET_BREAK -> "Uzatma öncesi"
                MatchPhase.PENALTIES -> "Penaltılar"; else -> sim.minuteLabel()
            }
            Text(phaseTxt + (sim.homePens?.let { " · Pen: $it-${sim.awayPens}" } ?: ""), color = Color.White)
            Text("Topa sahip olma %${sim.possessionHome()} - %${100 - sim.possessionHome()} · Şut ${sim.home.stats.shots}-${sim.away.stats.shots}", color = Muted, fontSize = 11.sp)
        }
        // kontroller
        Row(Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!sim.finished) {
                Button(onClick = { running = !running }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(if (running) "⏸ Durdur" else "▶ Devam") }
                Spacer(Modifier.width(6.dp))
                ChipRow(listOf(1, 2, 3, 4), speed, { listOf("1x", "2x", "4x", "⚡")[it - 1] }) { speed = it }
            } else {
                Button(onClick = { vm.finishLive() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) { Text("MAÇI TAMAMLA →", fontWeight = FontWeight.Bold) }
            }
        }
        TabRow(selectedTabIndex = tab, containerColor = Bg) {
            listOf("Anlatım", "İstatistik", "Kadro & Değişiklik", "Taktik").forEachIndexed { i, s -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(s, fontSize = 11.sp) }) }
        }
        when (tab) {
            0 -> {
                val list = sim.events.asReversed()
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    items(list.size) { i ->
                        val e = list[i]
                        val icon = when (e.type) {
                            EventType.GOAL, EventType.PENALTY_GOAL -> "⚽"; EventType.YELLOW -> "🟨"; EventType.RED, EventType.SECOND_YELLOW -> "🟥"
                            EventType.INJURY -> "🚑"; EventType.SUB -> "🔄"; EventType.VAR -> "📺"; EventType.SAVE -> "🧤"; EventType.PENALTY_MISS -> "❌"
                            EventType.GK_8_SECONDS -> "⏱"; EventType.CHANCE -> "💨"; EventType.SHOOTOUT -> "🎯"; else -> "•"
                        }
                        val isGoal = e.type == EventType.GOAL || e.type == EventType.PENALTY_GOAL
                        Row(Modifier.fillMaxWidth().background(if (isGoal) Color(0xFF214A35) else Color.Transparent).padding(vertical = 5.dp)) {
                            Text(if (e.added > 0) "${e.minute}+${e.added}'" else "${e.minute}'", Modifier.width(46.dp), color = Muted, fontSize = 12.sp)
                            Text(icon, Modifier.width(26.dp))
                            Text(e.text, fontSize = 13.sp, fontWeight = if (isGoal) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            1 -> StatsTable(sim.home.name, sim.away.name, sim.home.stats, sim.away.stats, sim.possessionHome())
            2 -> LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
                item { Text("Değişiklik: ${userSide.subsUsed}/5 · Pencere: ${userSide.subWindowsUsed}/3 (devre arası sayılmaz)", color = Muted, fontSize = 12.sp) }
                item { SectionTitle("Sahada (çıkarmak için dokun)") }
                items(userSide.lineup.indices.toList()) { i ->
                    val id = userSide.lineup[i]
                    val p = gs.players[id]
                    if (id >= 0 && p != null) Row(Modifier.fillMaxWidth().clickable { subOut = id }.background(if (subOut == id) Color(0xFF214A35) else Color.Transparent).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosBadge(userSide.slots.getOrElse(i) { Position.CM })
                        Text("  ${p.shortName}", Modifier.weight(1f))
                        if (id in userSide.yellow) Text("🟨 ")
                        if (id in userSide.injured) Text("🚑 ")
                        Text("%${(sim.fitness[id] ?: 100.0).toInt()}", color = Muted, fontSize = 12.sp)
                        Text("  ${String.format("%.1f", sim.ratings[id] ?: 6.0)}", fontWeight = FontWeight.Bold)
                    }
                }
                item { SectionTitle(if (subOut != null) "Yedekler (oyuna almak için dokun)" else "Yedekler") }
                items(userSide.bench.toList()) { id ->
                    val p = gs.players[id] ?: return@items
                    Row(Modifier.fillMaxWidth().clickable(enabled = subOut != null) {
                        val err = sim.makeSub(userSide, subOut!!, id)
                        if (err != null) vm.say(err)
                        subOut = null; tick++
                    }.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosBadge(p.position); Text("  ${p.shortName}", Modifier.weight(1f)); RatingBox(p.overall, 28)
                    }
                }
            }
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
                val tac = userSide.tactic
                Text("Oyun anlayışı", fontWeight = FontWeight.Bold)
                ChipRow(Mentality.values().toList(), tac.mentality, { it.tr }) { sim.changeTactic(userSide, tac.copy(mentality = it)); tick++ }
                Text("Tempo", fontWeight = FontWeight.Bold)
                ChipRow(Tempo.values().toList(), tac.tempo, { it.tr }) { sim.changeTactic(userSide, tac.copy(tempo = it)); tick++ }
                Text("Pres", fontWeight = FontWeight.Bold)
                ChipRow(PressIntensity.values().toList(), tac.press, { it.tr }) { sim.changeTactic(userSide, tac.copy(press = it)); tick++ }
                Text("Pas stili", fontWeight = FontWeight.Bold)
                ChipRow(PassingStyle.values().toList(), tac.passing, { it.tr }) { sim.changeTactic(userSide, tac.copy(passing = it)); tick++ }
                Text("Savunma hattı", fontWeight = FontWeight.Bold)
                ChipRow(DefensiveLine.values().toList(), tac.defLine, { it.tr }) { sim.changeTactic(userSide, tac.copy(defLine = it)); tick++ }
                Text("Diziliş", fontWeight = FontWeight.Bold)
                ChipRow(Formation.values().toList(), tac.formation, { it.tr }) { sim.changeTactic(userSide, tac.copy(formation = it)); tick++ }
            }
        }
    }
}

@Composable
fun StatsTable(hn: String, an: String, h: TeamMatchStats, a: TeamMatchStats, poss: Int) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row { Text(hn, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text(an, Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold) }
        @Composable fun line(label: String, x: String, y: String) {
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(x, Modifier.width(60.dp), fontWeight = FontWeight.Bold)
                Text(label, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted)
                Text(y, Modifier.width(60.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
            }
        }
        line("Topa sahip olma", "%$poss", "%${100 - poss}")
        line("Şut", "${h.shots}", "${a.shots}")
        line("İsabetli şut", "${h.onTarget}", "${a.onTarget}")
        line("Gol beklentisi (xG)", String.format("%.2f", h.xg), String.format("%.2f", a.xg))
        line("Pas", "${h.passes}", "${a.passes}")
        line("Korner", "${h.corners}", "${a.corners}")
        line("Faul", "${h.fouls}", "${a.fouls}")
        line("Ofsayt", "${h.offsides}", "${a.offsides}")
        line("Kurtarış", "${h.saves}", "${a.saves}")
        line("Sarı kart", "${h.yellows}", "${a.yellows}")
        line("Kırmızı kart", "${h.reds}", "${a.reds}")
    }
}

/* ======================= MAÇ RAPORU ======================= */

@Composable
fun MatchReportScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val fx = gs.fixtures.firstOrNull { it.id == vm.selectedFixtureId }
    val r = gs.matchReports[vm.selectedFixtureId]
    if (fx == null) { EmptyNote("Maç bulunamadı."); return }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            InfoCard {
                Text("${gs.competitions[fx.competitionId]?.name ?: ""} · ${fx.roundLabel}", color = Gold, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(gs.clubName(fx.homeId), Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    Text("  ${fx.homeGoals} - ${fx.awayGoals}  ", fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text(gs.clubName(fx.awayId), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                if (fx.homePens != null) Text("Penaltılar: ${fx.homePens}-${fx.awayPens}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (fx.extraTime) Text("Uzatmalar oynandı", color = Muted, fontSize = 12.sp)
                Text("Seyirci: ${fx.attendance}", color = Muted, fontSize = 12.sp)
            }
        }
        if (r == null) { item { EmptyNote("Bu maç için ayrıntılı rapor yok.") }; return@LazyColumn }
        item {
            r.motmId?.let { id -> InfoCard { Text("⭐ Maçın oyuncusu: ${gs.players[id]?.fullName ?: "?"} (${String.format("%.1f", r.ratings[id] ?: 0.0)})", fontWeight = FontWeight.Bold) } }
            val tot = r.home.possessionTicks + r.away.possessionTicks
            StatsTable(gs.clubName(r.homeId), gs.clubName(r.awayId), r.home, r.away, if (tot == 0) 50 else r.home.possessionTicks * 100 / tot)
            SectionTitle("Önemli anlar")
        }
        items(r.events.filter { it.type in setOf(EventType.GOAL, EventType.PENALTY_GOAL, EventType.RED, EventType.SECOND_YELLOW, EventType.YELLOW, EventType.INJURY, EventType.PENALTY_MISS, EventType.VAR, EventType.SUB) }) { e ->
            Text("${e.minute}'${if (e.added > 0) "+${e.added}" else ""}  ${e.text}", fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
        }
        item { SectionTitle("Oyuncu reytingleri") }
        items((r.homeLineup + r.awayLineup + r.ratings.keys).distinct().filter { r.ratings.containsKey(it) }.sortedByDescending { r.ratings[it] }) { id ->
            val p = gs.players[id]
            Row(Modifier.fillMaxWidth().clickable { if (p != null) vm.openPlayer(id) }.padding(vertical = 3.dp)) {
                Text(p?.fullName ?: "?", Modifier.weight(1f), fontSize = 13.sp)
                Text(p?.contract?.clubId?.let { gs.clubs[it]?.shortName } ?: "", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(end = 8.dp))
                Text(String.format("%.1f", r.ratings[id] ?: 0.0), fontWeight = FontWeight.Bold, color = ratingColor(((r.ratings[id] ?: 6.0) * 10).toInt()))
            }
        }
    }
}

/* ======================= FİKSTÜR ======================= */

@Composable
fun FixturesScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val list = GameEngine.userFixtures(gs)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (list.indexOfFirst { !it.played } - 2).coerceAtLeast(0))
    LazyColumn(Modifier.fillMaxSize().padding(8.dp), state = state) {
        items(list, key = { it.id }) { f ->
            Column(Modifier.padding(vertical = 2.dp)) {
                Text("${Scheduler.dateStr(gs.season, f.day)} · ${gs.competitions[f.competitionId]?.name ?: ""} · ${f.roundLabel}", color = Muted, fontSize = 11.sp)
                FixtureLine(gs, f) { if (f.played) vm.openReport(f.id) }
                HorizontalDivider(color = Color(0xFF1F3328))
            }
        }
    }
}

/* ======================= PUAN DURUMU ======================= */

@Composable
fun TablesScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var leagueId by remember { mutableIntStateOf(gs.userClub.leagueId) }
    var pickCountry by remember { mutableStateOf(false) }
    val lg = gs.leagues[leagueId] ?: return
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(lg.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { pickCountry = true }) { Text("Lig seç") }
        }
        LeagueTable(vm, gs, Scheduler.sortedTable(gs, lg.id), lg.promotions, lg.relegations, if (lg.tier == 1) 4 else 0)
    }
    if (pickCountry) AlertDialog(onDismissRequest = { pickCountry = false }, title = { Text("Lig seç") }, text = {
        LazyColumn(Modifier.heightIn(max = 480.dp)) {
            items(gs.leagues.values.sortedWith(compareByDescending<League> { gs.countryCoefficients[it.country] ?: 0.0 }.thenBy { it.tier })) { l ->
                Text("${GameEngine.nationName(l.country)} – ${l.name}", modifier = Modifier.fillMaxWidth().clickable { leagueId = l.id; pickCountry = false }.padding(10.dp))
            }
        }
    }, confirmButton = {})
}

@Composable
fun LeagueTable(vm: GameViewModel, gs: GameState, rows: List<TableRow>, promo: Int, releg: Int, europe: Int) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 6.dp)) {
        item {
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("#", Modifier.width(26.dp), color = Muted); Text("Takım", Modifier.weight(1f), color = Muted)
                for (h in listOf("O", "G", "B", "M", "AV", "P")) Text(h, Modifier.width(if (h == "AV") 36.dp else 28.dp), color = Muted, textAlign = TextAlign.Center)
            }
        }
        items(rows.withIndex().toList()) { (i, r) ->
            val me = r.clubId == gs.userClubId
            val zone = when { i < promo || i < europe -> Good; i >= rows.size - releg -> Bad; else -> Color.Transparent }
            Row(Modifier.fillMaxWidth().background(if (me) Color(0xFF214A35) else Color.Transparent).clickable { vm.openClub(r.clubId) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(18.dp).background(zone)); Spacer(Modifier.width(3.dp))
                Text("${i + 1}", Modifier.width(20.dp), fontSize = 13.sp)
                Text(gs.clubName(r.clubId), Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, fontWeight = if (me) FontWeight.Bold else FontWeight.Normal)
                for (x in listOf(r.p, r.w, r.d, r.l)) Text("$x", Modifier.width(28.dp), fontSize = 13.sp, textAlign = TextAlign.Center)
                Text("${r.gd}", Modifier.width(36.dp), fontSize = 13.sp, textAlign = TextAlign.Center)
                Text("${r.pts}", Modifier.width(28.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

/* ======================= TURNUVALAR ======================= */

@Composable
fun CompetitionsScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var sel by remember { mutableStateOf<Int?>(null) }
    val user = gs.userClubId
    val comps = gs.competitions.values.filter { it.type != CompetitionType.LEAGUE && it.type != CompetitionType.NATIONS_FRIENDLY }
        .sortedWith(compareByDescending<Competition> { user in it.participants }.thenBy { if (Scheduler.isUclType(it.type)) 0 else 1 }.thenBy { it.name })
    if (sel == null) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            items(comps, key = { it.id }) { c ->
                InfoCard(Modifier.clickable { sel = c.id }) {
                    Row {
                        Text(c.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        if (user in c.participants) Text("Katılıyorsunuz", color = Good, fontSize = 11.sp)
                    }
                    Text(if (c.finished) "Şampiyon: ${c.winnerId?.let { gs.clubName(it) }}" else "Aşama: ${c.stage.ifBlank { "-" }}", color = Muted, fontSize = 12.sp)
                }
            }
        }
    } else {
        val c = gs.competitions[sel!!] ?: return
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { sel = null }) { Text("← Turnuvalar") }
            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 12.dp))
            var tab by remember { mutableIntStateOf(0) }
            val hasTable = gs.tables.containsKey(c.id)
            if (hasTable) TabRow(selectedTabIndex = tab, containerColor = Bg) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Lig aşaması") }); Tab(tab == 1, { tab = 1 }, text = { Text("Maçlar") })
            }
            if (hasTable && tab == 0) LeagueTable(vm, gs, Scheduler.sortedTable(gs, c.id), 8, 12, 0)
            else {
                val fx = gs.fixtures.filter { it.competitionId == c.id }
                LazyColumn(Modifier.padding(8.dp)) {
                    val groups = fx.groupBy { it.roundLabel.substringBefore(" (") }
                    groups.forEach { (label, list) ->
                        item { SectionTitle(label) }
                        items(list, key = { it.id }) { f -> FixtureLine(gs, f) { if (gs.matchReports.containsKey(f.id)) vm.openReport(f.id) } }
                    }
                }
            }
        }
    }
}
