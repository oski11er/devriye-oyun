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

/* ======================= KADRO ======================= */

@Composable
fun SquadScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    var sort by remember { mutableStateOf("Pozisyon") }
    val squad = c.playerIds.mapNotNull { gs.players[it] }.let { l ->
        when (sort) {
            "Güç" -> l.sortedByDescending { it.overall }
            "Yaş" -> l.sortedBy { it.birthYear * -1 }
            "Değer" -> l.sortedByDescending { Economy.value(it, gs.season) }
            "Form" -> l.sortedByDescending { it.form }
            else -> l.sortedWith(compareBy<Player> { it.position.ordinal }.thenByDescending { it.overall })
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(8.dp)) {
                Text("${squad.size} oyuncu · Ortalama yaş ${if (squad.isEmpty()) 0 else squad.map { it.age(gs.season) }.average().toInt()} · Maaş ${Economy.fmt(Economy.weeklyWageBill(gs, c))}/hafta",
                    color = Muted, fontSize = 12.sp)
                ChipRow(listOf("Pozisyon", "Güç", "Değer", "Form", "Yaş"), sort, { it }) { sort = it }
                Text("Çubuklar: yeşil = kondisyon, mavi = moral", color = Muted, fontSize = 11.sp)
            }
        }
        items(squad, key = { it.id }) { p ->
            PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, trailing = {
                val inXI = p.id in c.tactic.lineup
                if (inXI) Text("İLK11", color = Good, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 4.dp))
            })
            HorizontalDivider(color = Color(0xFF1F3328))
        }
    }
}

/* ======================= TAKTİK ======================= */

