package org.inquest.entities.isac

import com.fasterxml.jackson.annotation.JsonCreator
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase
import io.smallrye.mutiny.Uni
import org.bson.codecs.pojo.annotations.BsonId
import org.inquest.utils.mapNotNull

data class IsacBoon @JsonCreator constructor(
    val id: Long,
    val name: String,
    val emote: String?,
    val isStacks: Boolean = false,
    val isPrimary: Boolean = false,
) : Comparable<IsacBoon> {
    override fun compareTo(other: IsacBoon): Int = if (this.isPrimary) {
        if (other.isPrimary) this.name.compareTo(other.name) else -1
    } else if (other.isPrimary) {
        1
    } else {
        this.name.compareTo(other.name)
    }
}

data class IsacBoss(
    val boss: String,
    val eliteInsightsId: Long,
    val wingmanId: Long,
    val validForTopStat: Boolean,
    val validForBoons: Boolean? = true,
    val emote: IsacEmote?,
    val shortname: String?,
    val targets: List<Int>?,
    val phases: List<Int>,
)

data class IsacEmote(val normal: String?, val challenge: String?)

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
