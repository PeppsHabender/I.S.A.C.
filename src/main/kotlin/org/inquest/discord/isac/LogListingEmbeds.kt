package org.inquest.discord.isac

import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData

/**
 * Contains embeds related to listing all evaluated logs.
 */
object LogListingEmbeds {
    /**
     * Creates the embed for all successful logs.
     */
    fun createSuccessLogsEmbed(
        analysis: RunAnalysis,
        bossData: BossData,
    ) = createEmbed(
        analysis.successLogsStr(bossData),
        CustomEmojis.SUCCESS + "__Success Logs__",
        CustomColors.GREEN_COLOR,
    )

    private fun RunAnalysis.successLogsStr(bossData: BossData) = StringBuilder()
        .apply {
            pulls
                .filter { it.success }
                .forEach {
                    bossData.emoteFor(it.eiEncounterId, it.cm)?.let { emote ->
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
    fun createWipeLogsEmbed(
        analysis: RunAnalysis,
        bossData: BossData,
    ) = createEmbed(
        analysis.wipeLogsStr(bossData),
        CustomEmojis.WIPES + "__Wipe Logs__",
        CustomColors.RED_COLOR,
    )

    private fun RunAnalysis.wipeLogsStr(bossData: BossData) = StringBuilder()
        .apply {
            pulls
                .filter { !it.success }
                .forEach {
                    append("[")
                    if (it.cm) append("[CM] ")
                    append(bossData.shortName(it.eiEncounterId) ?: it.boss)
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
