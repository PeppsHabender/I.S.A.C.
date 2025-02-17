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
    val playerStats: List<PlayerAnalysis>
)

data class Pull(
    val bossId: Long,
    val boss: String,
    val link: String,
    val success: Boolean,
    val cm: Boolean,
    val duration: Duration,
    val remainingHp: Double,
)

class PlayerAnalysis(val name: String) {
    private val profCount: MutableMap<String, Int> = mutableMapOf()
    private val dps: MutableList<Int> = mutableListOf()
    private val dpsPos: MutableList<Int> = mutableListOf()
    private val heal: MutableList<Int> = mutableListOf()
    private val barrier: MutableList<Int> = mutableListOf()
    var cc: Int = 0
    var resTime: Double = 0.0
    var condiCleanse: Int = 0
    var boonStrips: Int = 0
    var damageTaken: Int = 0
    var downstates: Int = 0

    fun withDps(dps: Int) {
        this.dps += dps
    }

    fun avgDps() = this.dps.average()

    fun withDpsPos(dpsPos: Int) {
        this.dpsPos += dpsPos
    }

    fun avgDpsPos() = this.dpsPos.average()

    fun withHeal(heal: Int, barrier: Int) {
        this.heal += heal
        this.barrier += barrier
    }

    fun avgHeal() = this.heal.average()

    fun avgBarrier() = this.barrier.average()

    fun playedProfession(profession: String) {
        this.profCount.getOrPut(profession) { 1 }
    }

    fun mostPlayed(): String = this.profCount.maxBy { it.value }.key
}