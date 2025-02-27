package org.inquest.discord.isac

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.inquest.discord.CustomColors
import org.inquest.discord.createEmbed
import org.inquest.discord.withEmbed
import org.inquest.discord.withFile
import reactor.core.publisher.Mono

/**
 * Contains all embeds related to error handling. Usually includes the stacktrace as an attachment
 */
object ErrorEmbeds {
    private const val NO_LOGS_EXC_MSG = "**Are you sure there even was any dps to analyze? Because I.S.A.C. could not " +
        "extract any logs from your input!**\nDid you include 'https' in your dps.report links?"
    private const val FETCHING_EXC_MSG = "**It looks like there was an error downloading your logs.. Maybe dps.report is down?**"
    private const val ANALYZE_EXC_MSG = "**Oups.. Looks like your dps might have been too low, I.S.A.C. failed to analyze your logs!**" +
        "\nMaybe this could help..."
    private const val ANALYZE_WM_EXC_MSG = "**Oups.. Looks like wingman isnt too nice to us!**\nMaybe this could help..."
    private const val ANALYZE_BOONS_EXC_MSG = "**Failed to analyze your boons...**"

    /**
     * Embed to be shown when we didn't find any logs.
     */
    fun <T> ChatInputInteractionEvent.noLogsException(): Mono<T> = editReply()
        .withEmbed(NO_LOGS_EXC_MSG, color = CustomColors.RED_COLOR)
        .then(Mono.empty())

    /**
     * Embed to be shown when there was an error fetching logs from dps.report.
     */
    fun <T> ChatInputInteractionEvent.handleFetchingException(): Mono<T> = editReply()
        .withEmbed(FETCHING_EXC_MSG, color = CustomColors.RED_COLOR)
        .then(Mono.empty())

    /**
     * Embed to be shown when there was an error analyzing the users logs.
     */
    fun <T> ChatInputInteractionEvent.handleAnalyzeException(ex: Throwable): Mono<T> = editReply()
        .withEmbed(ANALYZE_EXC_MSG, color = CustomColors.RED_COLOR)
        .withFile("stacktrace.log", ex.also(Throwable::printStackTrace).stackTraceToString())
        .then(Mono.empty())

    /**
     * Embed to be shown when there was an error when comparing to wingman.
     */
    fun analyzeWmException() = createEmbed(ANALYZE_WM_EXC_MSG, color = CustomColors.RED_COLOR)

    /**
     * Embed to be shown when there was an error when comparing to wingman.
     */
    fun analyzeBoonsException() = createEmbed(ANALYZE_BOONS_EXC_MSG, color = CustomColors.RED_COLOR)
}
