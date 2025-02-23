package org.inquest.discord.isac

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.temporal.Temporal
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.time.Duration

/**
 * @return The duration in minutes, rounded to int
 */
fun Duration.inRoundedMinutes() = (inWholeSeconds / 60.0).roundToInt()

/**
 * @return A string with [this] formatted according to [pattern]
 */
fun Double.format(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.GERMAN)).also {
    it.minimumFractionDigits = 1
}.format(this)

/**
 * Pads this with [padding] spaces
 */
fun Int.padded(padding: Int) = "%1$${padding}s".format(this)

/**
 * Pads this with [padding] spaces
 */
fun Long.padded(padding: Int) = "%1$${padding}s".format(this)

/**
 * Converts [this] into a discord timestamp of the form <t:...>
 */
fun Temporal.toDiscordTimestamp() = "<t:${Instant.from(this).epochSecond}>"

/**
 * Appends the discord timestamp to [this] using [toDiscordTimestamp]
 */
fun StringBuilder.appendTimestamp(temporal: Temporal) = append(" ${temporal.toDiscordTimestamp()} ")

/**
 * Appends a space
 */
fun StringBuilder.space() = append(' ')

/**
 * Appends md text formatted in bold
 */
fun StringBuilder.appendBold(any: Any) = append("**$any**")

/**
 * Appends md text formatted in italic
 */
fun StringBuilder.appendItalic(any: Any) = append("_${any}_")

/**
 * Appends md monospace text
 */
fun StringBuilder.appendMono(any: Any) = append("``$any``")

/**
 * Appends md underlined text
 */
fun StringBuilder.appendUnderlined(any: Any) = append("__${any}__")
