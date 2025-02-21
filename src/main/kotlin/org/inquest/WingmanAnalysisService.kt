package org.inquest

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.clients.WingmanService
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.Pull

/**
 * Uses the [WingmanService] and provides a detailed dps comparison to the wingman benchmarks.
 */
@ApplicationScoped
class WingmanAnalysisService {
    @Inject
    private lateinit var wingmanService: WingmanService

    fun compareToWingman(bosses: List<Pull>, players: List<PlayerAnalysis>): List<WingmanComparison> = players.mapNotNull { player ->
        val comparisons = compareToBench(bosses, player).filterNot { it.profession == "*" }
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
                null,
                null,
                dpsSum / benchSum,
                dpsSum / bosses.size,
                benchSum / bosses.size,
            ),
            comparisons.first(),
            comparisons.last(),
        )
    }.sortedByDescending { it.average.dps }

    private fun compareToBench(bosses: List<Pull>, player: PlayerAnalysis): List<DpsComparison> = bosses.filter { it.success && it.triggerId != 24485L }.mapNotNull {
        val triggerId = if (it.cm) -it.triggerId else it.triggerId
        val dpsBench = this.wingmanService.bossBench(triggerId) ?: return@mapNotNull null
        val profession = player.pulls[it]?.profession ?: return@mapNotNull null
        val profBench = (if (profession.isCondi) dpsBench.getCondiBench(profession.name) else dpsBench.getPowerBench(profession.name)) ?: return@mapNotNull null

        DpsComparison(
            profession.name,
            profession.isCondi,
            it.link,
            player.pulls[it]!!.dps.toDouble() / profBench,
            player.pulls[it]!!.dps.toDouble(),
            profBench.toDouble(),
        )
    }.sortedBy { it.percent }
}

data class WingmanComparison(
    val player: String,
    val average: DpsComparison,
    val lowest: DpsComparison,
    val highest: DpsComparison,
)

data class DpsComparison(
    val profession: String?,
    val isCondi: Boolean?,
    val log: String?,
    val percent: Double,
    val dps: Double,
    val bench: Double,
)
