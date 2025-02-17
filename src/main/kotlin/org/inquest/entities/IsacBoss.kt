package org.inquest.entities

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
