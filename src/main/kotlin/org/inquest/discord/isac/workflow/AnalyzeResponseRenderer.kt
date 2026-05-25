package org.inquest.discord.isac.workflow

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.Button
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.ThreadChannel
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.StartThreadSpec
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.model.Pull
import org.inquest.catalog.IsacDataService
import org.inquest.discord.isac.embed.BoonStatsEmbed
import org.inquest.discord.isac.embed.ErrorEmbeds
import org.inquest.discord.isac.embed.LogListingEmbeds.createSuccessLogsEmbed
import org.inquest.discord.isac.embed.LogListingEmbeds.createWipeLogsEmbed
import org.inquest.discord.isac.embed.OverviewEmbed
import org.inquest.discord.isac.embed.TopStatsEmbed
import org.inquest.discord.isac.embed.WingmanEmbed
import org.inquest.discord.isac.shared.CommonIds
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.discord.support.createMessageOrShowError
import org.inquest.discord.support.dynamic
import org.inquest.persistence.mongo.ChannelAnalysis
import org.inquest.persistence.mongo.ChannelSettings
import org.inquest.shared.collections.isIsacWipe
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import org.inquest.shared.reactor.infoLog
import org.inquest.shared.reactor.toMono
import org.inquest.shared.reactor.toUni
import reactor.core.publisher.Mono

@ApplicationScoped
class AnalyzeResponseRenderer : WithLogger {
    companion object {
        private val ACTION_BUTTONS = listOf(
            Button.primary(CommonIds.TIME_EVOLUTION, ReactionEmoji.of(CustomEmojis.TIME_EMOJI)),
            Button.primary(CommonIds.GROUP_DPS_EVOLUTION, ReactionEmoji.of(CustomEmojis.GROUP_DPS_EMOJI)),
            Button.primary(CommonIds.DPS_EVOLUTION, ReactionEmoji.of(CustomEmojis.DPS_EMOJI)),
        )
        private val INFO_BUTTON = Button.secondary(CommonIds.INFO_EMBED, ReactionEmoji.of(CustomEmojis.INFO_EMOJI))
    }

    @Inject
    private lateinit var isacDataService: IsacDataService

    @Inject
    private lateinit var wingmanEmbed: WingmanEmbed

    @Inject
    private lateinit var boonStatsEmbed: BoonStatsEmbed

    fun render(interactionId: String, event: ChatInputInteractionEvent, prepared: PreparedAnalysis): Mono<Void> =
        editSummaryReply(interactionId, event, prepared)
            .infoLog(LOG, "$interactionId: Successfully built embeds.")
            .flatMap { context -> persistAnalysis(context) }
            .flatMap { context -> startDetailsThread(interactionId, context) }
            .toUni()
            .call { (settings, analysis, thread) ->
                thread.createForOption(settings.compareWingman, {
                    wingmanEmbed.createWingmanEmbed(
                        interactionId,
                        analysis.currentAnalysis.analysis.pulls,
                        analysis.currentAnalysis.accountMap,
                        analysis.currentAnalysis.analysis.playerStats,
                    ).dynamic()
                }) { createEmbed(ErrorEmbeds.ANALYZE_WM_EXC_MSG, color = CustomColors.RED_COLOR) }
            }.call { (settings, analysis, thread) ->
                thread.createForOption(settings.compareWingman, {
                    wingmanEmbed.createWingmanEmbed(
                        interactionId,
                        analysis.currentAnalysis.analysis.pulls,
                        analysis.currentAnalysis.accountMap,
                        analysis.currentAnalysis.analysis.playerStats,
                        true,
                    ).dynamic()
                }) { createEmbed(ErrorEmbeds.ANALYZE_WM_EXC_MSG, color = CustomColors.RED_COLOR) }
            }.call { (settings, analysis, thread) ->
                thread.createForOption(settings.analyzeBoons, {
                    boonStatsEmbed.createOverviewEmbed(analysis.currentAnalysis.analysis, event).dynamic()
                }) { createEmbed(ErrorEmbeds.ANALYZE_BOONS_EXC_MSG, color = CustomColors.RED_COLOR) }
            }.toMono().then()

    private fun editSummaryReply(
        interactionId: String,
        event: ChatInputInteractionEvent,
        prepared: PreparedAnalysis,
    ): Mono<RenderContext<Message>> {
        var reply = event.editReply().withEmbeds(*prepared.currentAnalysis.createEmbeds(prepared.channelSettings).toTypedArray())

        reply = if (prepared.hasPreviousRuns) {
            reply.withComponents(ActionRow.of(*ACTION_BUTTONS.toTypedArray(), INFO_BUTTON))
        } else {
            reply.withComponents(ActionRow.of(INFO_BUTTON))
        }

        return reply.map { RenderContext(prepared.channelSettings, prepared, it) }
            .doOnSubscribe { LOG.info("$interactionId: Putting together embeds...") }
    }

    private fun persistAnalysis(context: RenderContext<Message>): Mono<RenderContext<Message>> {
        if (!MongoProfile.enabled()) {
            return Mono.just(context)
        }

        return ChannelAnalysis().apply {
            this.id = context.subject.id.asString()
            this.channelId = context.subject.channelId.asString()
            this.name = context.channelSettings.name
            this.analysis = context.preparedAnalysis.currentAnalysis.analysis
        }.persistOrUpdate<ChannelAnalysis>().toMono().map { context }
    }

    private fun startDetailsThread(interactionId: String, context: RenderContext<Message>): Mono<RenderContext<ThreadChannel>> {
        val settings = context.channelSettings
        if (!settings.compareWingman && !settings.analyzeBoons) {
            return Mono.empty()
        }

        return context.subject.startThread(StartThreadSpec.builder().name("More Details").build())
            .doOnSubscribe { LOG.debug("$interactionId: Creating thread for detailed analysis...") }
            .map { RenderContext(settings, context.preparedAnalysis, it) }
    }

    private fun ThreadChannel.createForOption(
        create: Boolean,
        message: () -> Array<EmbedCreateSpec>,
        error: (Throwable) -> EmbedCreateSpec,
    ) = if (create) {
        createMessageOrShowError(LOG, message, error).toUni()
    } else {
        Uni.createFrom().voidItem()
    }

    private fun CurrentAnalysis.createEmbeds(channelSettings: ChannelSettings): List<EmbedCreateSpec> {
        val embeds = mutableListOf<EmbedCreateSpec>()
        embeds += OverviewEmbed.createOverviewEmbed(this.analysis, channelSettings.name).dynamic()
        embeds += TopStatsEmbed.createTopStatsEmbed(
            this.analysis,
            this.accountMap,
            channelSettings.withHeal,
            CustomEmojis.TOP_STATS,
            "Top Stats",
            0,
            CustomColors.GOLD_COLOR,
        ).dynamic()
        embeds += TopStatsEmbed.createTopStatsEmbed(
            this.analysis,
            this.accountMap,
            channelSettings.withHeal,
            CustomEmojis.SEC_TOP_STATS,
            "Second Best",
            1,
            CustomColors.SILVER_COLOR,
        ).dynamic()
        embeds += createSuccessLogsEmbed(this.analysis, isacDataService).dynamic()
        if (this.analysis.pulls.any(Pull::isIsacWipe)) embeds += createWipeLogsEmbed(this.analysis, isacDataService)

        return embeds
    }
}

private data class RenderContext<T>(val channelSettings: ChannelSettings, val preparedAnalysis: PreparedAnalysis, val subject: T)
