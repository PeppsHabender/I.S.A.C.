package org.inquest.analysis

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.catalog.IsacDataService
import org.inquest.integration.dpsreport.dto.JsonActorParent

@ApplicationScoped
class DpsExtractor {
    @Inject
    private lateinit var isacDataService: IsacDataService

    fun groupDps(players: List<JsonActorParent.JsonPlayer>, bossId: Long?): Int = players
        .groupBy({ it }) { player -> fetchDps(player, bossId).first }
        .mapValues { it.value.sum() }
        .entries
        .sumOf { it.value }

    fun fetchDps(player: JsonActorParent.JsonPlayer, bossId: Long?): Pair<Int, Boolean> = player.dpsTargets
        .slice(isacDataService.targets(bossId).filter { it < player.dpsTargets.size }.ifEmpty { player.dpsTargets.indices })
        .map { dpsTarget ->
            val phases = isacDataService.phases(bossId).filter { it < dpsTarget.size }.ifEmpty { listOf(0) }
            val condi = dpsTarget.slice(phases).sumOf { it.condiDps ?: 0 }
            val power = dpsTarget.slice(phases).sumOf { it.powerDps ?: 0 }

            condi to power
        }.reduce { (con1, pow1), (con2, pow2) -> (con1 + con2) to (pow1 + pow2) }.let { (con, pow) ->
            (con + pow) to (con > pow)
        }
}
