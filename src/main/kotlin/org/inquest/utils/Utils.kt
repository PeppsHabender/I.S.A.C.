package org.inquest.utils

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import io.smallrye.mutiny.Uni
import org.inquest.entities.JsonLog
import org.reactivestreams.FlowAdapters
import reactor.core.publisher.Mono
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> Uni<T>.toMono() = Mono.from(FlowAdapters.toPublisher(convert().toPublisher()))

fun <T> Mono<T>.toUni() = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this)).flatMap {
    if (it == null) Uni.createFrom().nothing() else Uni.createFrom().item(it)
}

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")

fun JsonLog.startTime(): OffsetDateTime = this.timeStartStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun JsonLog.endTime(): OffsetDateTime = this.timeEndStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun ChatInputInteractionEvent.optionAsString(option: String): String? = getOption(option).flatMap {
    it.value
}.map { it.asString() }.orElse(null)

fun ChatInputInteractionEvent.optionAsBoolean(option: String, default: Boolean = false): Boolean = getOption(option).flatMap {
    it.value
}.map { it.asBoolean() }.orElse(default)

fun String.uppercased(vararg idxs: Int) = this.mapIndexed { i, c ->
    if (i in idxs) c.uppercase() else c.lowercase()
}.joinToString(separator = "")

fun Iterable<Double>.averageOrNull(): Double? = if (this.iterator().hasNext()) {
    average()
} else {
    null
}

fun <K, V> mapWithPutDefault(defaultValue: (key: K) -> V): ReadWriteProperty<Any?, Map<K, V>> =
    object : ReadWriteProperty<Any?, Map<K, V>> {
        private var map: MutableMap<K, V> = with(mutableMapOf<K, V>()) {
            withDefault { key -> getOrPut(key) { defaultValue(key) } }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): Map<K, V> = map

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Map<K, V>) {
            this.map = value.toMutableMap()
        }
    }
