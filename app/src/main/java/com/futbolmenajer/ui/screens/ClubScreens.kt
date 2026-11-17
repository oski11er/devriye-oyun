@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.*
import com.futbolmenajer.ui.*

/* ======================= ANA SAYFA ======================= */

@Composable
fun HomeScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val club = gs.userClub
    val league = gs.leagues.getValue(club.leagueId)
    Row(Modifier.fillMaxSize().padding(8.dp)) {
      Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(end = 6.dp)) {
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClubDot(club.primaryColor, 28); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(club.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("${league.name} · ${GameEngine.leaguePosition(gs, club.id)}. sırada", color = Muted)
                }
                RatingBox(Lineups.teamStrength(gs, club), 42)
            }
        }
        val next = GameEngine.nextUserFixture(gs)
        InfoCard {
            Text("Sıradaki maç", color = Muted, fontSize = 12.sp)
            if (next == null) Text("Planlanmış maç yok") else {
                val home = next.homeId == club.id
                val opp = gs.clubs[if (home) next.awayId else next.homeId]
                Text("${opp?.name ?: "?"} (${if (home) "İç saha" else "Deplasman"})", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("${gs.competitions[next.competitionId]?.name} · ${next.roundLabel}", color = Muted, fontSize = 13.sp)
                Text(Scheduler.dateStr(gs.season, next.day), color = Gold, fontSize = 13.sp)
                if (next.day == gs.day) Button(onClick = { vm.selectedFixtureId = next.id; vm.go(Screen.PRE_MATCH) }, modifier = Modifier.padding(top = 6.dp)) { Text("Maça Git") }
            }
        }
        Row {
            InfoCard(Modifier.weight(1f).padding(end = 4.dp)) {
                Text("Yönetim güveni", color = Muted, fontSize = 12.sp)
                Text("${club.boardConfidence}/100", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (club.boardConfidence > 50) Good else if (club.boardConfidence > 25) Warn else Bad)
                Text(gs.objective?.description ?: "", fontSize = 11.sp, color = Muted)
            }
            InfoCard(Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Taraftar", color = Muted, fontSize = 12.sp)
                Text("${club.fanMood}/100", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Transfer: ${Economy.fmt(club.finances.transferBudget)}", fontSize = 11.sp, color = Muted)
            }
        }
        FansPanel(gs, club)
      }
      Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 6.dp)) {
        // mini puan tablosu
        InfoCard {
            Text("Puan durumu", color = Muted, fontSize = 12.sp)
            val table = Scheduler.sortedTable(gs, league.id)
            val idx = table.indexOfFirst { it.clubId == club.id }
            val from = (idx - 3).coerceAtLeast(0); val to = (from + 7).coerceAtMost(table.size)
            for (i in from until to) {
                val r = table[i]
                val me = r.clubId == club.id
                Row(Modifier.fillMaxWidth().background(if (me) Color(0xFF214A35) else Color.Transparent).padding(vertical = 3.dp)) {
                    Text("${i + 1}.", Modifier.width(30.dp), color = Muted)
                    Text(gs.clubName(r.clubId), Modifier.weight(1f), fontWeight = if (me) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                    Text("${r.p}", Modifier.width(28.dp), color = Muted)
                    Text("${r.gd}", Modifier.width(36.dp), color = Muted)
                    Text("${r.pts}", Modifier.width(30.dp), fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { vm.go(Screen.TABLES) }) { Text("Tüm tablo →") }
        }
        val unread = gs.inbox.filter { !it.read }.take(4)
        if (unread.isNotEmpty()) InfoCard {
            Text("Okunmamış mesajlar", color = Muted, fontSize = 12.sp)
            unread.forEach { Text("• ${it.title}", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            TextButton(onClick = { vm.go(Screen.INBOX) }) { Text("Gelen kutusu →") }
        }
        val results = GameEngine.userFixtures(gs).filter { it.played }.takeLast(5).reversed()
        if (results.isNotEmpty()) InfoCard {
            Text("Son sonuçlar", color = Muted, fontSize = 12.sp)
            results.forEach { f -> FixtureLine(gs, f) { vm.openReport(f.id) } }
        }
      }
    }
}

@Composable
fun FixtureLine(gs: GameState, f: Fixture, onClick: () -> Unit) {
    val user = gs.userClubId
    val res = if (!f.played) "" else {
        val ug = if (f.homeId == user) f.homeGoals else f.awayGoals
        val og = if (f.homeId == user) f.awayGoals else f.homeGoals
        when { f.homeId != user && f.awayId != user -> ""; ug > og -> "G"; ug < og -> "M"; else -> "B" }
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (res.isNotEmpty()) Box(Modifier.size(22.dp).background(when (res) { "G" -> Good; "M" -> Bad; else -> Warn }), contentAlignment = Alignment.Center) { Text(res, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        else Spacer(Modifier.width(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(gs.clubName(f.homeId), Modifier.weight(1f), maxLines = 1, fontSize = 13.sp, fontWeight = if (f.homeId == user) FontWeight.Bold else FontWeight.Normal)
        Text(if (f.played) "${f.homeGoals} - ${f.awayGoals}" + (if (f.homePens != null) " (p)" else "") else Scheduler.date(gs.season, f.day).let { "${it.dayOfMonth}.${it.monthValue}" },
            Modifier.padding(horizontal = 6.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(gs.clubName(f.awayId), Modifier.weight(1f), maxLines = 1, fontSize = 13.sp, fontWeight = if (f.awayId == user) FontWeight.Bold else FontWeight.Normal)
    }
}

/* ======================= GELEN KUTUSU ======================= */

@Composable
fun InboxScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var open by remember { mutableStateOf<Int?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        item {
            Row { TextButton(onClick = { gs.inbox.forEach { it.read = true }; vm.bump() }) { Text("Tümünü okundu say") } }
        }
        if (gs.inbox.isEmpty()) item { EmptyNote("Mesaj yok.") }
        items(gs.inbox, key = { it.id }) { m ->
            val icon = when (m.type) { MsgType.TRANSFER -> "💼"; MsgType.BOARD -> "🏛"; MsgType.INJURY -> "🚑"; MsgType.MEDIA -> "📰"; MsgType.YOUTH -> "🎓"; MsgType.MATCH -> "⚽"; MsgType.FINANCE -> "💶"; else -> "ℹ️" }
            InfoCard(Modifier.clickable { open = if (open == m.id) null else m.id; m.read = true; vm.bump() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 18.sp); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.title, fontWeight = if (m.read) FontWeight.Normal else FontWeight.Bold, fontSize = 14.sp)
                        Text(Scheduler.dateStr(gs.season, m.day), color = Muted, fontSize = 11.sp)
                    }
                    if (!m.read) Box(Modifier.size(8.dp).background(Gold))
                }
                if (open == m.id) {
                    Spacer(Modifier.height(8.dp))
                    Text(m.body, fontSize = 13.sp)
                    if (m.offerId != null && gs.offers.any { it.id == m.offerId && it.status in TransferMarket.activeStatuses }) {
                        Button(onClick = { vm.go(Screen.TRANSFERS) }, modifier = Modifier.padding(top = 8.dp)) { Text("Teklife git") }
                    }
                }
            }
        }
    }
}

/* ======================= KULÜP & HEDEFLER ======================= */

@Composable
fun ClubScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        InfoCard {
            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            KV("Menajer", gs.managerName)
            KV("Menajer itibarı", "${gs.managerReputation}/100")
            KV("Kulüp itibarı", "${c.reputation}/100")
            KV("Stadyum", "${c.stadiumName} (${c.facilities.stadiumCapacity})")
            KV("Ülke", GameEngine.nationName(c.country))
        }
        FansPanel(gs, c)
        SectionTitle("Sezon hedefi")
        InfoCard {
            Text(gs.objective?.description ?: "-", fontWeight = FontWeight.Bold)
            KV("Şu anki sıra", "${GameEngine.leaguePosition(gs, c.id)}.")
            KV("Yönetim güveni", "${c.boardConfidence}/100", if (c.boardConfidence > 50) Good else Bad)
            KV("Taraftar memnuniyeti", "${c.fanMood}/100")
        }
        SectionTitle("Kupalar")
        InfoCard { if (gs.trophies.isEmpty()) Text("Henüz kupa yok.", color = Muted) else gs.trophies.forEach { Text("🏆 $it") } }
        SectionTitle("Kariyer geçmişi")
        InfoCard {
            if (gs.seasonRecords.isEmpty()) Text("İlk sezonunuz.", color = Muted)
            gs.seasonRecords.reversed().forEach { Text("${Scheduler.seasonLabel(it.season)}: ${it.clubName} – ${it.leagueName} ${it.position}. ${it.note}", fontSize = 13.sp) }
        }
        SectionTitle("Dünya futbolu geçmişi")
        InfoCard {
            if (gs.history.isEmpty()) Text("—", color = Muted)
            gs.history.reversed().take(20).forEach { Text(it, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp)) }
        }
    }
}

/* ======================= MİLLİ TAKIMLAR ======================= */

@Composable
fun NationalScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var sel by remember { mutableStateOf<String?>(null) }
    val list = gs.nationalTeams.values.sortedByDescending { it.fifaPoints }
    if (sel == null) {
        LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
            item { Text("Dünya sıralaması (FIFA puanı benzeri)", color = Muted, modifier = Modifier.padding(8.dp)) }
            items(list.withIndex().toList()) { (i, nt) ->
                Row(Modifier.fillMaxWidth().clickable { sel = nt.code }.padding(10.dp)) {
                    Text("${i + 1}.", Modifier.width(36.dp), color = Muted)
                    Text(nt.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${nt.fifaPoints.toInt()}", color = Gold)
                }
                HorizontalDivider(color = Color(0xFF1F3328))
            }
        }
    } else {
        val nt = gs.nationalTeams.getValue(sel!!)
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { sel = null }) { Text("← Sıralama") }
            Text("${nt.name} – Kadro (${nt.squad.size})", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            val recent = gs.fixtures.filter { it.isNational && it.played && (it.homeId == nt.id || it.awayId == nt.id) }
            if (recent.isNotEmpty()) Text("Son maçlar: " + recent.takeLast(4).joinToString(" | ") { "${gs.clubName(it.homeId)} ${it.homeGoals}-${it.awayGoals} ${gs.clubName(it.awayId)}" },
                fontSize = 12.sp, color = Muted, modifier = Modifier.padding(12.dp))
            LazyColumn {
                items(nt.squad.mapNotNull { gs.players[it] }.sortedBy { it.position.ordinal }) { p ->
                    PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, showClub = true)
                }
            }
        }
    }
}

