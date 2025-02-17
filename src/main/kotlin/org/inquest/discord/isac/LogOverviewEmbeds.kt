package org.inquest.discord.isac

import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData

object LogOverviewEmbeds {
    fun createSuccessLogsEmbed(analysis: RunAnalysis, bossData: BossData) =
        EmbedCreateSpec.builder()
            .title(CustomEmojis.SUCCESS + "__Success Logs__")
            .description(
                StringBuilder()
                    .apply {
                        analysis.pulls
                            .filter { it.success }
                            .forEach {
                                bossData.emoteFor(it.bossId, it.cm)?.let { emote ->
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
                    }
                    .toString()
            )
            .color(Color.of(0, 148, 0))
            .build()

    fun createWipeLogsEmbed(analysis: RunAnalysis, bossData: BossData) =
        EmbedCreateSpec.builder()
            .title(CustomEmojis.WIPES + "__Wipe Logs__")
            .description(
                StringBuilder()
                    .apply {
                        analysis.pulls
                            .filter { !it.success }
                            .forEach {
                                append("[")
                                if (it.cm) append("[CM] ")
                                append(bossData.shortName(it.bossId) ?: it.boss)
                                space()
                                appendMono("(${(it.remainingHp * 100).format("#.##")}%)")
                                append("](")
                                append(it.link)
                                append(")")
                                append(" | ")
                            }
                        deleteRange(length - 3, length)
                    }
                    .toString()
            )
            .color(Color.of(129, 0, 0))
            .build()
}
