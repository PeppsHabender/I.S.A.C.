package org.inquest.discord

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import reactor.core.publisher.Mono

/**
 * Base class for listening to discord sent events.
 */
interface EventListener<T : Event> : WithLogger {
    /**
     * Type of the event to listen to
     */
    val eventType: Class<T>

    /**
     * Executed when the event was received.
     */
    fun execute(event: T): Mono<Void>

    /**
     * Executed in case of an error in [execute]
     */
    fun handleError(error: Throwable?): Mono<Void> {
        LOG.error("Unable to process " + eventType.simpleName, error)
        return Mono.empty()
    }
}

interface InteractionEventListener<T : ComponentInteractionEvent> : EventListener<T> {
    val handlesId: String
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
