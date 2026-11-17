package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*

/** Teknik ekip yönetimi. */
object Staffing {
    fun q(club: Club, role: StaffRole): Int = club.staff.firstOrNull { it.role == role }?.quality ?: 40

    fun effect(role: StaffRole): String = when (role) {
        StaffRole.ASSISTANT -> "Maç içi takım performansı ve genel oyuncu gelişimi"
        StaffRole.GK_COACH -> "Kalecilerin gelişim hızı"
        StaffRole.FITNESS -> "Antrenman sakatlık riskini azaltır"
        StaffRole.SCOUT -> "Potansiyel tahmini isabeti"
        StaffRole.PHYSIO -> "Sakatlık sürelerini kısaltır"
        StaffRole.YOUTH_COACH -> "Akademi oyuncularının gelişimi"
    }

    /** Havuzdan teknik ekip üyesi işe al (aynı roldeki mevcut kişi havuza döner, tazminat ödenir). */
    fun hire(gs: GameState, staffId: Int): String {
        val club = gs.userClub
        val s = gs.staffPool.firstOrNull { it.id == staffId } ?: return "Kişi bulunamadı."
        val signFee = s.weeklyWage * 4
        if (club.finances.balance < signFee) return "Kasada yeterli para yok."
        val old = club.staff.firstOrNull { it.role == s.role }
        if (old != null) {
            Economy.addLedger(gs, club, "Tazminat: ${old.name}", -old.weeklyWage * 8)
            club.staff.remove(old); gs.staffPool.add(old)
        }
        gs.staffPool.remove(s); club.staff.add(s)
        Economy.addLedger(gs, club, "İmza ücreti: ${s.name}", -signFee)
        return "${s.name} ${s.role.tr} olarak göreve başladı."
    }
}
