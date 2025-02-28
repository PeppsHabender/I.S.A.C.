package org.inquest.discord.isac

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.spec.EmbedCreateSpec
import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.discord.optionAsString
import org.inquest.discord.toDiscordTimestamp
import org.inquest.entities.isac.Pull
import org.inquest.entities.isac.RunAnalysis
import org.inquest.utils.appendBold
import org.inquest.utils.appendMono
import org.inquest.utils.inRoundedMinutes
import org.inquest.utils.padded
import org.inquest.utils.space
import java.text.NumberFormat
import java.util.Locale

/**
 * The embed for the general overview of all logs. Contains duration information, number of pulls, etc
 */
object OverviewEmbed {
    /**
     * @see OverviewEmbed
     */
    fun createOverviewEmbed(analysis: RunAnalysis, event: ChatInputInteractionEvent): EmbedCreateSpec = createEmbed(
        StringBuilder()
            .append(createTime(analysis))
            .appendLine()
            .append(createPulls(analysis))
            .toString(),
        (event.optionAsString("name") ?: "Run Analysis") +
            " " +
            analysis.start.toDiscordTimestamp(),
        CustomColors.TRANSPARENT_COLOR,
    )

    private fun createTime(analysis: RunAnalysis): String = StringBuilder(CustomEmojis.TIME)
        .apply {
            appendMono("Time  >>")
            space()
            appendBold(analysis.duration.inRoundedMinutes())
            space()
            appendBold("min")
            append(" |")
            append(CustomEmojis.INFIGHT)
            appendBold((analysis.duration - analysis.downtime).inRoundedMinutes())
            space()
            appendBold("min")
            append(" |")
            append(CustomEmojis.DOWNTIME)
            appendBold(analysis.downtime.inRoundedMinutes())
            space()
            appendBold("min")
        }.toString()

    private fun createPulls(analysis: RunAnalysis): String = StringBuilder()
        .apply {
            createPullsAnalysis("Pulls >>", CustomEmojis.PULLS, analysis.pulls)

            val cms = analysis.pulls.filter { it.cm }
            if (cms.isNotEmpty()) {
                appendLine()
                createPullsAnalysis("CMs   >>", CustomEmojis.CM_SUCCESS, cms)
            }

            appendLine()
            append(CustomEmojis.GROUP_DPS)
            appendMono("Dps   >>")
            space()
            appendBold(NumberFormat.getInstance(Locale.GERMAN).format(analysis.groupDps))
        }.toString()

    private fun StringBuilder.createPullsAnalysis(title: String, emoji: String, pulls: List<Pull>) {
        append(emoji)
        appendMono(title)
        space()
        append(' ')
        appendMono(pulls.size.padded(2))
        append(" |")
        append(CustomEmojis.SUCCESS)
        append("Kills: ")
        appendMono(pulls.filter(Pull::success).size.padded(2))
        append(" |")
        append(CustomEmojis.WIPE)
        append("Wipes: ")
        appendMono(pulls.filterNot(Pull::success).size.padded(2))
    }
}
