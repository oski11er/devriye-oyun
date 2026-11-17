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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futbolmenajer.data.SaveManager
import com.futbolmenajer.data.db.LeagueCatalog
import com.futbolmenajer.engine.Economy
import com.futbolmenajer.engine.GameEngine
import com.futbolmenajer.engine.Lineups
import com.futbolmenajer.ui.*

@Composable
fun MenuScreen(vm: GameViewModel) {
    val v = vm.version
    var showLoad by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PitchDark, Bg)))) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 120.dp, vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("⚽", fontSize = 40.sp)
            Text("FUTBOL MENAJER", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
            Text("2026-27 Sezonu · Güncel kadrolar", color = Gold, fontSize = 16.sp)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.newWorld() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("YENİ KARİYER", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
            Spacer(Modifier.height(12.dp))
            val anySave = (1..SaveManager.SLOTS).mapNotNull { vm.saves.meta(it) }.maxByOrNull { it.savedAt }
            if (anySave != null) {
                Button(onClick = { vm.load(anySave.slot) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DEVAM ET", fontWeight = FontWeight.Bold)
                        Text("${anySave.clubName} · ${anySave.dateLabel}", fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            OutlinedButton(onClick = { showLoad = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("KARİYER YÜKLE") }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showAbout = true }) { Text("Hakkında", color = Muted) }
        }
    }
    if (showLoad) AlertDialog(onDismissRequest = { showLoad = false }, title = { Text("Kayıt Yükle") }, text = {
        Column {
            for (s in 1..SaveManager.SLOTS) {
                val m = vm.saves.meta(s)
                OutlinedButton(onClick = { if (m != null) { showLoad = false; vm.load(s) } }, enabled = m != null, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(if (m == null) "Yuva $s – boş" else "Yuva $s: ${m.clubName}\n${m.manager} · ${m.dateLabel}", textAlign = TextAlign.Center)
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { showLoad = false }) { Text("Kapat") } })
    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false }, title = { Text("Futbol Menajer") }, text = {
        Text("Çevrimdışı futbol menajerlik simülasyonu.\n\n• ${LeagueCatalog.countries.size} ülke, ${LeagueCatalog.countries.sumOf { it.leagues.size }} lig\n• UEFA Şampiyonlar Ligi, Avrupa Ligi, Konferans Ligi\n• Yerel kupalar, süper kupalar, milli maçlar, Dünya Kupası ve EURO\n• Bonservis, takas, kiralık ve serbest transfer; ajan pazarlığı\n• 2025-26 IFAB kuralları: 5 değişiklik/3 pencere, VAR, kaleci 8 saniye kuralı\n\nOyuncu güç değerleri oyun dengesi için tahminidir. Gerçek kişilerle ilgili fotoğraf içermez; veritabanını RealPlayers.kt ve LeagueCatalog.kt dosyalarından güncelleyebilirsiniz.")
    }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("Tamam") } })
}

@Composable
fun ClubSelectScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf<String?>(null) }
    var leagueId by remember { mutableStateOf<Int?>(null) }
    var clubId by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Yeni Kariyer", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = name, onValueChange = { name = it.take(30) }, label = { Text("Menajer adınız") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        when {
            country == null -> {
                Text("Ülke seçin", color = Muted)
                LazyColumn(Modifier.weight(1f)) {
                    items(LeagueCatalog.countries.sortedByDescending { it.coefficient }) { c ->
                        val clubs = gs.leagues.values.filter { it.country == c.code }.sumOf { it.clubIds.size }
                        Row(Modifier.fillMaxWidth().clickable { country = c.code }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.nameTr, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("${c.leagues.size} lig · $clubs kulüp", color = Muted, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color(0xFF1F3328))
                    }
                }
            }
            leagueId == null -> {
                TextButton(onClick = { country = null }) { Text("← Ülkeler") }
                LazyColumn(Modifier.weight(1f)) {
                    items(gs.leagues.values.filter { it.country == country }.sortedBy { it.tier }) { lg ->
                        Row(Modifier.fillMaxWidth().clickable { leagueId = lg.id }.padding(12.dp)) {
                            Text("${lg.tier}. ${lg.name}", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Text("${lg.clubIds.size} takım", color = Muted, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = Color(0xFF1F3328))
                    }
                }
            }
            else -> {
                TextButton(onClick = { leagueId = null; clubId = null }) { Text("← Ligler") }
                val lg = gs.leagues.getValue(leagueId!!)
                LazyColumn(Modifier.weight(1f)) {
                    items(lg.clubIds.mapNotNull { gs.clubs[it] }.sortedByDescending { it.reputation }) { c ->
                        val sel = clubId == c.id
                        Row(Modifier.fillMaxWidth().background(if (sel) Color(0xFF214A35) else Color.Transparent).clickable { clubId = c.id }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            ClubDot(c.primaryColor, 18)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, fontWeight = FontWeight.SemiBold)
                                Text("İtibar ${c.reputation} · Bütçe ${Economy.fmt(c.finances.transferBudget)} · Stadyum ${c.facilities.stadiumCapacity}", color = Muted, fontSize = 11.sp)
                            }
                            RatingBox(Lineups.teamStrength(gs, c))
                        }
                        HorizontalDivider(color = Color(0xFF1F3328))
                    }
                }
                Button(onClick = { clubId?.let { vm.startCareer(it, name) } }, enabled = clubId != null, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(clubId?.let { "${gs.clubs[it]?.name} ile başla" } ?: "Bir kulüp seçin", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SaveLoadScreen(vm: GameViewModel) {
    val v = vm.version
    var refresh by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text("Kayıt yuvaları", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Oyun cihazınıza kaydedilir, internet gerekmez.", color = Muted, fontSize = 12.sp)
        for (s in 1..SaveManager.SLOTS) {
            val m = remember(refresh, v) { vm.saves.meta(s) }
            InfoCard {
                Text("Yuva $s", fontWeight = FontWeight.Bold)
                Text(if (m == null) "Boş" else "${m.clubName} · ${m.manager}\n${m.dateLabel}", color = Muted, fontSize = 13.sp)
                Row {
                    Button(onClick = { vm.save(s); refresh++ }, modifier = Modifier.padding(end = 8.dp)) { Text("Kaydet") }
                    OutlinedButton(onClick = { vm.load(s) }, enabled = m != null, modifier = Modifier.padding(end = 8.dp)) { Text("Yükle") }
                    TextButton(onClick = { vm.saves.delete(s); refresh++ }, enabled = m != null) { Text("Sil", color = Bad) }
                }
            }
        }
    }
}

@Composable
fun SackedScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Görevden alındınız", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Bad)
        Text("Menajer itibarınız: ${gs.managerReputation}. Aşağıdaki kulüpler size iş teklif ediyor:", color = Muted)
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(gs.jobOffers.mapNotNull { gs.clubs[it] }) { c ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClubDot(c.primaryColor, 18); Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold)
                            Text("${gs.leagues[c.leagueId]?.name} · İtibar ${c.reputation} · Bütçe ${Economy.fmt(c.finances.transferBudget)}", color = Muted, fontSize = 12.sp)
                        }
                        Button(onClick = { vm.acceptJob(c.id) }) { Text("Kabul") }
                    }
                }
            }
        }
        TextButton(onClick = { vm.quitToMenu() }) { Text("Ana menüye dön") }
    }
}
