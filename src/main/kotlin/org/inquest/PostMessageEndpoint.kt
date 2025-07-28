package org.inquest

import discord4j.common.util.Snowflake
import discord4j.discordjson.json.MessageData
import io.smallrye.faulttolerance.api.RateLimit
import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.ServerErrorException
import org.inquest.discord.DiscordService
import org.inquest.discord.DiscordSettings
import org.inquest.discord.toDiscordTimestamp
import org.inquest.utils.appendBold
import org.inquest.utils.toUni
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Path("/post")
class PostMessageEndpoint {
    @Inject
    private lateinit var discordService: DiscordService

    @Inject
    private lateinit var discordSettings: DiscordSettings

    @POST
    @RateLimit(value = 5, window = 20, windowUnit = ChronoUnit.SECONDS)
    fun postMessage(message: Message): Uni<MessageData> {
        if (this.discordSettings.channelId() == -1L) throw ServerErrorException("No channel id set!", 500)
        if (message.sender.length + message.message.length > 3900) throw BadRequestException("Message too long!")

        return this.discordService.gatewayDiscordClient.getChannelById(Snowflake.of(this.discordSettings.channelId()))
            .flatMap {
                StringBuilder().apply {
                    append("New message from ")
                    appendBold(message.sender)
                    append(" on")
                    append(OffsetDateTime.now().toDiscordTimestamp())
                    append(":\n")
                    append(message.message)
                }.toString().let(it.restChannel::createMessage)
            }.toUni()
    }
}

data class Message(val sender: String, val message: String)
