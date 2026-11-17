@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui.screens

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

@Composable
fun TransferScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ara", "Tekliflerim", "Gelen", "Kısa liste", "Geçmiş")
    Column(Modifier.fillMaxSize()) {
        val open = Scheduler.transferWindowOpen(gs.day)
        Text(if (open) "🟢 Transfer dönemi AÇIK" else "🔴 Transfer dönemi kapalı (serbest oyuncular alınabilir)",
            color = if (open) Good else Warn, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 4.dp, containerColor = Bg) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> SearchTab(vm, gs)
            1 -> MyOffersTab(vm, gs)
            2 -> IncomingTab(vm, gs)
            3 -> LazyColumn { items(gs.shortlist.mapNotNull { gs.players[it] }, key = { it.id }) { p -> PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, showClub = true) } }
            else -> LazyColumn(Modifier.padding(8.dp)) { items(gs.transferHistory.take(200)) { Text(it, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) } }
        }
    }
}

@Composable
private fun SearchTab(vm: GameViewModel, gs: GameState) {
    var pos by remember { mutableStateOf<Position?>(null) }
    var scope by remember { mutableStateOf("Tümü") }
    var maxAge by remember { mutableFloatStateOf(40f) }
    var minOvr by remember { mutableFloatStateOf(60f) }
    var maxValue by remember { mutableFloatStateOf(1f) }
    var query by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(true) }
    val budget = gs.userClub.finances.transferBudget
    val maxV = (maxValue * maxValue * 200_000_000).toLong()
    val results = remember(pos, scope, maxAge, minOvr, maxValue, query, vm.version) {
        val base: Sequence<Player> = when (scope) {
            "Serbest" -> gs.freeAgents.asSequence().mapNotNull { gs.players[it] }
            "Ülkem" -> gs.clubs.values.asSequence().filter { it.country == gs.userClub.country && it.id != gs.userClubId }.flatMap { c -> c.playerIds.asSequence().mapNotNull { gs.players[it] } }
            "Listede" -> gs.players.values.asSequence().filter { (it.transferListed || it.loanListed) && it.contract?.clubId != gs.userClubId }
            else -> gs.players.values.asSequence().filter { it.contract?.clubId != gs.userClubId && !it.isYouth }
        }
        base.filter { p ->
            (pos == null || p.position == pos) && p.age(gs.season) <= maxAge && p.overall >= minOvr &&
                (query.isBlank() || p.fullName.contains(query, ignoreCase = true)) &&
                (maxValue >= 1f || Economy.value(p, gs.season) <= maxV)
        }.sortedByDescending { it.overall }.take(150).toList()
    }
    Column {
        if (showFilters) Column(Modifier.padding(horizontal = 8.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("İsim ara") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ChipRow(listOf<Position?>(null) + Position.values().toList(), pos, { it?.tr ?: "Hepsi" }) { pos = it }
            ChipRow(listOf("Tümü", "Ülkem", "Listede", "Serbest"), scope, { it }) { scope = it }
            Text("Maks. yaş: ${maxAge.toInt()}   Min. güç: ${minOvr.toInt()}   Maks. değer: ${if (maxValue >= 1f) "sınırsız" else Economy.fmt(maxV)}", fontSize = 12.sp, color = Muted)
            Slider(maxAge, { maxAge = it }, valueRange = 16f..40f)
            Slider(minOvr, { minOvr = it }, valueRange = 30f..95f)
            Slider(maxValue, { maxValue = it }, valueRange = 0.02f..1f)
        }
        TextButton(onClick = { showFilters = !showFilters }) { Text(if (showFilters) "Filtreleri gizle ▲" else "Filtreleri göster ▼") }
        Text("${results.size} sonuç · Bütçe ${Economy.fmt(budget)}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp))
        LazyColumn {
            items(results, key = { it.id }) { p -> PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, showClub = true) }
        }
    }
}

