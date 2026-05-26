package org.inquest.discord.support

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.ThreadChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionApplicationCommandCallbackReplyMono
import discord4j.core.spec.InteractionReplyEditMono
import discord4j.core.spec.MessageCreateFields
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.possible.Possible
import discord4j.rest.util.Color
import org.inquest.shared.reactor.errorLog
import org.inquest.shared.text.splitStringByNewLine
import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.temporal.Temporal
import kotlin.jvm.optionals.getOrNull

/**
 * Adds a new string option to the current command.
 */
fun stringOption(name: String, description: String = "", required: Boolean = true): ApplicationCommandOptionData =
    baseCommandOption(name, description, required)
        .type(ApplicationCommandOption.Type.STRING.value)
        .build()

/**
 * Adds a new boolean option to the current command.
 */
fun booleanOption(name: String, description: String = "", required: Boolean = true): ApplicationCommandOptionData =
    baseCommandOption(name, description, required)
        .type(ApplicationCommandOption.Type.BOOLEAN.value)
        .build()

private fun baseCommandOption(name: String, description: String = "", required: Boolean = true) = ApplicationCommandOptionData.builder()
    .name(name)
    .description(description)
    .required(required)

/**
 * Creates a new embed within this thread, or falls back to an error one.
 */
fun ThreadChannel.createMessageOrShowError(
    logger: Logger,
    message: () -> Array<EmbedCreateSpec>,
    error: (Throwable) -> EmbedCreateSpec,
): Mono<Message> = try {
    Flux.fromIterable(message().chunkedByEmbedSize())
        .concatMap { createMessage(*it.toTypedArray()) }
        .last()
        .onErrorResume { ex ->
            createMessage(error(ex))
                .withFiles(MessageCreateFields.File.of("stacktrace.log", ex.stackTraceToString().byteInputStream()))
                .errorLog(logger, ex.message ?: "Unknown Message", ex)
        }
} catch (ex: Throwable) {
    createMessage(error(ex))
        .withFiles(MessageCreateFields.File.of("stacktrace.log", ex.stackTraceToString().byteInputStream()))
        .errorLog(logger, ex.message ?: "Unknown Message", ex)
}

private fun Array<EmbedCreateSpec>.chunkedByEmbedSize(): List<List<EmbedCreateSpec>> {
    val chunks = mutableListOf<MutableList<EmbedCreateSpec>>()
    var currentChunk = mutableListOf<EmbedCreateSpec>()
    var currentSize = 0

    forEach { embed ->
        val embedSize = embed.estimatedSize()
        if (currentChunk.isNotEmpty() && currentSize + embedSize > MAX_MESSAGE_EMBED_SIZE) {
            chunks += currentChunk
            currentChunk = mutableListOf()
            currentSize = 0
        }

        currentChunk += embed
        currentSize += embedSize
    }

    if (currentChunk.isNotEmpty()) {
        chunks += currentChunk
    }

    return chunks
}

private fun EmbedCreateSpec.estimatedSize(): Int = (description().toOptional().getOrNull()?.length ?: 0) +
    (title().toOptional().getOrNull()?.length ?: 0)

private const val MAX_MESSAGE_EMBED_SIZE = 6000

/**
 * Makes this embed dynamic, should it reach more than 4096 chars, it is split up into multiple embeds.
 * Title and footer will be added to the first and last one respectively.
 */
fun EmbedCreateSpec.dynamic(): Array<EmbedCreateSpec> = if (this.isDescriptionPresent) {
    val split = description().get().splitStringByNewLine().map {
        createEmbed(
            it,
            color = color().toOptional().getOrNull(),
        )
    }.toTypedArray()

    split.first().let { split[0] = it.withTitle(title()) }
    split.last().let { split[split.size - 1] = it.withFooter(footer()) }
    split
} else {
    arrayOf(this)
}

/**
 * Creates a new embed.
 */
fun createEmbed(description: String, title: String? = null, color: Color? = null): EmbedCreateSpec = EmbedCreateSpec
    .builder()
    .description(description)
    .title(title.possible())
    .color(color.possible()).build()

/**
 * Adds a new embed to this reply.
 */
fun InteractionReplyEditMono.withEmbed(description: String, title: String? = null, color: Color? = null): InteractionReplyEditMono =
    withEmbeds(createEmbed(description, title, color))

/**
 * Adds a new file to this reply.
 */
fun InteractionReplyEditMono.withFile(fileName: String, bytes: ByteArrayInputStream): InteractionReplyEditMono =
    withFiles(MessageCreateFields.File.of(fileName, bytes))

fun InteractionApplicationCommandCallbackReplyMono.withFile(
    fileName: String,
    bytes: ByteArrayInputStream,
): InteractionApplicationCommandCallbackReplyMono = withFiles(MessageCreateFields.File.of(fileName, bytes))

/**
 * Adds a new file to this reply.
 */
fun InteractionReplyEditMono.withFile(fileName: String, str: String) = withFile(fileName, str.byteInputStream())

/**
 * Converts [this] into a discord timestamp of the form <t:...>
 */
fun Temporal.toDiscordTimestamp() = "<t:${Instant.from(this).epochSecond}>"

fun ChatInputInteractionEvent.optionAsOptions(option: String): List<ApplicationCommandInteractionOption> = getOption(option).map {
    it.options
}.orElse(emptyList())

fun ChatInputInteractionEvent.optionAsString(option: String): String? = getOption(option).flatMap {
    it.value
}.map { it.asString() }.getOrNull()

fun ChatInputInteractionEvent.optionAsBoolean(option: String): Boolean? = getOption(option).flatMap {
    it.value
}.map { it.asBoolean() }.getOrNull()

fun ChatInputInteractionEvent.optionAsBoolean(option: String, default: Boolean): Boolean = getOption(option).flatMap {
    it.value
}.map { it.asBoolean() }.orElse(default)

private fun <T> (T)?.possible(): Possible<T> = if (this == null) Possible.absent() else Possible.of(this)
