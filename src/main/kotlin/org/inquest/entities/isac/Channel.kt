package org.inquest.entities.isac

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import io.quarkus.mongodb.panache.common.MongoEntity
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoCompanionBase
import io.quarkus.mongodb.panache.kotlin.reactive.ReactivePanacheMongoEntityBase
import io.smallrye.mutiny.Uni
import org.bson.Document
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

        override fun findById(id: String): Uni<Channel?> = find(eq("_id", id)).firstResult()
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

@MongoEntity
class ChannelAnalysis @BsonCreator constructor() : ReactivePanacheMongoEntityBase() {
    companion object : ReactivePanacheMongoCompanionBase<ChannelAnalysis, String> {
        override fun findById(id: String): Uni<ChannelAnalysis?> = find(eq("_id", id)).firstResult()

        fun findLast(channelId: String, name: String, num: Int = 10) = find(
            and(eq("channelId", channelId), eq("name", name)),
            Document("analysis.start", -1),
        ).page(0, num).list()
    }

    @BsonId
    lateinit var id: String
    lateinit var channelId: String
    lateinit var name: String

    @BsonProperty("analysis")
    lateinit var analysis: RunAnalysis
}
