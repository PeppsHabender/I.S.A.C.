package org.inquest

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.JsonActorParent
import org.inquest.entities.JsonLog
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.PlayerPull
import org.inquest.entities.Profession
import org.inquest.entities.Pull
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData
import org.inquest.utils.endTime
import org.inquest.utils.startTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

@ApplicationScoped
class AnalysisService {
    @Inject private lateinit var bossData: BossData

    fun analyze(logs: List<Pair<String, JsonLog>>): RunAnalysis = logs
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
                    if (end == null) {
                        Duration.ZERO
                    } else {
                        java.time.Duration.between(end, log.startTime()).toKotlinDuration()
                    }
                end = log.endTime()

                val logDuration =
                    java.time.Duration.between(log.startTime(), log.endTime())
                        .toKotlinDuration()
                val targetAlive = log.targets.firstOrNull { it.finalHealth != 0 }
                pulls +=
                    Pull(
                        log.eiEncounterID ?: -1,
                        log.triggerID ?: -1,
                        log.fightName ?: "Unknown",
                        link,
                        log.success,
                        log.isCM,
                        log.isEmbo(),
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

                addPlayerStats(playerStats, log, pulls.last())
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

    private fun List<JsonActorParent.JsonPlayer>.groupDps(bossId: Long?) = groupBy({ it }) { player ->
        player.fetchDps(bossId).first
    }.mapValues { it.value.sum() }
        .entries
        .let { players -> players.sumOf { it.value } }

    private fun addPlayerStats(base: MutableMap<String, PlayerAnalysis>, log: JsonLog, pull: Pull) {
        log.players.filterNot { it.account == null }.map {
            it to it.fetchDps(log.eiEncounterID)
        }.sortedBy { it.second.first }.forEachIndexed { i, (player, dps) ->
            val analysis: PlayerAnalysis = base.computeIfAbsent(player.account!!, ::PlayerAnalysis)

            analysis.pulls += pull to PlayerPull(
                Profession(player.profession ?: "*", dps.second),
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
            )
        }

        base.values.filterNot { a -> log.players.any { it.account == a.name } }.forEach {
            it.pulls[pull] = PlayerPull()
        }
    }

    private fun JsonLog.isEmbo(): Boolean = (
        this.presentInstanceBuffs.firstOrNull {
            it[0] == 68087L
        }?.get(1) ?: 0
        ) > 0

    private fun JsonActorParent.JsonPlayer.fetchDps(bossId: Long?): Pair<Int, Boolean> = this.dpsTargets
        .slice(bossData.targets(bossId))
        .mapNotNull { dpsTargets ->
            dpsTargets[0].condiDps?.let { condi -> dpsTargets[0].powerDps?.let { condi to it } }
            // dpsTargets.slice(bossData.phases(bossId)).mapNotNull { it.dps }
        }.reduce { p1, p2 -> (p1.first + p2.first) to (p1.second + p2.second) }.let {
            (it.first + it.second) to (it.first > it.second)
        }
}