/* ======================= FİNANS ======================= */

@Composable
fun FinanceScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    val f = c.finances
    val bill = Economy.weeklyWageBill(gs, c)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        InfoCard {
            KV("Kulüp kasası", Economy.fmt(f.balance), if (f.balance >= 0) Good else Bad)
            KV("Transfer bütçesi", Economy.fmt(f.transferBudget))
            KV("Haftalık maaş gideri", Economy.fmt(bill))
            KV("Haftalık maaş bütçesi", Economy.fmt(f.weeklyWageBudget), if (bill <= f.weeklyWageBudget) Good else Bad)
            KV("Sezon geliri", Economy.fmt(f.seasonIncome))
            KV("Sezon gideri", Economy.fmt(f.seasonExpense))
            KV("Haftalık sponsorluk", Economy.fmt(Economy.weeklyCommercial(c)))
        }
        SectionTitle("Bütçe ayarı")
        InfoCard {
            Text("Transfer bütçesi ↔ maaş bütçesi (1 €/hafta = 52 € transfer)", color = Muted, fontSize = 12.sp)
            Row {
                OutlinedButton(onClick = { vm.say(GameEngine.shiftBudget(gs, 5_000_000)); vm.bump() }, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("€5M → maaş", fontSize = 12.sp) }
                OutlinedButton(onClick = { vm.say(GameEngine.shiftBudget(gs, -5_000_000)); vm.bump() }, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("maaş → €5M", fontSize = 12.sp) }
            }
        }
        SectionTitle("Bilet fiyatı")
        InfoCard {
            var price by remember(v) { mutableFloatStateOf(f.ticketPrice.toFloat()) }
            Text("€${price.toInt()}  (adil fiyat ≈ €${Economy.fairTicketPrice(c.reputation)})", fontWeight = FontWeight.Bold)
            Slider(value = price, onValueChange = { price = it }, valueRange = 5f..200f, onValueChangeFinished = { f.ticketPrice = price.toInt(); vm.bump() })
            Text("Tahmini seyirci: ${Economy.attendance(c, 1.0)} / ${c.facilities.stadiumCapacity}", color = Muted, fontSize = 12.sp)
        }
        SectionTitle("Hesap hareketleri")
        InfoCard {
            if (f.ledger.isEmpty()) Text("Henüz hareket yok.", color = Muted)
            f.ledger.take(60).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(Scheduler.date(gs.season, e.day).let { "${it.dayOfMonth}.${it.monthValue}" }, Modifier.width(44.dp), color = Muted, fontSize = 12.sp)
                    Text(e.desc, Modifier.weight(1f), fontSize = 12.sp, maxLines = 1)
                    Text(Economy.fmt(e.amount), color = if (e.amount >= 0) Good else Bad, fontSize = 12.sp)
                }
            }
        }
    }
}

