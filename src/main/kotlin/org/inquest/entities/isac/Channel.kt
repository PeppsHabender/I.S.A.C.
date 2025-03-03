package org.inquest.entities.isac

import com.mongodb.client.model.Filters
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase
import io.smallrye.mutiny.Uni
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.codecs.pojo.annotations.BsonProperty
import org.inquest.utils.mapNotNull

@MongoEntity
class Channel : ReactivePanacheMongoEntityBase() {
    companion object : ReactivePanacheMongoCompanionBase<Channel, String> {
        fun findOrPut(id: String): Uni<Channel> = findById(id).invoke { x ->
            println()
        }.onItem().ifNull().switchTo {
            Uni.createFrom().item(Channel().apply { this.channelId = id }).call(::persist)
        }.mapNotNull()

        override fun findById(id: String): Uni<Channel?> = find(Filters.eq("_id", id)).firstResult()
    }

    @BsonId
    lateinit var channelId: String

    @BsonProperty("channelSettings")
    var channelSettings: ChannelSettings = ChannelSettings()
}

@MongoEntity
data class ChannelSettings @BsonCreator constructor(
    @BsonProperty("name") val name: String,
    @BsonProperty("withHeal") val withHeal: Boolean,
    @BsonProperty("compareWingman") val compareWingman: Boolean,
    @BsonProperty("analyzeBoons") val analyzeBoons: Boolean,
) {
    companion object {
        operator fun invoke(): ChannelSettings = ChannelSettings(
            "Run Analysis",
            withHeal = false,
            compareWingman = true,
            analyzeBoons = false,
        )
    }

    fun copy(name: String? = null, withHeal: Boolean? = null, compareWingman: Boolean? = null, analyzeBoons: Boolean? = null) =
        ChannelSettings(
            name = name ?: this.name,
            withHeal = withHeal ?: this.withHeal,
            compareWingman = compareWingman ?: this.compareWingman,
            analyzeBoons = analyzeBoons ?: this.analyzeBoons,
        )
}
