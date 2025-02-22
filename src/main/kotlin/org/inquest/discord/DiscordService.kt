package org.inquest.discord

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import io.quarkus.arc.All
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.Startup
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@ConfigMapping(prefix = "discord")
interface DiscordSettings {
    @WithName("token")
    fun token(): String

    @WithName("application-id")
    fun applicationId(): Long

    @ConfigProperty(name = "guild-id", defaultValue = "-1")
    fun guildId(): Long
}

@Startup
@ApplicationScoped
class DiscordService {
    @Inject
    private lateinit var settings: DiscordSettings

    @All
    @Inject
    lateinit var eventListeners: MutableList<EventListener<*>>

    @All
    @Inject
    lateinit var commands: MutableList<CommandListener>

    @PostConstruct
    fun initDiscordBot() {
        createDiscordClient().apply {
            installEventListeners()
            installSlashCommands()
            installCommandHandlers()
        }
    }

    private fun createDiscordClient() = DiscordClientBuilder
        .create(settings.token())
        .build()
        .gateway()
        .setEnabledIntents(INTENTS)
        .login()
        .block()!!

    private fun GatewayDiscordClient.installEventListeners() {
        eventListeners
            .map { it as EventListener<Event> }
            .map {
                on(it.eventType)
                    .flatMap { e -> it.execute(e) }
                    .onErrorResume { e -> it.handleError(e) }
                    .subscribe()
            }
    }

    private fun GatewayDiscordClient.installSlashCommands() {
        if (settings.guildId() != -1L) {
            deleteOldGuildCommands().then(Flux.fromIterable(commands).flatMap { registerGuildCommand(it) }.then()).subscribe()
        }

        if (!LaunchMode.current().isDevOrTest) {
            deleteOldGlobalCommands().then(Flux.fromIterable(commands).map { registerGlobalCommand(it) }.then()).subscribe()
        }
    }

    private fun GatewayDiscordClient.deleteOldGuildCommands(): Mono<Void> = commands.map { it.devName() }.toSet().let { cmds ->
        this.restClient
            .applicationService
            .getGuildApplicationCommands(settings.applicationId(), settings.guildId())
            .filter { it.name() !in cmds }
            .map { it.id() }
            .flatMap {
                println(it)
                this.restClient
                    .applicationService
                    .deleteGuildApplicationCommand(settings.applicationId(), settings.guildId(), it.asLong())
            }.then()
    }

    private fun GatewayDiscordClient.registerGuildCommand(cmd: CommandListener): Mono<Void> = this.restClient
        .applicationService
        .createGuildApplicationCommand(
            settings.applicationId(),
            settings.guildId(),
            ApplicationCommandRequest.builder()
                .from(cmd.build(this))
                .name(cmd.devName())
                .build(),
        ).then()

    private fun GatewayDiscordClient.deleteOldGlobalCommands(): Mono<Void> = commands.map { it.name }.toSet().let { cmds ->
        this.restClient
            .applicationService
            .getGlobalApplicationCommands(settings.applicationId())
            .filter { it.name() !in cmds }
            .map { it.id() }
            .flatMap {
                this.restClient
                    .applicationService
                    .deleteGlobalApplicationCommand(settings.applicationId(), it.asLong())
            }.then()
    }

    private fun GatewayDiscordClient.registerGlobalCommand(cmd: CommandListener): Mono<Void> = this.restClient
        .applicationService
        .createGlobalApplicationCommand(settings.applicationId(), cmd.build(this))
        .then()

    private fun GatewayDiscordClient.installCommandHandlers() {
        on(ChatInputInteractionEvent::class.java) { e ->
            commands.firstOrNull { it.devName() == e.commandName }?.handle(e) ?: Mono.empty()
        }.subscribe()
    }

    private fun CommandListener.devName() = name.devName()

    private fun String.devName() = if (LaunchMode.current().isDevOrTest) "${this}_test" else this

    companion object {
        private val INTENTS =
            IntentSet.of(Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS, Intent.MESSAGE_CONTENT)
    }
}
