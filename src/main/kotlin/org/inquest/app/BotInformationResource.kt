package org.inquest.app

import io.smallrye.mutiny.Uni
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import org.inquest.catalog.BuildInfo
import org.inquest.catalog.IsacDataService
import org.inquest.discord.core.DiscordService
import org.inquest.persistence.mongo.ChannelAnalysis
import org.inquest.shared.reactor.toUni
import reactor.core.publisher.Mono

@Path("/info")
class BotInformationResource {
    @Inject
    private lateinit var discordService: DiscordService

    @Inject
    private lateinit var isacDataService: IsacDataService

    @GET
    fun isacInfo(): Uni<IsacInformation> = Mono.just(IsacInformation(this.isacDataService.buildInfo))
        .flatMap { info -> this.discordService.gatewayDiscordClient.guilds.count().map { info.copy(activeGuilds = it) } }
        .toUni()
        .flatMap { info -> ChannelAnalysis.count().map { info.copy(numAnalyses = it) } }
}

data class IsacInformation(val buildInfo: BuildInfo, val activeGuilds: Long = 0, val numAnalyses: Long = 0)
