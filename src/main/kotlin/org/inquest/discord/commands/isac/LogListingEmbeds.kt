package org.inquest.discord.commands.isac

import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.entities.isac.RunAnalysis
import org.inquest.services.IsacDataService
import org.inquest.utils.DoubleExtensions.format
import org.inquest.utils.appendMono
import org.inquest.utils.space
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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
                .filter { !it.success && it.duration > 20.0.toDuration(DurationUnit.SECONDS) && it.remainingHpPercent < 95 }
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
