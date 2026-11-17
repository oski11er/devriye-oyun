@file:OptIn(ExperimentalMaterial3Api::class)

package com.futbolmenajer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.futbolmenajer.data.model.*
import com.futbolmenajer.engine.Economy
import com.futbolmenajer.engine.GameEngine

/* ---------------- TEMA ---------------- */

val Pitch = Color(0xFF0B6E4F)
val PitchDark = Color(0xFF0B3D2E)
val Gold = Color(0xFFFFC107)
val Bg = Color(0xFF0F1A14)
val CardBg = Color(0xFF16261D)
val Muted = Color(0xFF9FB3A8)
val Good = Color(0xFF4CAF50)
val Bad = Color(0xFFE53935)
val Warn = Color(0xFFFFA000)

private val scheme = darkColorScheme(
    primary = Color(0xFF2ECC71), onPrimary = Color.Black, secondary = Gold, onSecondary = Color.Black,
    background = Bg, surface = CardBg, onSurface = Color(0xFFE8F5E9), onBackground = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF1F3328), onSurfaceVariant = Muted, primaryContainer = PitchDark, onPrimaryContainer = Color.White,
    secondaryContainer = Color(0xFF2B4536), onSecondaryContainer = Color.White
)

@Composable
fun FutbolTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

/* ---------------- ORTAK BİLEŞENLER ---------------- */

fun ratingColor(v: Int): Color = when {
    v >= 85 -> Color(0xFF00E676); v >= 75 -> Color(0xFF66BB6A); v >= 65 -> Color(0xFFC0CA33)
    v >= 55 -> Color(0xFFFFA726); else -> Color(0xFFEF5350)
}

fun lineColor(p: Position): Color = when (p.line) {
    Line.GK -> Color(0xFFFFB300); Line.DEF -> Color(0xFF42A5F5); Line.MID -> Color(0xFF66BB6A); Line.ATT -> Color(0xFFEF5350)
}

@Composable
fun PosBadge(p: Position, modifier: Modifier = Modifier) {
    Box(modifier.width(40.dp).clip(RoundedCornerShape(6.dp)).background(lineColor(p)).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
        Text(p.tr, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RatingBox(v: Int, size: Int = 34) {
    Box(Modifier.size(size.dp).clip(RoundedCornerShape(8.dp)).background(ratingColor(v)), contentAlignment = Alignment.Center) {
        Text("$v", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = (size / 2.4).sp)
    }
}

@Composable
fun ClubDot(color: Long, size: Int = 14) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color(color)).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape))
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun KV(k: String, v: String, vColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(k, color = Muted, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Text(v, fontWeight = FontWeight.SemiBold, color = vColor, fontSize = 14.sp)
    }
}

@Composable
fun AttrBar(label: String, v: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(110.dp), fontSize = 13.sp, color = Muted)
        LinearProgressIndicator(progress = { v / 100f }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = ratingColor(v), trackColor = Color(0xFF263D30))
        Text("$v", Modifier.width(34.dp).padding(start = 8.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ratingColor(v))
    }
}

@Composable
fun MiniBar(v: Int, color: Color, width: Int = 40) {
    Box(Modifier.width(width.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF263D30))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(v.coerceIn(0, 100) / 100f).background(color))
    }
}

@Composable
fun PlayerRow(gs: GameState, p: Player, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null, showClub: Boolean = false, posOverride: Position? = null) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PosBadge(posOverride ?: p.position)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.fullName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
                if (p.injuryDays > 0) Text("  ✚${p.injuryDays}g", color = Bad, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (p.suspendedMatches > 0) Text("  🟥${p.suspendedMatches}", fontSize = 11.sp)
                if (p.transferListed) Text("  TL", color = Warn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (p.contract?.loanFromClubId != null) Text("  K", color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            val sub = buildString {
                append("${p.age(gs.season)} yaş · ${GameEngine.nationName(p.nationality)}")
                if (showClub) append(" · ${p.contract?.clubId?.let { gs.clubs[it]?.shortName } ?: "Serbest"}")
                else append(" · ${Economy.fmt(Economy.value(p, gs.season))}")
            }
            Text(sub, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniBar(p.fitness, if (p.fitness > 80) Good else if (p.fitness > 60) Warn else Bad)
                Spacer(Modifier.width(4.dp))
                MiniBar(p.morale, Color(0xFF29B6F6))
            }
        }
        if (trailing != null) trailing()
        Spacer(Modifier.width(6.dp))
        RatingBox(posOverride?.let { p.ratingAt(it) } ?: p.overall)
    }
}

@Composable
fun <T> ChipRow(items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp)) {
        items.forEach { it ->
            FilterChip(selected = it == selected, onClick = { onSelect(it) }, label = { Text(label(it), fontSize = 12.sp) },
                modifier = Modifier.padding(end = 6.dp))
        }
    }
}

@Composable
fun MoneySlider(label: String, value: Long, min: Long, max: Long, onChange: (Long) -> Unit) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row { Text(label, color = Muted, modifier = Modifier.weight(1f)); Text(Economy.fmt(value), fontWeight = FontWeight.Bold) }
        val range = (max - min).coerceAtLeast(1)
        Slider(value = ((value - min).toFloat() / range).coerceIn(0f, 1f), onValueChange = { f -> onChange(Economy.roundNice(min + (range * f).toLong())) })
    }
}

@Composable
fun EmptyNote(text: String) {
    Text(text, color = Muted, modifier = Modifier.fillMaxWidth().padding(24.dp))
}
