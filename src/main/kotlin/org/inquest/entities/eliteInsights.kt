package org.inquest.entities

data class JsonDamageDist(
    val totalDamage: Int? = null,
    val totalBreakbarDamage: Double? = null,
    val min: Int? = null,
    val max: Int? = null,
    val hits: Int? = null,
    val connectedHits: Int? = null,
    val crit: Int? = null,
    val glance: Int? = null,
    val flank: Int? = null,
    val againstMoving: Int? = null,
    val missed: Int? = null,
    val invulned: Int? = null,
    val interrupted: Int? = null,
    val evaded: Int? = null,
    val blocked: Int? = null,
    val shieldDamage: Int? = null,
    val critDamage: Int? = null,
    val id: Long? = null,
    val indirectDamage: Boolean? = null,
)

data class JsonSkill(
    val castTime: Int? = null,
    val duration: Int? = null,
    val timeGained: Int? = null,
    val quickness: Double? = null,
)

data class JsonRotation(val id: Long? = null, val skills: List<JsonSkill> = listOf())

data class JsonActorCombatReplayData(
    val start: Long? = null,
    val end: Long? = null,
    val iconURL: String? = null,
    val positions: List<List<Float>> = listOf(),
    val orientations: List<Float> = listOf(),
    val dead: List<List<Long>> = listOf(),
    val down: List<List<Long>> = listOf(),
    val dc: List<List<Long>> = listOf(),
)

data class JsonMinions(
    val name: String? = null,
    val id: Int? = null,
    val totalDamage: IntList = listOf(),
    val totalTargetDamage: IntListList = listOf(),
    val totalBreakbarDamage: DoubleList = listOf(),
    val totalTargetBreakbarDamage: DoubleListList = listOf(),
    val totalShieldDamage: IntList = listOf(),
    val totalTargetShieldDamage: IntListList = listOf(),
    val totalDamageDist: ListList<JsonDamageDist> = listOf(),
    val targetDamageDist: ListListList<JsonDamageDist> = listOf(),
    val rotation: List<JsonRotation> = listOf(),
    val extHealingStats: EXTJsonMinionsHealingStats? = null,
    val extBarrierStats: EXTJsonMinionsBarrierStats? = null,
    val combatReplayData: List<JsonActorCombatReplayData> = listOf(),
)

data class JsonDPS(
    val dps: Int? = null,
    val damage: Int? = null,
    val condiDps: Int? = null,
    val condiDamage: Int? = null,
    val powerDps: Int? = null,
    val powerDamage: Int? = null,
    val breakbarDamage: Double? = null,
    val actorDps: Int? = null,
    val actorDamage: Int? = null,
    val actorCondiDps: Int? = null,
    val actorCondiDamage: Int? = null,
    val actorPowerDps: Int? = null,
    val actorPowerDamage: Int? = null,
    val actorBreakbarDamage: Double? = null,
)

data class JsonDefensesAll(
    val damageTaken: Long? = null,
    val breakbarDamageTaken: Double? = null,
    val blockedCount: Int? = null,
    val evadedCount: Int? = null,
    val missedCount: Int? = null,
    val dodgeCount: Int? = null,
    val invulnedCount: Int? = null,
    val damageBarrier: Int? = null,
    val interruptedCount: Int? = null,
    val downCount: Int? = null,
    val downDuration: Long? = null,
    val deadCount: Int? = null,
    val deadDuration: Long? = null,
    val dcCount: Int? = null,
    val dcDuration: Double? = null,
    val boonStrips: Int? = null,
    val boonStripsTime: Double? = null,
    val conditionCleanses: Int? = null,
    val conditionCleansesTime: Double? = null,
)

data class JsonBuffsUptimeData(
    val uptime: Double? = null,
    val presence: Double? = null,
    val generated: StringDoubleMap = mapOf(),
    val overstacked: StringDoubleMap = mapOf(),
    val wasted: StringDoubleMap = mapOf(),
    val unknownExtended: StringDoubleMap = mapOf(),
    val byExtension: StringDoubleMap = mapOf(),
    val extended: StringDoubleMap = mapOf(),
)

data class JsonBuffsUptime(
    val id: Long? = null,
    val buffData: List<JsonBuffsUptimeData> = listOf(),
    val states: IntListList = listOf(),
    val statesPerSource: Map<String, IntListList> = mapOf(),
)

