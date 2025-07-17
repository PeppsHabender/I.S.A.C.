package org.inquest.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.isac.IsacBoon
import org.inquest.entities.isac.IsacBoss
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Properties

/**
 * Contains isac specific information on bosses.
 */
@ApplicationScoped
class IsacDataService {
    /**
     * Used to read the static boss information
     */
    @Inject
    private lateinit var objectMapper: ObjectMapper

    private lateinit var bossData: Map<Long, IsacBoss>
    private lateinit var boonDataPriv: Map<Long, IsacBoon>
    val boonData: Map<Long, IsacBoon>
        get() = this.boonDataPriv
    val buildInfo: BuildInfo = this::class.java.classLoader.getResourceAsStream("buildInfo.properties").use {
        val props = Properties().apply { load(it) }
        val buildTime = Instant.ofEpochMilli(props["buildTime"].toString().toLong())

        BuildInfo(LocalDateTime.ofInstant(buildTime, ZoneId.of("Europe/Berlin")), props["version"].toString())
    }

    /**
     * Reads the static boss information.
     */
    @PostConstruct
    fun init() {
        this::class.java.classLoader.getResourceAsStream("boss_data.json")?.use { `is` ->
            this.bossData =
                this.objectMapper.readValue<List<IsacBoss>>(`is`).associateBy { it.eliteInsightsId }
        }

        this::class.java.classLoader.getResourceAsStream("boon_data.json")?.use {
            this.boonDataPriv = this.objectMapper.readValue(it)
        }
    }

    /**
     * @return true when the boss associated with [id] should be ignored for analysis
     */
    fun ignore(id: Long?) = !(this.bossData[id]?.validForTopStat ?: true)

    /**
     * @return true when the boss associated with [id] should be ignored for boon analysis
     */
    fun ignoreForBoons(id: Long?) = !(this.bossData[id]?.validForBoons ?: true)

    /**
     * @return An emote for the boss with [id] or null if none was found
     */
    fun emoteFor(id: Long?, cm: Boolean) = this.bossData[id]?.emote?.let { if (cm) it.challenge else it.normal }

    /**
     * @return Targets that are relevant for the analysis of the boss with [id]
     */
    fun targets(id: Long?) = this.bossData[id]?.targets.ifNullOrEmpty { emptyList() }

    /**
     * @return Phases that are relevant for the analysis of the boss with [id]
     */
    fun phases(id: Long?) = this.bossData[id]?.phases.ifNullOrEmpty { emptyList() }

    /**
     * @return Short name for the boss associated with [id]
     */
    fun shortName(id: Long?) = this.bossData[id]?.shortname

    /**
     * @return Wingman fallback id for the given boss
     */
    fun wingmanId(id: Long?, cm: Boolean) = this.bossData[id]?.wingmanId?.let { if (cm) -it else it }

    private fun <T> List<T>?.ifNullOrEmpty(ls: () -> List<T>): List<T> = if (isNullOrEmpty()) ls() else this
}

data class BuildInfo(val buildTime: LocalDateTime, val version: String)
