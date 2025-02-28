package org.inquest.discord.isac

import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.entities.isac.RunAnalysis
import org.inquest.services.IsacDataService
import org.inquest.utils.appendMono
import org.inquest.utils.format
import org.inquest.utils.space

/**
 * Contains embeds related to listing all evaluated logs.
 */
object LogListingEmbeds {
    /**
     * Creates the embed for all successful logs.
     */
    fun createSuccessLogsEmbed(analysis: RunAnalysis, isacDataService: IsacDataService) = createEmbed(
        analysis.successLogsStr(isacDataService),
        CustomEmojis.SUCCESS + "__Success Logs__",
        CustomColors.GREEN_COLOR,
    )

    private fun RunAnalysis.successLogsStr(isacDataService: IsacDataService) = StringBuilder()
        .apply {
            pulls
                .filter { it.success }
                .forEach {
                    isacDataService.emoteFor(it.eiEncounterId, it.cm)?.let { emote ->
                        append("$emote ")
                    }

                    append("[")
                    append(it.boss.replace("CM", "[CM]"))
                    space()
                    appendMono("[${it.duration}]")
                    append("](")
                    append(it.link)
                    append(")")
                    appendLine()
                }
        }.toString()

    /**
     * Creates the embed for all wipes.
     */
    fun createWipeLogsEmbed(analysis: RunAnalysis, isacDataService: IsacDataService) = createEmbed(
        analysis.wipeLogsStr(isacDataService),
        CustomEmojis.WIPES + "__Wipe Logs__",
        CustomColors.RED_COLOR,
    )

    private fun RunAnalysis.wipeLogsStr(isacDataService: IsacDataService) = StringBuilder()
        .apply {
            pulls
                .filter { !it.success }
                .forEach {
                    append("[")
                    if (it.cm) append("[CM] ")
                    append(isacDataService.shortName(it.eiEncounterId) ?: it.boss)
                    space()
                    appendMono("(${(it.remainingHp * 100).format("#.##")}%)")
                    append("](")
                    append(it.link)
                    append(")")
                    append(" | ")
                }
            deleteRange(length - 3, length)
        }.toString()
}
