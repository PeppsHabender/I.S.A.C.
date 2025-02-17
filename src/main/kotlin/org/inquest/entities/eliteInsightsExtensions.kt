package org.inquest.entities

data class EXTJsonBarrierDist(
    val totalBarrier: Int? = null,
    val min: Int? = null,
    val max: Int? = null,
    val hits: Int? = null,
    val id: Long? = null,
    val indirectBarrier: Boolean? = null,
)

data class EXTJsonIncomingBarrierStatistics(val barrier: Int? = null)

data class EXTJsonOutgoingBarrierStatistics(
    val bps: Int? = null,
    val barrier: Int? = null,
    val actorBps: Int? = null,
    val actorBarrier: Int? = null,
)

data class EXTJsonMinionsBarrierStats(
    val totalBarrier: List<Int> = listOf(),
    val totalAlliedBarrier: IntListList = listOf(),
    val totalBarrierDist: ListList<EXTJsonBarrierDist> = listOf(),
    val alliedBarrierDist: ListListList<EXTJsonBarrierDist> = listOf()
)

data class EXTJsonPlayerBarrierStats(
    val outgoingBarrierAllies: ListList<EXTJsonOutgoingBarrierStatistics> = listOf(),
    val outgoingBarrier: List<EXTJsonOutgoingBarrierStatistics> = listOf(),
    val incomingBarrier: List<EXTJsonIncomingBarrierStatistics> = listOf(),
    val alliedBarrier1S: IntListListList = listOf(),
    val barrier1S: IntListList = listOf(),
    val alliedBarrierDist: ListListList<EXTJsonBarrierDist> = listOf(),
    val totalBarrierDist: ListList<EXTJsonBarrierDist> = listOf(),
    val totalIncomingBarrierDist: ListList<EXTJsonBarrierDist> = listOf(),
)

data class EXTJsonHealingDist(
    val totalHealing: Int? = null,
    val totalDownedHealing: Int? = null,
    val min: Int? = null,
    val max: Int? = null,
    val hits: Int? = null,
    val id: Long? = null,
    val indirectHealing: Boolean? = null,
)

data class EXTJsonIncomingHealingStatistics(
    val healed: Int? = null,
    val healingPowerHealed: Int? = null,
    val conversionHealed: Int? = null,
    val hybridHealed: Int? = null,
    val downedHealed: Int? = null,
)

data class EXTJsonOutgoingHealingStatistics(
    val hps: Int? = null,
    val healing: Int? = null,
    val healingPowerHps: Int? = null,
    val healingPowerHealing: Int? = null,
    val conversionHps: Int? = null,
    val conversionHealing: Int? = null,
    val hybridHps: Int? = null,
    val hybridHealing: Int? = null,
    val downedHps: Int? = null,
    val downedHealing: Int? = null,
    val actorHps: Int? = null,
    val actorHealing: Int? = null,
    val actorHealingPowerHps: Int? = null,
    val actorHealingPowerHealing: Int? = null,
    val actorConversionHps: Int? = null,
    val actorConversionHealing: Int? = null,
    val actorHybridHps: Int? = null,
    val actorHybridHealing: Int? = null,
    val actorDownedHps: Int? = null,
    val actorDownedHealing: Int? = null,
)

data class EXTJsonMinionsHealingStats(
    val totalHealing: List<Int> = listOf(),
    val totalAlliedHealing: IntListList = listOf(),
    val totalHealingDist: ListList<EXTJsonHealingDist> = listOf(),
    val alliedHealingDist: ListListList<EXTJsonHealingDist> = listOf()
)

data class EXTJsonPlayerHealingStats(
    val outgoingHealingAllies: ListList<EXTJsonOutgoingHealingStatistics> = listOf(),
    val outgoingHealing: List<EXTJsonOutgoingHealingStatistics> = listOf(),
    val incomingHealing: List<EXTJsonIncomingHealingStatistics> = listOf(),
    val alliedHealing1S: IntListListList = listOf(),
    val alliedHealingPowerHealing1S: IntListListList = listOf(),
    val alliedConversionHealingHealing1S: IntListListList = listOf(),
    val alliedHybridHealing1S: IntListListList = listOf(),
    val healing1S: IntListList = listOf(),
    val healingPowerHealing1S: IntListList = listOf(),
    val conversionHealingHealing1S: IntListList = listOf(),
    val hybridHealing1S: IntListList = listOf(),
    val alliedHealingDist: ListListList<EXTJsonHealingDist> = listOf(),
    val totalHealingDist: ListList<EXTJsonHealingDist> = listOf(),
    val totalIncomingHealingDist: ListList<EXTJsonHealingDist> = listOf(),
)