sealed interface JsonGameplayStatsParent {
    data class JsonGameplayStats(
        val totalDamageCount: Int? = null,
        val totalDmg: Int? = null,
        val directDamageCount: Int? = null,
        val directDmg: Int? = null,
        val connectedDirectDamageCount: Int? = null,
        val connectedDirectDmg: Int? = null,
        val connectedDamageCount: Int? = null,
        val connectedDmg: Int? = null,
        val critableDirectDamageCount: Int? = null,
        val criticalRate: Int? = null,
        val criticalDmg: Int? = null,
        val flankingRate: Int? = null,
        val againstMovingRate: Int? = null,
        val glanceRate: Int? = null,
        val missed: Int? = null,
        val evaded: Int? = null,
        val blocked: Int? = null,
        val interrupts: Int? = null,
        val invulned: Int? = null,
        val killed: Int? = null,
        val downed: Int? = null,
        val downContribution: Int? = null,
    ) : JsonGameplayStatsParent

    data class JsonGameplayStatsAll(
        val totalDamageCount: Int? = null,
        val totalDmg: Int? = null,
        val directDamageCount: Int? = null,
        val directDmg: Int? = null,
        val connectedDirectDamageCount: Int? = null,
        val connectedDirectDmg: Int? = null,
        val connectedDamageCount: Int? = null,
        val connectedDmg: Int? = null,
        val critableDirectDamageCount: Int? = null,
        val criticalRate: Int? = null,
        val criticalDmg: Int? = null,
        val flankingRate: Int? = null,
        val againstMovingRate: Int? = null,
        val glanceRate: Int? = null,
        val missed: Int? = null,
        val evaded: Int? = null,
        val blocked: Int? = null,
        val interrupts: Int? = null,
        val invulned: Int? = null,
        val killed: Int? = null,
        val downed: Int? = null,
        val downContribution: Int? = null,
        val wasted: Int? = null,
        val timeWasted: Double? = null,
        val saved: Int? = null,
        val timeSaved: Double? = null,
        val stackDist: Double? = null,
        val distToCom: Double? = null,
        val avgBoons: Double? = null,
        val avgActiveBoons: Double? = null,
        val avgConditions: Double? = null,
        val avgActiveConditions: Double? = null,
        val swapCount: Int? = null,
        val skillCastUptime: Double? = null,
        val skillCastUptimeNoAA: Double? = null,
    ) : JsonGameplayStatsParent
}

data class JsonPlayerSupport(
    val resurrects: Long? = null,
    val resurrectTime: Double? = null,
    val condiCleanse: Long? = null,
    val condiCleanseTime: Double? = null,
    val condiCleanseSelf: Long? = null,
    val condiCleanseTimeSelf: Double? = null,
    val boonStrips: Long? = null,
    val boonStripsTime: Double? = null,
)

data class JsonDamageModifierItem(
    val hitCount: Int? = null,
    val totalHitCount: Int? = null,
    val damageGain: Double? = null,
    val totalDamage: Int? = null,
)

data class JsonDamageModifierData(
    val id: Int? = null,
    val damageModifiers: List<JsonDamageModifierItem> = listOf(),
)

data class JsonBuffsGenerationData(
    val generation: Double? = null,
    val overstack: Double? = null,
    val wasted: Double? = null,
    val unknownExtended: Double? = null,
    val byExtension: Double? = null,
    val extended: Double? = null,
)

data class JsonPlayerBuffsGeneration(
    val id: Long? = null,
    val buffData: List<JsonBuffsGenerationData> = listOf(),
)

data class JsonDeathRecapDamageItem(
    val id: Long? = null,
    val indirectDamage: Boolean? = null,
    val src: String? = null,
    val damage: Int? = null,
    val time: Int? = null,
)

data class JsonDeathRecap(
    val deathTime: Long? = null,
    val toDown: List<JsonDeathRecapDamageItem> = listOf(),
    val toKill: List<JsonDeathRecapDamageItem> = listOf(),
)

data class JsonConsumable(
    val stack: Int? = null,
    val duration: Int? = null,
    val time: Long? = null,
    val id: Long? = null,
)

