package org.inquest.discord.isac.embed

import discord4j.core.spec.EmbedCreateSpec
import org.inquest.analysis.model.Pull
import org.inquest.analysis.model.RunAnalysis
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.discord.support.toDiscordTimestamp
import org.inquest.shared.collections.isIsacWipe
import org.inquest.shared.numbers.IntExtensions.padded
import org.inquest.shared.text.appendBold
import org.inquest.shared.text.appendMono
import org.inquest.shared.text.space
import org.inquest.shared.time.inRoundedMinutes
import java.text.NumberFormat
import java.util.Locale

/**
 * The embed for the general overview of all logs. Contains duration information, number of pulls, etc
 */
object OverviewEmbed {
    /**
     * @see OverviewEmbed
     */
    fun createOverviewEmbed(analysis: RunAnalysis, name: String): EmbedCreateSpec = createEmbed(
        StringBuilder()
            .append(createTime(analysis))
            .appendLine()
            .append(createPulls(analysis))
            .toString(),
        name + " " + analysis.start.toDiscordTimestamp(),
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
        appendMono(pulls.filter(Pull::isIsacWipe).size.padded(2))
    }
}