/* ======================= TESİSLER ======================= */

@Composable
fun FacilitiesScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    val fac = c.facilities
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        fac.upgradeInProgress?.let { InfoCard { Text("🚧 Devam eden: ${it.type.tr} – ${it.daysLeft} gün kaldı", fontWeight = FontWeight.Bold, color = Gold) } }
        for (t in FacilityType.values()) {
            val lvl = Economy.facilityLevel(fac, t)
            val desc = when (t) {
                FacilityType.STADIUM -> "Kapasite: ${fac.stadiumCapacity}. Daha fazla bilet geliri."
                FacilityType.TRAINING -> "Oyuncu gelişim hızını artırır."
                FacilityType.YOUTH -> "Akademiden daha yetenekli ve daha çok genç oyuncu çıkar."
                FacilityType.MEDICAL -> "Sakatlık sürelerini kısaltır."
                FacilityType.SCOUTING -> "Oyuncu potansiyelini daha isabetli görürsünüz."
                FacilityType.COMMERCIAL -> "Sponsorluk ve mağaza gelirlerini artırır."
            }
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(t.tr, fontWeight = FontWeight.Bold)
                        Text(desc, color = Muted, fontSize = 12.sp)
                    }
                    Text("Sv. $lvl/10", color = Gold, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(progress = { lvl / 10f }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                if (lvl < 10) {
                    Text("Yükseltme: ${Economy.fmt(Economy.facilityUpgradeCost(t, lvl))} · ${Economy.facilityUpgradeDays(t, lvl)} gün", fontSize = 12.sp)
                    Button(onClick = { vm.say(GameEngine.requestUpgrade(gs, t)); vm.bump() }, enabled = fac.upgradeInProgress == null, modifier = Modifier.padding(top = 4.dp)) { Text("Yükselt") }
                }
            }
        }
    }
}

