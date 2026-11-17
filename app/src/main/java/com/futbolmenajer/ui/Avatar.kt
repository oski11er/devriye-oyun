package com.futbolmenajer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.futbolmenajer.data.db.NameBank
import com.futbolmenajer.data.db.NameGroup
import kotlin.random.Random

/**
 * PROSEDÜREL PORTRE
 * Her kişi (oyuncu, teknik ekip, taraftar) için kimliğinden türetilen, her açılışta aynı kalan
 * çizim portre: ten rengi (uyruğa göre dağılım), saç modeli/rengi, sakal, kaş, göz, ağız, forma.
 * Gerçek fotoğraf kullanılmaz.
 */
data class FaceSpec(
    val skin: Color, val hair: Color, val hairStyle: Int, val beard: Int, val eyeColor: Color,
    val browThick: Float, val mouth: Int, val faceW: Float, val ears: Float, val glasses: Boolean
)

private val skinLight = listOf(Color(0xFFF6D7C3), Color(0xFFEFC7A8), Color(0xFFE8B996), Color(0xFFF3CFB3))
private val skinMed = listOf(Color(0xFFD9A47E), Color(0xFFC68C62), Color(0xFFB97D55), Color(0xFFD5A078))
private val skinDark = listOf(Color(0xFF8D5A3B), Color(0xFF6E4430), Color(0xFF5A3726), Color(0xFF7A4B32))
private val skinEast = listOf(Color(0xFFF1D2B4), Color(0xFFE6C29E), Color(0xFFEBCBA8))

fun faceFor(seed: Int, nationality: String, age: Int): FaceSpec {
    val r = Random(seed * 7919 + 17)
    val group = NameBank.groupFor(nationality)
    val roll = r.nextDouble()
    val skin = when (group) {
        NameGroup.WEST_AFRICAN -> if (roll < 0.9) skinDark.random(r) else skinMed.random(r)
        NameGroup.ARABIC, NameGroup.HEBREW -> if (roll < 0.75) skinMed.random(r) else if (roll < 0.9) skinLight.random(r) else skinDark.random(r)
        NameGroup.JAPANESE, NameGroup.KOREAN, NameGroup.CHINESE -> skinEast.random(r)
        NameGroup.PORTUGUESE, NameGroup.SPANISH -> when { roll < 0.45 -> skinLight.random(r); roll < 0.8 -> skinMed.random(r); else -> skinDark.random(r) }
        NameGroup.ENGLISH, NameGroup.FRENCH, NameGroup.DUTCH -> when { roll < 0.62 -> skinLight.random(r); roll < 0.75 -> skinMed.random(r); else -> skinDark.random(r) }
        NameGroup.TURKISH, NameGroup.GREEK, NameGroup.ITALIAN, NameGroup.SLAVIC_SOUTH -> if (roll < 0.6) skinLight.random(r) else skinMed.random(r)
        else -> if (roll < 0.85) skinLight.random(r) else skinMed.random(r)
    }
    val dark = skin == skinDark.firstOrNull { it == skin } || skin in skinDark
    val hairPalette = when {
        dark || group in setOf(NameGroup.ARABIC, NameGroup.JAPANESE, NameGroup.KOREAN, NameGroup.CHINESE, NameGroup.TURKISH) ->
            listOf(Color(0xFF111111), Color(0xFF1E1410), Color(0xFF2B1D15))
        group in setOf(NameGroup.NORDIC, NameGroup.DUTCH, NameGroup.GERMAN, NameGroup.SLAVIC_EAST, NameGroup.SLAVIC_WEST) ->
            listOf(Color(0xFFD8B46A), Color(0xFFB88A4A), Color(0xFF6B4A2E), Color(0xFF3A2A1E), Color(0xFFC9A55A))
        else -> listOf(Color(0xFF1A1310), Color(0xFF3A2A1E), Color(0xFF5C4030), Color(0xFF7A5232), Color(0xFFA0522D))
    }
    var hair = hairPalette.random(r)
    if (age >= 36 && r.nextDouble() < (age - 33) / 12.0) hair = Color(0xFF9E9E9E)
    if (age >= 48) hair = if (r.nextBoolean()) Color(0xFFBDBDBD) else Color(0xFF8D8D8D)
    val styles = if (dark) listOf(0, 2, 2, 3, 3, 3, 5) else listOf(0, 1, 1, 1, 2, 4, 4, 5)
    var style = styles.random(r)
    if (age >= 45 && r.nextDouble() < 0.4) style = 6 // kel/ seyrek
    return FaceSpec(
        skin = skin, hair = hair, hairStyle = style, beard = listOf(0, 0, 0, 1, 1, 2).random(r),
        eyeColor = if (!dark && group in setOf(NameGroup.NORDIC, NameGroup.GERMAN, NameGroup.DUTCH, NameGroup.ENGLISH) && r.nextDouble() < 0.5) Color(0xFF3D6FA8) else Color(0xFF3B2A20),
        browThick = 0.8f + r.nextFloat() * 0.8f, mouth = r.nextInt(3), faceW = 0.92f + r.nextFloat() * 0.16f,
        ears = 0.9f + r.nextFloat() * 0.3f, glasses = age >= 40 && r.nextDouble() < 0.25
    )
}

