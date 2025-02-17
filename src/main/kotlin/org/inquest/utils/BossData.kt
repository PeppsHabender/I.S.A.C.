package org.inquest.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.inquest.entities.IsacBoss

@ApplicationScoped
class BossData {
    @Inject private lateinit var objectMapper: ObjectMapper

    private lateinit var bossData: Map<Long, IsacBoss>

    @PostConstruct
    fun init() {
        this::class.java.classLoader.getResourceAsStream("boss_data.json")?.use { `is` ->
            this.bossData =
                this.objectMapper.readValue<List<IsacBoss>>(`is`).associateBy { it.eliteInsightsId }
        }
    }

    fun ignore(id: Long?) = !(this.bossData[id]?.validForTopStat ?: true)

    fun emoteFor(id: Long?, cm: Boolean) =
        this.bossData[id]?.emote?.let { if (cm) it.challenge else it.normal }

    fun targets(id: Long?) = this.bossData[id]?.targets ?: listOf(0)

    fun shortName(id: Long?) = this.bossData[id]?.shortname
}
