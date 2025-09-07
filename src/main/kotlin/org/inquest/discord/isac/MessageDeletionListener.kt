package org.inquest.discord.isac

import discord4j.core.event.domain.message.MessageDeleteEvent
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.inquest.discord.EventListener
import org.inquest.entities.isac.ChannelAnalysis
import org.inquest.utils.LogExtension.LOG
import org.inquest.utils.WithLogger
import org.inquest.utils.mapNotNull
import org.inquest.utils.toMono
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
