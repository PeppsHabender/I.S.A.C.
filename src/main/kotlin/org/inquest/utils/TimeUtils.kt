package org.inquest.utils

import org.inquest.entities.logs.JsonLog
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.time.Duration

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX")

fun JsonLog.startTime(): OffsetDateTime = this.timeStartStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

fun JsonLog.endTime(): OffsetDateTime = this.timeEndStd?.let { OffsetDateTime.parse(it, formatter) } ?: OffsetDateTime.now()

/**
 * @return The duration in minutes, rounded to int
 */
fun Duration.inRoundedMinutes() = (inWholeSeconds / 60.0).roundToInt()
