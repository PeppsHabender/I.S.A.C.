package org.inquest.integration.dpsreport

import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.inquest.integration.dpsreport.dto.JsonLog
import org.inquest.shared.logging.LogExtension.LOG
import org.inquest.shared.logging.WithLogger
import java.util.UUID

/**
 * Fetches Elite Insights JSON, bypassing the CDN cache once if the regular request fails.
 */
@ApplicationScoped
class DpsReportLogFetcher(
    @param:RestClient private val dpsReportClient: DpsReportClient,
) : WithLogger {
    fun fetchJson(permalink: String): Uni<JsonLog> =
        dpsReportClient.fetchJson(permalink, null)
            .onFailure()
            .recoverWithUni { failure ->
                LOG.warn("Failed to fetch $permalink. Retrying once with a fresh cache key.", failure)
                dpsReportClient.fetchJson(permalink, UUID.randomUUID().toString())
            }
}
