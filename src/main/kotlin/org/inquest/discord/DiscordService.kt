package org.inquest.discord

import discord4j.core.DiscordClientBuilder
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import io.quarkus.arc.All
import io.quarkus.runtime.Startup
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.context.ManagedExecutor
import reactor.core.publisher.Mono

@ConfigMapping(prefix = "discord")
interface DiscordSettings {
    @WithName("token") fun token(): String

    @WithName("application-id") fun applicationId(): Long
}

@Startup
@ApplicationScoped
class DiscordConfiguration {

    @Inject private lateinit var settings: DiscordSettings

    @Inject private lateinit var executor: ManagedExecutor

    @All @Inject lateinit var eventListeners: MutableList<EventListener<*>>

    @All @Inject lateinit var commands: MutableList<CommandListener>

    @PostConstruct
    private fun gatewayClient() {
        val discordClient =
            DiscordClientBuilder.create(settings.token())
                .build()
                .gateway()
                .setEnabledIntents(INTENTS)
                .login()
                .block()!!

        eventListeners
            .map { it as EventListener<Event> }
            .map {
                discordClient
                    .on(it.eventType)
                    .flatMap { e -> it.execute(e) }
                    .onErrorResume { e -> it.handleError(e) }
                    .subscribe()
            }

        commands.forEach {
            discordClient.restClient.applicationService
                .createGlobalApplicationCommand(this.settings.applicationId(), it.build())
                .subscribe()
            // discordClient.restClient.applicationService.createGuildApplicationCommand(this.settings.applicationId(),
            // guild-id, it.build()).subscribe()
        }

        discordClient
            .on(ChatInputInteractionEvent::class.java) { e ->
                commands.firstOrNull { it.name == e.commandName }?.handle(e) ?: Mono.empty()
            }
            .subscribe()
    }

    companion object {
        private val INTENTS =
            IntentSet.of(Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS, Intent.MESSAGE_CONTENT)
    }
}
