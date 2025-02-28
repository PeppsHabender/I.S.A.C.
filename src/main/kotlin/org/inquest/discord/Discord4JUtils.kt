package org.inquest.discord

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.channel.ThreadChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionReplyEditMono
import discord4j.core.spec.MessageCreateFields
import discord4j.core.spec.MessageCreateMono
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import discord4j.discordjson.possible.Possible
import discord4j.rest.util.Color
import io.smallrye.mutiny.Uni
import org.inquest.utils.splitStringByNewLine
import org.reactivestreams.FlowAdapters
import reactor.core.publisher.Mono
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.temporal.Temporal
import kotlin.jvm.optionals.getOrNull

/**
 * Adds a new string option to the current command.
 */
fun ImmutableApplicationCommandRequest.Builder.withStringOption(name: String, description: String = "", required: Boolean = true) =
    addOption(baseCommandOption(name, description, required).type(ApplicationCommandOption.Type.STRING.value).build())

/**
 * Adds a new boolean option to the current command.
 */
fun ImmutableApplicationCommandRequest.Builder.withBooleanOption(name: String, description: String = "", required: Boolean = true) =
    addOption(baseCommandOption(name, description, required).type(ApplicationCommandOption.Type.BOOLEAN.value).build())

private fun baseCommandOption(name: String, description: String = "", required: Boolean = true) = ApplicationCommandOptionData.builder()
    .name(name)
    .description(description)
    .required(required)

/**
 * Creates a new embed within this thread, or falls back to an error one.
 */
fun ThreadChannel.createMessageOrShowError(
    message: () -> Array<EmbedCreateSpec>,
    error: (Throwable) -> EmbedCreateSpec,
): MessageCreateMono = try {
    createMessage(*message())
} catch (ex: Throwable) {
    ex.printStackTrace()
    createMessage(error(ex)).withFiles(MessageCreateFields.File.of("stacktrace.log", ex.stackTraceToString().byteInputStream()))
}

/**
 * Makes this embed dynamic, should it reach more than 4096 chars, it is split up into multiple embeds. Title and footer will be added to the first and last one respectively.
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
fun InteractionReplyEditMono.withEmbed(description: String, title: String? = null, color: Color? = null) =
    withEmbeds(createEmbed(description, title, color))

/**
 * Adds a new file to this reply.
 */
fun InteractionReplyEditMono.withFile(fileName: String, bytes: ByteArrayInputStream) =
    withFiles(MessageCreateFields.File.of(fileName, bytes))

/**
 * Adds a new file to this reply.
 */
fun InteractionReplyEditMono.withFile(fileName: String, str: String) = withFile(fileName, str.byteInputStream())

/**
 * Converts [this] into a discord timestamp of the form <t:...>
 */
fun Temporal.toDiscordTimestamp() = "<t:${Instant.from(this).epochSecond}>"

fun <T> Uni<T>.toMono() = Mono.from(FlowAdapters.toPublisher(convert().toPublisher()))

fun <T> Mono<T>.toUni() = Uni.createFrom().publisher(FlowAdapters.toFlowPublisher(this)).flatMap {
    if (it == null) Uni.createFrom().nothing() else Uni.createFrom().item(it)
}

fun ChatInputInteractionEvent.optionAsString(option: String): String? = getOption(option).flatMap {
    it.value
}.map { it.asString() }.orElse(null)

fun ChatInputInteractionEvent.optionAsBoolean(option: String, default: Boolean = false): Boolean = getOption(option).flatMap {
    it.value
}.map { it.asBoolean() }.orElse(default)

private fun <T> (T)?.possible(): Possible<T> = if (this == null) Possible.absent() else Possible.of(this)
