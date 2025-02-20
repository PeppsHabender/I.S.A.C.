package org.inquest.discord.isac

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateFields
import org.inquest.discord.CustomColors
import org.inquest.discord.withEmbed
import org.inquest.discord.withFile
import reactor.core.publisher.Mono

object ErrorEmbed {
    const val FETCHING_EXC_MSG = "**It looks like there was an error downloading your logs.. Maybe dps.report is down?**";
    const val ANALYZE_EXC_MSG = "**Oups.. Looks like your dps might have been too low, I.S.A.C. failed to analyze your logs!**\nMaybe this could help..."

    fun <T> ChatInputInteractionEvent.handleFetchingException(): Mono<T> = editReply()
        .withEmbed(FETCHING_EXC_MSG, color = CustomColors.RED_COLOR)
        .then(Mono.empty())

    fun <T> ChatInputInteractionEvent.handleAnalyzeException(ex: Throwable): Mono<T> = editReply()
        .withEmbed(ANALYZE_EXC_MSG, color = CustomColors.RED_COLOR)
        .withFile("stacktrace.log", ex.stackTraceToString())
        .then(Mono.empty())
}
