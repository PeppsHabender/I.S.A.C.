package org.inquest.discord

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
import java.io.ByteArrayInputStream
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
    createMessage(error(ex)).withFiles(MessageCreateFields.File.of("stacktrace.log", ex.stackTraceToString().byteInputStream()))
}

/**
 * Makes this embed dynamic, should it reach more than 4096 chars, it is split up into multiple embeds. Title and footer will be added to the first and last one respectively.
 */
fun EmbedCreateSpec.dynamic(): Array<EmbedCreateSpec> = if (this.isDescriptionPresent) {
    val split = splitStringByNewLine(description().get()).map {
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

private fun splitStringByNewLine(input: String, maxLength: Int = 4096): List<String> {
    if (input.length <= maxLength) return listOf(input)

    return input.split("\n").fold(mutableListOf()) { acc, line ->
        if (line.length > maxLength) {
            throw IllegalArgumentException("A single line exceeds the maximum allowed length of $maxLength characters.")
        }
        if (acc.isEmpty() || (acc.last().length + line.length + if (acc.last().isEmpty()) 0 else 1) > maxLength) {
            acc.add(line)
        } else {
            acc[acc.lastIndex] += "\n$line"
        }
        acc
    }
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

private fun <T> (T)?.possible(): Possible<T> = if (this == null) Possible.absent() else Possible.of(this)

object CustomEmojis {
    const val TIME = " <:time:1340019219638652968> "
    const val INFIGHT = " <:infight:1340021072942202981> "
    const val DOWNTIME = " <:downtime:1340021105699717231> "
    const val PULLS = " <:pulls:1340023111227277382> "
    const val SUCCESS = " <:success:1340023129929551913> "
    const val CM_SUCCESS = " <:cmsuccess:1340023147059216404> "
    const val WIPE = " <:wipe:1340023173785194628> "
    const val GROUP_DPS = " <:groupdps:1340055121337389217> "
    const val TOP_STATS = " <:topstats:1340084035812331664> "
    const val DPS = " <:dps:1340084146755735583> "
    const val CC = " <:cc:1340084228120903790> "
    const val RES_TIME = " <:restime:1340084310123872306> "
    const val CONDI_CLEANSE = " <:condicleanse:1340084376662446140> "
    const val BOON_STRIPS = " <:boonstrips:1340084457217982464> "
    const val DMG_TAKEN = " <:damagetaken:1340084800710774814> "
    const val DOWNSTATES = " <:downstates:1340084874027077692> "
    const val HEAL = " <:heal:1340085836510924810> "
    const val BARRIER = " <:barrier:1340085847155806310> "
    const val SEC_TOP_STATS = " <:sectopstats:1340087197541597214> "
    const val WIPES = " <:wipes:1340797369242878028> "
    const val CONDI = " <:condi:1342553033112027239> "
    const val POWER = " <:power:1342553024836665345> "

    private val PROFESSIONS = mapOf(
        "guardian" to "<:guardian:1342553720310988871>",
        "dragonhunter" to "<:dragonhunter:1342553707748786186>",
        "firebrand" to "<:firebrand:1342553696596262973>",
        "willbender" to "<:willbender:1342553684780777594>",
        "revenant" to "<:revenant:1342553665436647556>",
        "herald" to "<:herald:1342553652828704808>",
        "renegade" to "<:renegade:1342553640518549534>",
        "vindicator" to "<:vindicator:1342553616862679172>",
        "warrior" to "<:warrior:1342553606078992405>",
        "berserker" to "<:berserker:1342553586810224670>",
        "spellbreaker" to "<:spellbreaker:1342553565066956904>",
        "bladesworn" to "<:bladesworn:1342553555353210962>",
        "engineer" to "<:engineer:1342553529578946631>",
        "scrapper" to "<:scrapper:1342553517373526170>",
        "holosmith" to "<:holosmith:1342553492472070174>",
        "mechanist" to "<:mechanist:1342553456832942110>",
        "ranger" to "<:ranger:1342553443981721790>",
        "druid" to "<:druid:1342553432338206782>",
        "soulbeast" to "<:soulbeast:1342553418979610714>",
        "untamed" to "<:untamed:1342553405721149471>",
        "thief" to "<:thief:1342553394509778964>",
        "daredevil" to "<:daredevil:1342553378282147840>",
        "deadeye" to "<:deadeye:1342553355888758854>",
        "specter" to "<:specter:1342553347990749395>",
        "elementalist" to "<:elementalist:1342553318655918080>",
        "tempest" to "<:tempest:1342553265900093490>",
        "weaver" to "<:weaver:1342553257964208288>",
        "catalyst" to "<:catalyst:1342553247877038193>",
        "mesmer" to "<:mesmer:1342553238087401542>",
        "chronomancer" to "<:chronomancer:1342553207540416512>",
        "mirage" to "<:mirage:1342553197394395278>",
        "virtuoso" to "<:virtuoso:1342553149205909535>",
        "necromancer" to "<:necromancer:1342553079886909490>",
        "reaper" to "<:reaper:1342553068574609479>",
        "scourge" to "<:scourge:1342553058571456592>",
        "harbinger" to "<:harbinger:1342553045632028784>",
    )

    /**
     * @return Emote for the given [profession]
     */
    fun professionEmote(profession: String?) = profession?.let {
        " ${PROFESSIONS[it.lowercase()]} "
    }
}

object CustomColors {
    val TRANSPARENT_COLOR = Color.of(43, 45, 49)
    val GOLD_COLOR = Color.of(237, 178, 39)
    val SILVER_COLOR = Color.of(130, 138, 146)
    val GREEN_COLOR = Color.of(0, 148, 0)
    val RED_COLOR = Color.of(129, 0, 0)
}
