package org.inquest.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.IsacBoon
import org.inquest.entities.IsacBoss

/**
 * Contains isac specific information on bosses.
 */
@ApplicationScoped
class IsacData {
    /**
     * Used to read the static boss information
     */
    @Inject
    private lateinit var objectMapper: ObjectMapper

    private lateinit var bossData: Map<Long, IsacBoss>
    private lateinit var boonDataPriv: Map<Long, IsacBoon>
    val boonData: Map<Long, IsacBoon>
        get() = this.boonDataPriv

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
     * @return An emote for the boss with [id] or null if none was found
     */
    fun emoteFor(id: Long?, cm: Boolean) = this.bossData[id]?.emote?.let { if (cm) it.challenge else it.normal }

    /**
     * @return Targets that are relevant for the analysis of the boss with [id]
     */
    fun targets(id: Long?) = this.bossData[id]?.targets ?: listOf(0)

    /**
     * @return Short name for the boss associated with [id]
     */
    fun shortName(id: Long?) = this.bossData[id]?.shortname
}
