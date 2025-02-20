package org.inquest.discord.isac

import discord4j.rest.util.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.temporal.Temporal
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

fun Duration.inRoundedMinutes() = (inWholeSeconds / 60.0).roundToInt()

fun Double.format(pattern: String) =
    DecimalFormat(pattern, DecimalFormatSymbols(Locale.GERMAN)).format(this)

fun Int.padded(padding: Int) = "%1$${padding}s".format(this)

fun Long.padded(padding: Int) = "%1$${padding}s".format(this)

fun Temporal.toDiscordTimestamp() = "<t:${Instant.from(this).epochSecond}>"

fun StringBuilder.appendTimestamp(temporal: Temporal) = append(" ${temporal.toDiscordTimestamp()} ")

fun StringBuilder.space() = append(' ')

fun StringBuilder.appendBold(any: Any) = append("**$any**")

fun StringBuilder.appendItalic(any: Any) = append("_${any}_")

fun StringBuilder.appendMono(any: Any) = append("``$any``")

fun StringBuilder.appendUnderlined(any: Any) = append("__${any}__")