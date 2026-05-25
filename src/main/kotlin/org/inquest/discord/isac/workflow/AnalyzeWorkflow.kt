package org.inquest.discord.isac.workflow

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.analysis.AnalysisService
import org.inquest.discord.isac.embed.ErrorEmbeds
import org.inquest.discord.isac.embed.ErrorEmbeds.raiseException
import org.inquest.discord.isac.shared.CommonOptions.BOONS_OPTION
import org.inquest.discord.isac.shared.CommonOptions.HEAL_OPTION
import org.inquest.discord.isac.shared.CommonOptions.NAME_OPTION
import org.inquest.discord.isac.shared.CommonOptions.WM_OPTION
import org.inquest.discord.support.optionAsBoolean
import org.inquest.discord.support.optionAsString
import org.inquest.integration.dpsreport.DpsReportClient
import org.inquest.persistence.mongo.Channel
import org.inquest.persistence.mongo.ChannelAnalysis
import org.inquest.persistence.mongo.ChannelSettings
import org.inquest.persistence.mongo.Gw2ToDiscord
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import org.inquest.shared.reactor.debugLog
import org.inquest.shared.reactor.infoLog
import org.inquest.shared.reactor.toMono
import org.inquest.shared.reactor.toUni
import org.inquest.shared.time.startTime
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@ApplicationScoped
class AnalyzeWorkflow : WithLogger {
    @RestClient
    private lateinit var dpsReportClient: DpsReportClient

    @Inject
    private lateinit var analysisService: AnalysisService

    @Inject
    private lateinit var managedExecutor: ManagedExecutor

    fun prepare(interactionId: String, event: ChatInputInteractionEvent): Mono<PreparedAnalysis> = fetchLogs(interactionId, event)
        .flatMap { logs ->
            if (logs.isEmpty()) {
                Mono.empty()
            } else {
                Mono.just(analysisService.analyze(interactionId, logs))
                    .flatMap { analysis -> attachAccountMappings(event, analysis) }
            }
        }.onErrorResume { event.raiseException(LOG, ErrorEmbeds.ANALYZE_EXC_MSG, it, true) }
        .flatMap { currentAnalysis -> loadChannelContext(event, currentAnalysis) }

    private fun fetchLogs(interactionId: String, event: ChatInputInteractionEvent) = Mono.just(AnalyzeRequestParser.extractLogs(event))
        .infoLog(LOG, { "$interactionId: Fetching ${it.size} logs..." })
        .flatMapMany { Flux.fromIterable(it) }
        .parallel()
        .runOn(Schedulers.fromExecutor(managedExecutor))
        .debugLog(LOG) { "$interactionId: Downloading log $it..." }
        .flatMap { link ->
            dpsReportClient
                .fetchJson(link)
                .map { Pair(link, it) }
                .toMono()
        }.collectSortedList { o1, o2 -> o1.second.startTime().compareTo(o2.second.startTime()) }
        .flatMap { if (it.isEmpty()) event.raiseException(LOG, ErrorEmbeds.NO_LOGS_EXC_MSG) else Mono.just(it) }
        .infoLog(LOG, "$interactionId: Downloaded logs.")
        .onErrorResume { event.raiseException(LOG, ErrorEmbeds.FETCHING_EXC_MSG) }

    private fun attachAccountMappings(
        event: ChatInputInteractionEvent,
        analysis: org.inquest.analysis.model.RunAnalysis,
    ): Mono<CurrentAnalysis> = event.interaction.guild.flatMap { guild ->
        if (guild == null) {
            return@flatMap Mono.just(CurrentAnalysis(mapOf(), analysis))
        }

        val mapUnis = analysis.playerStats.map { player ->
            Gw2ToDiscord.findByGw2Account(player.name)
                .toMono()
                .flatMap<Pair<String, Gw2ToDiscord?>> { account ->
                    if (account?.discordId == null) {
                        Mono.empty()
                    } else {
                        guild.getMemberById(Snowflake.of(account.discordId))
                            .map { player.name to account }
                            .onErrorResume { _ -> Mono.empty() }
                            .switchIfEmpty(Mono.empty())
                    }
                }
                .onErrorResume { _ -> Mono.empty() }
                .defaultIfEmpty("" to null)
                .toUni()
                .onFailure()
                .recoverWithNull()
        }

        Uni.combine().all().unis<Pair<String, Gw2ToDiscord?>>(mapUnis)
            .with { results ->
                @Suppress("UNCHECKED_CAST")
                val filteredMap = (results.filterNotNull() as List<Pair<String, Gw2ToDiscord?>>).toMap()
                CurrentAnalysis(filteredMap, analysis)
            }
            .toMono()
    }.switchIfEmpty(Mono.just(CurrentAnalysis(mapOf(), analysis)))

    private fun loadChannelContext(event: ChatInputInteractionEvent, analysis: CurrentAnalysis): Mono<PreparedAnalysis> {
        if (!MongoProfile.enabled()) {
            return Mono.just(PreparedAnalysis(ChannelSettings(), analysis, false))
        }

        return event.interaction.channel.flatMap { channel ->
            Channel.findOrPut(channel.id.asString()).toMono().map {
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
                ChannelAnalysis.findLast(channel.id.asString(), it.name)
                    .map { runs -> runs.size }
                    .onFailure().recoverWithItem(0)
                    .map { count -> PreparedAnalysis(it, analysis, count > 0) }
                    .toMono()
            }
        }
    }
}