@Composable
fun TacticsScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    val t = c.tactic
    var slotPick by remember { mutableStateOf<Int?>(null) }
    var insPick by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) { Lineups.ensureUserLineup(gs, c); vm.bump() }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        item {
            InfoCard {
                Text("Diziliş", fontWeight = FontWeight.Bold)
                ChipRow(Formation.values().toList(), t.formation, { it.tr }) { f ->
                    t.formation = f
                    // oyuncuları yeni dizilişe göre yeniden yerleştir
                    val (l, b) = Lineups.autoPick(Lineups.squadOf(gs, c), f, rotate = t.autoRotate)
                    t.lineup = l; t.bench = b; vm.bump()
                }
                Text("Oyun anlayışı", fontWeight = FontWeight.Bold)
                ChipRow(Mentality.values().toList(), t.mentality, { it.tr }) { t.mentality = it; vm.bump() }
                Text("Tempo", fontWeight = FontWeight.Bold)
                ChipRow(Tempo.values().toList(), t.tempo, { it.tr }) { t.tempo = it; vm.bump() }
                Text("Pres yoğunluğu", fontWeight = FontWeight.Bold)
                ChipRow(PressIntensity.values().toList(), t.press, { it.tr }) { t.press = it; vm.bump() }
                Text("Pas stili", fontWeight = FontWeight.Bold)
                ChipRow(PassingStyle.values().toList(), t.passing, { it.tr }) { t.passing = it; vm.bump() }
                Text("Savunma hattı", fontWeight = FontWeight.Bold)
                ChipRow(DefensiveLine.values().toList(), t.defLine, { it.tr }) { t.defLine = it; vm.bump() }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = t.autoRotate, onCheckedChange = { t.autoRotate = it; vm.bump() })
                    Text("  Yorgun oyuncuları otomatik dinlendir", fontSize = 13.sp)
                }
                Row {
                    Button(onClick = { val (l, b) = Lineups.autoPick(Lineups.squadOf(gs, c), t.formation, rotate = t.autoRotate); t.lineup = l; t.bench = b; vm.bump() }) { Text("En iyi 11'i seç") }
                }
            }
            PitchView(gs, t) { slotPick = it }
            SectionTitle("İlk 11 (değiştirmek için dokun, talimat için uzun bas → ⚙)")
        }
        items(t.lineup.indices.toList()) { i ->
            val id = t.lineup[i]
            val p = gs.players[id]
            val pos = t.formation.slots.getOrElse(i) { Position.CM }
            if (p != null) Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    PlayerRow(gs, p, onClick = { slotPick = i }, posOverride = pos, trailing = {
                        val ins = t.instructions[id]
                        if (t.captainId == id) Text("Ⓒ ", color = Gold, fontWeight = FontWeight.Bold)
                        if (ins != null && ins != PlayerInstruction.NONE) Text(ins.tr, fontSize = 10.sp, color = Gold, modifier = Modifier.padding(end = 4.dp))
                    })
                }
                TextButton(onClick = { insPick = id }) { Text("⚙") }
            }
            HorizontalDivider(color = Color(0xFF1F3328))
        }
        item { SectionTitle("Yedekler (${t.bench.size}/9)") }
        items(t.bench.toList()) { id ->
            gs.players[id]?.let { p -> PlayerRow(gs, p, onClick = { vm.openPlayer(id) }) }
        }
        item {
            SectionTitle("Duran toplar & kaptan")
            InfoCard {
                KV("Kaptan", t.captainId?.let { gs.players[it]?.shortName } ?: "Otomatik")
                KV("Penaltı", t.penaltyTakerId?.let { gs.players[it]?.shortName } ?: "Otomatik")
                KV("Serbest vuruş", t.freeKickTakerId?.let { gs.players[it]?.shortName } ?: "Otomatik")
                Text("Ayarlamak için ilk 11'deki oyuncunun ⚙ menüsünü kullanın.", color = Muted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
    slotPick?.let { i ->
        val pos = t.formation.slots.getOrElse(i) { Position.CM }
        val cands = Lineups.squadOf(gs, c).sortedByDescending { it.ratingAt(pos) }
        AlertDialog(onDismissRequest = { slotPick = null }, title = { Text("${pos.tr} pozisyonu için oyuncu") }, text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                items(cands, key = { it.id }) { p ->
                    PlayerRow(gs, p, posOverride = pos, onClick = {
                        if (!Lineups.selectable(p)) { vm.say("${p.shortName} sakat veya cezalı."); return@PlayerRow }
                        val old = t.lineup.getOrNull(i)
                        val existing = t.lineup.indexOf(p.id)
                        if (existing >= 0 && old != null) t.lineup[existing] = old
                        else if (old != null) { t.bench.remove(p.id); if (old !in t.bench) t.bench.add(0, old) }
                        if (i < t.lineup.size) t.lineup[i] = p.id
                        if (t.bench.size > 9) t.bench = t.bench.take(9).toMutableList()
                        slotPick = null; vm.bump()
                    })
                }
            }
        }, confirmButton = { TextButton(onClick = { slotPick = null }) { Text("Kapat") } })
    }
    insPick?.let { id ->
        AlertDialog(onDismissRequest = { insPick = null }, title = { Text(gs.players[id]?.fullName ?: "") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Bireysel talimat", fontWeight = FontWeight.Bold)
                PlayerInstruction.values().forEach { ins ->
                    TextButton(onClick = { if (ins == PlayerInstruction.NONE) t.instructions.remove(id) else t.instructions[id] = ins; insPick = null; vm.bump() }) {
                        Text((if (t.instructions[id] == ins) "✓ " else "") + ins.tr)
                    }
                }
                HorizontalDivider()
                TextButton(onClick = { t.captainId = id; insPick = null; vm.bump() }) { Text("Kaptan yap") }
                TextButton(onClick = { t.penaltyTakerId = id; insPick = null; vm.bump() }) { Text("Penaltıcı yap") }
                TextButton(onClick = { t.freeKickTakerId = id; insPick = null; vm.bump() }) { Text("Serbest vuruşçu yap") }
                TextButton(onClick = { insPick = null; vm.openPlayer(id) }) { Text("Oyuncu profili") }
            }
        }, confirmButton = {})
    }
}

