package org.inquest.discord

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import io.quarkus.runtime.LaunchMode
import jakarta.inject.Inject
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

/**
 * Base class for listening to discord sent events.
 */
interface EventListener<T : Event> : WithLogger {
    /**
     * Type of the event to listen to
     */
    val eventType: Class<T>

    /**
     * @return true if this listener wants to handle [event], false otherwise
     */
    fun wantsToHandle(event: T): Boolean

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

abstract class InteractionEventListener<T : ComponentInteractionEvent> : EventListener<T> {
    @Inject
    private lateinit var discordSettings: DiscordSettings

    abstract val handlesId: String

    final override fun wantsToHandle(event: T): Boolean = if (LaunchMode.current().isDevOrTest) {
        event.interaction.data.guildId().toOptional().map { it.asLong() }.getOrNull() == this.discordSettings.guildId()
    } else {
        true
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
