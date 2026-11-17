package com.futbolmenajer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.futbolmenajer.data.model.EventType
import com.futbolmenajer.data.model.MatchEvent
import com.futbolmenajer.data.model.Position
import com.futbolmenajer.engine.MatchSim
import com.futbolmenajer.engine.TeamSide
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * 2.5D CANLI MAÇ GÖRÜNÜMÜ
 * Yayın kamerası açısında perspektifli saha; 22 oyuncu, top, tribünlerde taraftarlar.
 * Konumlar diziliş + topa sahip olan takım + topun yeri ile hesaplanır ve kare kare yumuşatılır.
 * Simülasyondaki olaylar (şut, gol, korner, kart, kurtarış) sahada animasyon olarak oynatılır.
 * Normalize koordinat: x = 0 (ev sahibi kalesi) → 1 (deplasman kalesi), y = 0 (üst taç) → 1 (alt taç).
 */
class PitchAnimator(private val sim: MatchSim) {
    private val rnd = Random(42)
    var frame by mutableLongStateOf(0L); private set

    class Dot(var x: Float, var y: Float, var tx: Float, var ty: Float, val home: Boolean, var id: Int, var num: Int, var active: Boolean = true)

    val dots = mutableListOf<Dot>()
    var ballX = 0.5f; var ballY = 0.5f
    private var ballTx = 0.5f; private var ballTy = 0.5f
    private var ballSpeed = 1.6f
    private var owner: Dot? = null
    private var passTimer = 0f
    var banner: String? = null; private set
    var bannerColor: Color = Color.White; private set
    private var bannerTime = 0f
    var flash = 0f; private set
    var cardDot: Pair<Dot, Color>? = null; private set
    private var cardTime = 0f
    private var scriptTime = 0f   // olay animasyonu süresince serbest dolaşım durur

    init { rebuild() }

    private fun baseOf(pos: Position, k: Int, n: Int): Pair<Float, Float> {
        val spread = if (n <= 1) 0.5f else 0.25f + 0.5f * k / (n - 1)
        return when (pos) {
            Position.GK -> 0.04f to 0.5f
            Position.CB -> 0.18f to spread
            Position.LB -> 0.24f to 0.13f
            Position.RB -> 0.24f to 0.87f
            Position.DM -> 0.31f to spread
            Position.CM -> 0.40f to spread
            Position.AM -> 0.50f to spread
            Position.LW -> 0.56f to 0.15f
            Position.RW -> 0.56f to 0.85f
            Position.ST -> 0.63f to spread
        }
    }

    /** Diziliş/oyuncu değişikliklerinden sonra listeyi yeniden kur. */
    fun rebuild() {
        val old = dots.associateBy { it.id }
        dots.clear()
        for (side in listOf(sim.home, sim.away)) {
            val home = side === sim.home
            val counts = side.slots.groupingBy { it }.eachCount()
            val seen = HashMap<Position, Int>()
            side.lineup.forEachIndexed { i, id ->
                if (id < 0) return@forEachIndexed
                val pos = side.slots.getOrElse(i) { Position.CM }
                val k = seen.getOrDefault(pos, 0); seen[pos] = k + 1
                val (bx, by) = baseOf(pos, k, counts[pos] ?: 1)
                val x = if (home) bx else 1f - bx
                val y = if (home) by else 1f - by
                val prev = old[id]
                dots.add(Dot(prev?.x ?: x, prev?.y ?: y, x, y, home, id, i + 1))
            }
        }
        if (owner == null || owner!!.id !in dots.map { it.id }) owner = dots.firstOrNull { !it.home && it.num == 10 } ?: dots.firstOrNull()
    }

    private fun sideOf(d: Dot): TeamSide = if (d.home) sim.home else sim.away

    private fun formationTargets() {
        val possHome = sim.possessionSideId == sim.home.id
        for (side in listOf(sim.home, sim.away)) {
            val home = side === sim.home
            val counts = side.slots.groupingBy { it }.eachCount()
            val seen = HashMap<Position, Int>()
            val attacking = possHome == home
            side.lineup.forEachIndexed { i, id ->
                if (id < 0) return@forEachIndexed
                val d = dots.firstOrNull { it.id == id } ?: return@forEachIndexed
                val pos = side.slots.getOrElse(i) { Position.CM }
                val k = seen.getOrDefault(pos, 0); seen[pos] = k + 1
                var (bx, by) = baseOf(pos, k, counts[pos] ?: 1)
                if (pos != Position.GK) {
                    bx += if (attacking) 0.13f else -0.04f
                    // topa doğru kayma
                    val ballOwnY = if (home) ballY else 1f - ballY
                    by += (ballOwnY - by) * 0.25f
                    val ballOwnX = if (home) ballX else 1f - ballX
                    bx += (ballOwnX - 0.5f) * 0.18f
                }
                bx += (rnd.nextFloat() - 0.5f) * 0.03f; by += (rnd.nextFloat() - 0.5f) * 0.04f
                d.tx = (if (home) bx else 1f - bx).coerceIn(0.01f, 0.99f)
                d.ty = (if (home) by else 1f - by).coerceIn(0.03f, 0.97f)
            }
        }
    }

