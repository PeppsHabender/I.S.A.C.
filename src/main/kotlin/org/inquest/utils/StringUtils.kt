package org.inquest.utils

import discord4j.core.`object`.entity.Member
import org.inquest.discord.toDiscordTimestamp
import java.time.temporal.Temporal

fun String.padRight(len: Int, with: Char = ' ') = if (this.length < len) {
    this + with.toString().repeat(len - this.length)
} else {
    this
}

fun String.uppercased(vararg idxs: Int) = this.mapIndexed { i, c ->
    if (i in idxs) c.uppercase() else c.lowercase()
}.joinToString(separator = "")

fun String.splitStringByNewLine(maxLength: Int = 4096): List<String> {
    if (this.length <= maxLength) return listOf(this)

    return this.split("\n").fold(mutableListOf()) { acc, line ->
        if (acc.isEmpty() || (acc.last().length + line.length + if (acc.last().isEmpty()) 0 else 1) > maxLength) {
            acc.add(line)
        } else {
            acc[acc.lastIndex] += "\n$line"
        }
        acc
    }
}

/**
 * Appends the discord timestamp to [this] using [toDiscordTimestamp]
 */
fun StringBuilder.appendTimestamp(temporal: Temporal) = append(" ${temporal.toDiscordTimestamp()} ")

/**
 * Mentions the given [member].
 */
fun StringBuilder.mention(member: Member) = append("<@${member.id.asString()}>")

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
