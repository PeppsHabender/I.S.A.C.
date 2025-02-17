package org.inquest.discord.isac

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Color
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.stream.Stream
import kotlin.streams.asStream
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.AnalyzerService
import org.inquest.clients.DpsReportClient
import org.inquest.discord.CommandListener
import org.inquest.discord.isac.LogOverviewEmbeds.createSuccessLogsEmbed
import org.inquest.discord.isac.LogOverviewEmbeds.createWipeLogsEmbed
import org.inquest.discord.isac.OverviewEmbed.createOverviewEmbed
import org.inquest.discord.isac.TopStatsEmbed.createTopStatsEmbed
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData
import org.inquest.utils.startTime
import org.inquest.utils.toMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@ApplicationScoped
class IsacCommand : CommandListener {
    override val name: String = "analyze"

    @RestClient private lateinit var dpsReportClient: DpsReportClient

    @Inject private lateinit var analyzerService: AnalyzerService

    @Inject private lateinit var bossData: BossData

    @Inject private lateinit var managedExecutor: ManagedExecutor

    override fun build(): ApplicationCommandRequest =
        ApplicationCommandRequest.builder()
            .name(name)
            .description("Analyzes the given list of dps.report links")
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("logs")
                    .description("Your name")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(true)
                    .build()
            )
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("name")
                    .description("Name of the run")
                    .type(ApplicationCommandOption.Type.STRING.value)
                    .required(false)
                    .build()
            )
            .addOption(
                ApplicationCommandOptionData.builder()
                    .name("with_heal")
                    .description("Include heal/barrier stats")
                    .type(ApplicationCommandOption.Type.BOOLEAN.value)
                    .required(false)
                    .build()
            )
            .build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> =
        event.deferReply().then(longRunning(event))

    fun longRunning(event: ChatInputInteractionEvent): Mono<Void> {
        return Flux.fromStream(event.extractLogs())
            .parallel()
            .runOn(Schedulers.fromExecutor(this.managedExecutor))
            .flatMap { link ->
                this.dpsReportClient.fetchJson(link).map { Pair(link, it) }.toMono()
            }
            .collectSortedList { o1, o2 -> o1.second.startTime().compareTo(o2.second.startTime()) }
            .map { this.analyzerService.analyze(it) }
            .flatMap { event.editReply().withEmbeds(*it.createEmbeds(event).toTypedArray()) }
            .then()
    }

    fun ChatInputInteractionEvent.extractLogs(): Stream<String> {
        return DPS_REPORT_RGX.findAll(
                getOption("logs").flatMap { it.value }.map { it.asString() }.get()
            )
            .map { it.value }
            .asStream()
    }

    fun RunAnalysis.createEmbeds(event: ChatInputInteractionEvent): List<EmbedCreateSpec> =
        listOf(
                createOverviewEmbed(this, event),
                createTopStatsEmbed(
                    this,
                    event,
                    CustomEmojis.TOP_STATS,
                    "Top Stats",
                    0,
                    Color.of(237, 178, 39),
                ),
                createTopStatsEmbed(
                    this,
                    event,
                    CustomEmojis.SEC_TOP_STATS,
                    "Second Best",
                    1,
                    Color.of(130, 138, 146),
                ),
                createSuccessLogsEmbed(this, bossData),
            )
            .let { ls ->
                if (this.pulls.any { !it.success }) ls + createWipeLogsEmbed(this, bossData) else ls
            }

    companion object {
        private val DPS_REPORT_RGX = Regex("https://(?:[ab]\\.)?dps.report/[\\w-]+(?=\\s*?https|$)")
    }
}
