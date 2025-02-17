package org.inquest.utils

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import io.smallrye.mutiny.Uni
import org.inquest.entities.JsonLog
import org.reactivestreams.FlowAdapters
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

fun <T> Uni<T>.toMono() = Mono.from(FlowAdapters.toPublisher(convert().toPublisher()))

fun <T> Mono<T>.toUni() = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this))

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")
fun JsonLog.startTime(): OffsetDateTime = this.timeStartStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()
fun JsonLog.endTime(): OffsetDateTime = this.timeEndStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun ChatInputInteractionEvent.optionAsString(option: String): String? = getOption(option).flatMap {
    it.value
}.map { it.asString() }.orElse(null)

fun ChatInputInteractionEvent.optionAsBoolean(option: String, default: Boolean = false): Boolean = getOption(option).flatMap {
    it.value
}.map { it.asBoolean() }.orElse(default)

fun Duration.inRoundedMinutes() = (inWholeSeconds / 60.0).roundToInt()

val ignoredBosses = setOf(
    262401, // Freezie
    132354, // River
    132355, // Broken King
    132356, // Soul Eater
    132357, // Eyes
    131843, // Twisted Castle
    131841, // Escort
    131586, // Trio
    262662, // Varinia Stormsounder
)

object CustomEmojis {
    const val TIME = " <:time:1340019219638652968> "
    const val INFIGHT = " <:infight:1340021072942202981> "
    const val DOWNTIME = " <:downtime:1340021105699717231> "
    const val PULLS = " <:pulls:1340023111227277382> "
    const val SUCCESS = " <:success:1340023129929551913> "
    const val CM_SUCCESS = " <:cmsuccess:1340023147059216404> "
    const val WIPE = " <:wipe:1340023173785194628> "
    const val GROUP_DPS = " <:groupdps:1340055121337389217> "
    const val TOP_STATS = " <:topstats:1340084035812331664> "
    const val DPS = " <:dps:1340084146755735583> "
    const val CC = " <:cc:1340084228120903790> "
    const val RES_TIME = " <:restime:1340084310123872306> "
    const val CONDI_CLEANSE = " <:condicleanse:1340084376662446140> "
    const val BOON_STRIPS = " <:boonstrips:1340084457217982464> "
    const val DMG_TAKEN = " <:damagetaken:1340084800710774814> "
    const val DOWNSTATES = " <:downstates:1340084874027077692> "
    const val HEAL = " <:heal:1340085836510924810> "
    const val BARRIER = " <:barrier:1340085847155806310> "
    const val SEC_TOP_STATS = " <:sectopstats:1340087197541597214> "
    const val WIPES = " <:wipes:1340797369242878028> "
}