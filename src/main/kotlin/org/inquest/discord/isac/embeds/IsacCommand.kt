package org.inquest.discord.isac.embeds

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.channel.ThreadChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.StartThreadSpec
import discord4j.discordjson.json.ApplicationCommandRequest
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.clients.DpsReportClient
import org.inquest.discord.CommandListener
import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.createEmbed
import org.inquest.discord.createMessageOrShowError
import org.inquest.discord.dynamic
import org.inquest.discord.isac.CommonIds
import org.inquest.discord.isac.CommonOptions
import org.inquest.discord.isac.CommonOptions.BOONS_OPTION
import org.inquest.discord.isac.CommonOptions.HEAL_OPTION
import org.inquest.discord.isac.CommonOptions.LOGS_OPTION
import org.inquest.discord.isac.CommonOptions.NAME_OPTION
import org.inquest.discord.isac.CommonOptions.WM_OPTION
import org.inquest.discord.isac.embeds.ErrorEmbeds.raiseException
import org.inquest.discord.isac.embeds.IsacCommand.Companion.DPS_REPORT_RGX
import org.inquest.discord.isac.embeds.LogListingEmbeds.createSuccessLogsEmbed
import org.inquest.discord.isac.embeds.LogListingEmbeds.createWipeLogsEmbed
import org.inquest.discord.isac.interactionId
import org.inquest.discord.optionAsBoolean
import org.inquest.discord.optionAsString
import org.inquest.discord.stringOption
import org.inquest.entities.isac.Channel
import org.inquest.entities.isac.ChannelAnalysis
import org.inquest.entities.isac.ChannelSettings
import org.inquest.entities.isac.Pull
import org.inquest.entities.isac.RunAnalysis
import org.inquest.services.AnalysisService
import org.inquest.services.IsacDataService
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import org.inquest.utils.debugLog
import org.inquest.utils.infoLog
import org.inquest.utils.isIsacWipe
import org.inquest.utils.startTime
import org.inquest.utils.toMono
import org.inquest.utils.toUni
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

/**
 * Main entry point of the bot. Provides a slash command with the name 'analyze', that takes a list of logs and analyzes them for our defined top stats.
 *
 * Options:
 * - logs: List of logs matching [DPS_REPORT_RGX]
 * - [CommonOptions]
 */
