package org.inquest.discord.isac.embed

import discord4j.core.spec.EmbedCreateSpec
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.DpsComparison
import org.inquest.analysis.WingmanAnalysisService
import org.inquest.analysis.WingmanComparison
import org.inquest.analysis.model.PlayerAnalysis
import org.inquest.analysis.model.Pull
import org.inquest.catalog.IsacDataService
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.discord.support.toDiscordTimestamp
import org.inquest.integration.wingman.WingmanService
import org.inquest.persistence.mongo.Gw2ToDiscord
import org.inquest.shared.numbers.DoubleExtensions.format
import org.inquest.shared.text.appendBold
import org.inquest.shared.text.appendItalic
import org.inquest.shared.text.appendMono
import org.inquest.shared.text.mention
import org.inquest.shared.text.space

/**
 * Uses the [wingmanService] and [WingmanAnalysisService] to create an embed which compares the dps performance
 * of all players against the wingman benchmarks of the respective bosses.
 */
@ApplicationScoped
class WingmanEmbed {
    companion object {
        private const val TITLE_FMT = "${CustomEmojis.DPS}__%sDps Performance__"
        private const val HEADER_FMT = "Compared to Wingman %sTop-Dps for Boss and Class."
        private const val NO_DATA = "I.S.A.C. has no wingman data available right now..."
    }

    @Inject
    private lateinit var wingmanService: WingmanService

    @Inject
    private lateinit var analysisService: WingmanAnalysisService

    @Inject
    private lateinit var isacDataService: IsacDataService

    fun createWingmanEmbed(
        interactionId: String,
        bosses: List<Pull>,
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
        players: List<PlayerAnalysis>,
        supports: Boolean = false,
    ): EmbedCreateSpec = createEmbed(
        createDescription(
            gw2ToDiscord,
            this.analysisService.compareToWingman(interactionId, bosses, players, supports),
            players.associate { it.name to it.mostPlayed(supports) },
            supports,
        ),
        title = TITLE_FMT.format(if (supports)"Support " else ""),
        color = CustomColors.TRANSPARENT_COLOR,
    )

    private fun createDescription(
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
        wingman: List<WingmanComparison>,
        professions: Map<String, String>,
        supports: Boolean,
    ) = StringBuilder().apply {
        if (!wingmanService.hasData) {
            appendBold(NO_DATA)
            return@apply
        }

        header(supports)
        appendLine()
        appendLine()

        wingman.forEach {
            createComparison(gw2ToDiscord, it, professions)
            appendLine()
            appendLine()
        }

        deleteRange(length - 2, length)
    }.toString()

    private fun StringBuilder.header(supports: Boolean) {
        appendItalic(HEADER_FMT.format(if (supports) "Support-" else ""))
        appendLine()
        append("_")
        append("Wingman stats last updated on ")
        append(wingmanService.lastUpdated.toDiscordTimestamp())
        append("_")
    }

    private fun StringBuilder.createComparison(
        gw2ToDiscord: Map<String, Gw2ToDiscord?>,
        comparison: WingmanComparison,
        professions: Map<String, String>,
    ) {
        gw2ToDiscord[comparison.player]?.let { mention(it.discordId) } ?: appendBold(comparison.player)
        appendLine()
        CustomEmojis.professionEmote(professions[comparison.player])?.let(::append)
        createComparison("Average >>", comparison.average)
        appendLine()
        createComparison("Highest >>", comparison.highest)
        appendLine()
        createComparison("Lowest  >>", comparison.lowest)
    }

    private fun StringBuilder.createComparison(title: String, comparison: DpsComparison) {
        comparison.profession?.let {
            CustomEmojis.professionEmote(it)?.let(::append)
        }
        appendMono(title)
        comparison.boonEmote?.let(::append) ?: space()
        appendBold((comparison.percent * 100).format("#.#") + " %")

        if (comparison.isCondi == null) {
            space()
        } else if (comparison.isCondi) {
            append(CustomEmojis.CONDI)
        } else {
            append(CustomEmojis.POWER)
        }

        append("(")
        append((comparison.dps / 1000).format("#.# k"))
        append(" of ")
        comparison.benchLog?.let { append("[") }
        append((comparison.bench / 1000).format("#.# k"))
        comparison.benchLog?.let { append("]($it)") }
        append(")")
        comparison.eiEncounterId?.let {
            space()
            isacDataService.emoteFor(it, comparison.cm)?.let(::append)
        }
    }
}
