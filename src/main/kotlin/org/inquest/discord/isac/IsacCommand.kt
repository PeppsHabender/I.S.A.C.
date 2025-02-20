package org.inquest.discord.isac

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.discordjson.json.ApplicationCommandRequest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.AnalyzerService
import org.inquest.clients.DpsReportClient
import org.inquest.discord.CommandListener
import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.isac.ErrorEmbeds.handleAnalyzeException
import org.inquest.discord.isac.ErrorEmbeds.handleFetchingException
import org.inquest.discord.isac.LogListingEmbeds.createSuccessLogsEmbed
import org.inquest.discord.isac.LogListingEmbeds.createWipeLogsEmbed
import org.inquest.discord.isac.OverviewEmbed.createOverviewEmbed
import org.inquest.discord.isac.TopStatsEmbed.createTopStatsEmbed
import org.inquest.discord.withBooleanOption
import org.inquest.discord.withStringOption
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData
import org.inquest.utils.optionAsString
import org.inquest.utils.startTime
import org.inquest.utils.toMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Main entry point of the bot. Provides a slash command with the name 'analyze'.
 *
 * Options:
 * - logs: List of logs matching [DPS_REPORT_RGX]
 * - (name): Name of the analysis. Default: 'Run Analysis'
 * - (with_heal): Wether to include heal stats or not, heal stats only really make sense if at least both of the healers use the extension. Default: false
 */
@ApplicationScoped
class IsacCommand : CommandListener {
    override val name: String = "testest"

    /**
     * To be able to pull ei logs from dps.report
     */
    @RestClient
    private lateinit var dpsReportClient: DpsReportClient

    /**
     * Used to analyze downloaded logs into a [RunAnalysis]
     */
    @Inject
    private lateinit var analyzerService: AnalyzerService

    /**
     * Isac specific boss data
     */
    @Inject
    private lateinit var bossData: BossData

    /**
     * Used to schedule the parallel downloading of logs
     */
    @Inject
    private lateinit var managedExecutor: ManagedExecutor

    /**
     * The discord client which will be initialized on build
     */
    private lateinit var gatewayClient: GatewayDiscordClient

    override fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest {
        this.gatewayClient = gatewayClient

        return ApplicationCommandRequest
            .builder()
            .name(name)
            .description("Analyzes the given list of dps.report links")
            .withStringOption("logs", "The list of logs to be analyzed, ideally separated by white spaces.")
            .withStringOption("name", "Name of the run.", required = false)
            .withBooleanOption("with_heal", "Include heal/barrier stats.", required = false)
            .build()
    }

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> = event.deferReply().then(handleLogs(event))

    /**
     * Extracts logs from the user input, downloads the ei jsons, analyzes the downloaded logs and finally builds the embeded responses.
     */
    private fun handleLogs(event: ChatInputInteractionEvent): Mono<Void> = Flux.fromStream(event.extractLogs())
        .parallel()
        .runOn(Schedulers.fromExecutor(this.managedExecutor))
        .flatMap { link ->
            this.dpsReportClient
                .fetchJson(link)
                .map { Pair(link, it) }
                .toMono()
        }.collectSortedList { o1, o2 -> o1.second.startTime().compareTo(o2.second.startTime()) }
        .onErrorResume { event.handleFetchingException() }
        .flatMap {
            if (it == null) Mono.empty() else Mono.just(this.analyzerService.analyze(it))
        }.onErrorResume { event.handleAnalyzeException(it) }
        .flatMap {
            if (it == null) {
                Mono.empty()
            } else {
                event.editReply().withEmbeds(*it.createEmbeds(event).toTypedArray())
            }
        }.then()

    private fun ChatInputInteractionEvent.extractLogs(): Stream<String> = DPS_REPORT_RGX.findAll(optionAsString("logs")!!).map { it.value }.asStream()

    private fun RunAnalysis.createEmbeds(event: ChatInputInteractionEvent): List<EmbedCreateSpec> = listOf(
        createOverviewEmbed(this, event),
        createTopStatsEmbed(
            this,
            event,
            CustomEmojis.TOP_STATS,
            "Top Stats",
            0,
            CustomColors.GOLD_COLOR,
        ),
        createTopStatsEmbed(
            this,
            event,
            CustomEmojis.SEC_TOP_STATS,
            "Second Best",
            1,
            CustomColors.SILVER_COLOR,
        ),
        createSuccessLogsEmbed(this, bossData),
    ).let { ls ->
        if (this.pulls.any { !it.success }) ls + createWipeLogsEmbed(this, bossData) else ls
    }

    companion object {
        private val DPS_REPORT_RGX =
            Regex("https://(?:[ab]\\.)?dps.report/[\\w-]+(?=\\s*?https|$|\\s)")
    }
}
