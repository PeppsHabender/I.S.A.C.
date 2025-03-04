package org.inquest.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

interface WithLogger

object LogExtension {
    val loggers: Map<KClass<*>, Logger> by mapWithPutDefault {
        LoggerFactory.getLogger(it.java)
    }

    inline val <reified T : WithLogger> T.LOG
        get() = loggers.getValue(T::class)
}
