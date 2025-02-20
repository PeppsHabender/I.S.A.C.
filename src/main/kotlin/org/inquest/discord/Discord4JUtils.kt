package org.inquest.discord

import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.InteractionReplyEditMono
import discord4j.core.spec.MessageCreateFields
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ImmutableApplicationCommandRequest
import discord4j.discordjson.possible.Possible
import discord4j.rest.util.Color
import java.io.ByteArrayInputStream

fun ImmutableApplicationCommandRequest.Builder.withStringOption(
    name: String,
    description: String = "",
    required: Boolean = true,
) = addOption(baseCommandOption(name, description, required).type(ApplicationCommandOption.Type.STRING.value).build())

fun ImmutableApplicationCommandRequest.Builder.withBooleanOption(
    name: String,
    description: String = "",
    required: Boolean = true,
) = addOption(baseCommandOption(name, description, required).type(ApplicationCommandOption.Type.BOOLEAN.value).build())

private fun baseCommandOption(
    name: String,
    description: String = "",
    required: Boolean = true,
) = ApplicationCommandOptionData.builder()
    .name(name)
    .description(description)
    .required(required)

fun createEmbed(
    description: String,
    title: String? = null,
    color: Color? = null,
): EmbedCreateSpec = EmbedCreateSpec
    .builder()
    .description(description)
    .title(title.possible())
    .color(color.possible()).build()

fun InteractionReplyEditMono.withEmbed(
    description: String,
    title: String? = null,
    color: Color? = null,
) = withEmbeds(createEmbed(description, title, color))

fun InteractionReplyEditMono.withFile(
    fileName: String,
    bytes: ByteArrayInputStream,
) = withFiles(MessageCreateFields.File.of(fileName, bytes))

fun InteractionReplyEditMono.withFile(
    fileName: String,
    str: String,
) = withFile(fileName, str.byteInputStream())

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
}

object CustomColors {
    val TRANSPARENT_COLOR = Color.of(43, 45, 49)
    val GOLD_COLOR = Color.of(237, 178, 39)
    val SILVER_COLOR = Color.of(130, 138, 146)
    val GREEN_COLOR = Color.of(0, 148, 0)
    val RED_COLOR = Color.of(129, 0, 0)
}
