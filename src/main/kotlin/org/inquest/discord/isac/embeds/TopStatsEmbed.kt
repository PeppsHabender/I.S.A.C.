package org.inquest.discord.isac.embeds

import discord4j.rest.util.Color
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.entities.isac.Gw2ToDiscord
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.entities.isac.RunAnalysis
import org.inquest.utils.DoubleExtensions.format
import org.inquest.utils.appendBold
import org.inquest.utils.appendMono
import org.inquest.utils.mention
import org.inquest.utils.space
import kotlin.math.roundToInt

/**
 * The embed which includes all top-stats we currently calculate. Essentially takes the calculated [RunAnalysis] and reformats it into discord md.
 */
object TopStatsEmbed {
    /**
     * @see TopStatsEmbed
     */
    fun createTopStatsEmbed(
        analysis: RunAnalysis,
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
        withHeal: Boolean,
        emoji: String,
        title: String,
        idx: Int,
        color: Color,
    ) = createEmbed(
        StringBuilder().append(createTopStats(gw2ToDiscord, withHeal, analysis, idx)).toString(),
        emoji + "__${title}__",
        color,
    )

    private fun createTopStats(gw2ToDiscord: Map<String, Gw2ToDiscord?>, withHeal: Boolean, analysis: RunAnalysis, idx: Int = 0) =
        StringBuilder()
            .apply {
                val isTop = idx == 0

                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.DPS,
                    "Dps           >>",
                    sortBy = { it.avgDpsPos() },
                    extractor = { it.avgDps() },
                    formatter = { (it / 1000).format("#.# k") },
                    numPrefix = "⌀ ",
                    ascending = true,
                )
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.CC,
                    "Cc            >>",
                    sortBy = { it.cc() },
                )
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.RES_TIME,
                    "Res Time      >>",
                    sortBy = { it.resTime() },
                    formatter = { it.roundToInt() },
                    numSuffix = " s",
                )
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.CONDI_CLEANSE,
                    "Condi Cleanse >>",
                    sortBy = { it.condiCleanse() },
                )
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.BOON_STRIPS,
                    "Boon Strips   >>",
                    sortBy = { it.boonStrips() },
                )
                if (withHeal && analysis.playerStats.any { it.avgHeal() > 0 }) {
                    createTopStat(
                        gw2ToDiscord,
                        analysis,
                        isTop,
                        CustomEmojis.HEAL,
                        "Hps           >>",
                        sortBy = { it.avgHeal() },
                        formatter = { (it / 1000).format("#.# k") },
                    )
                    createTopStat(
                        gw2ToDiscord,
                        analysis,
                        isTop,
                        CustomEmojis.BARRIER,
                        "Bps           >>",
                        sortBy = { it.avgBarrier() },
                        formatter = { (it / 1000).format("#.# k") },
                    )
                }
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.DMG_TAKEN,
                    "Damage Taken  >>",
                    sortBy = { it.damageTaken() },
                    formatter = { (it / 1000.0).roundToInt() },
                    numSuffix = " k",
                )
                createTopStat(
                    gw2ToDiscord,
                    analysis,
                    isTop,
                    CustomEmojis.DOWNSTATES,
                    "Downstates    >>",
                    sortBy = { it.downstates() },
                )
            }.toString()

    private fun <T : Comparable<T>> StringBuilder.createTopStat(
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
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

        if (isTop) {
            val taken = mapped.takeWhile { it == mapped.first() }

            taken.forEachIndexed { i, it ->
                topStatMd(gw2ToDiscord, emoji, title, true, sorted[i].name, numPrefix, formatter(it), numSuffix)
            }
        } else {
            val dropped = mapped.dropWhile { it == mapped.first() }
            sorted = sorted.takeLast(dropped.size)

            dropped
                .takeWhile { it == dropped.first() }
                .forEachIndexed { i, it ->
                    topStatMd(
                        gw2ToDiscord,
                        emoji,
                        title,
                        false,
                        sorted[i].name,
                        numPrefix,
                        formatter(it),
                        numSuffix,
                    )
                }
        }
    }

    private fun StringBuilder.topStatMd(
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
        emoji: String,
        title: String,
        isTop: Boolean,
        player: String,
        numPrefix: String,
        value: Any,
        numSuffix: String,
    ) {
        append(emoji)
        appendMono(title)
        space()
        gw2ToDiscord[player]?.let { mention(it.discordId) } ?: appendBold(player)
        space()
        append("(_")
        append(numPrefix)
        append(value)
        append(numSuffix)
        append("_)")
        appendLine()
    }
}
