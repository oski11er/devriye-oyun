@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

/* ======================= TEKNİK EKİP ======================= */

@Composable
fun StaffScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    val c = gs.userClub
    var hireRole by remember { mutableStateOf<StaffRole?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(8.dp)) {
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(777_777 + gs.managerName.hashCode(), c.country, 42, c.primaryColor, c.secondaryColor, 64)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Teknik Direktör", color = Muted, fontSize = 12.sp)
                        Text(gs.managerName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Menajer itibarı: ${gs.managerReputation}/100", color = Gold, fontSize = 12.sp)
                    }
                }
                Text("Teknik ekip haftalık maliyeti: ${Economy.fmt(c.staff.sumOf { it.weeklyWage })}", color = Muted, fontSize = 12.sp)
            }
        }
        items(StaffRole.values().toList()) { role ->
            val s = c.staff.firstOrNull { it.role == role }
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s != null) Avatar(s.id, s.nationality, s.age, c.primaryColor, c.secondaryColor, 52)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(role.tr, color = Muted, fontSize = 12.sp)
                        Text(s?.name ?: "Boş", fontWeight = FontWeight.Bold)
                        if (s != null) Text("${GameEngine.nationName(s.nationality)} · ${s.age} yaş · ${Economy.fmt(s.weeklyWage)}/hafta", color = Muted, fontSize = 11.sp)
                        Text(Staffing.effect(role), fontSize = 11.sp, color = Color(0xFF81C784))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RatingBox(s?.quality ?: 0, 38)
                        TextButton(onClick = { hireRole = role }) { Text("Değiştir", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
    hireRole?.let { role ->
        val cands = gs.staffPool.filter { it.role == role }.sortedByDescending { it.quality }
        AlertDialog(onDismissRequest = { hireRole = null }, title = { Text("${role.tr} adayları") }, text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (cands.isEmpty()) item { Text("Şu an uygun aday yok.", color = Muted) }
                items(cands) { s ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(s.id, s.nationality, s.age, 0xFF37474F, 0xFFFFFFFF, 40)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${GameEngine.nationName(s.nationality)} · ${s.age} · ${Economy.fmt(s.weeklyWage)}/hf", color = Muted, fontSize = 11.sp)
                        }
                        RatingBox(s.quality, 32)
                        TextButton(onClick = { vm.say(Staffing.hire(gs, s.id)); hireRole = null; vm.bump() }) { Text("İşe al") }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { hireRole = null }) { Text("Kapat") } })
    }
}

/* ======================= WONDERKIDS & YILDIZLAR ======================= */

@Composable
fun StarsScreen(vm: GameViewModel) {
    val v = vm.version
    val gs = vm.gs ?: return
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Wonderkids", "Veteranlar", "En iyi 100", "Gerçek yıldızlar")
    val list = remember(tab, v) {
        val all = gs.players.values.asSequence().filter { it.contract != null || gs.freeAgents.contains(it.id) }
        when (tab) {
            0 -> all.filter { it.age(gs.season) <= 21 && it.potential >= 82 }.sortedByDescending { it.potential * 2 + it.overall }.take(150).toList()
            1 -> all.filter { it.age(gs.season) >= 33 && it.overall >= 72 }.sortedByDescending { it.overall }.take(150).toList()
            2 -> all.sortedByDescending { it.overall }.take(100).toList()
            else -> all.filter { it.isReal }.sortedByDescending { it.overall }.take(300).toList()
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 4.dp, containerColor = Bg) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        Text(when (tab) {
            0 -> "21 yaş ve altı, yüksek potansiyelli genç yetenekler (potansiyel scout ağınıza göre tahminidir)."
            1 -> "33 yaş ve üstü, hâlâ üst düzey oynayan deneyimli futbolcular."
            2 -> "Dünyanın en güçlü 100 futbolcusu."
            else -> "Veritabanındaki gerçek futbolcular (2026-27 kadroları)."
        }, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
        LazyColumn {
            items(list, key = { it.id }) { p ->
                PlayerRow(gs, p, onClick = { vm.openPlayer(p.id) }, showClub = true, trailing = {
                    if (tab == 0) Text("P ${GameEngine.roughPotential(gs, p)}", color = Gold, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                })
            }
        }
    }
}

/* ======================= TARAFTAR TRİBÜNÜ ======================= */

@Composable
fun FansPanel(gs: GameState, club: Club) {
    val mood = club.fanMood
    val quotes = when {
        mood >= 75 -> listOf("Bu sezon şampiyonluk geliyor!", "Hocam seni seviyoruz!", "Tribünler dolu, takım uçuyor!", "Bu kadroyla Avrupa'yı sallarız!")
        mood >= 50 -> listOf("Fena değil ama daha iyisi olabilir.", "Bir golcü transferi şart!", "Maç maç iyiye gidiyoruz.", "Gençlere şans verilmeli.")
        mood >= 30 -> listOf("Bu oyunla bir yere varamayız.", "Yönetim istifa!", "Hocanın taktiği yanlış.", "Bilet fiyatları çok pahalı!")
        else -> listOf("Bu takım bizi temsil etmiyor!", "Teknik direktör istifa!", "Stada gelmeyeceğiz.", "Transfer yapılmazsa protesto var!")
    }
    InfoCard {
        Text("Taraftar tribünü – memnuniyet ${mood}/100", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LazyRow {
            items((0 until 6).toList()) { i ->
                Column(Modifier.width(110.dp).padding(end = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val nat = if (i % 4 == 3) "ENG" else club.country
                    Avatar(club.id * 1000 + i + gs.season, nat, 18 + (i * 9) % 45, club.primaryColor, club.secondaryColor, 56)
                    Text("\"${quotes[i % quotes.size]}\"", fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}
