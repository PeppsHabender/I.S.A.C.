package org.inquest.discord.isac.workflow

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.inquest.discord.isac.shared.CommonOptions.LOGS_OPTION
import org.inquest.discord.support.optionAsString

object AnalyzeRequestParser {
    private val dpsReportRegex =
        Regex("https://(?:[ab]\\.)?dps.report/[\\w-]+(?=\\s*?https|$|\\s)")

    fun extractLogs(event: ChatInputInteractionEvent): List<String> =
        dpsReportRegex.findAll(event.optionAsString(LOGS_OPTION)!!).map { it.value }.toList()
}
