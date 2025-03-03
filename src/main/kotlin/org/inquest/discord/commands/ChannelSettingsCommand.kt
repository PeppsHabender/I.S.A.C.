package org.inquest.discord.commands

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.`object`.command.ApplicationCommandInteractionOption
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.core.`object`.entity.Member
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.CommandListener
import org.inquest.discord.commands.CommonOptions.BOONS_OPTION
import org.inquest.discord.commands.CommonOptions.HEAL_OPTION
import org.inquest.discord.commands.CommonOptions.NAME_OPTION
import org.inquest.discord.commands.CommonOptions.WM_OPTION
import org.inquest.discord.commands.isac.ErrorEmbeds.CHANNEL_CONFIG_EXC_MSG
import org.inquest.discord.commands.isac.ErrorEmbeds.raiseException
import org.inquest.discord.optionAsOptions
import org.inquest.entities.isac.Channel
import org.inquest.entities.isac.ChannelSettings
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.appendBold
import org.inquest.utils.debugLog
import org.inquest.utils.mention
import org.inquest.utils.toMono
import reactor.core.publisher.Mono
import kotlin.jvm.optionals.getOrNull

/**
 * Command 'save-config' which lets users provide a default config when using [org.inquest.discord.commands.isac.IsacCommand] in a channel.
 *
 * Options:
 * - [org.inquest.discord.commands.CommonOptions]
 */
@ApplicationScoped
class ChannelSettingsCommand : CommandListener {
    companion object {
        private const val VIEW_CMD = "view"
        private const val UPDATE_CMD = "update"
    }

    override val name: String = "configuration"

    override fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest = ApplicationCommandRequest
        .builder()
        .name(name)
        .description("Saves a default configuration for further I.S.A.C. analyses done in this channel.")
        .addOption(
            ApplicationCommandOptionData.builder()
                .type(ApplicationCommandOption.Type.SUB_COMMAND.value)
                .description("Views I.S.A.C.s configuration in this channel.")
                .name(VIEW_CMD)
                .build(),
        ).addOption(
            ApplicationCommandOptionData.builder()
                .type(ApplicationCommandOption.Type.SUB_COMMAND.value)
                .description("Updates the default configuration I.S.A.C. uses for further analyses.")
                .name(UPDATE_CMD)
                .addAllOptions(CommonOptions.DEFAULT_OPTIONS)
                .build(),
        ).build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        val interactionId = interactionId()

        return event.deferReply().then(
            Channel.findOrPut(event.interaction.channelId.asString())
                .toMono()
                .debugLog("$interactionId: Received configuration event...")
                .flatMap {
                    if (event.getOption(VIEW_CMD).isPresent) {
                        it.writeSettings(event)
                    } else if (event.getOption(UPDATE_CMD).isPresent) {
                        val options = event.optionAsOptions(UPDATE_CMD)
                        it.updateSettings(interactionId, event, options)
                    } else {
                        event.editReply("Unknown Option")
                    }
                }.onErrorResume { event.raiseException(CHANNEL_CONFIG_EXC_MSG, it, true) }
                .then(),
        )
    }

    private fun Channel.writeSettings(event: ChatInputInteractionEvent) =
        event.editReply().withContentOrNull(StringBuilder().displayChannelSettings(this.channelSettings).toString())

    private fun Channel.updateSettings(
        interactionId: String,
        event: ChatInputInteractionEvent,
        options: List<ApplicationCommandInteractionOption>,
    ): Mono<*> {
        this.channelSettings = options.createSettings(this.channelSettings)

        return update<Channel>().toMono().flatMap {
            StringBuilder().createMessage(
                event.interaction.member.getOrNull(),
                it.channelSettings,
            ).let(event.editReply()::withContentOrNull)
        }.doOnSuccess {
            LOG.info("$interactionId: Successfully updated channel configuration.")
        }
    }

    private fun List<ApplicationCommandInteractionOption>.createSettings(base: ChannelSettings): ChannelSettings = base.copy(
        name = asString(NAME_OPTION),
        withHeal = asBoolean(HEAL_OPTION),
        compareWingman = asBoolean(WM_OPTION),
        analyzeBoons = asBoolean(BOONS_OPTION),
    )

    private fun List<ApplicationCommandInteractionOption>.asString(option: String) = firstOrNull {
        it.name == option
    }?.value?.map { it.asString() }?.getOrNull()

    private fun List<ApplicationCommandInteractionOption>.asBoolean(option: String) = firstOrNull {
        it.name == option
    }?.value?.map { it.asBoolean() }?.getOrNull()

    private fun StringBuilder.createMessage(member: Member?, settings: ChannelSettings): String {
        if (member == null) {
            append("I.S.A.C. settings updated to:")
        } else {
            mention(member)
            append(" updated I.S.A.C. settings to:")
        }

        appendLine()
        displayChannelSettings(settings)

        return toString()
    }

    private fun StringBuilder.displayChannelSettings(settings: ChannelSettings): StringBuilder {
        fun display(option: String, extractor: ChannelSettings.() -> Any) {
            appendBold(option)
            append(": ")
            append(settings.extractor())
            appendLine()
        }

        display(NAME_OPTION, ChannelSettings::name)
        display(HEAL_OPTION, ChannelSettings::withHeal)
        display(WM_OPTION, ChannelSettings::compareWingman)
        display(BOONS_OPTION, ChannelSettings::analyzeBoons)

        return this
    }
}
