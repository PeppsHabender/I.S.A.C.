package org.inquest.clients

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.inquest.entities.DpsReport
import org.inquest.entities.JsonLog

/**
 * Allows connection to dps report for fetching log information.
 */
@RegisterRestClient(baseUri = DpsReportClient.ENDPOINT, configKey = DpsReportClient.STRING)
interface DpsReportClient {
    /**
     * Fetches the meta-data for the given [permalink].
     *
     * @return The [DpsReport] entity
     */
    @GET
    @Path(META_DATA_PATH)
    fun fetchLog(@QueryParam("permalink") permalink: String): Uni<DpsReport>

    /**
     * Fetches the whole ei json log for the given [permalink].
     *
     * @return The [JsonLog] entity
     */
    @GET
    @Path(JSON_PATH)
    fun fetchJson(@QueryParam("permalink") permalink: String): Uni<JsonLog>

    companion object {
        const val STRING = "DpsReport"
        const val ENDPOINT = "https://dps.report"
        const val META_DATA_PATH = "/getUploadMetadata"
        const val JSON_PATH = "/getJson"
    }
}
