package org.inquest.discord

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateSpec
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import discord4j.rest.util.Color
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.context.ManagedExecutor
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.AnalyzerService
import org.inquest.clients.DpsReportClient
import org.inquest.entities.PlayerAnalysis
import org.inquest.entities.Pull
import org.inquest.entities.RunAnalysis
import org.inquest.utils.BossData
import org.inquest.utils.CustomEmojis
import org.inquest.utils.inRoundedMinutes
import org.inquest.utils.optionAsBoolean
import org.inquest.utils.optionAsString
import org.inquest.utils.startTime
import org.inquest.utils.toMono
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale
import java.util.stream.Stream
import kotlin.math.roundToInt
import kotlin.streams.asStream

@ApplicationScoped
class IsacCommand: CommandListener {
    override val name: String = "analyze"

    @RestClient
    private lateinit var dpsReportClient: DpsReportClient

    @Inject
    private lateinit var analyzerService: AnalyzerService

    @Inject
    private lateinit var bossData: BossData

    @Inject
    private lateinit var managedExecutor: ManagedExecutor

    override fun build(): ApplicationCommandRequest = ApplicationCommandRequest.builder()
        .name(name)
        .description("Analyzes the given list of dps.report links")
        .addOption(
            ApplicationCommandOptionData.builder()
                .name("logs")
                .description("Your name")
                .type(ApplicationCommandOption.Type.STRING.value)
                .required(true)
                .build()
        ).addOption(
            ApplicationCommandOptionData.builder()
                .name("name")
                .description("Name of the run")
                .type(ApplicationCommandOption.Type.STRING.value)
                .required(false)
                .build()
        ).addOption(
            ApplicationCommandOptionData.builder()
                .name("with_heal")
                .description("Include heal/barrier stats")
                .type(ApplicationCommandOption.Type.BOOLEAN.value)
                .required(false)
                .build()
        ).build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> = event.deferReply().then(longRunning(event))

    fun longRunning(event: ChatInputInteractionEvent): Mono<Void> {
        return Flux.fromStream(event.extractLogs()).parallel().runOn(Schedulers.fromExecutor(this.managedExecutor)).flatMap { link ->
            this.dpsReportClient.fetchJson(link).map { Pair(link, it) }.toMono()
        }.collectSortedList { o1, o2 ->  o1.second.startTime().compareTo(o2.second.startTime())}
        .map { this.analyzerService.analyze(it) }
        .flatMap { event.editReply().withEmbeds(*it.createEmbeds(event).toTypedArray()) }.then()
    }

    fun ChatInputInteractionEvent.extractLogs(): Stream<String> {
        return DPS_REPORT_RGX.findAll(getOption("logs").flatMap { it.value }.map { it.asString() }.get()).map { it.value }.asStream()
    }

    fun RunAnalysis.createEmbeds(event: ChatInputInteractionEvent): List<EmbedCreateSpec> {
        return listOf(
            createOverview(event),
            createTopStatsView(event, CustomEmojis.TOP_STATS, "Top Stats", 0, Color.of(237, 178, 39)),
            createTopStatsView(event, CustomEmojis.SEC_TOP_STATS, "Second Best", 1, Color.of(130, 138, 146)),
            createSuccessLogs(),
        ).let { ls ->
            if(this.pulls.any { !it.success }) ls + createWipeLogs() else ls
        }
    }

    fun RunAnalysis.createOverview(event: ChatInputInteractionEvent): EmbedCreateSpec = EmbedCreateSpec.builder()
        .title((event.optionAsString("name") ?: "Run Analysis") + " " + this.start.toDiscordTimestamp())
        .description(StringBuilder()
            .append(createTime(this))
            .appendLine()
            .append(createPulls(this))
            .toString()
        ).color(Color.of(43, 45, 49))
        .build()

    fun createTime(analysis: RunAnalysis): String = StringBuilder(CustomEmojis.TIME).apply {
        appendMono("Time :")
        space()
        appendBold(analysis.duration.inRoundedMinutes())
        space()
        appendBold("min")
        append(" |")
        append(CustomEmojis.INFIGHT)
        appendBold((analysis.duration - analysis.downtime).inRoundedMinutes())
        space()
        appendBold("min")
        append(" |")
        append(CustomEmojis.DOWNTIME)
        appendBold(analysis.downtime.inRoundedMinutes())
        space()
        appendBold("min")
    }.toString()

    fun createPulls(analysis: RunAnalysis): String = StringBuilder().apply {
        createPullsAnalysis("Pulls:", CustomEmojis.PULLS, analysis.pulls)

        val cms = analysis.pulls.filter { it.cm }
        if(cms.isNotEmpty()) {
            appendLine()
            createPullsAnalysis("CMs  :", CustomEmojis.CM_SUCCESS, cms)
        }

        appendLine()
        append(CustomEmojis.GROUP_DPS)
        appendMono("Dps  :")
        space()
        appendBold(NumberFormat.getInstance(Locale.GERMAN).format(analysis.groupDps))
    }.toString()

    private fun StringBuilder.createPullsAnalysis(title: String, emoji: String, pulls: List<Pull>) {
        append(emoji)
        appendMono(title)
        space()
        append(' ')
        appendMono(pulls.size.padded(2))
        append(" |")
        append(CustomEmojis.SUCCESS)
        append("Kills: ")
        appendMono(pulls.filter(Pull::success).size.padded(2))
        append(" |")
        append(CustomEmojis.WIPE)
        append("Wipes: ")
        appendMono(pulls.filterNot(Pull::success).size.padded(2))
    }

