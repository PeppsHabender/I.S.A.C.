package org.inquest

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.clients.WingmanService
import org.inquest.entities.BossBench
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.Profession
import org.inquest.entities.Pull
import org.inquest.utils.IsacData
import org.inquest.utils.averageOrNull

/**
 * Uses the [WingmanService] and provides a detailed dps comparison to the wingman benchmarks.
 */
@ApplicationScoped
class WingmanAnalysisService {
    @Inject
    private lateinit var wingmanService: WingmanService

    @Inject
    private lateinit var isacData: IsacData

    fun compareToWingman(bosses: List<Pull>, players: List<PlayerAnalysis>, supports: Boolean): List<WingmanComparison> =
        players.mapNotNull { player ->
            val comparisons = compareToBench(bosses, player, supports).filterNot { it.profession == "*" }
            if (comparisons.isEmpty()) return@mapNotNull null

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
                    comparisons.mapNotNull { it.boonEmote }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
                ),
                comparisons.first(),
                comparisons.last(),
            )
        }.sortedByDescending { it.average.dps }

    private fun compareToBench(bosses: List<Pull>, player: PlayerAnalysis, supports: Boolean): List<DpsComparison> = bosses.filter {
        it.success && it.triggerId != 24485L && !isacData.ignore(it.eiEncounterId) && !it.embo
    }.mapNotNull { boss ->
        val pull = player.pulls[boss]
        if (pull?.maybeHealer == true || (pull?.boonSupport == null) == supports) return@mapNotNull null

        val triggerId = if (boss.cm) -boss.triggerId else boss.triggerId
        val dpsBench = this.wingmanService.bossBench(triggerId) ?: return@mapNotNull null
        val profession = pull?.profession ?: return@mapNotNull null
        val profBench = professionBench(profession, dpsBench, supports) ?: return@mapNotNull null

        val playerDps = if (pull.boonSupport == null) {
            null
        } else {
            val isacBoon = this.isacData.boonData[pull.boonSupport.boon]
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
            pull.boonSupport?.let { this.isacData.boonData[it.boon]?.emote },
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
    val boonEmote: String?,
)
