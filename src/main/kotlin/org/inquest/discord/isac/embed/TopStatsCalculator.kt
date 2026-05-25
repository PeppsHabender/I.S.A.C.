package org.inquest.discord.isac.embed

import org.inquest.analysis.model.PlayerAnalysis
import org.inquest.analysis.model.RunAnalysis
import org.inquest.discord.support.CustomEmojis
import org.inquest.shared.numbers.DoubleExtensions.format
import kotlin.math.roundToInt

object TopStatsCalculator {
    fun calculate(analysis: RunAnalysis, withHeal: Boolean, idx: Int = 0): List<TopStatLine> = buildList {
        val isTop = idx == 0

        addTopStat(
            analysis,
            isTop,
            CustomEmojis.DPS,
            "Dps           >>",
            sortBy = { it.avgDpsPos() },
            extractor = { it.avgDps() },
            formatter = { (it / 1000).format("#.# k") },
            numPrefix = "\u2300 ",
            ascending = true,
        )
        addTopStat(analysis, isTop, CustomEmojis.CC, "Cc            >>", sortBy = { it.cc() })
        addTopStat(
            analysis,
            isTop,
            CustomEmojis.RES_TIME,
            "Res Time      >>",
            sortBy = { it.resTime() },
            formatter = { it.roundToInt() },
            numSuffix = " s",
        )
        addTopStat(analysis, isTop, CustomEmojis.CONDI_CLEANSE, "Condi Cleanse >>", sortBy = { it.condiCleanse() })
        addTopStat(analysis, isTop, CustomEmojis.BOON_STRIPS, "Boon Strips   >>", sortBy = { it.boonStrips() })
        if (withHeal && analysis.playerStats.any { it.avgHeal() > 0 }) {
            addTopStat(
                analysis,
                isTop,
                CustomEmojis.HEAL,
                "Hps           >>",
                sortBy = { it.avgHeal() },
                formatter = { (it / 1000).format("#.# k") },
            )
            addTopStat(
                analysis,
                isTop,
                CustomEmojis.BARRIER,
                "Bps           >>",
                sortBy = { it.avgBarrier() },
                formatter = { (it / 1000).format("#.# k") },
            )
        }
        addTopStat(
            analysis,
            isTop,
            CustomEmojis.DMG_TAKEN,
            "Damage Taken  >>",
            sortBy = { it.damageTaken() },
            formatter = { (it / 1000.0).roundToInt() },
            numSuffix = " k",
        )
        addTopStat(analysis, isTop, CustomEmojis.DOWNSTATES, "Downstates    >>", sortBy = { it.downstates() })
    }

    private fun <T : Comparable<T>> MutableList<TopStatLine>.addTopStat(
        analysis: RunAnalysis,
        isTop: Boolean,
        emoji: String,
        title: String,
        sortBy: (PlayerAnalysis) -> T,
        extractor: (PlayerAnalysis) -> T = sortBy,
        formatter: (T) -> Any = { it },
        numPrefix: String = "",
        numSuffix: String = "",
        ascending: Boolean = false,
    ) {
        var sorted =
            analysis.playerStats
                .sortedByDescending { sortBy(it) }
                .let { if (ascending) it.reversed() else it }
        val mapped = sorted.map { extractor(it) }
        if (mapped.isEmpty()) return

        if (isTop) {
            val taken = mapped.takeWhile { it == mapped.first() }

            taken.forEachIndexed { i, it ->
                add(TopStatLine(emoji, title, sorted[i].name, numPrefix, formatter(it), numSuffix))
            }
        } else {
            val dropped = mapped.dropWhile { it == mapped.first() }
            if (dropped.isEmpty()) return
            sorted = sorted.takeLast(dropped.size)

            dropped
                .takeWhile { it == dropped.first() }
                .forEachIndexed { i, it ->
                    add(TopStatLine(emoji, title, sorted[i].name, numPrefix, formatter(it), numSuffix))
                }
        }
    }
}

data class TopStatLine(
    val emoji: String,
    val title: String,
    val player: String,
    val numPrefix: String,
    val value: Any,
    val numSuffix: String,
)
