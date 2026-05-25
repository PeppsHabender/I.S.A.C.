package org.inquest.discord.isac.embed

import org.inquest.analysis.model.Pull
import org.inquest.analysis.model.RunAnalysis
import org.inquest.catalog.IsacDataService
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.shared.collections.isIsacWipe
import org.inquest.shared.numbers.DoubleExtensions.format
import org.inquest.shared.text.appendMono
import org.inquest.shared.text.space

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
                .filter(Pull::isIsacWipe)
                .forEach {
                    append("[")
                    if (it.cm) append("[CM] ")
                    append(isacDataService.shortName(it.eiEncounterId) ?: it.boss)
                    space()
                    appendMono("(${it.remainingHpPercent.format("#.##")}%)")
                    append("](")
                    append(it.link)
                    append(")")
                    append(" | ")
                }
            deleteRange(length - 3, length)
        }.toString()
}
