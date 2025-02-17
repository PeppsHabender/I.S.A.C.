package org.inquest.utils

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import io.smallrye.mutiny.Uni
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.inquest.entities.JsonLog
import org.reactivestreams.FlowAdapters
import reactor.core.publisher.Mono

fun <T> Uni<T>.toMono() = Mono.from(FlowAdapters.toPublisher(convert().toPublisher()))

fun <T> Mono<T>.toUni() = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this))

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")

fun JsonLog.startTime(): OffsetDateTime =
    this.timeStartStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun JsonLog.endTime(): OffsetDateTime =
    this.timeEndStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun ChatInputInteractionEvent.optionAsString(option: String): String? =
    getOption(option).flatMap { it.value }.map { it.asString() }.orElse(null)

fun ChatInputInteractionEvent.optionAsBoolean(option: String, default: Boolean = false): Boolean =
    getOption(option).flatMap { it.value }.map { it.asBoolean() }.orElse(default)
