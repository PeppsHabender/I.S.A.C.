package org.inquest.discord.isac.component
import discord4j.core.event.domain.message.MessageDeleteEvent
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.core.EventListener
import org.inquest.discord.isac.shared.interactionId
import org.inquest.persistence.mongo.ChannelAnalysis
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import org.inquest.shared.reactor.mapNotNull
import org.inquest.shared.reactor.toMono
import reactor.core.publisher.Mono

/**
 * Listens for the message deletion event in order to dynamically remove analyses from the database.
 */
@ApplicationScoped
class MessageDeletionListener :
    EventListener<MessageDeleteEvent>,
    WithLogger {
    override val eventType: Class<MessageDeleteEvent> = MessageDeleteEvent::class.java

    override fun wantsToHandle(event: MessageDeleteEvent) = true

    override fun execute(event: MessageDeleteEvent): Mono<Void> {
        val interactionId = interactionId()

        return Uni.createFrom().item(event.messageId.asString())
            .flatMap { ChannelAnalysis.findById(it) }
            .mapNotNull()
            .invoke { it -> LOG.info("$interactionId: Deleting previous analysis ${it.id}...") }
            .flatMap { ChannelAnalysis.deleteById(it.id) }
            .invoke { it ->
                if (it) {
                    LOG.info("$interactionId: Successfully deleted analysis.")
                } else {
                    LOG.info("$interactionId: Failed to delete analysis!")
                }
            }.toMono()
            .then()
    }
}
