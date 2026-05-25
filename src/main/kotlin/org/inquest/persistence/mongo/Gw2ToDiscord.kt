package org.inquest.persistence.mongo

import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase
import io.smallrye.mutiny.Uni
import org.bson.codecs.pojo.annotations.BsonId
import org.inquest.shared.reactor.mapNotNull

@MongoEntity
class Gw2ToDiscord : ReactivePanacheMongoEntityBase() {
    companion object : ReactivePanacheMongoCompanionBase<Gw2ToDiscord, String> {
        fun findOrPut(discordId: String): Uni<Gw2ToDiscord> = findById(discordId).onItem().ifNull().switchTo {
            Uni.createFrom().item(Gw2ToDiscord().apply { this.discordId = discordId }).call(::persist)
        }.mapNotNull()

        fun findByGw2Account(accountName: String): Uni<Gw2ToDiscord?> = find("gw2Accounts", accountName).firstResult()
    }

    @BsonId
    lateinit var discordId: String

    var gw2Accounts: Set<String> = emptySet()
}
