package org.inquest.utils

import io.smallrye.mutiny.Uni
import org.reactivestreams.FlowAdapters
import org.slf4j.Logger
import reactor.core.publisher.Mono
import reactor.core.publisher.ParallelFlux

fun <T> Uni<T>.toMono(): Mono<T> = Mono.fromCompletionStage(subscribeAsCompletionStage())

fun <T> Mono<T>.toUni(): Uni<T> = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this)).flatMap {
    if (it == null) Uni.createFrom().nothing() else Uni.createFrom().item(it)
}

fun <T> Uni<T?>.mapNotNull(): Uni<T> = flatMap {
    if (it == null) {
        Uni.createFrom().nothing()
    } else {
        Uni.createFrom().item(it)
    }
}

fun <T> Mono<T>.infoLog(logger: Logger, log: (T) -> String, vararg args: Any): Mono<T> = doOnNext { logger.info(log(it), args) }

fun <T> Mono<T>.infoLog(logger: Logger, log: String, vararg args: Any): Mono<T> = doOnNext { logger.info(log, args) }

fun <T> Mono<T>.debugLog(logger: Logger, log: String, vararg args: Any): Mono<T> = doOnNext { logger.debug(log, args) }

fun <T> Mono<T>.errorLog(logger: Logger, log: String, ex: Throwable? = null, vararg args: Any): Mono<T> = doOnNext {
    logger.error(log, ex, args)
}

fun <T> ParallelFlux<T>.debugLog(logger: Logger, log: (T) -> String): ParallelFlux<T> = doOnNext { logger.debug(log(it)) }