sealed interface JsonActorParent {
    data class JsonActor(
        val name: String? = null,
        val totalHealth: Int? = null,
        val condition: Int? = null,
        val concentration: Int? = null,
        val healing: Int? = null,
        val toughness: Int? = null,
        val hitboxHeight: Int? = null,
        val hitboxWidth: Int? = null,
        val instanceID: Int? = null,
        val teamID: Long? = null,
        val minions: List<JsonMinions> = listOf(),
        val isFake: Boolean? = null,
        val dpsAll: List<JsonDPS> = listOf(),
        val statsAll: List<JsonGameplayStatsParent.JsonGameplayStatsAll> = listOf(),
        val defenses: List<JsonDefensesAll> = listOf(),
        val totalDamageDist: ListList<JsonDamageDist> = listOf(),
        val totalDamageTaken: ListList<JsonDamageDist> = listOf(),
        val rotation: List<JsonRotation> = listOf(),
        val damage1S: IntListList = listOf(),
        val powerDamage1S: IntListList = listOf(),
        val conditionDamage1S: IntListList = listOf(),
        val breakbarDamage1S: DoubleListList = listOf(),
        val conditionsStates: IntListList = listOf(),
        val boonsStates: IntListList = listOf(),
        val activeCombatMinions: IntListList = listOf(),
        val healthPercents: DoubleListList = listOf(),
        val barrierPercents: DoubleListList = listOf(),
        val combatReplayData: JsonActorCombatReplayData? = null,
    ) : JsonActorParent

    data class JsonNPC(
        val name: String? = null,
        val totalHealth: Int? = null,
        val condition: Int? = null,
        val concentration: Int? = null,
        val healing: Int? = null,
        val toughness: Int? = null,
        val hitboxHeight: Int? = null,
        val hitboxWidth: Int? = null,
        val instanceID: Int? = null,
        val teamID: Long? = null,
        val minions: List<JsonMinions> = listOf(),
        val isFake: Boolean? = null,
        val dpsAll: List<JsonDPS> = listOf(),
        val statsAll: List<JsonGameplayStatsParent.JsonGameplayStatsAll> = listOf(),
        val defenses: List<JsonDefensesAll> = listOf(),
        val totalDamageDist: ListList<JsonDamageDist> = listOf(),
        val totalDamageTaken: ListList<JsonDamageDist> = listOf(),
        val rotation: List<JsonRotation> = listOf(),
        val damage1S: IntListList = listOf(),
        val powerDamage1S: IntListList = listOf(),
        val conditionDamage1S: IntListList = listOf(),
        val breakbarDamage1S: DoubleListList = listOf(),
        val conditionsStates: IntListList = listOf(),
        val boonsStates: IntListList = listOf(),
        val activeCombatMinions: IntListList = listOf(),
        val healthPercents: DoubleListList = listOf(),
        val barrierPercents: DoubleListList = listOf(),
        val combatReplayData: JsonActorCombatReplayData? = null,
        val id: Int? = null,
        val finalHealth: Int? = null,
        val healthPercentBurned: Double? = null,
        val firstAware: Int? = null,
        val lastAware: Int? = null,
        val buffs: List<JsonBuffsUptime> = listOf(),
        val enemyPlayer: Boolean? = null,
        val breakbarPercents: DoubleListList = listOf(),
    ) : JsonActorParent

