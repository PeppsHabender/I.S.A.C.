package org.inquest.discord.commands

import org.inquest.discord.booleanOption
import org.inquest.discord.commands.CommonOptions.BOONS_OPTION
import org.inquest.discord.commands.CommonOptions.HEAL_OPTION
import org.inquest.discord.commands.CommonOptions.NAME_OPTION
import org.inquest.discord.commands.CommonOptions.WM_OPTION
import org.inquest.discord.stringOption
import java.util.UUID

fun interactionId() = UUID.randomUUID().toString().substringBefore("-")

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
