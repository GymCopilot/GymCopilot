package com.zexo.gymcopilot.network

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName

data class MegaSyncResponse(
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: MegaSyncData?
)

data class MegaSyncData(
    @SerializedName("config") val config: JsonArray?,
    @SerializedName("plans") val plans: JsonArray?,
    @SerializedName("members") val members: JsonArray?,
    @SerializedName("professors") val professors: JsonArray?,
    @SerializedName("schedules") val schedules: JsonArray?,
    @SerializedName("routines") val routines: JsonArray?,
    @SerializedName("store") val store: JsonArray?
)