@Composable
private fun MyOffersTab(vm: GameViewModel, gs: GameState) {
    val mine = gs.offers.filter { it.fromClubId == gs.userClubId }.sortedByDescending { it.createdDay }
    var terms by remember { mutableStateOf<Int?>(null) }
    var improve by remember { mutableStateOf<TransferOffer?>(null) }
    LazyColumn(Modifier.padding(8.dp)) {
        if (mine.isEmpty()) item { EmptyNote("Henüz teklif yapmadınız. Bir oyuncunun profilinden \"Teklif yap\" deyin.") }
        items(mine, key = { it.id }) { o ->
            val p = gs.players[o.playerId]
            InfoCard {
                Text(p?.fullName ?: "?", fontWeight = FontWeight.Bold, modifier = Modifier.clickable { p?.let { vm.openPlayer(it.id) } })
                Text("${o.type.tr} · ${o.toClubId?.let { gs.clubName(it) } ?: "Serbest"}", color = Muted, fontSize = 12.sp)
                val detail = when (o.type) {
                    OfferType.LOAN -> "Maaş katkısı %${o.loanWageShare}, bedel ${Economy.fmt(o.loanFeePerSeason)}" + (o.loanBuyOption?.let { ", opsiyon ${Economy.fmt(it)}" } ?: "")
                    OfferType.SWAP -> "Takas: ${o.swapPlayerIds.mapNotNull { gs.players[it]?.shortName }.joinToString()} + ${Economy.fmt(o.fee)}"
                    OfferType.FREE -> "Bedelsiz"
                    else -> Economy.fmt(o.fee) + (if (o.installments > 1) " (${o.installments} taksit)" else "") + (if (o.sellOnPercent > 0) ", %${o.sellOnPercent} sonradan satış" else "")
                }
                Text(detail, fontSize = 13.sp)
                Text("Durum: ${o.status.tr}", color = when (o.status) { OfferStatus.COMPLETED, OfferStatus.ACCEPTED_CLUB -> Good; OfferStatus.REJECTED, OfferStatus.PLAYER_REJECTED -> Bad; else -> Gold }, fontSize = 13.sp)
                if (o.message.isNotBlank()) Text(o.message, fontSize = 12.sp, color = Muted)
                Row {
                    when (o.status) {
                        OfferStatus.COUNTERED -> {
                            Button(onClick = { vm.say(TransferMarket.acceptCounter(gs, o.id)); vm.bump() }, modifier = Modifier.padding(end = 6.dp)) { Text("Kabul", fontSize = 12.sp) }
                            OutlinedButton(onClick = { improve = o }, modifier = Modifier.padding(end = 6.dp)) { Text("Yeni teklif", fontSize = 12.sp) }
                        }
                        OfferStatus.ACCEPTED_CLUB -> Button(onClick = { terms = o.id }, modifier = Modifier.padding(end = 6.dp)) { Text("Oyuncuyla görüş", fontSize = 12.sp) }
                        OfferStatus.REJECTED -> if (o.negotiationRound < 3 && o.type != OfferType.FREE) OutlinedButton(onClick = { improve = o }, modifier = Modifier.padding(end = 6.dp)) { Text("Teklifi artır", fontSize = 12.sp) }
                        else -> {}
                    }
                    if (o.status in TransferMarket.activeStatuses) TextButton(onClick = { TransferMarket.withdraw(gs, o.id); vm.bump() }) { Text("Geri çek", color = Bad, fontSize = 12.sp) }
                }
            }
        }
    }
    terms?.let { TermsDialog(vm, gs, it) { terms = null } }
    improve?.let { o ->
        val p = gs.players[o.playerId]
        val value = p?.let { Economy.value(it, gs.season) } ?: 1_000_000
        var fee by remember(o.id) { mutableLongStateOf(o.counterFee ?: if (o.type == OfferType.LOAN) o.loanFeePerSeason else o.fee) }
        AlertDialog(onDismissRequest = { improve = null }, title = { Text("Yeni teklif") }, text = {
            Column {
                Text("Kalan pazarlık turu: ${3 - o.negotiationRound}", color = Muted, fontSize = 12.sp)
                MoneySlider(if (o.type == OfferType.SWAP) "Nakit ek" else "Bedel", fee, 0, value * 3) { fee = it }
            }
        }, confirmButton = { TextButton(onClick = { vm.say(TransferMarket.improveOffer(gs, o.id, fee)); improve = null; vm.bump() }) { Text("Gönder") } },
            dismissButton = { TextButton(onClick = { improve = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun IncomingTab(vm: GameViewModel, gs: GameState) {
    val inc = gs.offers.filter { it.toClubId == gs.userClubId && it.status in TransferMarket.activeStatuses }
    var counter by remember { mutableStateOf<TransferOffer?>(null) }
    LazyColumn(Modifier.padding(8.dp)) {
        if (inc.isEmpty()) item { EmptyNote("Bekleyen teklif yok. Oyuncuları transfer/kiralık listesine koyarak teklif çekebilirsiniz.") }
        items(inc, key = { it.id }) { o ->
            val p = gs.players[o.playerId] ?: return@items
            InfoCard {
                Text("${gs.clubName(o.fromClubId)} → ${p.fullName}", fontWeight = FontWeight.Bold)
                Text(if (o.type == OfferType.LOAN) "Kiralık · maaşın %${o.loanWageShare}'i · bedel ${Economy.fmt(o.loanFeePerSeason)}" else "Teklif: ${Economy.fmt(o.fee)} (değer ${Economy.fmt(Economy.value(p, gs.season))})", fontSize = 13.sp)
                Row {
                    Button(onClick = { vm.say(TransferMarket.respondIncoming(gs, o.id, true)); vm.bump() }, modifier = Modifier.padding(end = 6.dp)) { Text("Kabul", fontSize = 12.sp) }
                    if (o.type != OfferType.LOAN) OutlinedButton(onClick = { counter = o }, modifier = Modifier.padding(end = 6.dp)) { Text("Karşı teklif", fontSize = 12.sp) }
                    TextButton(onClick = { vm.say(TransferMarket.respondIncoming(gs, o.id, false)); vm.bump() }) { Text("Reddet", color = Bad, fontSize = 12.sp) }
                }
            }
        }
    }
    counter?.let { o ->
        var fee by remember(o.id) { mutableLongStateOf(o.fee * 13 / 10) }
        AlertDialog(onDismissRequest = { counter = null }, title = { Text("Karşı teklif") }, text = {
            MoneySlider("İstediğiniz bedel", fee, o.fee, o.fee * 3) { fee = it }
        }, confirmButton = { TextButton(onClick = { vm.say(TransferMarket.respondIncoming(gs, o.id, false, fee)); counter = null; vm.bump() }) { Text("Gönder") } })
    }
}

/* ======================= TEKLİF DİYALOĞU ======================= */

@Composable
fun OfferDialog(vm: GameViewModel, gs: GameState, p: Player, onDone: (Int?) -> Unit) {
    val value = Economy.value(p, gs.season)
    val ask = TransferMarket.askingPrice(gs, p)
    var type by remember { mutableStateOf(OfferType.TRANSFER) }
    var fee by remember { mutableLongStateOf(value) }
    var installments by remember { mutableIntStateOf(1) }
    var sellOn by remember { mutableIntStateOf(0) }
    var wageShare by remember { mutableIntStateOf(100) }
    var loanFee by remember { mutableLongStateOf(0L) }
    var buyOpt by remember { mutableStateOf(false) }
    var buyAmount by remember { mutableLongStateOf(value) }
    val swap = remember { mutableStateListOf<Int>() }
    var error by remember { mutableStateOf<String?>(null) }
    val user = gs.userClub
    AlertDialog(onDismissRequest = { onDone(null) }, title = { Text("Teklif: ${p.shortName}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Değer ${Economy.fmt(value)} · Bütçe ${Economy.fmt(user.finances.transferBudget)}", color = Muted, fontSize = 12.sp)
            p.contract?.releaseClause?.let { Text("Serbest kalma bedeli: ${Economy.fmt(it)}", color = Gold, fontSize = 12.sp) }
            ChipRow(listOf(OfferType.TRANSFER, OfferType.SWAP, OfferType.LOAN), type, { it.tr }) { type = it }
            when (type) {
                OfferType.TRANSFER -> {
                    MoneySlider("Bonservis bedeli", fee, value / 4, maxOf(value * 3, ask * 2)) { fee = it }
                    Text("Taksit sayısı: $installments", fontSize = 13.sp)
                    ChipRow(listOf(1, 2, 3, 4), installments, { "$it" }) { installments = it }
                    Text("Sonradan satıştan pay (satıcıya): %$sellOn", fontSize = 13.sp)
                    ChipRow(listOf(0, 10, 20, 30), sellOn, { "%$it" }) { sellOn = it }
                }
                OfferType.SWAP -> {
                    Text("Takasa dahil edilecek oyuncularınız:", fontSize = 13.sp)
                    user.playerIds.mapNotNull { gs.players[it] }.sortedByDescending { it.overall }.forEach { sp ->
                        Row(Modifier.fillMaxWidth().clickable { if (sp.id in swap) swap.remove(sp.id) else swap.add(sp.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = sp.id in swap, onCheckedChange = { if (it) swap.add(sp.id) else swap.remove(sp.id) })
                            Text("${sp.position.tr} ${sp.shortName} (${sp.overall}) ${Economy.fmt(Economy.value(sp, gs.season))}", fontSize = 12.sp)
                        }
                    }
                    MoneySlider("Ek nakit", fee.coerceAtMost(value * 2), 0, value * 2) { fee = it }
                }
                OfferType.LOAN -> {
                    Text("Maaş katkısı: %$wageShare", fontSize = 13.sp)
                    ChipRow(listOf(25, 50, 75, 100), wageShare, { "%$it" }) { wageShare = it }
                    MoneySlider("Kiralama bedeli", loanFee, 0, value / 2 + 100_000) { loanFee = it }
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(buyOpt, { buyOpt = it }); Text("Satın alma opsiyonu", fontSize = 13.sp) }
                    if (buyOpt) MoneySlider("Opsiyon bedeli", buyAmount, value / 2, value * 3) { buyAmount = it }
                }
                else -> {}
            }
            error?.let { Text(it, color = Bad, fontSize = 12.sp) }
        }
    }, confirmButton = {
        TextButton(onClick = {
            val r = when (type) {
                OfferType.LOAN -> TransferMarket.submitOffer(gs, p.id, OfferType.LOAN, 0, loanWageShare = wageShare, loanFee = loanFee, buyOption = if (buyOpt) buyAmount else null)
                OfferType.SWAP -> TransferMarket.submitOffer(gs, p.id, OfferType.SWAP, fee, swapIds = swap.toList())
                else -> TransferMarket.submitOffer(gs, p.id, OfferType.TRANSFER, fee, sellOn = sellOn, installments = installments)
            }
            r.onSuccess { vm.bump(); onDone(it.id) }.onFailure { error = it.message }
        }) { Text("Teklif gönder") }
    }, dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Vazgeç") } })
}

/* ======================= KİŞİSEL ŞARTLAR ======================= */

@Composable
fun TermsDialog(vm: GameViewModel, gs: GameState, offerId: Int, onClose: () -> Unit) {
    val o = gs.offers.firstOrNull { it.id == offerId } ?: run { onClose(); return }
    val p = gs.players[o.playerId] ?: run { onClose(); return }
    val user = gs.userClub
    var role by remember { mutableStateOf(SquadRole.FIRST_TEAM) }
    var demand by remember(role) { mutableStateOf(TransferMarket.demandTerms(gs, p, user, role, o.fee)) }
    var wage by remember(role) { mutableLongStateOf(demand.weeklyWage * 9 / 10) }
    var years by remember { mutableIntStateOf(demand.years) }
    var bonus by remember(role) { mutableLongStateOf(demand.signingBonus) }
    var agentFee by remember(role) { mutableLongStateOf(demand.agentFee * 9 / 10) }
    var clause by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onClose, title = { Text("Kişisel şartlar: ${p.shortName}") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (o.type == OfferType.LOAN) {
                Text("Kiralık transferde oyuncu mevcut sözleşmesiyle gelir. Oynama süresi garantisi isteyecektir.", fontSize = 13.sp)
            } else {
                Text("Ajan: ${gs.agents[p.agentId]?.name ?: "-"}", color = Muted, fontSize = 12.sp)
                Text("Rol vaadi", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ChipRow(SquadRole.values().toList(), role, { it.tr }) { role = it }
                MoneySlider("Haftalık maaş", wage, demand.weeklyWage / 3, demand.weeklyWage * 2) { wage = it }
                Text("Süre: $years yıl", fontSize = 13.sp)
                ChipRow((1..5).toList(), years, { "$it" }) { years = it }
                MoneySlider("İmza parası", bonus, 0, demand.signingBonus * 3 + 10_000) { bonus = it }
                MoneySlider("Ajan ücreti", agentFee, 0, demand.agentFee * 2 + 10_000) { agentFee = it }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(clause, { clause = it }); Text("Serbest kalma maddesi (değerin 3 katı)", fontSize = 12.sp) }
            }
            if (msg.isNotBlank()) Text(msg, color = if (done) Good else Gold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }, confirmButton = {
        if (!done && o.status == OfferStatus.ACCEPTED_CLUB) TextButton(onClick = {
            val terms = PersonalTerms(wage, years, bonus, agentFee, if (clause) Economy.value(p, gs.season) * 3 else null, role)
            val r = TransferMarket.negotiateTerms(gs, offerId, terms)
            msg = r.message
            if (r.accepted || r.finalRejection) done = true
            r.demand?.let { d -> demand = d }
            vm.bump()
        }) { Text("Teklif et") }
    }, dismissButton = { TextButton(onClick = onClose) { Text(if (done) "Kapat" else "Sonra") } })
}
