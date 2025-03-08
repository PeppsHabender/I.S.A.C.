package org.inquest.discord.commands

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent
import discord4j.core.`object`.component.ActionRow
import discord4j.core.`object`.component.TextInput
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.MessageCreateFields
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.InteractionEventListener
import org.inquest.discord.commands.PlotCommons.dateX
import org.inquest.entities.isac.ChannelAnalysis
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.utils.DoubleExtensions.format
import org.inquest.utils.defaultStyle
import org.inquest.utils.epochMillis
import org.inquest.utils.mapNotNull
import org.inquest.utils.toMono
import org.inquest.utils.uppercased
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.toPNG
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.hLine
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.points
import org.jetbrains.kotlinx.kandy.letsplot.layers.text
import org.jetbrains.kotlinx.kandy.letsplot.settings.LineType
import org.jetbrains.kotlinx.kandy.letsplot.x
import org.jetbrains.kotlinx.kandy.util.color.Color
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException

/**
 * Creates a plot which shows the dps development for a specific player, asks the user for account name in a modal.
 */
@ApplicationScoped
class DpsEvolutionPlot : InteractionEventListener<ButtonInteractionEvent>() {
    companion object {
        private const val ACC_NAME = "account_name"
        private const val DPS_FMT = "dps_fmt"
        private const val PLAYER_DPS = "Player Dps"
    }

    override val handlesId: String = CommonIds.DPS_EVOLUTION
    override val eventType: Class<ButtonInteractionEvent> = ButtonInteractionEvent::class.java

    override fun execute(event: ButtonInteractionEvent): Mono<Void> {
        val interactionId = interactionId()

        val tempListener = event.client.on(ModalSubmitInteractionEvent::class.java) {
            if (it.customId == interactionId && event.message.isPresent) {
                it.handleModalSubmit(event.message.get())
            } else {
                Mono.empty()
            }
        }.timeout(Duration.of(5, ChronoUnit.MINUTES))
            .onErrorResume(TimeoutException::class.java) { Mono.empty() }

        return event.presentModal()
            .withTitle("Dps Evolution")
            .withCustomId(interactionId)
            .withComponents(ActionRow.of(TextInput.small(ACC_NAME, "Enter Account Name").placeholder("InquestAgent.6969").required()))
            .thenMany(tempListener)
            .then()
    }

    private fun ModalSubmitInteractionEvent.handleModalSubmit(message: Message): Mono<Void> {
        val accName = getComponents(TextInput::class.java).first { it.customId == ACC_NAME }.value.get().trim()

        return ChannelAnalysis.findById(message.id.asString())
            .mapNotNull()
            .map { it.name }
            .flatMap {
                ChannelAnalysis.findLast(message.channelId.asString(), it)
            }.map { ls ->
                ls.map { run ->
                    run.analysis.start to run.analysis.playerStats.associateBy { it.name }
                }
            }.toMono()
            .flatMap {
                reply().withEphemeral(true).withFiles(
                    MessageCreateFields.File.of("plot.png", it.plot(accName, "$accName's Dps Evolution").inputStream()),
                )
            }
    }

    private fun List<Pair<OffsetDateTime, Map<String, PlayerAnalysis>>>.plot(accName: String, title: String): ByteArray {
        val times = map { it.first.epochMillis }
        var playerDps = map { it.second[accName]?.avgDps() }

        val allPulls = map { (_, p) -> p.filterKeys { it != accName }.values.flatMap { it.pulls.values } }
        val avgDps = allPulls.map { pulls -> pulls.filter { !it.isSupport && !it.maybeHealer }.map { it.dps }.average() }
        val avgSuppDps = allPulls.map { pulls -> pulls.filter { it.isSupport && !it.maybeHealer }.map { it.dps }.average() }
        val n = times.size

        val playerTimes = times.mapIndexedNotNull { i, time -> if (playerDps[i] == null) null else time }
        playerDps = playerDps.filterNotNull()
        val dfPlayer = dataFrameOf(
            PlotCommons.DATE to playerTimes,
            PlotCommons.DPS to playerDps,
            DPS_FMT to playerDps.map { it / 1000 }.map { it.format("#.#k") },
            PlotCommons.SERIES to List(playerTimes.size) { PLAYER_DPS },
        )

        val dfAvg = dataFrameOf(
            PlotCommons.DATE to times,
            PlotCommons.DPS to avgDps,
            PlotCommons.SERIES to List(n) { PlotCommons.AVERAGE_DPS },
        )

        val dfAvgSupp = dataFrameOf(
            PlotCommons.DATE to times,
            PlotCommons.DPS to avgSuppDps,
            PlotCommons.SERIES to List(n) { PlotCommons.AVERAGE_BOON_DPS },
        )

        val df = dfPlayer.concat(dfAvg, dfAvgSupp)

        return df.plot {
            line {
                dateX()
                y(PlotCommons.DPS) {
                    axis.name = PlotCommons.DPS.uppercased(0)
                }
                color(PlotCommons.SERIES) {
                    legend.name = ""
                    scale = categorical(
                        PLAYER_DPS to Color.rgb(160, 0, 0),
                        PlotCommons.AVERAGE_DPS to Color.BLUE,
                        PlotCommons.AVERAGE_BOON_DPS to Color.PURPLE,
                    )
                }
                width = 1.3
            }

            hLine {
                yIntercept(listOf(playerDps.average()))
                width = 0.8
                type = LineType.DASHED
                color = Color.rgb(160, 0, 0)
            }

            withData(dfPlayer) {
                points {
                    x(PlotCommons.DATE)
                    y(PlotCommons.DPS)
                    color = Color.rgb(160, 0, 0)
                    size = 4.0
                }

                text {
                    x(PlotCommons.DATE)
                    y(PlotCommons.DPS)
                    label(DPS_FMT)

                    font.color = Color.WHITE
                    font.size = 6.5
                }
            }

            layout {
                caption = title
                x.axis.name = ""
                x.axis.breaks(times)
                defaultStyle {
                    xAxis.text {
                        angle = 90.0
                    }
                }
            }
        }.toPNG()
    }
}
