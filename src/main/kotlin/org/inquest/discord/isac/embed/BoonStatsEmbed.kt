package org.inquest.discord.isac.embed

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.spec.EmbedCreateSpec
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.analysis.model.RunAnalysis
import org.inquest.catalog.IsacDataService
import org.inquest.catalog.model.IsacBoon
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.shared.collections.mapWithPutDefault
import org.inquest.shared.numbers.DoubleExtensions.format
import org.inquest.shared.text.appendBold
import org.inquest.shared.text.appendItalic
import org.inquest.shared.text.appendMono
import org.inquest.shared.text.padRight
import org.inquest.shared.text.space

@ApplicationScoped
class BoonStatsEmbed {
    companion object {
        private const val TITLE = "${CustomEmojis.MIGHT}__Boon Uptimes__"
        private const val DESCR_STARTER = "Excludes \"event encounters\", Xera, Dhuum, Qadim and Qadim the Peerless!\n\n"
    }

    @Inject
    private lateinit var isacDataService: IsacDataService

    fun createOverviewEmbed(analysis: RunAnalysis, event: ChatInputInteractionEvent): EmbedCreateSpec = createEmbed(
        StringBuilder().appendItalic(DESCR_STARTER).createBoonAnalysis(analysis).toString(),
        TITLE,
        CustomColors.ORANGE_COLOR,
    )

    fun StringBuilder.createBoonAnalysis(analysis: RunAnalysis): StringBuilder {
        val boonAnalyses: Map<String, StringBuilder> by mapWithPutDefault { StringBuilder().appendBold("Subgroup $it").appendLine() }

        isacDataService.boonData.values.sorted().forEach { boon ->
            analysis.pulls.filter { it.success && !isacDataService.ignoreForBoons(it.eiEncounterId) }.flatMap { pull ->
                pull.boonUptimes.mapNotNull { (group, uptimes) ->
                    uptimes.uptimes[boon.id.toString()]?.let { group to (it to pull.link) }
                }
            }.groupBy({ it.first }) {
                it.second
            }.map { (k, v) ->
                val sorted = v.map { if (it.first.isNaN()) 0.0 to it.second else it }.sortedBy { it.first }

                k to BoonStat(
                    boon,
                    sorted.map { it.first }.average(),
                    sorted.first().first,
                    sorted.last().first,
                    boon.emote,
                    sorted.first().second,
                )
            }.forEach { (k, v) ->
                boonAnalyses.getValue(k).createBoonStat(v)
            }
        }

        boonAnalyses.entries.sortedBy { it.key }.map { it.value }.forEach {
            append(it.toString())
            appendLine()
        }

        return this
    }

    private fun StringBuilder.createBoonStat(boonStat: BoonStat) {
        fun StringBuilder.appendPercent(extractor: BoonStat.() -> Double) {
            append("``")
            boonStat.extractor().let {
                append((if (it.isNaN()) "0.0" else it.format("##.#")).padRight(5))
            }
            if (boonStat.isacBoon.isStacks) space() else append("%")
            append("``")
        }

        boonStat.emote?.let(::append)
        appendMono(" >>")
        append("\u2300")
        appendPercent { this.average }
        space()
        append("|")
        append(CustomEmojis.ARROW_UP)
        appendPercent { this.highest }
        space()
        append("|")
        append(CustomEmojis.ARROW_DOWN)
        append("[")
        appendPercent { this.lowest }
        append("](")
        append(boonStat.link)
        append(")")
        appendLine()
    }
}

private data class BoonStat(
    val isacBoon: IsacBoon,
    val average: Double,
    val lowest: Double,
    val highest: Double,
    val emote: String?,
    val link: String,
)
