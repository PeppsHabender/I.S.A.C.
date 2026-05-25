package org.inquest.analysis

import jakarta.enterprise.context.ApplicationScoped
import org.inquest.integration.dpsreport.dto.JsonActorParent
import org.inquest.integration.dpsreport.dto.JsonLog

@ApplicationScoped
class EligiblePlayerSelector {
    fun eligiblePlayers(log: JsonLog): List<JsonActorParent.JsonPlayer> = log.players.filter {
        it.account != null && it.friendlyNPC == false && it.isFake == false && !it.isProbHk(log)
    }

    private fun JsonActorParent.JsonPlayer.isProbHk(log: JsonLog) = if (log.eiEncounterID != 132100L) {
        false
    } else {
        (log.players.groupBy { it.group }[this.group]?.size ?: 5) < 2
    }
}
