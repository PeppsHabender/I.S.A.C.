package org.inquest.discord

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName
import org.eclipse.microprofile.config.inject.ConfigProperty

@ConfigMapping(prefix = "discord")
interface DiscordSettings {
    @WithName("token")
    fun token(): String

    @WithName("application-id")
    fun applicationId(): Long

    @ConfigProperty(name = "guild-id", defaultValue = "-1")
    fun guildId(): Long

    @ConfigProperty(name = "channel-id", defaultValue = "-1")
    fun channelId(): Long
}
