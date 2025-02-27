package org.inquest.entities

import java.time.OffsetDateTime
import kotlin.time.Duration

data class RunAnalysis(
    val start: OffsetDateTime,
    val end: OffsetDateTime,
    val downtime: Duration,
    val duration: Duration,
    val pulls: List<Pull>,
    val groupDps: Int,
    val playerStats: List<PlayerAnalysis>,
)

data class BoonUptime(val highest: Double, val average: Double, val lowest: Double)

data class Pull(
    val eiEncounterId: Long,
    val triggerId: Long,
    val boss: String,
    val link: String,
    val success: Boolean,
    val cm: Boolean,
    val embo: Boolean,
    val duration: Duration,
    val remainingHp: Double,
) {
    lateinit var boonUptimes: Map<Int, Map<IsacBoon, Double>>
}

data class Profession(val name: String, val isCondi: Boolean)

data class PlayerPull(
    val profession: Profession = Profession("*", false),
    val group: Int = -1,
    val dps: Int = 0,
    val dpsPos: Int = 11,
    val heal: Int = 0,
    val barrier: Int = 0,
    val cc: Int = 0,
    val resTime: Double = 0.0,
    val condiCleanse: Int = 0,
    val boonStrips: Int = 0,
    val damageTaken: Int = 0,
    val downstates: Int = 0,
    val boonSupport: BoonSupport? = null,
    val maybeHealer: Boolean = false,
    val boonUptimes: Map<IsacBoon, Double> = emptyMap(),
)

data class BoonSupport(val boon: Long, val generation: Double)

class PlayerAnalysis(val name: String, val pulls: MutableMap<Pull, PlayerPull> = mutableMapOf()) {
    fun mostPlayed(): String = this.pulls.values.map {
        it.profession
    }.filterNot { it.name == "*" }.groupingBy { it.name }.eachCount().maxBy { it.value }.key

    fun avgDps() = this.pulls.values.map { it.dps }.average()

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
