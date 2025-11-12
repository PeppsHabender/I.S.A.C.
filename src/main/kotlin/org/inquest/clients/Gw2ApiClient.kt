package org.inquest.clients

import io.smallrye.mutiny.Uni
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@RegisterRestClient(baseUri = Gw2ApiClient.ENDPOINT, configKey = Gw2ApiClient.STRING)
interface Gw2ApiClient {
    companion object {
        const val STRING = "Gw2Api"
        const val ENDPOINT = "https://api.guildwars2.com/v2/"
        const val ACCOUNT_PATH = "account"
    }

    @GET
    @Path(ACCOUNT_PATH)
    fun fetchAccount(@HeaderParam("Authorization") authorization: String): Uni<Gw2Account>
}

data class Gw2Account(val name: String)
