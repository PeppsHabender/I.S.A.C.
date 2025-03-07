package org.inquest.discord.commands

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.spec.MessageCreateFields
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.InteractionEventListener
import org.inquest.discord.commands.PlotCommons.dateX
import org.inquest.entities.isac.ChannelAnalysis
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.entities.isac.RunAnalysis
import org.inquest.utils.DoubleExtensions.format
import org.inquest.utils.defaultStyle
import org.inquest.utils.epochMillis
import org.inquest.utils.mapNotNull
import org.inquest.utils.toMono
import org.inquest.utils.uppercased
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.toPNG
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.layers.text
import org.jetbrains.kotlinx.kandy.letsplot.scales.Transformation
import org.jetbrains.kotlinx.kandy.letsplot.x
import org.jetbrains.kotlinx.kandy.letsplot.y
import org.jetbrains.kotlinx.kandy.util.color.Color
import reactor.core.publisher.Mono

/**
 * Creates a plot which shows the development of group dps against average dps and average boon dps.
 */
@ApplicationScoped
class GroupDpsEvolutionPlot : InteractionEventListener<ButtonInteractionEvent>() {
    companion object {
        private const val GROUP_DPS = "Group Dps"
    }

    override val handlesId: String = CommonIds.GROUP_DPS_EVOLUTION
    override val eventType: Class<ButtonInteractionEvent> = ButtonInteractionEvent::class.java

    override fun execute(event: ButtonInteractionEvent): Mono<Void> {
        if (event.message.isEmpty) return Mono.empty()

        return ChannelAnalysis.findById(event.messageId.asString())
            .mapNotNull()
            .map { it.name }
            .flatMap {
                ChannelAnalysis.findLast(event.message.get().channelId.asString(), it)
            }.map { ls ->
                ls.map { run ->
                    run.analysis
                }
            }.toMono().flatMap {
                event.reply().withFiles(
                    MessageCreateFields.File.of(PlotCommons.PLOT_FILE, it.plot("Group Dps Evolution").inputStream()),
                )
            }
    }

    private fun List<RunAnalysis>.plot(title: String): ByteArray {
        val playerStats: List<Map<String, PlayerAnalysis>> = map { run -> run.playerStats.associateBy { it.name } }

        val times = map { it.start.epochMillis }

        val allPulls = playerStats.map { p -> p.values.flatMap { it.pulls.values } }
        val avgDps = allPulls.map { pulls -> pulls.filter { !it.isSupport && !it.maybeHealer }.map { it.dps }.average() }
        val avgSuppDps = allPulls.map { pulls -> pulls.filter { it.isSupport && !it.maybeHealer }.map { it.dps }.average() }
        val n = times.size

        val groupDps = dataFrameOf(
            PlotCommons.DATE to times,
            PlotCommons.DPS to map { it.groupDps },
            PlotCommons.SERIES to List(n) { GROUP_DPS },
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

        val df = groupDps.concat(dfAvg, dfAvgSupp)

        return df.plot {
            line {
                dateX()
                y(PlotCommons.DPS) {
                    scale = continuous(transform = Transformation.LOG10)
                    axis.name = PlotCommons.DPS.uppercased(0)
                }
                color(PlotCommons.SERIES) {
                    legend.name = ""
                    scale = categorical(
                        GROUP_DPS to Color.rgb(160, 0, 0),
                        PlotCommons.AVERAGE_DPS to Color.BLUE,
                        PlotCommons.AVERAGE_BOON_DPS to Color.PURPLE,
                    )
                }
                width = 1.3
            }

            layout {
                caption = title
                x.axis.name = ""
                x.axis.breaks(times)

                val breaks = listOf(avgSuppDps.average(), avgDps.average(), map { it.groupDps }.average()).sorted()
                y.axis.breaksLabeled(breaks, breaks.map { it / 1000 }.map { it.format("#.#k") })
                defaultStyle {
                    xAxis.text {
                        angle = 90.0
                    }
                }
            }
        }.toPNG()
    }
}
