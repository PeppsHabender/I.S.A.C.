package org.inquest.discord.isac.embeds

import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.discord.CustomColors
import org.inquest.discord.CustomEmojis
import org.inquest.discord.InteractionEventListener
import org.inquest.discord.createEmbed
import org.inquest.discord.isac.CommonIds
import org.inquest.services.IsacDataService
import org.inquest.utils.appendBold
import org.inquest.utils.appendTimestamp
import org.inquest.utils.hyperlink
import reactor.core.publisher.Mono

@ApplicationScoped
class InfoEmbed : InteractionEventListener<ButtonInteractionEvent>() {
    @Inject
    private lateinit var dataService: IsacDataService

    override val handlesId: String = CommonIds.INFO_EMBED
    override val eventType: Class<ButtonInteractionEvent> = ButtonInteractionEvent::class.java

    override fun execute(event: ButtonInteractionEvent): Mono<Void> = event.reply().withEmbeds(createInfoEmbed()).withEphemeral(true)

    fun createInfoEmbed() = createEmbed(
        buildInfo().toString(),
        title = "${CustomEmojis.INFO}I.S.A.C. v${dataService.buildInfo.version}",
        color = CustomColors.TRANSPARENT_COLOR,
    )

    fun buildInfo() = StringBuilder().apply {
        appendBold("Latest Build ")
        appendTimestamp(dataService.buildInfo.buildTime)

        appendLine()
        appendBold("Latest Deployment ")
        appendTimestamp(dataService.buildInfo.deploymentTime)

        appendLine()
        appendLine()
        append("Bug Reports? Feature Requests? Suggestions? Check out the ")
        hyperlink("GitHub", "https://github.com/PeppsHabender/I.S.A.C")
        append(" repository")
    }
}
