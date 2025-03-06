package org.inquest.discord

import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.Event
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.interaction.ComponentInteractionEvent
import discord4j.discordjson.json.ApplicationCommandData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import discord4j.rest.service.ApplicationService
import io.quarkus.arc.All
import io.quarkus.runtime.LaunchMode
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Startup
@ApplicationScoped
class DiscordService : WithLogger {
    companion object {
        private val INTENTS = IntentSet.of(
            Intent.GUILD_MESSAGES,
            Intent.GUILD_MEMBERS,
            Intent.MESSAGE_CONTENT,
        )
    }

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
        LOG.info("Registering event listeners...")
        eventListeners
            .map { it as EventListener<Event> }
            .forEach { listener ->
                if (listener is InteractionEventListener) {
                    on(listener.eventType)
                        .filter { it is ComponentInteractionEvent && it.customId == listener.handlesId }
                        .flatMap { listener.execute(it) }
                        .onErrorResume { listener.handleError(it) }
                        .subscribe()
                } else {
                    on(listener.eventType)
                        .flatMap { e -> listener.execute(e) }
                        .onErrorResume { listener.handleError(it) }
                        .subscribe()
                }

                LOG.info("Registering event listener ${listener::class.qualifiedName}")
            }
        LOG.info("Registered event listeners.")
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
                group.updateDiscordCommands(this, cmdModifier)
            } else {
                group.deleteUnusedCommands(this, cmdDeleter).then(Mono.empty())
            }
        }.collectList().createNewCommands(this, cmdCreator).then()

    private fun Flux<ApplicationCommandData>.updateDiscordCommands(
        gatewayDiscordClient: GatewayDiscordClient,
        cmdModifier: ApplicationService.(Long, ApplicationCommandRequest) -> Mono<ApplicationCommandData>,
    ) = doFirst {
        LOG.info("Updating previous slash commands...")
    }.flatMap { discCmd ->
        gatewayDiscordClient.restClient.applicationService.cmdModifier(
            discCmd.id().asLong(),
            commands.first { cmd ->
                discCmd.name() == cmd.devName()
            }.build(gatewayDiscordClient),
        ).doOnSuccess {
            LOG.info("Updated /{}.", it.name())
        }
    }.doOnComplete {
        LOG.info("Successfully updated slash commands.")
    }

    private fun Flux<ApplicationCommandData>.deleteUnusedCommands(
        gatewayDiscordClient: GatewayDiscordClient,
        cmdDeleter: ApplicationService.(Long) -> Mono<Void>,
    ) = doFirst {
        LOG.info("Removing unused slash commands...")
    }.flatMap { discCmd ->
        gatewayDiscordClient.restClient.applicationService.cmdDeleter(discCmd.id().asLong()).doOnSuccess {
            LOG.info("Deleted /{}.", discCmd.name())
        }
    }.doOnComplete { LOG.info("Removed unused slash commands.") }

    private fun GatewayDiscordClient.installCommandHandlers() {
        on(ChatInputInteractionEvent::class.java) { e ->
            commands.firstOrNull { it.devName() == e.commandName }?.handle(e) ?: Mono.empty()
        }.subscribe()
    }

    private fun Mono<List<ApplicationCommandData>>.createNewCommands(
        gatewayDiscordClient: GatewayDiscordClient,
        cmdCreator: ApplicationService.(ApplicationCommandRequest) -> Mono<ApplicationCommandData>,
    ) = flatMap { discCmds ->
        commands.filterNot { cmd ->
            cmd.devName() in discCmds.map { it.name() }
        }.let { cmds ->
            if (cmds.isEmpty()) {
                return@let Mono.empty()
            }

            Flux.fromIterable(cmds).doOnSubscribe {
                LOG.info("Creating new slash commands...")
            }.flatMap { cmd ->
                gatewayDiscordClient
                    .restClient
                    .applicationService
                    .cmdCreator(cmd.build(gatewayDiscordClient))
                    .doOnSuccess { LOG.info("Created /{}.", it.name()) }
            }.collectList().doOnSuccess {
                LOG.info("Successfully created new slash commands.")
            }
        }
    }

    private fun ApplicationCommandRequest.addDevName() = ApplicationCommandRequest.builder()
        .from(this).name(name().devName()).build()

    private fun CommandListener.devName() = name.devName()

    private fun String.devName() = if (LaunchMode.current().isDevOrTest) "${this}_test" else this
}