    /** Simülasyon bir dakika ilerlediğinde çağrılır. */
    fun onStep(events: List<MatchEvent>) {
        if (events.any { it.type == EventType.SUB || it.type == EventType.RED || it.type == EventType.SECOND_YELLOW || it.type == EventType.INJURY }) rebuild()
        formationTargets()
        val possHome = sim.possessionSideId == sim.home.id
        if (owner?.home != possHome && scriptTime <= 0f) {
            owner = dots.filter { it.home == possHome && it.active }.let { l -> if (l.isEmpty()) null else l[rnd.nextInt(l.size)] }
        }
        for (e in events) {
            val homeEvent = e.teamId == sim.home.id
            val goalX = if (homeEvent) 1.0f else 0.0f
            when (e.type) {
                EventType.GOAL, EventType.PENALTY_GOAL -> {
                    val scorer = dots.firstOrNull { it.id == e.playerId }
                    scorer?.let { it.tx = if (homeEvent) 0.88f else 0.12f; it.ty = 0.5f }
                    owner = null
                    ballTx = goalX; ballTy = 0.47f + rnd.nextFloat() * 0.06f; ballSpeed = 3.2f
                    show("GOOOL!", Gold, 2.6f); flash = 1f; scriptTime = 2.2f
                }
                EventType.SAVE, EventType.CHANCE, EventType.PENALTY_MISS -> {
                    owner = null
                    ballTx = if (homeEvent) 0.97f else 0.03f; ballTy = 0.38f + rnd.nextFloat() * 0.24f; ballSpeed = 3f
                    show(if (e.type == EventType.SAVE) "KURTARIŞ!" else "KAÇTI!", Color.White, 1.4f); scriptTime = 1.2f
                }
                EventType.CORNER, EventType.GK_8_SECONDS -> {
                    owner = null
                    ballTx = if (homeEvent) 0.995f else 0.005f; ballTy = if (rnd.nextBoolean()) 0.01f else 0.99f; ballSpeed = 2f
                    show("KORNER", Color.White, 1.2f); scriptTime = 1f
                }
                EventType.YELLOW, EventType.RED, EventType.SECOND_YELLOW -> {
                    dots.firstOrNull { it.id == e.playerId }?.let { cardDot = it to (if (e.type == EventType.YELLOW) Color(0xFFFFEB3B) else Color(0xFFE53935)); cardTime = 2f }
                }
                EventType.KICKOFF, EventType.HALF_TIME -> { ballTx = 0.5f; ballTy = 0.5f; ballX = 0.5f; ballY = 0.5f; owner = null }
                EventType.VAR -> show("VAR", Color(0xFF64B5F6), 1.6f)
                EventType.SUB -> show("DEĞİŞİKLİK", Color(0xFF81C784), 1.2f)
                else -> {}
            }
        }
    }

    private fun show(t: String, c: Color, sec: Float) { banner = t; bannerColor = c; bannerTime = sec }

    /** Her karede çağrılır. */
    fun update(dt: Float) {
        val k = (dt * 3.2f).coerceAtMost(1f)
        for (d in dots) { d.x += (d.tx - d.x) * k; d.y += (d.ty - d.y) * k }
        if (scriptTime > 0f) scriptTime -= dt
        // serbest oyun: kısa paslar
        if (scriptTime <= 0f && !sim.finished) {
            passTimer -= dt
            val o = owner
            if (o != null) { ballTx = o.x + if (o.home) 0.012f else -0.012f; ballTy = o.y + 0.01f; ballSpeed = 6f }
            if (passTimer <= 0f) {
                passTimer = 0.35f + rnd.nextFloat() * 0.5f
                val possHome = sim.possessionSideId == sim.home.id
                val mates = dots.filter { it.home == possHome && it !== o && it.active }
                if (mates.isNotEmpty()) {
                    // ileri pas tercihi
                    val fwd = mates.sortedBy { if (possHome) -it.x else it.x }.take(6)
                    owner = fwd[rnd.nextInt(fwd.size)]; ballSpeed = 2.4f
                }
            }
        }
        val bk = (dt * ballSpeed).coerceAtMost(1f)
        ballX += (ballTx - ballX) * bk; ballY += (ballTy - ballY) * bk
        if (bannerTime > 0f) { bannerTime -= dt; if (bannerTime <= 0f) banner = null }
        if (flash > 0f) flash = (flash - dt * 0.6f).coerceAtLeast(0f)
        if (cardTime > 0f) { cardTime -= dt; if (cardTime <= 0f) cardDot = null }
        frame++
    }

