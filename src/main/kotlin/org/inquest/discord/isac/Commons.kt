package org.inquest.discord.isac

import org.inquest.discord.booleanOption
import org.inquest.discord.stringOption
import org.jetbrains.kotlinx.kandy.dsl.continuous
import org.jetbrains.kotlinx.kandy.letsplot.layers.builders.aes.WithX
import java.util.UUID

fun interactionId() = UUID.randomUUID().toString().substringBefore("-")

object CommonIds {
    const val GROUP_DPS_EVOLUTION = "group_dps_stats"
    const val DPS_EVOLUTION = "dps_stats"
    const val TIME_EVOLUTION = "time_stats"
}

/**
 * - (name): Name of the analysis. Default: 'Run Analysis'
 * - (with_heal): Wether to include heal stats or not, heal stats only really make sense if at least both of the healers use the extension. Default: false
 * - (compare_wingman): Wether to include wingman top (support) stats. Default: false
 * - (analyze_boons): Wether to include detailed boon uptime analysis. Default: true
 */
object CommonOptions {
    const val LOGS_OPTION = "logs"
    const val NAME_OPTION = "name"
    const val HEAL_OPTION = "with_heal"
    const val WM_OPTION = "compare_wingman"
    const val BOONS_OPTION = "analyze_boons"

    val DEFAULT_OPTIONS = listOf(
        stringOption(NAME_OPTION, "Name of the run. Default: 'Run Analysis'", required = false),
        booleanOption(HEAL_OPTION, "Include heal/barrier stats. Default: False", required = false),
        booleanOption(WM_OPTION, "Include a wingman bench dps comparison. Default: True", required = false),
        booleanOption(BOONS_OPTION, "Analyze sub-group specific boon uptimes. Default: False", required = false),
    )
}

/**
 * Common constants used in the plotting events.
 */
object PlotCommons {
    /**
     * Date series.
     */
    const val DATE = "date"

    /**
     * Series name for legends.
     */
    const val SERIES = "series"

    /**
     * Dps series.
     */
    const val DPS = "dps"

    /**
     * Average Dps series.
     */
    const val AVERAGE_DPS = "Avg Dps"

    /**
     * Average boon dps series.
     */
    const val AVERAGE_BOON_DPS = "Avg Boon Dps"

    /**
     * Plot name used for discord returns.
     */
    const val PLOT_FILE = "plot.png"

    /**
     * Adds the [DATE] to the x-axis.
     */
    fun WithX.dateX() = x(DATE) {
        scale = continuous()
        axis.breaks(format = "%d.%m.%Y")
    }
}
