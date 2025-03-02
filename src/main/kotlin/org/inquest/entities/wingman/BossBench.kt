package org.inquest.entities.wingman

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
    @JsonProperty("professions_top_Links")
    val professionsTopLinks: Map<String, String>? = emptyMap(),
    @JsonProperty("professions_topSupport")
    val professionsTopSupp: Map<String, Int>? = emptyMap(),
    @JsonProperty("professions_topSupport_Links")
    val professionsTopSuppLinks: Map<String, String>? = emptyMap(),
    val conditionDPS: DpsBenches?,
    val powerDPS: DpsBenches?,
    @JsonProperty("duration_top")
    val durationTop: Int? = null,
) {
    companion object {
        private const val WINGMAN_LOG_PREFIX = "https://gw2wingman.nevermindcreations.de/log/"
    }

    @JsonIgnore
    val bossId: Long? = this.bossID?.toLong()

    @JsonIgnore
    val topDuration: Duration =
        this.durationTop?.toDuration(DurationUnit.SECONDS) ?: Duration.INFINITE

    fun getSupportBench(profession: String): Pair<Int, String>? =
        this.professionsTopSupp?.get(profession.uppercased(0)).toLink(profession, professionsTopSuppLinks)

    fun getCondiBench(profession: String) =
        this.conditionDPS?.professionsTop?.get(profession.uppercased(0)).toLink(profession, professionsTopLinks)

    fun getPowerBench(profession: String) =
        this.powerDPS?.professionsTop?.get(profession.uppercased(0)).toLink(profession, professionsTopLinks)

    private fun Int?.toLink(profession: String, linkMap: Map<String, String>?) = this?.let { bench ->
        linkMap?.get(profession.uppercased(0))?.let {
            bench to (WINGMAN_LOG_PREFIX + it)
        }
    }
}

data class DpsBenches(@JsonProperty("professions_top") val professionsTop: Map<String, Int>? = emptyMap())