    fun sideColor(home: Boolean): Long = if (home) sim.home.clubColor else (if (sim.away.clubColor == sim.home.clubColor) sim.away.secondColor else sim.away.clubColor)
}

private class CrowdDot(val x: Float, val row: Int, val color: Color, val phase: Float)

@Composable
fun MatchPitch(anim: PitchAnimator, modifier: Modifier = Modifier) {
    val f = anim.frame // yeniden çizim tetikleyicisi
    val homeC = Color(anim.sideColor(true)); val awayC = Color(anim.sideColor(false))
    val crowd = remember(homeC, awayC) {
        val r = Random(7)
        List(900) { i ->
            val c = when (r.nextInt(10)) { in 0..6 -> homeC; 7 -> awayC; 8 -> Color(0xFFEEEEEE); else -> Color(0xFF333333) }
            CrowdDot(r.nextFloat(), r.nextInt(6), c, r.nextFloat() * 6.28f)
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val W = size.width; val H = size.height
        // gökyüzü / stadyum arka planı
        drawRect(Brush.verticalGradient(listOf(Color(0xFF0A1220), Color(0xFF14202E))))
        val standH = H * 0.20f
        // tribün (üst)
        drawRect(Color(0xFF1C2733), topLeft = Offset(0f, 0f), size = Size(W, standH))
        val t = f / 60f
        for (c in crowd) {
            val cy = standH * (0.12f + c.row * 0.14f)
            val bounce = if (anim.flash > 0f) sin(t * 9f + c.phase) * 3f * anim.flash else sin(t * 1.3f + c.phase) * 0.6f
            drawCircle(c.color, radius = W / 330f + c.row * 0.25f, center = Offset(c.x * W, cy + bounce))
        }
        // reklam panosu
        drawRect(Brush.horizontalGradient(listOf(Color(0xFF0B6E4F), Color(0xFF1565C0), Color(0xFF0B6E4F))), topLeft = Offset(0f, standH), size = Size(W, H * 0.03f))
        // saha (perspektif yamuk)
        val top = standH + H * 0.04f; val bottom = H * 0.98f
        val topScale = 0.80f
        fun px(x: Float, y: Float): Offset {
            val s = topScale + (1f - topScale) * y
            val sx = W / 2 + (x - 0.5f) * W * 0.94f * s
            val sy = top + (bottom - top) * y
            return Offset(sx, sy)
        }
        val grass = Path().apply { val a = px(0f, 0f); moveTo(a.x, a.y); lineTo(px(1f, 0f).x, px(1f, 0f).y); lineTo(px(1f, 1f).x, px(1f, 1f).y); lineTo(px(0f, 1f).x, px(0f, 1f).y); close() }
        drawPath(grass, Color(0xFF2E7D32))
        // çim şeritleri
        for (i in 0 until 12) if (i % 2 == 0) {
            val x0 = i / 12f; val x1 = (i + 1) / 12f
            val p = Path().apply { moveTo(px(x0, 0f).x, px(x0, 0f).y); lineTo(px(x1, 0f).x, px(x1, 0f).y); lineTo(px(x1, 1f).x, px(x1, 1f).y); lineTo(px(x0, 1f).x, px(x0, 1f).y); close() }
            drawPath(p, Color(0xFF388E3C))
        }
        val line = Color.White.copy(alpha = 0.85f); val sw = W / 450f
        drawPath(grass, line, style = Stroke(sw))
        drawLine(line, px(0.5f, 0f), px(0.5f, 1f), sw)
        // orta yuvarlak (elips)
        val c = px(0.5f, 0.5f); val rx = W * 0.07f; val ry = (bottom - top) * 0.13f
        drawOval(line, topLeft = Offset(c.x - rx, c.y - ry), size = Size(rx * 2, ry * 2), style = Stroke(sw))
        drawCircle(line, sw * 2, c)
        // ceza sahaları ve kaleler
        for (left in listOf(true, false)) {
            val x0 = if (left) 0f else 1f; val bx = if (left) 0.16f else 0.84f; val sx = if (left) 0.055f else 0.945f
            val box = Path().apply { moveTo(px(x0, 0.2f).x, px(x0, 0.2f).y); lineTo(px(bx, 0.2f).x, px(bx, 0.2f).y); lineTo(px(bx, 0.8f).x, px(bx, 0.8f).y); lineTo(px(x0, 0.8f).x, px(x0, 0.8f).y) }
            drawPath(box, line, style = Stroke(sw))
            val small = Path().apply { moveTo(px(x0, 0.37f).x, px(x0, 0.37f).y); lineTo(px(sx, 0.37f).x, px(sx, 0.37f).y); lineTo(px(sx, 0.63f).x, px(sx, 0.63f).y); lineTo(px(x0, 0.63f).x, px(x0, 0.63f).y) }
            drawPath(small, line, style = Stroke(sw))
            val g1 = px(x0, 0.44f); val g2 = px(x0, 0.56f)
            val gw = if (left) -W * 0.018f else W * 0.018f
            drawLine(Color.White, g1, Offset(g1.x + gw, g1.y - H * 0.012f), sw * 2)
            drawLine(Color.White, g2, Offset(g2.x + gw, g2.y - H * 0.012f), sw * 2)
            drawLine(Color.White, Offset(g1.x + gw, g1.y - H * 0.012f), Offset(g2.x + gw, g2.y - H * 0.012f), sw * 2)
            drawLine(Color.White.copy(alpha = 0.4f), g1, g2, sw * 3)
        }
        // oyuncular (uzaktan yakına sırala)
        val sorted = anim.dots.sortedBy { it.y }
        for (d in sorted) {
            val p = px(d.x, d.y)
            val s = topScale + (1f - topScale) * d.y
            val r = W / 95f * s
            drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(p.x - r * 1.1f, p.y + r * 0.6f), size = Size(r * 2.2f, r * 0.9f))
            val col = if (d.home) homeC else awayC
            // gövde
            drawRoundRectBody(p, r, col)
            // kafa
            drawCircle(Color(0xFFE0B48C), r * 0.55f, Offset(p.x, p.y - r * 1.55f))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = if (luminance(col) > 0.6f) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    textSize = r * 1.05f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true
                }
                drawText("${d.num}", p.x, p.y + r * 0.1f, paint)
            }
        }
        anim.cardDot?.let { (d, col) ->
            val p = px(d.x, d.y); val r = W / 95f
            drawRect(col, topLeft = Offset(p.x + r, p.y - r * 3.4f), size = Size(r * 0.9f, r * 1.3f))
        }
        // top
        val bp = px(anim.ballX, anim.ballY)
        val br = W / 210f
        drawOval(Color.Black.copy(alpha = 0.4f), topLeft = Offset(bp.x - br, bp.y + br * 0.4f), size = Size(br * 2, br))
        drawCircle(Color.White, br, Offset(bp.x, bp.y - br * 0.6f))
        drawCircle(Color.Black, br * 0.35f, Offset(bp.x, bp.y - br * 0.6f))
        // pankart
        anim.banner?.let { b ->
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(255, (anim.bannerColor.red * 255).toInt(), (anim.bannerColor.green * 255).toInt(), (anim.bannerColor.blue * 255).toInt())
                    textSize = H * 0.13f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; isAntiAlias = true
                    setShadowLayer(12f, 0f, 4f, android.graphics.Color.BLACK)
                }
                drawText(b, W / 2, H * 0.58f, paint)
            }
        }
        if (anim.flash > 0f) drawRect(Color(0xFFFFC107).copy(alpha = anim.flash * 0.18f))
    }
}

private fun luminance(c: Color) = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue

private fun DrawScope.drawRoundRectBody(p: Offset, r: Float, col: Color) {
    drawRoundRect(col, topLeft = Offset(p.x - r, p.y - r * 1.05f), size = Size(r * 2, r * 1.9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.7f))
    drawRoundRect(Color.Black.copy(alpha = 0.25f), topLeft = Offset(p.x - r, p.y + r * 0.45f), size = Size(r * 2, r * 0.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.2f))
}

@Suppress("unused")
private fun absf(x: Float) = abs(x)
