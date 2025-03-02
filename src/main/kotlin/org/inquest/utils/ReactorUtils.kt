package org.inquest.utils

import io.smallrye.mutiny.Uni
import org.inquest.utils.LogExtension.LOG
import org.reactivestreams.FlowAdapters
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

fun <T> Uni<T>.toMono(): Mono<T> = Mono.from(FlowAdapters.toPublisher(convert().toPublisher()))

fun <T> Mono<T>.toUni(): Uni<T> = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this)).flatMap {
    if (it == null) Uni.createFrom().nothing() else Uni.createFrom().item(it)
}

fun <T> Mono<T>.infoLog(log: (T) -> String, vararg args: Any): Mono<T> = doOnNext { LOG.info(log(it), args) }

fun <T> Mono<T>.infoLog(log: String, vararg args: Any): Mono<T> = doOnNext { LOG.info(log, args) }

fun <T> Mono<T>.errorLog(log: String, ex: Throwable? = null, vararg args: Any): Mono<T> = doOnNext { LOG.error(log, ex, args) }

fun <T> ParallelFlux<T>.debugLog(log: (T) -> String): ParallelFlux<T> = doOnNext { LOG.debug(log(it)) }
