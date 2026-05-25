package org.inquest.analysis

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.model.BoonSupport
import org.inquest.catalog.IsacDataService
import org.inquest.integration.dpsreport.dto.JsonActorParent

@ApplicationScoped
class BoonSupportAnalyzer {
    @Inject
    private lateinit var isacDataService: IsacDataService

    fun boonUptimes(player: JsonActorParent.JsonPlayer): Map<String, Double> = player.buffUptimes.filter {
        it.id in isacDataService.boonData && it.buffData[0].uptime != null
    }.associate {
        it.id!!.toString() to it.buffData[0].uptime!!
    }

    fun primaryBoon(player: JsonActorParent.JsonPlayer, encounterId: Long?, dhuumKite: String?): BoonSupport? = player.groupBuffs.filter {
        isacDataService.boonData[it.id]?.isPrimary ?: false
    }.mapNotNull { boon ->
        boon.id?.let { id -> boon.buffData.firstOrNull()?.generation?.let { BoonSupport(id, it) } }
    }.filter { (_, gen) ->
        if (dhuumKite == player.account) {
            // I hate dhuum kites
            gen > 3
        } else if (isacDataService.ignoreForBoons(encounterId)) {
            gen > 20
        } else {
            gen > 40
        }
    }.maxByOrNull { it.generation }
}
