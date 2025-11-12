package org.inquest.discord.isac

import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.discordjson.json.ApplicationCommandRequest
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.clients.Gw2ApiClient
import org.inquest.discord.CommandListener
import org.inquest.discord.isac.embeds.ErrorEmbeds
import org.inquest.discord.isac.embeds.ErrorEmbeds.raiseException
import org.inquest.discord.optionAsString
import org.inquest.discord.stringOption
import org.inquest.entities.isac.Gw2ToDiscord
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import org.inquest.utils.toMono
import reactor.core.publisher.Mono

@ApplicationScoped
class MemorizeCommand :
    CommandListener,
    WithLogger {
    companion object {
        const val API_KEY_OPTION = "api_key"
    }

    @RestClient
    lateinit var gw2ApiClient: Gw2ApiClient

    override val name: String = "stalk_me"

    override fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest = ApplicationCommandRequest.builder()
        .name(name)
        .description("Links your account name to your discord user.")
        .addOption(stringOption(API_KEY_OPTION, "Your api key, which will be discarded after the request."))
        .build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> {
        return event.deferReply().withEphemeral(true).then(
            Mono.defer {
                val apiKey = event.optionAsString(API_KEY_OPTION)
                    ?: return@defer Mono.error(IllegalArgumentException("Missing API key"))

                val userId = event.interaction.user.id.asString()

                val accountMono = Gw2ToDiscord.findOrPut(userId).toMono()
                val accNameMono = gw2ApiClient.fetchAccount("Bearer $apiKey").toMono()

                Mono.zip(accountMono, accNameMono)
                    .flatMap { tuple ->
                        val account = tuple.t1
                        val accName = tuple.t2

                        account.gw2Accounts += accName.name

                        Gw2ToDiscord.update(account).toMono().thenReturn(accName.name)
                    }
                    .flatMap { name -> event.editReply("I.S.A.C. successfully linked **$name** to your discord id!") }
                    .onErrorResume { event.raiseException(LOG, ErrorEmbeds.MEMORIZE_EXC_MSG, it) }
                    .then()
            },
        )
    }
}

@ApplicationScoped
class ForgetCommand : CommandListener {
    override val name: String = "unstalk_me"

    override fun build(gatewayClient: GatewayDiscordClient): ApplicationCommandRequest = ApplicationCommandRequest.builder()
        .name(name)
        .description("Forgets you ever existed.")
        .build()

    override fun handle(event: ChatInputInteractionEvent): Mono<Void> =
        Gw2ToDiscord.findById(event.interaction.member.get().id.asString()).flatMap {
            it?.delete() ?: Uni.createFrom().voidItem()
        }.toMono().then(event.reply("I.S.A.C. successfully unlinked your discord id!").withEphemeral(true))
}