    private fun RunAnalysis.createTopStatsView(
        event: ChatInputInteractionEvent,
        emoji: String,
        title: String,
        idx: Int,
        color: Color
    ) = EmbedCreateSpec.builder()
        .title(emoji + "__${title}__")
        .description(StringBuilder()
            .append(createTopStats(event, this, idx))
            .toString()
        ).color(color)
        .build()

    private fun createTopStats(event: ChatInputInteractionEvent, analysis: RunAnalysis, idx: Int = 0) = StringBuilder().apply {
        val isTop = idx == 0
        fun <T: Comparable<T>> createTopStat(
            emoji: String,
            title: String,
            sortBy: (PlayerAnalysis) -> T,
            extractor: (PlayerAnalysis) -> T = sortBy,
            formatter: (T) -> Any = { it },
            numPrefix: String = "",
            numSuffix: String = "",
            ascending: Boolean = false
        ) {
            var sorted = analysis.playerStats.sortedByDescending { sortBy(it) }.let {
                if(ascending) it.reversed() else it
            }
            val mapped = sorted.map { extractor(it) }

            if(isTop) {
                val taken = mapped.takeWhile { it == mapped.first() }

                taken.forEachIndexed { i, it ->
                    topStatMd(emoji, title, true, sorted[i].name, numPrefix, formatter(it), numSuffix)
                }
            } else {
                val dropped = mapped.dropWhile { it == mapped.first() }
                sorted = sorted.takeLast(dropped.size)

                dropped.takeWhile { it == dropped.first() }.forEachIndexed { i, it ->
                    topStatMd(emoji, title, false, sorted[i].name, numPrefix, formatter(it), numSuffix)
                }
            }
        }

        createTopStat(
            CustomEmojis.DPS,
            "Dps          :",
            sortBy = { it.avgDpsPos() },
            extractor = { it.avgDps() },
            formatter = { (it / 1000).format("#.# k") },
            numPrefix = "⌀",
            ascending = true
        )
        createTopStat(CustomEmojis.CC, "Cc           :", sortBy = { it.cc })
        createTopStat(
            CustomEmojis.RES_TIME,
            "Res Time     :",
            sortBy = { it.resTime },
            formatter = { it.roundToInt() },
            numSuffix = " s"
        )
        createTopStat(CustomEmojis.CONDI_CLEANSE, "Condi Cleanse:", sortBy = { it.condiCleanse })
        createTopStat(CustomEmojis.BOON_STRIPS, "Boon Strips  :", sortBy = { it.boonStrips })
        if(event.optionAsBoolean("with_heal") && analysis.playerStats.any { it.avgHeal() > 0 }) {
            createTopStat(
                CustomEmojis.HEAL,
                "Hps          :",
                sortBy = { it.avgHeal() },
                formatter = { (it / 1000).format("#.# k") },
            )
            createTopStat(
                CustomEmojis.BARRIER,
                "Bps          :",
                sortBy = { it.avgBarrier() },
                formatter = { (it / 1000).format("#.# k") },
            )
        }
        createTopStat(
            CustomEmojis.DMG_TAKEN,
            "Damage Taken :",
            sortBy = { it.damageTaken },
            formatter = { (it / 1000.0).roundToInt() },
            numSuffix = " k"
        )
        createTopStat(CustomEmojis.DOWNSTATES, "Downstates   :", sortBy = { it.downstates })
    }.toString()

    private fun StringBuilder.topStatMd(emoji: String, title: String, isTop: Boolean, player: String, numPrefix: String, value: Any, numSuffix: String) {
        append(emoji)
        appendMono(title)
        space()
        if(isTop) appendBold(player) else append(player)
        space()
        append("(_")
        append(numPrefix)
        append(value)
        append(numSuffix)
        append("_)")
        appendLine()
    }

    private fun RunAnalysis.createSuccessLogs() = EmbedCreateSpec.builder()
        .title(CustomEmojis.SUCCESS + "__Success Logs__")
        .description(
            StringBuilder().apply {
                this@createSuccessLogs.pulls.filter { it.success }.forEach {
                    this@IsacCommand.bossData.emoteFor(it.bossId, it.cm)?.let { emote ->
                        append("$emote ")
                    }

                    append("[")
                    if(it.cm) append("[CM] ")
                    append(it.boss)
                    space()
                    appendMono("[${it.duration}]")
                    append("](")
                    append(it.link)
                    append(")")
                    appendLine()
                }
            }.toString()
        ).color(Color.of(0, 148, 0))
        .build()

    private fun RunAnalysis.createWipeLogs() = EmbedCreateSpec.builder()
        .title(CustomEmojis.WIPES + "__Wipe Logs__")
        .description(
            StringBuilder().apply {
                this@createWipeLogs.pulls.filter { !it.success }.forEach {
                    append("[")
                    if(it.cm) append("[CM] ")
                    append(bossData.shortName(it.bossId) ?: it.boss)
                    space()
                    appendMono("(${(it.remainingHp * 100).format("#.##")}%)")
                    append("](")
                    append(it.link)
                    append(")")
                    append(" | ")
                }
                deleteRange(length - 3, length)
            }.toString()
        ).color(Color.of(129, 0, 0))
        .build()

    private fun Double.format(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.GERMAN)).format(this)

    companion object {
        private val DPS_REPORT_RGX = Regex("https://(?:[ab]\\.)?dps.report/[\\w-]+(?=\\s*?https|$)")
    }
}