/* ======================= GENÇLİK ======================= */

@Composable
fun YouthScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            InfoCard {
                Text("Gençlik Akademisi – Seviye ${c.facilities.youthLevel}/10", fontWeight = FontWeight.Bold)
                Text("Her yıl Mart ayında yeni oyuncular akademiye katılır. Potansiyel tahmini scout ağı seviyesine (${c.facilities.scoutingLevel}) bağlıdır.", color = Muted, fontSize = 12.sp)
            }
        }
        val youth = c.youthIds.mapNotNull { gs.players[it] }.sortedByDescending { it.potential }
        if (youth.isEmpty()) item { EmptyNote("Akademide oyuncu yok.") }
        items(youth, key = { it.id }) { p ->
            Column {
                PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, trailing = {
                    Text("P:${GameEngine.roughPotential(gs, p)}", color = Gold, fontSize = 12.sp, modifier = Modifier.padding(end = 4.dp))
                })
                Row(Modifier.padding(start = 56.dp, bottom = 6.dp)) {
                    Button(onClick = { vm.say(GameEngine.promoteYouth(gs, p.id)); vm.bump() }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("A takıma al", fontSize = 12.sp) }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { c.youthIds.remove(p.id); p.contract = null; p.isYouth = false; gs.freeAgents.add(p.id); vm.bump() }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Serbest bırak", fontSize = 12.sp) }
                }
                HorizontalDivider(color = Color(0xFF1F3328))
            }
        }
    }
}

/* ======================= ANTRENMAN ======================= */

@Composable
fun TrainingScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val plan = gs.training
    val c = gs.userClub
    var pick by remember { mutableStateOf<Int?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            InfoCard {
                Text("Takım antrenman odağı", fontWeight = FontWeight.Bold)
                ChipRow(TrainingFocus.values().toList(), plan.focus, { it.tr }) { plan.focus = it; vm.bump() }
                Text("Yoğunluk", fontWeight = FontWeight.Bold)
                ChipRow(TrainingIntensity.values().toList(), plan.intensity, { it.tr }) { plan.intensity = it; vm.bump() }
                Text("Yoğun antrenman daha hızlı gelişim sağlar ama yorgunluk ve sakatlık riskini artırır. Antrenman tesisi seviyesi: ${c.facilities.trainingLevel}/10",
                    color = Muted, fontSize = 12.sp)
            }
            SectionTitle("Bireysel odak (oyuncuya dokunun)")
        }
        items(c.playerIds.mapNotNull { gs.players[it] }.sortedBy { it.position.ordinal }, key = { it.id }) { p ->
            PlayerRow(gs, p, onClick = { pick = p.id }, trailing = {
                Text(plan.individual[p.id]?.tr ?: "Takım", color = if (plan.individual.containsKey(p.id)) Gold else Muted, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
            })
        }
    }
    pick?.let { id ->
        AlertDialog(onDismissRequest = { pick = null }, title = { Text(gs.players[id]?.fullName ?: "") }, text = {
            Column {
                TextButton(onClick = { plan.individual.remove(id); pick = null; vm.bump() }) { Text("Takım odağını uygula") }
                TrainingFocus.values().forEach { f -> TextButton(onClick = { plan.individual[id] = f; pick = null; vm.bump() }) { Text(f.tr) } }
            }
        }, confirmButton = {})
    }
}