@ApplicationScoped
class IsacCommand :
    CommandListener,
    WithLogger {
    companion object {
        private val DPS_REPORT_RGX =
            Regex("https://(?:[ab]\\.)?dps.report/[\\w-]+(?=\\s*?https|$|\\s)")
        private val ACTION_BUTTONS = listOf(
            Button.primary(CommonIds.TIME_EVOLUTION, ReactionEmoji.of(CustomEmojis.TIME_EMOJI)),
            Button.primary(CommonIds.GROUP_DPS_EVOLUTION, ReactionEmoji.of(CustomEmojis.GROUP_DPS_EMOJI)),
            Button.primary(CommonIds.DPS_EVOLUTION, ReactionEmoji.of(CustomEmojis.DPS_EMOJI)),
        )
        private val INFO_BUTTON = Button.secondary(CommonIds.INFO_EMBED, ReactionEmoji.of(CustomEmojis.INFO_EMOJI))
    }

    override val name: String = "analyze"

    /**
     * To be able to pull ei logs from dps.report
     */
    @RestClient
    private lateinit var dpsReportClient: DpsReportClient

    /**
     * Used to analyze downloaded logs into a [RunAnalysis]
     */
    @Inject
    private lateinit var analysisService: AnalysisService

    /**
     * Isac specific boss data
     */
    @Inject
    private lateinit var isacDataService: IsacDataService

    /**
     * Used to schedule the parallel downloading of logs
     */
    @Inject
    private lateinit var managedExecutor: ManagedExecutor

    @Inject
    private lateinit var wingmanEmbed: WingmanEmbed

    @Inject
    private lateinit var boonStatsEmbed: BoonStatsEmbed

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
            .addOption(stringOption(LOGS_OPTION, "The list of logs to be analyzed, ideally separated by white spaces."))
            .addAllOptions(CommonOptions.DEFAULT_OPTIONS)
            .build()
    }

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        val interactionId = interactionId()

        return event.deferReply().doOnSubscribe {
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
        }.then(handleLogs(interactionId, event))
    }

    /**
     * Extracts logs from the user input, downloads the ei jsons, analyzes the downloaded logs and finally builds the embeded responses.
     */
    private fun handleLogs(interactionId: String, event: ChatInputInteractionEvent): Mono<Void> = Mono.just(event.extractLogs())
        .infoLog(LOG, { "$interactionId: Fetching ${it.size} logs..." })
        .flatMapMany { Flux.fromIterable(it) }
        .parallel()
        .runOn(Schedulers.fromExecutor(this.managedExecutor))
        .debugLog(LOG) { "$interactionId: Downloading log $it..." }
        .flatMap { link ->
            this.dpsReportClient
                .fetchJson(link)
                .map { Pair(link, it) }
                .toMono()
        }.collectSortedList { o1, o2 -> o1.second.startTime().compareTo(o2.second.startTime()) }
        .flatMap { if (it.isEmpty()) event.raiseException(LOG, ErrorEmbeds.NO_LOGS_EXC_MSG) else Mono.just(it) }
        .infoLog(LOG, "$interactionId: Downloaded logs.")
        .onErrorResume { event.raiseException(LOG, ErrorEmbeds.FETCHING_EXC_MSG) }
        .flatMap { ls ->
            if (ls.isEmpty()) {
                Mono.empty()
            } else {
                Mono.just(this.analysisService.analyze(interactionId, ls))
            }
        }.onErrorResume { event.raiseException(LOG, ErrorEmbeds.ANALYZE_EXC_MSG, it, true) }
        .flatMap { analysis ->
            if (!mongoEnabled()) {
                return@flatMap Mono.just(TupleContext(ChannelSettings(), analysis, false))
            }

            event.interaction.channel.flatMap { ch ->
                Channel.findOrPut(ch.id.asString()).toMono().map {
                    it.channelSettings
                }.onErrorResume {
                    Mono.just(ChannelSettings())
                }.map {
                    it.copy(
                        name = event.optionAsString(NAME_OPTION),
                        withHeal = event.optionAsBoolean(HEAL_OPTION),
                        compareWingman = event.optionAsBoolean(WM_OPTION),
                        analyzeBoons = event.optionAsBoolean(BOONS_OPTION),
                    )
                }.flatMap {
                    ChannelAnalysis.findLast(ch.id.asString(), it.name)
                        .map { ls -> ls.size }
                        .onFailure().recoverWithItem(0)
                        .map { count -> TupleContext(it, analysis, count > 0) }
                        .toMono()
                }
            }
        }.flatMap { (settings, analysis, withButtons) ->
            var reply = event.editReply().withEmbeds(*analysis.createEmbeds(settings).toTypedArray())

            if (withButtons) {
                reply = reply.withComponents(
                    ActionRow.of(*ACTION_BUTTONS.toTypedArray(), INFO_BUTTON),
                )
            } else {
                reply = reply.withComponents(ActionRow.of(INFO_BUTTON))
            }

            return@flatMap reply.map { TupleContext(settings, analysis, it) }
                .doOnSubscribe { LOG.info("$interactionId: Putting together embeds...") }
        }.infoLog(LOG, "$interactionId: Successfully built embeds.")
        .flatMap { ctxt ->
            if (!mongoEnabled()) {
                return@flatMap Mono.just(ctxt)
            }

            ChannelAnalysis().apply {
                this.id = ctxt.subject2.id.asString()
                this.channelId = ctxt.subject2.channelId.asString()
                this.name = ctxt.channelSettings.name
                this.analysis = ctxt.subject1
            }.persistOrUpdate<ChannelAnalysis>().toMono().map { ctxt }
        }.flatMap { (settings, analysis, msg) ->
            if (settings.compareWingman || settings.analyzeBoons) {
                msg.startThread(StartThreadSpec.builder().name("More Details").build())
                    .doOnSubscribe { LOG.debug("$interactionId: Creating thread for detailed analysis...") }
                    .map { TupleContext(settings, analysis, it) }
            } else {
                Mono.empty()
            }
        }.toUni().call { (settings, analysis, thread) ->
            thread.createForOption(settings.compareWingman, {
                wingmanEmbed.createWingmanEmbed(interactionId, analysis.pulls, analysis.playerStats).dynamic()
            }) { createEmbed(ErrorEmbeds.ANALYZE_WM_EXC_MSG, color = CustomColors.RED_COLOR) }
        }.call { (settings, analysis, thread) ->
            thread.createForOption(settings.compareWingman, {
                wingmanEmbed.createWingmanEmbed(interactionId, analysis.pulls, analysis.playerStats, true).dynamic()
            }) { createEmbed(ErrorEmbeds.ANALYZE_WM_EXC_MSG, color = CustomColors.RED_COLOR) }
        }.call { (settings, analysis, thread) ->
            thread.createForOption(settings.analyzeBoons, {
                boonStatsEmbed.createOverviewEmbed(analysis, event).dynamic()
            }) { createEmbed(ErrorEmbeds.ANALYZE_BOONS_EXC_MSG, color = CustomColors.RED_COLOR) }
        }.toMono().then()

    private fun ThreadChannel.createForOption(
        create: Boolean,
        message: () -> Array<EmbedCreateSpec>,
        error: (Throwable) -> EmbedCreateSpec,
    ) = if (create) {
        createMessageOrShowError(LOG, message, error).toUni()
    } else {
        Uni.createFrom().voidItem()
    }

    private fun ChatInputInteractionEvent.extractLogs(): List<String> = DPS_REPORT_RGX.findAll(optionAsString("logs")!!).map {
        it.value
    }.toList()

    private fun RunAnalysis.createEmbeds(channelSettings: ChannelSettings): List<EmbedCreateSpec> {
        val embeds = mutableListOf<EmbedCreateSpec>()
        embeds += OverviewEmbed.createOverviewEmbed(this, channelSettings.name).dynamic()
        embeds += TopStatsEmbed.createTopStatsEmbed(
            this,
            channelSettings.withHeal,
            CustomEmojis.TOP_STATS,
            "Top Stats",
            0,
            CustomColors.GOLD_COLOR,
        ).dynamic()
        embeds += TopStatsEmbed.createTopStatsEmbed(
            this,
            channelSettings.withHeal,
            CustomEmojis.SEC_TOP_STATS,
            "Second Best",
            1,
            CustomColors.SILVER_COLOR,
        ).dynamic()
        embeds += createSuccessLogsEmbed(this, isacDataService).dynamic()
        if (this.pulls.any(Pull::isIsacWipe)) embeds += createWipeLogsEmbed(this, isacDataService)

        return embeds
    }
}

private fun mongoEnabled(): Boolean = "mongo" in ConfigProvider.getConfig()
    .getOptionalValue("quarkus.profile", String::class.java)
    .getOrDefault("").trim().split(",")

private data class Context<T>(val channelSettings: ChannelSettings, val subject: T)

private data class TupleContext<T, U>(val channelSettings: ChannelSettings, val subject1: T, val subject2: U)