@Composable
fun PitchView(gs: GameState, t: Tactic, onSlot: (Int) -> Unit) {
    // basit saha görünümü: sıralar → GK, DEF, MID, ATT
    val rows = listOf(Line.ATT, Line.MID, Line.DEF, Line.GK)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Pitch).padding(8.dp)) {
        for (line in rows) {
            val idxs = t.formation.slots.indices.filter { t.formation.slots[it].line == line }
                .let { l -> if (line == Line.MID) l.sortedBy { if (t.formation.slots[it] == Position.AM) 0 else 1 } else l }
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (i in idxs) {
                    val p = t.lineup.getOrNull(i)?.let { gs.players[it] }
                    Column(Modifier.clickable { onSlot(i) }.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(30.dp).background(Color.White.copy(alpha = 0.9f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Text("${p?.ratingAt(t.formation.slots[i]) ?: "-"}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(p?.lastName ?: t.formation.slots[i].tr, color = Color.White, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/* ======================= OYUNCU PROFİLİ ======================= */

@Composable
fun PlayerScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val p = gs.players[vm.selectedPlayerId] ?: run { EmptyNote("Oyuncu bulunamadı (emekli olmuş olabilir)."); return }
    val club = p.contract?.clubId?.let { gs.clubs[it] }
    val mine = club?.id == gs.userClubId
    var showOffer by remember { mutableStateOf(false) }
    var showAgent by remember { mutableStateOf(false) }
    var showRenew by remember { mutableStateOf(false) }
    var confirmRelease by remember { mutableStateOf(false) }
    var termsOffer by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.id, p.nationality, p.age(gs.season), club?.primaryColor ?: 0xFF455A64, club?.secondaryColor ?: 0xFFFFFFFF, 96)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.fullName, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${p.position.tr}${if (p.secondary.isNotEmpty()) " (" + p.secondary.joinToString { it.tr } + ")" else ""} · ${p.age(gs.season)} yaş · ${GameEngine.nationName(p.nationality)}", color = Muted)
                    Text(club?.name ?: "Serbest oyuncu", color = Gold, modifier = Modifier.clickable { club?.let { vm.openClub(it.id) } })
                    Text("${p.heightCm} cm · ${p.preferredFoot} ayak · ${p.personality.tr}", color = Muted, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RatingBox(p.overall, 52)
                    Text("Pot: ${GameEngine.roughPotential(gs, p)}", fontSize = 12.sp, color = Gold)
                }
            }
            if (p.injuryDays > 0) Text("✚ Sakat: ${p.injuryName} (${p.injuryDays} gün)", color = Bad)
            if (p.suspendedMatches > 0) Text("🟥 Cezalı: ${p.suspendedMatches} maç", color = Bad)
        }
        InfoCard {
            KV("Piyasa değeri", Economy.fmt(Economy.value(p, gs.season)))
            p.contract?.let { c ->
                KV("Maaş", "${Economy.fmt(c.weeklyWage)}/hafta")
                KV("Sözleşme bitişi", "${c.expiresSeason} yaz")
                c.releaseClause?.let { KV("Serbest kalma bedeli", Economy.fmt(it)) }
                c.loanFromClubId?.let { KV("Kiralık (sahibi)", gs.clubName(it)) }
            }
            KV("Moral", "${p.morale}/100"); KV("Form", "${p.form}/100"); KV("Kondisyon", "${p.fitness}%")
            KV("Milli maç", "${p.internationalCaps}")
        }
        SectionTitle("Yetenekler")
        InfoCard {
            val a = p.attrs
            if (p.position == Position.GK) AttrBar("Kalecilik", a.goalkeeping)
            AttrBar("Hız", a.pace); AttrBar("Şut", a.shooting); AttrBar("Pas", a.passing); AttrBar("Top sürme", a.dribbling)
            AttrBar("Savunma", a.defending); AttrBar("Fizik", a.physical); AttrBar("Oyun görüşü", a.vision)
            AttrBar("Soğukkanlılık", a.composure); AttrBar("Çalışkanlık", a.workRate); AttrBar("Liderlik", a.leadership)
            if (p.position != Position.GK) AttrBar("Kalecilik", a.goalkeeping)
        }
        SectionTitle("Bu sezon")
        InfoCard {
            KV("Maç", "${p.stats.apps}"); KV("Gol", "${p.stats.goals}"); KV("Asist", "${p.stats.assists}")
            KV("Ort. reyting", if (p.stats.apps > 0) String.format("%.2f", p.stats.avgRating) else "-")
            KV("Sarı / Kırmızı", "${p.stats.yellow} / ${p.stats.red}")
            if (p.position == Position.GK) KV("Gol yemeden", "${p.stats.cleanSheets}")
            KV("Kariyer", "${p.careerApps} maç, ${p.careerGoals} gol")
        }
        SectionTitle("İşlemler")
        if (mine) {
            Row {
                OutlinedButton(onClick = { p.transferListed = !p.transferListed; vm.bump() }, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text(if (p.transferListed) "Listeden çıkar" else "Transfer listesi", fontSize = 12.sp) }
                OutlinedButton(onClick = { p.loanListed = !p.loanListed; vm.bump() }, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text(if (p.loanListed) "Kiralıktan çıkar" else "Kiralık listesi", fontSize = 12.sp) }
            }
            Row {
                Button(onClick = { showRenew = true }, enabled = p.contract?.loanFromClubId == null, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text("Sözleşme yenile", fontSize = 12.sp) }
                OutlinedButton(onClick = { confirmRelease = true }, enabled = p.contract?.loanFromClubId == null, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Serbest bırak", fontSize = 12.sp, color = Bad) }
            }
        } else {
            Row {
                Button(onClick = {
                    if (p.contract == null) {
                        TransferMarket.submitOffer(gs, p.id, OfferType.FREE, 0).onSuccess { termsOffer = it.id }.onFailure { vm.say(it.message ?: "") }
                    } else showOffer = true
                }, modifier = Modifier.weight(1f).padding(end = 4.dp)) { Text(if (p.contract == null) "Sözleşme teklif et" else "Teklif yap", fontSize = 12.sp) }
                OutlinedButton(onClick = { showAgent = true }, modifier = Modifier.weight(1f).padding(start = 4.dp)) { Text("Ajanla görüş", fontSize = 12.sp) }
            }
            OutlinedButton(onClick = {
                if (p.id in gs.shortlist) gs.shortlist.remove(p.id) else gs.shortlist.add(p.id); vm.bump()
            }, modifier = Modifier.fillMaxWidth()) { Text(if (p.id in gs.shortlist) "Kısa listeden çıkar" else "Kısa listeye ekle ★") }
        }
        Spacer(Modifier.height(40.dp))
    }
    if (showOffer) OfferDialog(vm, gs, p) { offerId -> showOffer = false; if (offerId != null) vm.say("Teklif gönderildi. Yanıt 1-2 gün içinde gelecek (Transfer > Tekliflerim).") }
    if (showAgent) AlertDialog(onDismissRequest = { showAgent = false }, title = { Text("Ajan görüşmesi") },
        text = { Text(TransferMarket.agentTalk(gs, p), fontSize = 13.sp) }, confirmButton = { TextButton(onClick = { showAgent = false }) { Text("Tamam") } })
    if (showRenew) RenewDialog(vm, gs, p) { showRenew = false }
    if (confirmRelease) AlertDialog(onDismissRequest = { confirmRelease = false }, title = { Text("Serbest bırak") }, text = {
        val c = p.contract
        val pay = if (c == null) 0L else c.weeklyWage * 52L * (c.expiresSeason - gs.season).coerceAtLeast(0) / 2
        Text("${p.fullName} ile sözleşme feshedilsin mi? Tazminat: ${Economy.fmt(pay)}")
    }, confirmButton = { TextButton(onClick = { TransferMarket.release(gs, gs.userClub, p); confirmRelease = false; vm.say("${p.fullName} serbest bırakıldı."); vm.back(); vm.bump() }) { Text("Feshet", color = Bad) } },
        dismissButton = { TextButton(onClick = { confirmRelease = false }) { Text("Vazgeç") } })
    termsOffer?.let { oid -> TermsDialog(vm, gs, oid) { termsOffer = null } }
}

@Composable
fun RenewDialog(vm: GameViewModel, gs: GameState, p: Player, onClose: () -> Unit) {
    val d = remember { TransferMarket.renewDemand(gs, p) }
    var wage by remember { mutableLongStateOf(d.weeklyWage) }
    var years by remember { mutableIntStateOf(d.years) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Sözleşme yenileme: ${p.shortName}") }, text = {
        Column {
            Text("Oyuncunun beklentisi: ${Economy.fmt(d.weeklyWage)}/hafta, ${d.years} yıl", color = Muted, fontSize = 13.sp)
            MoneySlider("Haftalık maaş", wage, d.weeklyWage / 2, d.weeklyWage * 2) { wage = it }
            Text("Süre: $years yıl")
            ChipRow((1..5).toList(), years, { "$it" }) { years = it }
        }
    }, confirmButton = { TextButton(onClick = { vm.say(TransferMarket.renew(gs, p, wage, years)); onClose(); vm.bump() }) { Text("Teklif et") } },
        dismissButton = { TextButton(onClick = onClose) { Text("Vazgeç") } })
}

/* ======================= KULÜP GÖRÜNÜMÜ ======================= */

@Composable
fun ClubViewScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.clubs[vm.selectedClubId] ?: return
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ClubDot(c.primaryColor, 26); Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("${gs.leagues[c.leagueId]?.name} · ${GameEngine.leaguePosition(gs, c.id)}. sıra", color = Muted)
                    }
                    RatingBox(Lineups.teamStrength(gs, c), 42)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Avatar(c.id * 31 + 7, c.headCoachNat.ifBlank { c.country }, 50, c.primaryColor, c.secondaryColor, 44)
                    Spacer(Modifier.width(8.dp))
                    Column { Text("Teknik direktör", color = Muted, fontSize = 11.sp); Text(c.headCoach, fontWeight = FontWeight.Bold) }
                }
                KV("İtibar", "${c.reputation}")
                KV("Stadyum", "${c.facilities.stadiumCapacity} kişilik")
                KV("Transfer bütçesi", Economy.fmt(c.finances.transferBudget))
            }
        }
        items(c.playerIds.mapNotNull { gs.players[it] }.sortedWith(compareBy<Player> { it.position.ordinal }.thenByDescending { it.overall }), key = { it.id }) { p ->
            PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) })
        }
    }
}
