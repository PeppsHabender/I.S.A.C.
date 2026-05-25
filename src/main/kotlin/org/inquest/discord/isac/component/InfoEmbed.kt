package org.inquest.discord.isac.component
import discord4j.core.event.domain.interaction.ButtonInteractionEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.catalog.IsacDataService
import org.inquest.discord.core.InteractionEventListener
import org.inquest.discord.isac.shared.CommonIds
import org.inquest.discord.support.CustomColors
import org.inquest.discord.support.CustomEmojis
import org.inquest.discord.support.createEmbed
import org.inquest.shared.text.appendBold
import org.inquest.shared.text.appendTimestamp
import org.inquest.shared.text.hyperlink
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
        hyperlink("GitHub", "https://github.com/PeppsHabender/I.S.A.C./")
        append(" repository")
    }
}
