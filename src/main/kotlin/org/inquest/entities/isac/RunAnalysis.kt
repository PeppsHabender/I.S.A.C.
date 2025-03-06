package org.inquest.entities.isac

import io.quarkus.mongodb.panache.common.MongoEntity
import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonIgnore
import org.bson.codecs.pojo.annotations.BsonProperty
import org.inquest.utils.IntExtensions.averageOrNull
import java.time.OffsetDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@MongoEntity
data class RunAnalysis @BsonCreator constructor(
    @BsonProperty("start") val start: OffsetDateTime,
    @BsonProperty("end") val end: OffsetDateTime,
    @BsonProperty("downtimeMillis") val downtimeMillis: Long,
    @BsonProperty("durationMillis") val durationMillis: Long,
    @BsonProperty("pulls") val pulls: List<Pull>,
    @BsonProperty("groupDps") val groupDps: Int,
    @BsonProperty("playerStats") val playerStats: List<PlayerAnalysis>,
) {
    @get:BsonIgnore
    val downtime: Duration
        get() = this.downtimeMillis.milliseconds

    @get:BsonIgnore
    val duration: Duration
        get() = this.durationMillis.milliseconds
}

@MongoEntity
data class Pull @BsonCreator constructor(
    @BsonProperty("eiEncounterId") val eiEncounterId: Long,
    @BsonProperty("triggerId") val triggerId: Long,
    @BsonProperty("boss") val boss: String,
    @BsonProperty("link") val link: String,
    @BsonProperty("success") val success: Boolean,
    @BsonProperty("cm") val cm: Boolean,
    @BsonProperty("embo") val embo: Boolean,
    @BsonProperty("durationMillis") val durationMillis: Long,
    @BsonProperty("remainingHpPercent") val remainingHpPercent: Double,
) {
    @BsonProperty("boonUptimes")
    var boonUptimes: Map<String, BoonUptimes> = emptyMap()

    @get:BsonIgnore
    val duration: Duration
        get() = this.durationMillis.milliseconds
}

@MongoEntity
data class BoonUptimes @BsonCreator constructor(@BsonProperty("uptimes") val uptimes: Map<String, Double>)

@MongoEntity
data class Profession @BsonCreator constructor(@BsonProperty("name") val name: String, @BsonProperty("condi") val condi: Boolean)

@MongoEntity
data class PlayerPull @BsonCreator constructor(
    @BsonProperty("profession") val profession: Profession,
    @BsonProperty("group") val group: Int,
    @BsonProperty("dps") val dps: Int,
    @BsonProperty("dpsPos") val dpsPos: Int,
    @BsonProperty("heal") val heal: Int,
    @BsonProperty("barrier") val barrier: Int,
    @BsonProperty("cc") val cc: Int,
    @BsonProperty("resTime") val resTime: Double,
    @BsonProperty("condiCleanse") val condiCleanse: Int,
    @BsonProperty("boonStrips") val boonStrips: Int,
    @BsonProperty("damageTaken") val damageTaken: Int,
    @BsonProperty("downstates") val downstates: Int,
    @BsonProperty("boonSupport") val boonSupport: BoonSupport?,
    @BsonProperty("maybeHealer") val maybeHealer: Boolean,
    @BsonProperty("boonUptimes") val boonUptimes: Map<String, Double>,
) {
    companion object {
        operator fun invoke(): PlayerPull = PlayerPull(
            Profession("*", false),
            -1,
            0,
            11,
            0,
            0,
            0,
            0.0,
            0,
            0,
            0,
            0,
            null,
            false,
            emptyMap(),
        )
    }

    @BsonIgnore
    val isSupport: Boolean = this.boonSupport != null
}

@MongoEntity
data class BoonSupport @BsonCreator constructor(@BsonProperty("boon") val boon: Long, @BsonProperty("generation") val generation: Double)

@MongoEntity
class PlayerAnalysis @BsonCreator constructor(
    @BsonProperty("name") val name: String,
    @BsonProperty("pulls") val pulls: MutableMap<String, PlayerPull>,
) {
    fun mostPlayed(support: Boolean, healer: Boolean = false): String = this.pulls.values.filter {
        support == it.isSupport && healer == it.maybeHealer
    }.map {
        it.profession
    }.groupingBy { it.name }.eachCount().maxByOrNull { it.value }?.key ?: "*"

    fun avgDps() = this.pulls.values.map { it.dps }.average()

    fun avgDpsExcluding(supports: Boolean = false, heals: Boolean = true) = this.pulls.values
        .filterNot { if (supports) it.isSupport else false }
        .filterNot { if (heals) it.maybeHealer else false }
        .map { it.dps }.averageOrNull()

    fun avgDpsPos() = this.pulls.values.map { it.dpsPos }.average()

    fun avgHeal() = this.pulls.values.map { it.heal }.average()

    fun avgBarrier() = this.pulls.values.map { it.barrier }.average()

    fun cc() = this.pulls.values.sumOf { it.cc }

    fun resTime() = this.pulls.values.sumOf { it.resTime }

    fun condiCleanse() = this.pulls.values.sumOf { it.condiCleanse }

    fun boonStrips() = this.pulls.values.sumOf { it.boonStrips }

    fun damageTaken() = this.pulls.values.sumOf { it.damageTaken }

    fun downstates() = this.pulls.values.sumOf { it.downstates }
}
