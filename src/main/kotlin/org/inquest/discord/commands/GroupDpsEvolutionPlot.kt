package org.inquest.discord.commands

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.spec.MessageCreateFields
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.InteractionEventListener
import org.inquest.discord.commands.PlotCommons.dateX
import org.inquest.entities.isac.ChannelAnalysis
import org.inquest.entities.isac.PlayerAnalysis
import org.inquest.entities.isac.RunAnalysis
import org.inquest.utils.defaultStyle
import org.inquest.utils.epochMillis
import org.inquest.utils.mapNotNull
import org.inquest.utils.toIsacPNG
import org.inquest.utils.toMono
import org.inquest.utils.uppercased
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.concat
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.kandy.dsl.categorical
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.multiplot.plotGrid
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
        private const val GROUP_DPS = "Avg Group Dps"
    }

    override val handlesId: String = CommonIds.GROUP_DPS_EVOLUTION
    override val eventType: Class<ButtonInteractionEvent> = ButtonInteractionEvent::class.java

    override fun execute(event: ButtonInteractionEvent): Mono<Void> {
        if (event.message.isEmpty) return Mono.empty()

        return ChannelAnalysis.findById(event.messageId.asString())
            .mapNotNull()
            .map { it.name }
            .flatMap { name ->
                ChannelAnalysis.findLast(event.message.get().channelId.asString(), name).map { name to it }
            }.map { (name, ls) ->
                name to ls.map { run ->
                    run.analysis
                }
            }.toMono().flatMap { (name, ls) ->
                event.reply().withEphemeral(true).withFiles(
                    MessageCreateFields.File.of(PlotCommons.PLOT_FILE, ls.plot("$name Group Dps Evolution").inputStream()),
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

        val dfAvgGroup = dataFrameOf(
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

        fun DataFrame<*>.plotDps(title: String, withLabels: Boolean, vararg colors: Pair<String, Color>) = plot {
            line {
                dateX()
                y(PlotCommons.DPS) {
                    axis.name = PlotCommons.DPS.uppercased(0)
                }
                color(PlotCommons.SERIES) {
                    legend.name = ""
                    scale = categorical(*colors)
                }
                width = 1.3
            }

            layout {
                caption = title

                x.axis.name = ""
                if (withLabels) {
                    x.axis.breaks(times)
                } else {
                    x.axis.breaksLabeled(times, List(times.size) { "" })
                }

                y.axis.breaks(format = "{.2s}")
                defaultStyle {
                    xAxis.text {
                        angle = 90.0
                    }
                }
            }
        }

        return plotGrid(
            nCol = 1,
            hspace = 0,
            vspace = 0,
            align = true,
            plots = listOf(
                dfAvgGroup.plotDps(
                    "",
                    false,
                    GROUP_DPS to Color.rgb(160, 0, 0),
                ),
                dfAvg.concat(dfAvgSupp).plotDps(
                    title,
                    true,
                    PlotCommons.AVERAGE_DPS to Color.BLUE,
                    PlotCommons.AVERAGE_BOON_DPS to Color.PURPLE,
                ),
            ),
        ).toIsacPNG()
    }
}
