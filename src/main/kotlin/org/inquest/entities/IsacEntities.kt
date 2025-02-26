package org.inquest.entities

import com.fasterxml.jackson.annotation.JsonCreator

data class IsacBoon @JsonCreator constructor(val id: Long, val name: String, val emote: String?) : Comparable<IsacBoon> {
    override fun compareTo(other: IsacBoon): Int = if (this.id in PRIM_BOONS) {
        if (other.id in PRIM_BOONS) this.name.compareTo(other.name) else -1
    } else if (other.id in PRIM_BOONS) {
        1
    } else {
        this.name.compareTo(other.name)
    }

    companion object {
        val PRIM_BOONS = setOf(30328L, 1187L)
    }
}

data class IsacBoss(
    val boss: String,
    val eliteInsightsId: Long,
    val validForTopStat: Boolean,
    val emote: IsacEmote?,
    val shortname: String?,
    val targets: List<Int>?,
    val phases: List<Int>,
)

data class IsacEmote(val normal: String?, val challenge: String?)
