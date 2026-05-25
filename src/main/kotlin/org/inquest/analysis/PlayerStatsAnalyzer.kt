package org.inquest.analysis

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.model.BoonUptimes
import org.inquest.analysis.model.PlayerAnalysis
import org.inquest.analysis.model.PlayerPull
import org.inquest.analysis.model.Profession
import org.inquest.analysis.model.Pull
import org.inquest.catalog.IsacDataService
import org.inquest.integration.dpsreport.dto.JsonLog
import org.inquest.shared.collections.mapWithPutDefault
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import kotlin.math.roundToInt

@ApplicationScoped
class PlayerStatsAnalyzer : WithLogger {
    @Inject
    private lateinit var isacDataService: IsacDataService

    @Inject
    private lateinit var eligiblePlayerSelector: EligiblePlayerSelector

    @Inject
    private lateinit var dpsExtractor: DpsExtractor

    @Inject
    private lateinit var boonSupportAnalyzer: BoonSupportAnalyzer

    fun addPlayerStats(interactionId: String, base: MutableMap<String, PlayerAnalysis>, log: JsonLog, pull: Pull) {
        val boonUptimes: Map<Int, Map<String, MutableList<Double>>> by mapWithPutDefault {
            isacDataService.boonData.mapKeys { it.value.id.toString() }.mapValues { mutableListOf() }
        }

        val dhuumKite = log.mechanics.filter {
            it.name == "Mess Fix" || it.fullName == "Messenger Fixation"
        }.flatMap { it.mechanicsData }
            .groupingBy { it.actor }.eachCount()
            .maxByOrNull { it.value }?.key

        eligiblePlayerSelector.eligiblePlayers(log)
            .map { it to dpsExtractor.fetchDps(it, log.eiEncounterID) }
            .sortedBy { it.second.first }
            .forEachIndexed { i, (player, dps) ->
                LOG.debug("$interactionId: Analyzing stats for player ${player.account}...")
                val group = player.group ?: -1
                val analysis: PlayerAnalysis = base.computeIfAbsent(player.account!!) { PlayerAnalysis(it, mutableMapOf()) }

                val primBoon = boonSupportAnalyzer.primaryBoon(player, log.eiEncounterID, dhuumKite)
                analysis.pulls += pull.link to PlayerPull(
                    Profession(player.profession ?: "*", dps.second),
                    group,
                    dps.first,
                    log.players.size - i - 1,
                    player.extHealingStats?.let { it.outgoingHealing[0].hps } ?: 0,
                    player.extBarrierStats?.let { it.outgoingBarrier[0].bps } ?: 0,
                    player.dpsAll[0].breakbarDamage?.roundToInt() ?: 0,
                    player.support[0].resurrectTime ?: 0.0,
                    player.support[0].condiCleanse?.toInt() ?: 0,
                    player.support[0].boonStrips?.toInt() ?: 0,
                    player.totalDamageTaken[0].mapNotNull { it.totalDamage }.sum(),
                    player.combatReplayData?.down?.size ?: 0,
                    primBoon,
                    // For now.. Just assume that a player is a healer with a somewhat high heal score
                    (primBoon != null && dps.first < 4000) || (player.healing ?: 0) > 7,
                    boonSupportAnalyzer.boonUptimes(player),
                    // Deprecated since wingman skill-saving patch
                    dps.first,
                ).also {
                    isacDataService.boonData.keys.forEach { boon ->
                        val boonId = boon.toString()
                        boonUptimes.getValue(group)[boonId]?.add(it.boonUptimes[boonId] ?: 0.0)
                    }
                }
            }

        base.values.filterNot { a -> log.players.any { it.account == a.name } }.forEach {
            it.pulls[pull.link] = PlayerPull()
        }

        pull.boonUptimes =
            boonUptimes.mapKeys { it.key.toString() }.mapValues { group -> BoonUptimes(group.value.mapValues { it.value.average() }) }
    }
}
