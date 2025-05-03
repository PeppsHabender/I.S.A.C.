package org.inquest.entities.isac

import com.fasterxml.jackson.annotation.JsonCreator

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
    val targetsExclude: List<Int>?,
    val phases: List<Int>,
)

data class IsacEmote(val normal: String?, val challenge: String?)
