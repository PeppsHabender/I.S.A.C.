package org.inquest.discord.isac.embeds

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import org.inquest.discord.CustomColors
import org.inquest.discord.withEmbed
import org.inquest.discord.withFile
import org.inquest.utils.errorLog
import org.slf4j.Logger
import reactor.core.publisher.Mono

/**
 * Contains all embeds related to error handling. Usually includes the stacktrace as an attachment
 */
object ErrorEmbeds {
    const val NO_LOGS_EXC_MSG = "**Are you sure there even was any dps to analyze? Because I.S.A.C. could not " +
        "extract any logs from your input!**\nDid you include 'https' in your dps.report links?"
    const val FETCHING_EXC_MSG = "**It looks like there was an error downloading your logs.. Maybe dps.report is down?**"
    const val ANALYZE_EXC_MSG = "**Oups.. Looks like your dps might have been too low, I.S.A.C. failed to analyze your logs!**" +
        "\nMaybe this could help..."
    const val ANALYZE_WM_EXC_MSG = "**Oups.. Looks like wingman isnt too nice to us!**\nMaybe this could help..."
    const val ANALYZE_BOONS_EXC_MSG = "**Failed to analyze your boons...**"
    const val CHANNEL_CONFIG_EXC_MSG = "**I.S.A.C. failed to update/view channel configuration!**"

    /**
     * Raises an exception on this interaction, by creating an error embed.
     */
    fun <T> ChatInputInteractionEvent.raiseException(
        logger: Logger,
        message: String,
        exc: Throwable? = null,
        withStackTrace: Boolean = false,
    ): Mono<T> {
        val editMono = editReply().withEmbed(message, color = CustomColors.RED_COLOR)

        return if (withStackTrace && exc != null) {
            editMono.withFile("stacktrace.log", exc.also(Throwable::printStackTrace).stackTraceToString())
        } else {
            editMono
        }.errorLog(logger, message, exc).then(Mono.empty())
    }
}
