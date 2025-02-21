package org.inquest.clients

import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.inject.Singleton
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.entities.BossBench
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Allows connection to wingman.
 */
@RegisterRestClient(baseUri = WingmanClient.ENDPOINT, configKey = WingmanClient.STRING)
interface WingmanClient {
    /**
     * Fetches the [BossBench] for the given [bossId] in the given [era].
     */
    @GET
    @Path(BOSS_PATH)
    fun fetchBossBench(
        @QueryParam("bossID") bossId: Long,
        @QueryParam("era") era: String,
    ): Uni<BossBench>

    /**
     * Fetches all bosses currently handled by wingman.
     */
    @GET
    @Path(BOSSES_PATH)
    fun fetchBosses(): Uni<Map<Long, Any>>

    companion object {
        const val STRING = "Wingman"
        const val ENDPOINT = "https://gw2wingman.nevermindcreations.de/api"
        const val BOSS_PATH = "/boss"
        const val BOSSES_PATH = "/bosses"
    }
}

@Singleton
class WingmanService {
    @RestClient
    private lateinit var wingmanClient: WingmanClient

    /**
     * Time the benches were last updated.
     */
    var lastUpdated: Instant = Instant.now()
        private set

    /**
     * true, when this service has data, false otherwise
     */
    val hasData: Boolean
        get() = this.bossBench.isNotEmpty()

    private val bossBench: MutableMap<Long, BossBench> = ConcurrentHashMap()

    /**
     * Fetches all wingman bosses, and their respective benchmarks for nm and cm.
     */
    @Scheduled(every = "4h")
    fun acquireBossBenchData(): Uni<Void> {
        this.lastUpdated = Instant.now()
        return this.wingmanClient.fetchBosses().flatMap { bosses: Map<Long, Any> ->
            Multi
                .createFrom()
                .items(bosses.keys.flatMap { listOf(it, -it) }.stream())
                .flatMap {
                    this.wingmanClient
                        .fetchBossBench(it, "this")
                        .onFailure().recoverWithItem(UNKNOWN_BOSS)
                        .convert()
                        .toPublisher()
                }.filter { it != UNKNOWN_BOSS }
                .invoke { boss -> boss.bossId?.let { this.bossBench[it] = boss } }
                .collect()
                .asList()
                .replaceWithVoid()
        }
    }

    /**
     * @return The benchmark for the boss with the given [id]
     */
    fun bossBench(id: Long) = this.bossBench[id]

    companion object {
        private val UNKNOWN_BOSS = BossBench("-10000000", emptyMap(), null, null, null)
    }
}
