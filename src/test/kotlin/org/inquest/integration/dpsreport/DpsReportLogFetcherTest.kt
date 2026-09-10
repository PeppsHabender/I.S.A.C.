package org.inquest.integration.dpsreport

import io.smallrye.mutiny.Uni
import org.inquest.integration.dpsreport.dto.DpsReport
import org.inquest.integration.dpsreport.dto.JsonLog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.ArrayDeque

class DpsReportLogFetcherTest {
    @Test
    fun `successful request uses the regular cache key`() {
        val expected = JsonLog(fightName = "Slothasor")
        val client = RecordingClient(success(expected))

        val result = DpsReportLogFetcher(client).fetchJson(PERMALINK).await().indefinitely()

        assertSame(expected, result)
        assertEquals(1, client.requests.size)
        assertNull(client.requests.single().cacheBust)
    }

    @Test
    fun `failed request retries once with a fresh cache key`() {
        val expected = JsonLog(fightName = "Slothasor")
        val client = RecordingClient(failure(IllegalStateException("truncated response")), success(expected))

        val result = DpsReportLogFetcher(client).fetchJson(PERMALINK).await().indefinitely()

        assertSame(expected, result)
        assertEquals(2, client.requests.size)
        assertNull(client.requests.first().cacheBust)
        assertNotNull(client.requests.last().cacheBust)
    }

    @Test
    fun `failed retry is propagated without a third request`() {
        val retryFailure = IllegalStateException("retry failed")
        val client = RecordingClient(failure(IllegalStateException("initial failure")), failure(retryFailure))

        val thrown = assertThrows(IllegalStateException::class.java) {
            DpsReportLogFetcher(client).fetchJson(PERMALINK).await().indefinitely()
        }

        assertSame(retryFailure, thrown)
        assertEquals(2, client.requests.size)
    }

    private data class Request(val permalink: String, val cacheBust: String?)

    private class RecordingClient(vararg responses: Uni<JsonLog>) : DpsReportClient {
        private val responses = ArrayDeque(responses.toList())
        val requests = mutableListOf<Request>()

        override fun fetchLog(permalink: String): Uni<DpsReport> =
            Uni.createFrom().failure(UnsupportedOperationException("Not used by these tests"))

        override fun fetchJson(permalink: String, cacheBust: String?): Uni<JsonLog> {
            requests += Request(permalink, cacheBust)
            return responses.removeFirst()
        }
    }

    companion object {
        private const val PERMALINK = "https://dps.report/9VAN-20260907-210504_sloth"

        private fun success(log: JsonLog): Uni<JsonLog> = Uni.createFrom().item(log)

        private fun failure(exception: RuntimeException): Uni<JsonLog> = Uni.createFrom().failure(exception)
    }
}
