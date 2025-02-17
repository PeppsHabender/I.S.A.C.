package org.inquest

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.OffsetDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.toKotlinDuration
import org.inquest.entities.JsonActorParent
import org.inquest.entities.JsonLog
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.Pull
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData
import org.inquest.utils.endTime
import org.inquest.utils.startTime

@ApplicationScoped
class AnalyzerService {
    @Inject private lateinit var bossData: BossData

    fun analyze(logs: List<Pair<String, JsonLog>>): RunAnalysis =
        logs
            .sortedBy { it.second.startTime() }
            .let { sorted ->
                val start: OffsetDateTime = sorted.first().second.startTime()
                var end: OffsetDateTime? = null
                var downtime: Duration = Duration.ZERO
                val pulls: MutableList<Pull> = mutableListOf()
                val groupDps: MutableList<Int> = mutableListOf()
                val playerStats: MutableMap<String, PlayerAnalysis> = mutableMapOf()

                for ((link, log) in sorted) {
                    downtime +=
                        if (end == null) Duration.ZERO
                        else java.time.Duration.between(end, log.startTime()).toKotlinDuration()
                    end = log.endTime()

                    val logDuration =
                        java.time.Duration.between(log.startTime(), log.endTime())
                            .toKotlinDuration()
                    val targetAlive = log.targets.firstOrNull { it.finalHealth != 0 }
                    pulls +=
                        Pull(
                            log.eiEncounterID ?: -1,
                            log.fightName ?: "Unknown",
                            link,
                            log.success,
                            log.isCM,
                            logDuration,
                            (targetAlive?.finalHealth ?: 0) /
                                (targetAlive?.totalHealth ?: 1).toDouble(),
                        )
                    if (!log.success) {
                        downtime += logDuration
                        continue
                    } else if (this.bossData.ignore(log.eiEncounterID)) {
                        continue
                    }

                    groupDps += log.players.groupDps(log.eiEncounterID)

                    addPlayerStats(playerStats, log)
                }

                val duration: Duration = java.time.Duration.between(start, end).toKotlinDuration()

                RunAnalysis(
                    start,
                    end!!,
                    downtime,
                    duration,
                    pulls,
                    groupDps.average().roundToInt(),
                    playerStats.values.toList(),
                )
            }

    private fun List<JsonActorParent.JsonPlayer>.groupDps(bossId: Long?) =
        groupBy({ it }) { player -> player.fetchDps(bossId) }
            .mapValues { it.value.sum() }
            .entries
            .let { players -> players.sumOf { it.value } }

    private fun addPlayerStats(base: MutableMap<String, PlayerAnalysis>, log: JsonLog) {
        val players: MutableMap<Int, PlayerAnalysis> = mutableMapOf()
        for (player in log.players) {
            if (player.account == null) continue

            val analysis: PlayerAnalysis = base.computeIfAbsent(player.account, ::PlayerAnalysis)

            val dps = player.fetchDps(log.eiEncounterID)
            analysis.withDps(dps)
            players += dps to analysis
            analysis.cc += player.dpsAll[0].breakbarDamage?.roundToInt() ?: 0

            val healing: Int = player.extHealingStats?.let { it.outgoingHealing[0].hps } ?: 0
            val barrier: Int = player.extBarrierStats?.let { it.outgoingBarrier[0].bps } ?: 0
            analysis.withHeal(healing, barrier)

            analysis.resTime += player.support[0].resurrectTime ?: 0.0
            analysis.condiCleanse += player.support[0].condiCleanse?.toInt() ?: 0
            analysis.boonStrips += player.support[0].boonStrips?.toInt() ?: 0
            analysis.damageTaken += player.totalDamageTaken[0].mapNotNull { it.totalDamage }.sum()
            analysis.downstates += player.combatReplayData?.down?.size ?: 0
        }

        players.entries
            .sortedByDescending { it.key }
            .forEachIndexed { i, (_, v) -> v.withDpsPos(i) }

        for (analysis in base.values.filterNot { a -> log.players.any { it.account == a.name } }) {
            analysis.withDps(0)
            analysis.withDpsPos(11)
            analysis.withHeal(0, 0)
        }
    }

    private fun JsonActorParent.JsonPlayer.fetchDps(bossId: Long?) =
        this.dpsTargets
            .slice(bossData.targets(bossId))
            .mapNotNull { dpsTargets ->
                dpsTargets[0].dps
                // dpsTargets.slice(bossData.phases(bossId)).mapNotNull { it.dps }
            }
            .sum()
}