    data class JsonPlayer(
        val name: String? = null,
        val totalHealth: Int? = null,
        val condition: Int? = null,
        val concentration: Int? = null,
        val healing: Int? = null,
        val toughness: Int? = null,
        val hitboxHeight: Int? = null,
        val hitboxWidth: Int? = null,
        val instanceID: Int? = null,
        val teamID: Long? = null,
        val minions: List<JsonMinions> = listOf(),
        val isFake: Boolean? = null,
        val dpsAll: List<JsonDPS> = listOf(),
        val statsAll: List<JsonGameplayStatsParent.JsonGameplayStatsAll> = listOf(),
        val defenses: List<JsonDefensesAll> = listOf(),
        val totalDamageDist: ListList<JsonDamageDist> = listOf(),
        val totalDamageTaken: ListList<JsonDamageDist> = listOf(),
        val rotation: List<JsonRotation> = listOf(),
        val damage1S: IntListList = listOf(),
        val powerDamage1S: IntListList = listOf(),
        val conditionDamage1S: IntListList = listOf(),
        val breakbarDamage1S: DoubleListList = listOf(),
        val conditionsStates: IntListList = listOf(),
        val boonsStates: IntListList = listOf(),
        val activeCombatMinions: IntListList = listOf(),
        val healthPercents: DoubleListList = listOf(),
        val barrierPercents: DoubleListList = listOf(),
        val combatReplayData: JsonActorCombatReplayData? = null,
        val account: String? = null,
        val group: Int? = null,
        val hasCommanderTag: Boolean? = null,
        val profession: String? = null,
        val friendlyNPC: Boolean? = null,
        val notInSquad: Boolean? = null,
        val guildID: String? = null,
        val weapons: List<String> = listOf(),
        val activeClones: IntListList = listOf(),
        val dpsTargets: List<List<JsonDPS>> = listOf(),
        val targetDamage1S: IntListListList = listOf(),
        val targetPowerDamage1S: IntListListList = listOf(),
        val targetConditionDamage1S: IntListListList = listOf(),
        val targetBreakbarDamage1S: DoubleListListList = listOf(),
        val targetDamageDist: ListListList<JsonDamageDist> = listOf(),
        val statsTargets: ListList<JsonGameplayStatsParent.JsonGameplayStats> = listOf(),
        val support: List<JsonPlayerSupport> = listOf(),
        val damageModifiers: List<JsonDamageModifierData> = listOf(),
        val damageModifiersTarget: ListList<JsonDamageModifierData> = listOf(),
        val buffUptimes: List<JsonBuffsUptime> = listOf(),
        val selfBuffs: List<JsonPlayerBuffsGeneration> = listOf(),
        val groupBuffs: List<JsonPlayerBuffsGeneration> = listOf(),
        val offGroupBuffs: List<JsonPlayerBuffsGeneration> = listOf(),
        val squadBuffs: List<JsonPlayerBuffsGeneration> = listOf(),
        val buffUptimesActive: List<JsonBuffsUptime> = listOf(),
        val selfBuffsActive: List<JsonPlayerBuffsGeneration> = listOf(),
        val groupBuffsActive: List<JsonPlayerBuffsGeneration> = listOf(),
        val offGroupBuffsActive: List<JsonPlayerBuffsGeneration> = listOf(),
        val squadBuffsActive: List<JsonPlayerBuffsGeneration> = listOf(),
        val deathRecap: List<JsonDeathRecap> = listOf(),
        val consumables: List<JsonConsumable> = listOf(),
        val activeTimes: List<Long> = listOf(),
        val extHealingStats: EXTJsonPlayerHealingStats? = null,
        val extBarrierStats: EXTJsonPlayerBarrierStats? = null,
    ) : JsonActorParent
}

data class JsonMechanic(val time: Long? = null, val actor: String? = null)

data class JsonMechanics(
    val mechanicsData: List<JsonMechanic> = listOf(),
    val name: String? = null,
    val fullName: String? = null,
    val description: String? = null,
    val isAchievementEligibility: Boolean? = null,
)

data class BuffDesc(
    val name: String? = null,
    val icon: String? = null,
    val stacking: Boolean? = null,
    val conversionBasedHealing: Boolean? = null,
    val hybridHealing: Boolean? = null,
    val descriptions: List<String> = listOf(),
)

data class DamageModDesc(
    val name: String? = null,
    val icon: String? = null,
    val description: String? = null,
    val nonMultiplier: Boolean? = null,
    val skillBased: Boolean? = null,
    val approximate: Boolean? = null,
)

data class ExtensionDesc(
    val name: String? = null,
    val version: String? = null,
    val revision: Long? = null,
    val signature: Long? = null,
    val runningExtension: List<String> = listOf(),
)

data class SkillDesc(
    val name: String? = null,
    val autoAttack: Boolean? = null,
    val canCrit: Boolean? = null,
    val icon: String? = null,
    val isSwap: Boolean? = null,
    val isInstantCast: Boolean? = null,
    val isTraitProc: Boolean? = null,
    val isGearProc: Boolean? = null,
    val isNotAccurate: Boolean? = null,
    val conversionBasedHealing: Boolean? = null,
    val hybridHealing: Boolean? = null,
)

data class JsonPhase(
    val start: Long? = null,
    val end: Long? = null,
    val name: String? = null,
    val targets: IntList = listOf(),
    val subPhases: IntList = listOf(),
    val breakbarPhase: Boolean? = null,
)

data class CombatReplayMap(val url: String? = null, val interval: List<Long> = listOf())

data class JsonCombatReplayMetaData(
    val inchToPixel: Float? = null,
    val pollingRate: Int? = null,
    val sizes: List<Int> = listOf(),
    val maps: List<CombatReplayMap> = listOf(),
)
