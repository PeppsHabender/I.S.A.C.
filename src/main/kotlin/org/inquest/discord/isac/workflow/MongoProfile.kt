package org.inquest.discord.isac.workflow

import org.eclipse.microprofile.config.ConfigProvider
import kotlin.jvm.optionals.getOrDefault

object MongoProfile {
    fun enabled(): Boolean = "mongo" in ConfigProvider.getConfig()
        .getOptionalValue("quarkus.profile", String::class.java)
        .getOrDefault("")
        .trim()
        .split(",")
}
