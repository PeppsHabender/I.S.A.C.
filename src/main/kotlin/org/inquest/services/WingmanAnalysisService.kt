package org.inquest.services

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.entities.isac.Profession
import org.inquest.entities.isac.Pull
import org.inquest.entities.wingman.BossBench
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import org.inquest.utils.averageOrNull

/**
 * Uses the [WingmanService] and provides a detailed dps comparison to the wingman benchmarks.
 */
@ApplicationScoped
class WingmanAnalysisService : WithLogger {
    @Inject
    private lateinit var wingmanService: WingmanService

    @Inject
    private lateinit var isacDataService: IsacDataService

    fun compareToWingman(
        interactionId: String,
        bosses: List<Pull>,
        players: List<PlayerAnalysis>,
        supports: Boolean,
    ): List<WingmanComparison> = players.also {
        LOG.debug("$interactionId: Starting wingman analysis for ${players.size} players...")
    }.mapNotNull { player ->
        LOG.debug("$interactionId: Analyzing ${player.name} against wingman benchmarks!")
        val comparisons = compareToBench(bosses, player, supports).filterNot { it.profession == "*" }
        if (comparisons.isEmpty()) {
            LOG.debug("$interactionId: Found no possible comparisons for player ${player.name}!")
            return@mapNotNull null
        }

        var dpsSum = 0.0
        var benchSum = 0.0
        comparisons.forEach {
            dpsSum += it.dps
            benchSum += it.bench
        }

        WingmanComparison(
            player.name,
            DpsComparison(
                null,
                false,
                null,
                null,
                null,
                dpsSum / benchSum,
                dpsSum / bosses.filter { it.success }.size,
                benchSum / bosses.filter { it.success }.size,
                null,
                comparisons.mapNotNull { it.boonEmote }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            ),
            comparisons.first(),
            comparisons.last(),
        )
    }.sortedByDescending { it.average.dps }.also {
        LOG.debug("$interactionId: Finished wingman analysis.")
    }

    private fun compareToBench(bosses: List<Pull>, player: PlayerAnalysis, supports: Boolean): List<DpsComparison> = bosses.filter {
        it.success && it.triggerId != 24485L && !isacDataService.ignore(it.eiEncounterId) && !it.embo
    }.mapNotNull { boss ->
        val pull = player.pulls[boss] ?: return@mapNotNull null
        if (pull.maybeHealer || pull.isSupport != supports) return@mapNotNull null

        val triggerId = if (boss.cm) -boss.triggerId else boss.triggerId
        val dpsBench = this.wingmanService.bossBench(triggerId) ?: return@mapNotNull null
        val profession = pull.profession
        val (profBench, benchLog) = professionBench(profession, dpsBench, supports) ?: return@mapNotNull null

        val playerDps = if (!pull.isSupport) {
            null
        } else {
            val isacBoon = this.isacDataService.boonData[pull.boonSupport!!.boon]
            boss.boonUptimes.values.mapNotNull { it[isacBoon] }.averageOrNull()?.let {
                if (pull.boonSupport.generation < 50) {
                    pull.dps * pull.boonSupport.generation / 100
                } else {
                    pull.dps * it / 100
                }
            }
        } ?: pull.dps.toDouble()

        DpsComparison(
            boss.eiEncounterId,
            boss.cm,
            profession.name,
            profession.isCondi,
            boss.link,
            playerDps / profBench,
            playerDps,
            profBench.toDouble(),
            benchLog,
            pull.boonSupport?.let { this.isacDataService.boonData[it.boon]?.emote },
        )
    }.sortedBy { it.percent }

    private fun professionBench(profession: Profession, dpsBench: BossBench, supports: Boolean) = if (supports) {
        dpsBench.getSupportBench(profession.name)
    } else if (profession.isCondi) {
        dpsBench.getCondiBench(profession.name)
    } else {
        dpsBench.getPowerBench(profession.name)
    }
}

data class WingmanComparison(val player: String, val average: DpsComparison, val lowest: DpsComparison, val highest: DpsComparison)

data class DpsComparison(
    val eiEncounterId: Long?,
    val cm: Boolean,
    val profession: String?,
    val isCondi: Boolean?,
    val log: String?,
    val percent: Double,
    val dps: Double,
    val bench: Double,
    val benchLog: String?,
    val boonEmote: String?,
)
