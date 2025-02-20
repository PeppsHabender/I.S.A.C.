package org.inquest.discord

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

/**
 * Base class for listening to discord sent events.
 */
interface EventListener<T : Event> {
    /**
     * Type of the event to listen to
     */
    val eventType: Class<T>

    /**
     * Executed when the event was received
     */
    fun execute(event: T): Mono<Void>

    /**
     * Executed in case of an error in [execute]
     */
    fun handleError(error: Throwable?): Mono<Void> {
        LOG.error("Unable to process " + eventType.simpleName, error)
        return Mono.empty()
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(EventListener::class.java)
    }
}

/**
 * Base class for registering slash commands.
 */
interface CommandListener {
    /**
     * Name of the command
     */
    val name: String

    /**
     * Builds the command relative to the given [gatewayClient]
     */
    fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest

    /**
     * Executes when a user calls this command.
     */
    fun handle(event: ChatInputInteractionEvent): Mono<Void>
}
