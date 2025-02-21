package org.inquest.discord.isac

import discord4j.core.spec.EmbedCreateFields
import discord4j.core.spec.EmbedCreateSpec
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.DpsComparison
import org.inquest.WingmanAnalysisService
import org.inquest.WingmanComparison
import org.inquest.clients.WingmanService
import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.Pull

/**
 * Uses the [wingmanService] and [WingmanAnalysisService] to create an embed which compares the dps performance
 * of all players against the wingman benchmarks of the respective bosses.
 */
@ApplicationScoped
class WingmanEmbed {
    @Inject
    private lateinit var wingmanService: WingmanService

    @Inject
    private lateinit var analysisService: WingmanAnalysisService

    fun createWingmanEmbed(bosses: List<Pull>, players: List<PlayerAnalysis>): EmbedCreateSpec = createEmbed(
        createDescription(
            this.analysisService.compareToWingman(bosses, players),
            players.associate { it.name to it.mostPlayed() },
        ),
        title = "__Dps Performance__",
        color = CustomColors.TRANSPARENT_COLOR,
    ).withFooter(EmbedCreateFields.Footer.of("Please note that there is no comparison for KO, as I.S.A.C. uses cleave dps there...", null))

    private fun createDescription(wingman: List<WingmanComparison>, professions: Map<String, String>) = StringBuilder().apply {
        if (!wingmanService.hasData) {
            appendBold("I.S.A.C. has no wingman data available right now...")
            return@apply
        }

        header()
        appendLine()
        appendLine()

        wingman.forEach {
            createComparison(it, professions)
            appendLine()
            appendLine()
        }

        deleteRange(length - 2, length)
    }.toString()

    private fun StringBuilder.header() {
        appendItalic("Compared to Wingman Top-DPS for Boss and Class.")
        appendLine()
        append("_")
        append("Wingman stats last updated on ")
        append(wingmanService.lastUpdated.toDiscordTimestamp())
        append("_")
    }

    private fun StringBuilder.createComparison(comparison: WingmanComparison, professions: Map<String, String>) {
        appendBold(comparison.player)
        appendLine()
        CustomEmojis.professionEmote(professions[comparison.player])?.let(::append)
        createComparison("Average >>", comparison.average)
        appendLine()
        createComparison("Highest >>", comparison.highest)
        appendLine()
        createComparison("Lowest  >>", comparison.lowest)
    }

    private fun StringBuilder.createComparison(
        title: String,
        comparison: DpsComparison,
    ) {
        comparison.profession?.let {
            CustomEmojis.professionEmote(it)?.let(::append)
        }
        appendMono(title)
        appendBold(" " + (comparison.percent * 100).format("#.#") + " %")

        if (comparison.isCondi == null) {
            space()
        } else if (comparison.isCondi) {
            append(CustomEmojis.CONDI)
        } else {
            append(CustomEmojis.POWER)
        }

        comparison.log?.let { append("[") }
        append("(")
        append((comparison.dps / 1000).format("#.# k"))
        append(" of ")
        append((comparison.bench / 1000).format("#.# k"))
        append(")")
        comparison.log?.let { append("]($it)") }
    }
}
