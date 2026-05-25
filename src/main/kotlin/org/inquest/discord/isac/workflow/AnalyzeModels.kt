package org.inquest.discord.isac.workflow

import org.inquest.analysis.model.RunAnalysis
import org.inquest.persistence.mongo.ChannelSettings
import org.inquest.persistence.mongo.Gw2ToDiscord

data class CurrentAnalysis(val accountMap: Map<String, Gw2ToDiscord?>, val analysis: RunAnalysis)

data class PreparedAnalysis(val channelSettings: ChannelSettings, val currentAnalysis: CurrentAnalysis, val hasPreviousRuns: Boolean)
