package com.futbolmenajer.engine

import com.futbolmenajer.data.model.*
import kotlin.random.Random

/** Basın toplantıları: cevaplar oyuncu morali, taraftar ve yönetim güvenini etkiler. */
object Press {

    fun preMatch(gs: GameState, fx: Fixture, rnd: Random): PressConference {
        val user = gs.userClub
        val oppId = if (fx.homeId == user.id) fx.awayId else fx.homeId
        val opp = gs.clubName(oppId)
        val oppRep = gs.clubs[oppId]?.reputation ?: 50
        val stronger = oppRep > user.reputation + 4
        val comp = gs.competitions[fx.competitionId]?.name ?: ""
        val questions = if (stronger) listOf(
            "$opp kağıt üzerinde sizden güçlü görünüyor. Bu maçtan beklentiniz nedir?",
            "$opp karşısında favori değilsiniz. Takımınız bu baskıyla nasıl başa çıkacak?"
        ) else listOf(
            "$opp maçında favori sizsiniz. Rakibinizi hafife alma riski var mı?",
            "Taraftarlar $opp karşısında net bir galibiyet bekliyor. Ne söylersiniz?",
            "$comp maçı öncesi takımın durumu hakkında ne düşünüyorsunuz?"
        )
        val q = questions.random(rnd)
        val options = listOf(
            PressOption("Kazanmak için sahaya çıkacağız, kimseden korkumuz yok.", 4, 4, 1, "Menajer iddialı konuştu."),
            PressOption("Rakibimize saygı duyuyoruz, maç maç ilerliyoruz.", 1, 0, 2, "Menajer temkinli ve saygılı bir dil kullandı."),
            PressOption("Oyuncularıma tamamen güveniyorum, en iyi performanslarını verecekler.", 6, 1, 0, "Menajer oyuncularının arkasında durdu."),
            PressOption("Yorum yapmak istemiyorum.", -2, -3, -1, "Menajer soruları geçiştirdi.")
        )
        return PressConference(q, options, "Maç öncesi basın toplantısı – $opp")
    }

    fun postMatch(gs: GameState, fx: Fixture, ug: Int, og: Int, rnd: Random): PressConference {
        val user = gs.userClub
        val opp = gs.clubName(if (fx.homeId == user.id) fx.awayId else fx.homeId)
        return when {
            ug > og -> PressConference(
                listOf("$opp karşısında $ug-$og'lik galibiyet. Takımınızın performansını nasıl değerlendiriyorsunuz?", "Bu galibiyetin sırrı neydi?").random(rnd),
                listOf(
                    PressOption("Oyuncularım muhteşemdi, hepsini tebrik ediyorum.", 5, 2, 1, "Menajer oyuncularını övdü."),
                    PressOption("Sonuç güzel ama hâlâ geliştirmemiz gereken çok şey var.", 1, 0, 3, "Menajer ayakları yere basan bir açıklama yaptı."),
                    PressOption("Taktiğimiz kusursuz işledi.", 0, 2, 2, "Menajer taktik başarısının altını çizdi."),
                    PressOption("Hakem bile bizi durduramazdı!", 2, 5, -2, "Menajerin hakem göndermesi tartışma yarattı.")
                ), "Maç sonu basın toplantısı – $opp ($ug-$og)")
            ug < og -> PressConference(
                listOf("$opp karşısında $ug-$og'lik mağlubiyet. Neler yanlış gitti?", "Bu yenilgi sonrası taraftarlara mesajınız nedir?").random(rnd),
                listOf(
                    PressOption("Sorumluluk tamamen bende, takımımı koruyacağım.", 4, 2, 0, "Menajer sorumluluğu üstlendi."),
                    PressOption("Bazı oyuncular beklentimin çok altındaydı.", -6, 1, 1, "Menajer oyuncularını eleştirdi."),
                    PressOption("Hakem kararları maçın kaderini belirledi.", 1, 3, -3, "Menajer hakemi suçladı; federasyon inceleme başlatabilir."),
                    PressOption("Toparlanacağız, bir sonraki maça odaklanıyoruz.", 2, 0, 2, "Menajer ileriye bakmayı tercih etti.")
                ), "Maç sonu basın toplantısı – $opp ($ug-$og)")
            else -> PressConference(
                "$opp ile $ug-$ug berabere kaldınız. Bu sonucu nasıl yorumluyorsunuz?",
                listOf(
                    PressOption("Önemli bir puan aldık.", 2, 0, 1, "Menajer beraberlikten memnun."),
                    PressOption("Kazanmamız gereken bir maçtı, hayal kırıklığı yaşıyoruz.", -1, 2, 1, "Menajer hırsını gösterdi."),
                    PressOption("Oyuncularımın mücadelesinden gururluyum.", 4, 1, 0, "Menajer oyuncularına sahip çıktı.")
                ), "Maç sonu basın toplantısı – $opp ($ug-$og)")
        }
    }

    fun answer(gs: GameState, option: PressOption): String {
        val club = gs.userClub
        for (id in club.playerIds) gs.players[id]?.let { it.morale = (it.morale + option.moraleDelta).coerceIn(5, 100) }
        club.fanMood = (club.fanMood + option.fanDelta).coerceIn(0, 100)
        club.boardConfidence = (club.boardConfidence + option.boardDelta).coerceIn(0, 100)
        gs.pendingPress = null
        GameEngine.msg(gs, MsgType.MEDIA, "Basın: ${option.mediaNote}", "\"${option.text}\"\n\nOyuncu morali ${sign(option.moraleDelta)}, taraftar ${sign(option.fanDelta)}, yönetim ${sign(option.boardDelta)}")
        return option.mediaNote
    }

    private fun sign(v: Int) = if (v >= 0) "+$v" else "$v"
}
