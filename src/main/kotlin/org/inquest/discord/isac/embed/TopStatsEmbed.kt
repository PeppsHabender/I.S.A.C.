package org.inquest.discord.isac.embed

import discord4j.rest.util.Color
import org.inquest.analysis.model.RunAnalysis
import org.inquest.discord.support.createEmbed
import org.inquest.persistence.mongo.Gw2ToDiscord
import org.inquest.shared.text.appendBold
import org.inquest.shared.text.appendMono
import org.inquest.shared.text.mention
import org.inquest.shared.text.space

/**
 * Formats calculated top stats into a Discord embed.
 */
object TopStatsEmbed {
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
        StringBuilder().apply {
            TopStatsCalculator.calculate(analysis, withHeal, idx).forEach {
                topStatMd(gw2ToDiscord, it)
            }
        }.toString()

    private fun StringBuilder.topStatMd(gw2ToDiscord: Map<String, Gw2ToDiscord?>, line: TopStatLine) {
        append(line.emoji)
        appendMono(line.title)
        space()
        gw2ToDiscord[line.player]?.let { mention(it.discordId) } ?: appendBold(line.player)
        space()
        append("(_")
        append(line.numPrefix)
        append(line.value)
        append(line.numSuffix)
        append("_)")
        appendLine()
    }
}
