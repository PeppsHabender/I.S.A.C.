package org.inquest.clients

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.inquest.entities.DpsReport
import org.inquest.entities.JsonLog
import org.jboss.resteasy.reactive.RestQuery
import java.io.File

@RegisterRestClient(baseUri = DpsReportClient.ENDPOINT, configKey = DpsReportClient.STRING)
interface DpsReportClient {
    @GET
    @Path(META_DATA_PATH)
    fun fetchLog(@QueryParam("permalink") permalink: String): Uni<DpsReport>

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