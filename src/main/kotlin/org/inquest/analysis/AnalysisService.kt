package org.inquest.analysis

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.model.PlayerAnalysis
import org.inquest.analysis.model.Pull
import org.inquest.analysis.model.RunAnalysis
import org.inquest.catalog.IsacDataService
import org.inquest.integration.dpsreport.dto.JsonLog
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import org.inquest.shared.time.endTime
import org.inquest.shared.time.startTime
import java.time.OffsetDateTime
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

@ApplicationScoped
class AnalysisService : WithLogger {
    @Inject
    private lateinit var isacDataService: IsacDataService

    @Inject
    private lateinit var dpsExtractor: DpsExtractor

    @Inject
    private lateinit var playerStatsAnalyzer: PlayerStatsAnalyzer

    fun analyze(interactionId: String, logs: List<Pair<String, JsonLog>>): RunAnalysis = logs
        .sortedBy { it.second.startTime() }
        .let { sorted ->
            LOG.debug("$interactionId: Starting analysis for ${logs.size} logs...")
            analyzeSortedLogs(interactionId, sorted)
        }.also {
            LOG.debug("$interactionId: Finished analysis.")
        }

    private fun analyzeSortedLogs(interactionId: String, sortedLogs: List<Pair<String, JsonLog>>): RunAnalysis {
        val start: OffsetDateTime = sortedLogs.first().second.startTime()
        var end: OffsetDateTime? = null
        var downtime: Duration = Duration.ZERO
        val pulls: MutableList<Pull> = mutableListOf()
        val groupDps: MutableList<Int> = mutableListOf()
        val playerStats: MutableMap<String, PlayerAnalysis> = mutableMapOf()

        for ((link, log) in sortedLogs) {
            LOG.debug("$interactionId: Analyzing $link...")
            downtime += downtimeSincePreviousPull(end, log)
            end = log.endTime()

            val logDuration = java.time.Duration.between(log.startTime(), log.endTime()).toKotlinDuration()
            pulls += createPull(link, log, logDuration)

            if (!log.success) {
                LOG.debug("$interactionId: $link wasn't successful, skipping player analysis.")
                downtime += logDuration
                continue
            } else if (isacDataService.ignore(log.eiEncounterID)) {
                LOG.debug("$interactionId: $link contains an ignored encounter, skipping player analysis.")
                continue
            }

            groupDps += dpsExtractor.groupDps(log.players, log.eiEncounterID)

            LOG.debug("$interactionId: Analyzing player stats for $link...")
            playerStatsAnalyzer.addPlayerStats(interactionId, playerStats, log, pulls.last())
            LOG.debug("$interactionId: Analyzed player stats for $link.")
        }

        val duration = java.time.Duration.between(start, end).toKotlinDuration()

        return RunAnalysis(
            start,
            end!!,
            downtime.inWholeMilliseconds,
            duration.inWholeMilliseconds,
            pulls,
            groupDps.averageOrZero().roundToInt(),
            playerStats.values.toList(),
        )
    }

    private fun downtimeSincePreviousPull(previousEnd: OffsetDateTime?, log: JsonLog): Duration = if (previousEnd == null) {
        Duration.ZERO
    } else {
        java.time.Duration.between(previousEnd, log.startTime()).toKotlinDuration()
    }

    private fun createPull(link: String, log: JsonLog, logDuration: Duration): Pull {
        val targetAlive = log.targets.firstOrNull { it.finalHealth != 0 }
        return Pull(
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
    }

    private fun JsonLog.isEmbo(): Boolean = (
        this.presentInstanceBuffs.firstOrNull {
            it[0] == 68087L
        }?.get(1) ?: 0
        ) > 0

    private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
}
