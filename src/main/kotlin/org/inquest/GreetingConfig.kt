package org.inquest

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName


@ConfigMapping(prefix = "greeting")
interface GreetingConfig {

    @WithName("message")
    fun message(): String?

}