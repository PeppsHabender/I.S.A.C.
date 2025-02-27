package org.inquest.discord

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.service.ApplicationService
import io.quarkus.arc.All
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.Startup
import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.inquest.utils.LogExtension.LOG
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
            updateCommands(
                { getGuildApplicationCommands(settings.applicationId(), settings.guildId()) },
                { id, req -> modifyGuildApplicationCommand(settings.applicationId(), settings.guildId(), id, req.addDevName()) },
                { deleteGuildApplicationCommand(settings.applicationId(), settings.guildId(), it) },
                { createGuildApplicationCommand(settings.applicationId(), settings.guildId(), it.addDevName()) },
            ).subscribe()
        }

        if (!LaunchMode.current().isDevOrTest) {
            updateCommands(
                { getGlobalApplicationCommands(settings.applicationId()) },
                { id, req -> modifyGlobalApplicationCommand(settings.applicationId(), id, req) },
                { deleteGlobalApplicationCommand(settings.applicationId(), it) },
                { createGlobalApplicationCommand(settings.applicationId(), it) },
            ).subscribe()
        }
    }

    private fun GatewayDiscordClient.updateCommands(
        cmdGetter: ApplicationService.() -> Flux<ApplicationCommandData>,
        cmdModifier: ApplicationService.(Long, ApplicationCommandRequest) -> Mono<ApplicationCommandData>,
        cmdDeleter: ApplicationService.(Long) -> Mono<Void>,
        cmdCreator: ApplicationService.(ApplicationCommandRequest) -> Mono<ApplicationCommandData>,
    ): Mono<Void> = this.restClient
        .applicationService
        .cmdGetter()
        .groupBy { discCmd ->
            commands.any { discCmd.name() == it.devName() }
        }
        .flatMap { group ->
            if (group.key()) {
                group.doFirst {
                    LOG.info("Updating previous slash commands...")
                }.flatMap { discCmd ->
                    this.restClient.applicationService.cmdModifier(
                        discCmd.id().asLong(),
                        commands.first { cmd ->
                            discCmd.name() == cmd.devName()
                        }.build(this),
                    ).doOnSuccess {
                        LOG.info("Updated /{}.", it.name())
                    }
                }.doOnComplete {
                    LOG.info("Successfully updated slash commands.")
                }
            } else {
                group.doFirst {
                    LOG.info("Removing unused slash commands...")
                }.flatMap { discCmd ->
                    this.restClient.applicationService.cmdDeleter(discCmd.id().asLong()).doOnSuccess {
                        LOG.info("Deleted /{}.", discCmd.name())
                    }
                }.doOnComplete { LOG.info("Removed unused slash commands.") }.then(Mono.empty())
            }
        }.collectList().flatMap { discCmds ->
            commands.filterNot { cmd ->
                cmd.devName() in discCmds.map { it.name() }
            }.let { cmds ->
                if (cmds.isEmpty()) {
                    return@let Mono.empty()
                }

                Flux.fromIterable(cmds).doOnSubscribe {
                    LOG.info("Creating new slash commands...")
                }.flatMap { cmd ->
                    this.restClient.applicationService.cmdCreator(cmd.build(this)).doOnSuccess { LOG.info("Created /{}.", it.name()) }
                }.collectList().doOnSuccess {
                    LOG.info("Successfully created new slash commands.")
                }
            }
        }.then()

    private fun GatewayDiscordClient.installCommandHandlers() {
        on(ChatInputInteractionEvent::class.java) { e ->
            commands.firstOrNull { it.devName() == e.commandName }?.handle(e) ?: Mono.empty()
        }.subscribe()
    }

    private fun ApplicationCommandRequest.addDevName() = ApplicationCommandRequest.builder()
        .from(this).name(this.name().devName()).build()

    private fun CommandListener.devName() = name.devName()

    private fun String.devName() = if (LaunchMode.current().isDevOrTest) "${this}_test" else this

    companion object {
        private val INTENTS =
            IntentSet.of(Intent.GUILD_MESSAGES, Intent.GUILD_MEMBERS, Intent.MESSAGE_CONTENT)
    }
}
