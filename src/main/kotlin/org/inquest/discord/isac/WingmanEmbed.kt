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
import org.inquest.utils.IsacData

/**
 * Uses the [wingmanService] and [WingmanAnalysisService] to create an embed which compares the dps performance
 * of all players against the wingman benchmarks of the respective bosses.
 */
@ApplicationScoped
class WingmanEmbed {
    companion object {
        private const val TITLE_FMT = "${CustomEmojis.DPS}__%sDps Performance__"
        private const val HEADER_FMT = "Compared to Wingman %sTop-Dps for Boss and Class."
        private const val FOOTER = "Please note that there is no comparison for KO, as I.S.A.C. uses cleave dps there..."
        private const val NO_DATA = "I.S.A.C. has no wingman data available right now..."
    }

    @Inject
    private lateinit var wingmanService: WingmanService

    @Inject
    private lateinit var analysisService: WingmanAnalysisService

    @Inject
    private lateinit var isacData: IsacData

    fun createWingmanEmbed(bosses: List<Pull>, players: List<PlayerAnalysis>, supports: Boolean = false): EmbedCreateSpec = createEmbed(
        createDescription(
            this.analysisService.compareToWingman(bosses, players, supports),
            players.associate { it.name to it.mostPlayed() },
            supports,
        ),
        title = TITLE_FMT.format(if (supports)"Support " else ""),
        color = CustomColors.TRANSPARENT_COLOR,
    ).withFooter(EmbedCreateFields.Footer.of(FOOTER, null))

    private fun createDescription(wingman: List<WingmanComparison>, professions: Map<String, String>, supports: Boolean) =
        StringBuilder().apply {
            if (!wingmanService.hasData) {
                appendBold(NO_DATA)
                return@apply
            }

            header(supports)
            appendLine()
            appendLine()

            wingman.forEach {
                createComparison(it, professions)
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

        comparison.log?.let { append("[") }
        append("(")
        append((comparison.dps / 1000).format("#.# k"))
        append(" of ")
        append((comparison.bench / 1000).format("#.# k"))
        append(")")
        comparison.log?.let { append("]($it)") }
        comparison.eiEncounterId?.let {
            space()
            isacData.emoteFor(it, comparison.cm)?.let(::append)
        }
    }
}