private fun Color.darker(f: Float) = Color(red * f, green * f, blue * f, alpha)

@Composable
fun Avatar(seed: Int, nationality: String, age: Int, shirt: Long, collar: Long = 0xFFFFFFFF, sizeDp: Int = 40, modifier: Modifier = Modifier) {
    val spec = remember(seed, nationality, age) { faceFor(seed, nationality, age) }
    Canvas(modifier.size(sizeDp.dp)) { drawFace(spec, Color(shirt), Color(collar)) }
}

fun DrawScope.drawFace(f: FaceSpec, shirt: Color, collar: Color) {
    val w = size.width; val h = size.height
    val circle = Path().apply { addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h)) }
    clipPath(circle) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF2E4A3C), Color(0xFF14241B))))
        // omuzlar / forma
        drawOval(shirt, topLeft = Offset(w * 0.05f, h * 0.72f), size = Size(w * 0.9f, h * 0.6f))
        drawOval(shirt.darker(0.85f), topLeft = Offset(w * 0.05f, h * 0.80f), size = Size(w * 0.9f, h * 0.6f), alpha = 0.4f)
        // boyun
        drawRoundRect(f.skin.darker(0.9f), topLeft = Offset(w * 0.40f, h * 0.58f), size = Size(w * 0.20f, h * 0.2f), cornerRadius = CornerRadius(w * 0.05f))
        // yaka
        val cp = Path().apply { moveTo(w * 0.36f, h * 0.76f); lineTo(w * 0.5f, h * 0.86f); lineTo(w * 0.64f, h * 0.76f) }
        drawPath(cp, collar, style = Stroke(width = w * 0.05f))
        val fw = w * 0.44f * f.faceW; val fh = h * 0.52f
        val fx = w / 2 - fw / 2; val fy = h * 0.14f
        // uzun saç arka katman
        if (f.hairStyle == 4) drawRoundRect(f.hair, topLeft = Offset(fx - w * 0.05f, fy), size = Size(fw + w * 0.1f, fh * 0.95f), cornerRadius = CornerRadius(w * 0.2f))
        // kulaklar
        drawOval(f.skin.darker(0.92f), topLeft = Offset(fx - w * 0.04f * f.ears, fy + fh * 0.38f), size = Size(w * 0.08f * f.ears, h * 0.13f))
        drawOval(f.skin.darker(0.92f), topLeft = Offset(fx + fw - w * 0.04f * f.ears, fy + fh * 0.38f), size = Size(w * 0.08f * f.ears, h * 0.13f))
        // yüz
        drawOval(f.skin, topLeft = Offset(fx, fy), size = Size(fw, fh))
        drawOval(f.skin.darker(0.94f), topLeft = Offset(fx, fy + fh * 0.55f), size = Size(fw, fh * 0.45f), alpha = 0.35f)
        // sakal
        when (f.beard) {
            1 -> drawOval(f.hair.copy(alpha = 0.28f), topLeft = Offset(fx + fw * 0.08f, fy + fh * 0.55f), size = Size(fw * 0.84f, fh * 0.45f))
            2 -> drawOval(f.hair.copy(alpha = 0.85f), topLeft = Offset(fx + fw * 0.05f, fy + fh * 0.58f), size = Size(fw * 0.9f, fh * 0.44f))
        }
        // saç
        when (f.hairStyle) {
            1 -> drawArc(f.hair, 180f, 180f, true, topLeft = Offset(fx - w * 0.01f, fy - h * 0.03f), size = Size(fw + w * 0.02f, fh * 0.55f))
            2 -> drawArc(f.hair.copy(alpha = 0.9f), 180f, 180f, true, topLeft = Offset(fx + w * 0.01f, fy - h * 0.005f), size = Size(fw - w * 0.02f, fh * 0.42f))
            3 -> { // kıvırcık / afro
                val rr = w * 0.07f
                for (i in 0..6) for (j in 0..1) drawCircle(f.hair, rr, Offset(fx + fw * (0.08f + i * 0.14f), fy + h * (0.02f + j * 0.05f)))
            }
            4 -> drawArc(f.hair, 170f, 200f, true, topLeft = Offset(fx - w * 0.03f, fy - h * 0.04f), size = Size(fw + w * 0.06f, fh * 0.6f))
            5 -> { // yanlar kısa, üst dolgun
                drawArc(f.hair.copy(alpha = 0.5f), 180f, 180f, true, topLeft = Offset(fx, fy), size = Size(fw, fh * 0.42f))
                drawRoundRect(f.hair, topLeft = Offset(fx + fw * 0.25f, fy - h * 0.05f), size = Size(fw * 0.5f, h * 0.12f), cornerRadius = CornerRadius(w * 0.05f))
            }
            6 -> { // seyrek
                drawArc(f.hair.copy(alpha = 0.6f), 160f, 40f, false, topLeft = Offset(fx, fy + h * 0.02f), size = Size(fw, fh * 0.5f), style = Stroke(w * 0.04f))
                drawArc(f.hair.copy(alpha = 0.6f), 340f, 40f, false, topLeft = Offset(fx, fy + h * 0.02f), size = Size(fw, fh * 0.5f), style = Stroke(w * 0.04f))
            }
        }
        // kaşlar
        val ey = fy + fh * 0.42f
        drawLine(f.hair.darker(0.7f), Offset(fx + fw * 0.2f, ey - h * 0.045f), Offset(fx + fw * 0.4f, ey - h * 0.05f), strokeWidth = w * 0.025f * f.browThick)
        drawLine(f.hair.darker(0.7f), Offset(fx + fw * 0.6f, ey - h * 0.05f), Offset(fx + fw * 0.8f, ey - h * 0.045f), strokeWidth = w * 0.025f * f.browThick)
        // gözler
        for (ex in listOf(0.3f, 0.7f)) {
            drawOval(Color.White, topLeft = Offset(fx + fw * ex - w * 0.04f, ey - h * 0.018f), size = Size(w * 0.08f, h * 0.04f))
            drawCircle(f.eyeColor, w * 0.017f, Offset(fx + fw * ex, ey + h * 0.002f))
        }
        if (f.glasses) {
            for (ex in listOf(0.3f, 0.7f)) drawCircle(Color(0xFF222222), w * 0.06f, Offset(fx + fw * ex, ey), style = Stroke(w * 0.015f))
            drawLine(Color(0xFF222222), Offset(fx + fw * 0.39f, ey), Offset(fx + fw * 0.61f, ey), strokeWidth = w * 0.012f)
        }
        // burun
        drawArc(f.skin.darker(0.78f), 20f, 140f, false, topLeft = Offset(fx + fw * 0.43f, fy + fh * 0.52f), size = Size(fw * 0.14f, fh * 0.1f), style = Stroke(w * 0.015f))
        // ağız
        val my = fy + fh * 0.76f
        when (f.mouth) {
            0 -> drawArc(Color(0xFF8E4B44), 10f, 160f, false, topLeft = Offset(fx + fw * 0.34f, my - h * 0.03f), size = Size(fw * 0.32f, h * 0.05f), style = Stroke(w * 0.022f))
            1 -> drawLine(Color(0xFF8E4B44), Offset(fx + fw * 0.37f, my), Offset(fx + fw * 0.63f, my), strokeWidth = w * 0.022f)
            else -> drawOval(Color(0xFF7A3B36), topLeft = Offset(fx + fw * 0.4f, my - h * 0.012f), size = Size(fw * 0.2f, h * 0.035f))
        }
    }
    drawCircle(Color.White.copy(alpha = 0.25f), w / 2, style = Stroke(w * 0.03f))
}
