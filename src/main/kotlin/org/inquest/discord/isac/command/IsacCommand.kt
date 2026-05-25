package org.inquest.discord.isac.command

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.discord.core.CommandListener
import org.inquest.discord.isac.shared.CommonOptions
import org.inquest.discord.isac.shared.CommonOptions.LOGS_OPTION
import org.inquest.discord.isac.shared.interactionId
import org.inquest.discord.isac.workflow.AnalyzeResponseRenderer
import org.inquest.discord.isac.workflow.AnalyzeWorkflow
import org.inquest.discord.support.stringOption
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

/**
 * Slash command entry point for analyzing dps.report links.
 */
@ApplicationScoped
class IsacCommand :
    CommandListener,
    WithLogger {
    override val name: String = "analyze"

    @Inject
    private lateinit var workflow: AnalyzeWorkflow

    @Inject
    private lateinit var responseRenderer: AnalyzeResponseRenderer

    override fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest = ApplicationCommandRequest
        .builder()
        .name(name)
        .description("Analyzes the given list of dps.report links")
        .addOption(stringOption(LOGS_OPTION, "The list of logs to be analyzed, ideally separated by white spaces."))
        .addAllOptions(CommonOptions.DEFAULT_OPTIONS)
        .build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        val interactionId = interactionId()

        return event.deferReply()
            .doOnSubscribe { logRequest(interactionId, event) }
            .then(workflow.prepare(interactionId, event))
            .flatMap { responseRenderer.render(interactionId, event, it) }
    }

    private fun logRequest(interactionId: String, event: ChatInputInteractionEvent) {
        var guild: String? = null
        var channel: String? = null

        try {
            guild = event.interaction.guild.map { it.name }.blockOptional().getOrNull()
        } catch (_: Throwable) {}
        try {
            channel = event.interaction.channel.map { it.data.name().get() }.blockOptional().getOrNull()
        } catch (_: Throwable) {}

        LOG.info(
            "$interactionId: Received analyze request from [{}] in channel [{}]...",
            guild ?: "Unknown Guild",
            channel ?: "Unknown Channel",
        )
    }
}
