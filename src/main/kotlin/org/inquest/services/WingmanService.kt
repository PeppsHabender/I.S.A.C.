package org.inquest.services

import io.quarkus.scheduler.Scheduled
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import jakarta.inject.Singleton
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.clients.WingmanClient
import org.inquest.entities.wingman.BossBench
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

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
            Multi.createFrom()
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
