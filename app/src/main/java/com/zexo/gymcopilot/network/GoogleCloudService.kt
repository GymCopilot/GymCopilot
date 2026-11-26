package com.zexo.gymcopilot.network

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface GoogleCloudService {
    
    @POST("v1/projects")
    suspend fun createScriptProject(
        @Body request: CreateProjectRequest
    ): Response<ScriptProjectResponse>

    @PUT("v1/projects/{scriptId}/content")
    suspend fun updateScriptContent(
        @Path("scriptId") scriptId: String,
        @Body content: RequestBody
    ): Response<Unit>

    @POST("v1/projects/{scriptId}/versions")
    suspend fun createVersion(
        @Path("scriptId") scriptId: String,
        @Body request: VersionRequest
    ): Response<VersionResponse>

    @POST("v1/projects/{scriptId}/deployments")
    suspend fun createDeployment(
        @Path("scriptId") scriptId: String,
        @Body request: DeploymentRequest
    ): Response<DeploymentResponse>

    @POST("https://www.googleapis.com/drive/v3/files")
    suspend fun createDriveFolder(
        @Body metadata: DriveFileMetadata
    ): Response<DriveFileResponse>
}

data class CreateProjectRequest(val title: String)
data class ScriptProjectResponse(val scriptId: String, val title: String)

data class VersionRequest(val description: String)
data class VersionResponse(val versionNumber: Int)

data class DeploymentRequest(
    val versionNumber: Int,
    val manifestFileName: String? = null,
    val description: String = "GymCopilot Auto Deployment"
)

data class DeploymentResponse(
    val deploymentId: String,
    val entryPoints: List<EntryPoint>?
)

data class EntryPoint(
    val entryPointType: String,
    val webApp: WebApp?
)

data class WebApp(
    val url: String
)

data class DriveFileMetadata(
    val name: String,
    val mimeType: String = "application/vnd.google-apps.folder",
    val parents: List<String>? = null
)

data class DriveFileResponse(val id: String, val name: String)
