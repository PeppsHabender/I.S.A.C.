package org.inquest.clients

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.inquest.entities.wingman.BossBench

/**
 * Allows connection to wingman.
 */
@RegisterRestClient(baseUri = WingmanClient.ENDPOINT, configKey = WingmanClient.STRING)
interface WingmanClient {
    companion object {
        const val STRING = "Wingman"
        const val ENDPOINT = "https://gw2wingman.nevermindcreations.de/api"
        const val BOSS_PATH = "/boss"
        const val BOSSES_PATH = "/bosses"
    }

    /**
     * Fetches the [BossBench] for the given [bossId] in the given [era].
     */
    @GET
    @Path(BOSS_PATH)
    fun fetchBossBench(@QueryParam("bossID") bossId: Long, @QueryParam("era") era: String): Uni<BossBench>

    /**
     * Fetches all bosses currently handled by wingman.
     */
    @GET
    @Path(BOSSES_PATH)
    fun fetchBosses(): Uni<Map<Long, Any>>
}
