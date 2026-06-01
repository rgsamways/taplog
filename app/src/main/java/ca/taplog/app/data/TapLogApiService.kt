package ca.taplog.app.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TapLogApiService {

    @GET("api/v1/verticals")
    suspend fun getVerticals(): Response<List<VerticalConfig>>

    @GET("api/v1/verticals/{code}")
    suspend fun getVertical(@Path("code") code: String): Response<VerticalConfig>

    @POST("api/v1/organisations")
    suspend fun syncOrganisation(@Body org: OrganisationSyncRequest): Response<SyncResponse>

    @POST("api/v1/sites")
    suspend fun syncSite(@Body site: SiteSyncRequest): Response<SyncResponse>

    @POST("api/v1/assets")
    suspend fun syncAsset(@Body asset: AssetSyncRequest): Response<SyncResponse>

    @POST("api/v1/tag_events")
    suspend fun syncTagEvent(@Body tagEvent: TagEventSyncRequest): Response<SyncResponse>

    @POST("api/v1/inspections")
    suspend fun syncInspection(@Body inspection: InspectionSyncRequest): Response<SyncResponse>

    @POST("api/v1/deficiencies")
    suspend fun syncDeficiency(@Body deficiency: DeficiencySyncRequest): Response<SyncResponse>

    @POST("api/v1/scan_events")
    suspend fun syncScanEvent(@Body scanEvent: ScanEventSyncRequest): Response<SyncResponse>
}
