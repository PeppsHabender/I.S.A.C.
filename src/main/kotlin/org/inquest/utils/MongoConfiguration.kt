package org.inquest.utils

import com.mongodb.client.MongoClient
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.bson.BsonReader
import org.bson.BsonWriter
import org.bson.Document
import org.bson.codecs.Codec
import org.bson.codecs.DecoderContext
import org.bson.codecs.EncoderContext
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistry
import org.inquest.utils.LogExtension.LOG
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Encodes an [OffsetDateTime] into its epoch milli configuration, unfortunately we lose zone information in the process.
 */
private class DateTimeCodec : Codec<OffsetDateTime> {
    override fun encode(writer: BsonWriter, value: OffsetDateTime, encoderContext: EncoderContext) {
        writer.writeDateTime(value.toInstant().toEpochMilli())
    }

    override fun decode(reader: BsonReader, decoderContext: DecoderContext): OffsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), ZoneId.systemDefault())

    override fun getEncoderClass(): Class<OffsetDateTime> = OffsetDateTime::class.java
}

/**
 * Provides custom codecs to mongo.
 */
@ApplicationScoped
class CodecProvider : CodecProvider {
    override fun <T : Any?> get(clazz: Class<T>?, registry: CodecRegistry): Codec<T>? = when (clazz) {
        OffsetDateTime::class.java -> DateTimeCodec()
        else -> null
    } as Codec<T>?
}

/**
 * Configures the mongo db client on startup.
 */
@Startup
@ApplicationScoped
@IfBuildProfile("mongo")
class MongoConfig : WithLogger {
    @Inject
    private lateinit var mongoClient: MongoClient

    /**
     * Creates an index on analysis start times for easier access.
     */
    @PostConstruct
    fun configure() {
        LOG.debug("Setting up mongodb index...")
        this.mongoClient.getDatabase("isac").getCollection("ChannelAnalysis")
            .createIndex(Document("analysis.start", -1))
    }
}
