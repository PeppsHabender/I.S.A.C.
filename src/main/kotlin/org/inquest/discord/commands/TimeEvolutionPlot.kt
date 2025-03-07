package org.inquest.discord.commands

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.spec.MessageCreateFields
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.InteractionEventListener
import org.inquest.discord.commands.PlotCommons.dateX
import org.inquest.entities.isac.ChannelAnalysis
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
import org.jetbrains.kotlinx.kandy.letsplot.settings.LineType
import org.jetbrains.kotlinx.kandy.letsplot.x
import org.jetbrains.kotlinx.kandy.letsplot.y
import org.jetbrains.kotlinx.kandy.util.color.Color
import reactor.core.publisher.Mono
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Creates a time plot, which displays the development of run durations against down time and failed bosses.
 */
@ApplicationScoped
class TimeEvolutionPlot : InteractionEventListener<ButtonInteractionEvent>() {
    companion object {
        private const val TIME = "time"
        private const val DURATION = "duration"
        private const val DOWNTIME = "downtime"
    }

    override val handlesId: String = CommonIds.TIME_EVOLUTION
    override val eventType: Class<ButtonInteractionEvent> = ButtonInteractionEvent::class.java

    override fun execute(event: ButtonInteractionEvent): Mono<Void> {
        if (event.message.isEmpty) return Mono.empty()

        return plotTime(event.message.get()).flatMap {
            event.reply().withFiles(
                MessageCreateFields.File.of(PlotCommons.PLOT_FILE, it.inputStream()),
            )
        }
    }

    private fun plotTime(message: Message): Mono<ByteArray> = ChannelAnalysis.findById(message.id.asString())
        .mapNotNull()
        .map { it.name }
        .flatMap { name ->
            ChannelAnalysis.findLast(message.channelId.asString(), name).map { ls ->
                name to ls.map { run ->
                    val failed =
                        run.analysis.pulls.filterNot { it.success }.map { it.duration }.reduceOrNull { d1, d2 -> d1 + d2 } ?: Duration.ZERO
                    run.analysis.start to Triple(run.analysis.duration, run.analysis.downtime, failed)
                }
            }
        }.map { (name, data) ->
            val times = data.map { it.first.epochMillis }
            val durations = data.map { it.second.first.toJavaDuration().toMillis() }
            val downtimes = data.map { it.second.second.toJavaDuration().toMillis() }
            val failed = data.map { it.second.third.toJavaDuration().toMillis() }

            val dfDuration = dataFrameOf(
                PlotCommons.DATE to times,
                TIME to durations,
                PlotCommons.SERIES to List(times.size) { DURATION },
            )

            val dfDowntime = dataFrameOf(
                PlotCommons.DATE to times,
                TIME to downtimes,
                PlotCommons.SERIES to List(times.size) { DOWNTIME },
            )

            val dfFailed = dataFrameOf(
                PlotCommons.DATE to times,
                TIME to failed,
                PlotCommons.SERIES to List(times.size) { "Failed Bosses" },
            )

            dfDuration.concat(dfDowntime, dfFailed).plot {
                line {
                    dateX()
                    y(TIME) {
                        axis.name = TIME.uppercased(0)
                        axis.breaks(format = "%Hh %Mm %Ss")
                    }

                    color(PlotCommons.SERIES) {
                        legend.name = ""
                        scale = categorical(
                            DURATION to Color.GREEN,
                            DOWNTIME to Color.RED,
                            "Failed Bosses" to Color.rgb(130, 0, 0),
                        )
                    }

                    width = 1.3
                }

                hLine {
                    yIntercept(listOf(durations.average()))
                    width = 0.8
                    type = LineType.DASHED
                    color = Color.GREEN
                }

                layout {
                    caption = "$name Duration Evolution"
                    x.axis.name = ""
                    x.axis.breaks(times)
                    defaultStyle {
                        xAxis.text {
                            angle = 90.0
                        }
                    }
                }
            }.toPNG()
        }.toMono()
}
