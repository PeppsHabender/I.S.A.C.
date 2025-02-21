package org.inquest.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.inquest.utils.uppercased
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class BossBench(
    val bossID: String? = null,
    @JsonProperty("professions_top")
    val professionsTop: Map<String, Int>? = emptyMap(),
    val conditionDPS: DpsBenches?,
    val powerDPS: DpsBenches?,
    @JsonProperty("duration_top")
    val durationTop: Int? = null,
) {
    @JsonIgnore
    val bossId: Long? = this.bossID?.toLong()

    @JsonIgnore
    val topDuration: Duration =
        this.durationTop?.toDuration(DurationUnit.SECONDS) ?: Duration.INFINITE

    fun getBench(profession: String): Int? = this.professionsTop?.get(profession.uppercased(0))

    fun getCondiBench(profession: String) = this.conditionDPS?.professionsTop?.get(profession.uppercased(0))

    fun getPowerBench(profession: String) = this.powerDPS?.professionsTop?.get(profession.uppercased(0))
}

data class DpsBenches(
    @JsonProperty("professions_top") val professionsTop: Map<String, Int>? = emptyMap(),
)
