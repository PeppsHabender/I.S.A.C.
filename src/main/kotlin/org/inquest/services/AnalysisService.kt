package org.inquest.services

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.isac.BoonSupport
import org.inquest.entities.isac.BoonUptimes
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.entities.isac.PlayerPull
import org.inquest.entities.isac.Profession
import org.inquest.entities.isac.Pull
import org.inquest.entities.isac.RunAnalysis
import org.inquest.entities.logs.JsonActorParent
import org.inquest.entities.logs.JsonLog
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import org.inquest.utils.endTime
import org.inquest.utils.mapWithPutDefault
import org.inquest.utils.startTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

@ApplicationScoped
class AnalysisService : WithLogger {
    @Inject
    private lateinit var isacDataService: IsacDataService

    fun analyze(interactionId: String, logs: List<Pair<String, JsonLog>>): RunAnalysis = logs
        .sortedBy { it.second.startTime() }
        .let { sorted ->
            LOG.debug("$interactionId: Starting analysis for ${logs.size} logs...")

            val start: OffsetDateTime = sorted.first().second.startTime()
            var end: OffsetDateTime? = null
            var downtime: Duration = Duration.ZERO
            val pulls: MutableList<Pull> = mutableListOf()
            val groupDps: MutableList<Int> = mutableListOf()
            val playerStats: MutableMap<String, PlayerAnalysis> = mutableMapOf()

            for ((link, log) in sorted) {
                LOG.debug("$interactionId: Analyzing $link...")
                downtime += if (end == null) {
                    Duration.ZERO
                } else {
                    java.time.Duration.between(end, log.startTime()).toKotlinDuration()
                }
                end = log.endTime()

                val logDuration =
                    java.time.Duration.between(log.startTime(), log.endTime())
                        .toKotlinDuration()
                val targetAlive = log.targets.firstOrNull { it.finalHealth != 0 }
                pulls += Pull(
                    log.eiEncounterID ?: -1,
                    log.triggerID ?: -1,
                    log.fightName ?: "Unknown",
                    link,
                    log.success,
                    log.isCM,
                    log.isEmbo(),
                    logDuration.inWholeMilliseconds,
                    (targetAlive?.finalHealth ?: 0) /
                        (targetAlive?.totalHealth ?: 1).toDouble() * 100,
                )

                if (!log.success) {
                    LOG.debug("$interactionId: $link wasn't successful, skipping player analysis.")
                    downtime += logDuration
                    continue
                } else if (this.isacDataService.ignore(log.eiEncounterID)) {
                    LOG.debug("$interactionId: $link contains an ignored encounter, skipping player analysis.")
                    continue
                }

                groupDps += log.players.groupDps(log.eiEncounterID)

                LOG.debug("$interactionId: Analyzing player stats for $link...")
                addPlayerStats(interactionId, playerStats, log, pulls.last())
                LOG.debug("$interactionId: Analyzed player stats for $link.")
            }

            val duration: Duration = java.time.Duration.between(start, end).toKotlinDuration()

            RunAnalysis(
                start,
                end!!,
                downtime.inWholeMilliseconds,
                duration.inWholeMilliseconds,
                pulls,
                groupDps.average().roundToInt(),
                playerStats.values.toList(),
            )
        }.also {
            LOG.debug("$interactionId: Finished analysis.")
        }

    private fun List<JsonActorParent.JsonPlayer>.groupDps(bossId: Long?) = groupBy({ it }) { player ->
        player.fetchDps(bossId).first
    }.mapValues { it.value.sum() }
        .entries
        .let { players -> players.sumOf { it.value } }

    private fun addPlayerStats(interactionId: String, base: MutableMap<String, PlayerAnalysis>, log: JsonLog, pull: Pull) {
        val boonUptimes: Map<Int, Map<String, MutableList<Double>>> by mapWithPutDefault {
            this.isacDataService.boonData.mapKeys { it.value.id.toString() }.mapValues { mutableListOf() }
        }

        val dhuumKite = log.mechanics.filter {
            it.name == "Mess Fix" || it.fullName == "Messenger Fixation"
        }.flatMap { it.mechanicsData }
            .groupingBy { it.actor }.eachCount()
            .maxByOrNull { it.value }?.key

        log.players.filter {
            it.account != null && it.friendlyNPC == false && it.isFake == false && !it.isProbHk(log)
        }.map {
            it to it.fetchDps(log.eiEncounterID)
        }.sortedBy { it.second.first }.forEachIndexed { i, (player, dps) ->
            LOG.debug("$interactionId: Analyzing stats for player ${player.account}...")
            val group = player.group ?: -1
            val analysis: PlayerAnalysis = base.computeIfAbsent(player.account!!) { PlayerAnalysis(it, mutableMapOf()) }

            val primBoon = player.primaryBoon(log.eiEncounterID, dhuumKite)
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
                player.boonUptimes(),
                player.fetchTargetDps(),
            ).also {
                it.boonUptimes.forEach { (boon, upt) -> boonUptimes.getValue(group)[boon]?.add(upt) }
            }
        }

        base.values.filterNot { a -> log.players.any { it.account == a.name } }.forEach {
            it.pulls[pull.link] = PlayerPull()
        }

        pull.boonUptimes =
            boonUptimes.mapKeys { it.key.toString() }.mapValues { group -> BoonUptimes(group.value.mapValues { it.value.average() }) }
    }

    private fun JsonActorParent.JsonPlayer.isProbHk(log: JsonLog) = if (log.eiEncounterID != 132100L) {
        false
    } else {
        (log.players.groupBy { it.group }[this.group]?.size ?: 5) < 2
    }

    private fun JsonActorParent.JsonPlayer.boonUptimes(): Map<String, Double> = this.buffUptimes.filter {
        it.id in isacDataService.boonData && it.buffData[0].uptime != null
    }.associate {
        it.id!!.toString() to it.buffData[0].uptime!!
    }

    private fun JsonActorParent.JsonPlayer.primaryBoon(encounterId: Long?, dhuumKite: String?): BoonSupport? = this.groupBuffs.filter {
        isacDataService.boonData[it.id]?.isPrimary ?: false
    }.mapNotNull { boon ->
        boon.id?.let { id -> boon.buffData.firstOrNull()?.generation?.let { BoonSupport(id, it) } }
    }.filter { (_, gen) ->
        if (dhuumKite == account) {
            // I hate dhuum kites
            gen > 3
        } else if (isacDataService.ignoreForBoons(encounterId)) {
            gen > 20
        } else {
            gen > 40
        }
    }.maxByOrNull { it.generation }

    private fun JsonLog.isEmbo(): Boolean = (
        this.presentInstanceBuffs.firstOrNull {
            it[0] == 68087L
        }?.get(1) ?: 0
        ) > 0

    private fun JsonActorParent.JsonPlayer.fetchDps(bossId: Long?): Pair<Int, Boolean> = this.dpsTargets
        .slice(isacDataService.targets(bossId))
        .mapNotNull { dpsTargets ->
            dpsTargets[0].condiDps?.let { condi -> dpsTargets[0].powerDps?.let { condi to it } }
            // dpsTargets.slice(bossData.phases(bossId)).mapNotNull { it.dps }
        }.reduce { p1, p2 -> (p1.first + p2.first) to (p1.second + p2.second) }.let {
            (it.first + it.second) to (it.first > it.second)
        }

    private fun JsonActorParent.JsonPlayer.fetchTargetDps(): Int = this.dpsTargets[0][0].dps ?: 0
}
