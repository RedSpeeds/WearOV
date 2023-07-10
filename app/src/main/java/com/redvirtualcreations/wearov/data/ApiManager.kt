package com.redvirtualcreations.wearov.data

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.redvirtualcreations.wearov.BuildConfig
import com.redvirtualcreations.wearov.jsonObjects.VertrektijdenApi
import com.redvirtualcreations.wearov.presentation.LatLon
import okhttp3.OkHttpClient
import okhttp3.Request

class ApiManager {
    private val gson: Gson = GsonBuilder().create()
    private val httpClient : OkHttpClient = OkHttpClient()
    private val baseUrl = "https://api.vertrektijd.info/departures/_geo/"
    private val apiKey = BuildConfig.apiKey

    public suspend fun getApiInfo(loc: LatLon): VertrektijdenApi {
        val request = Request.Builder().url("${baseUrl}+${loc.latitude}/${loc.longitude}/0.5")
            .addHeader("X-Vertrektijd-Client-Api-Key", apiKey).get().build()
        val response = httpClient.newCall(request).execute()
        return gson.fromJson(response.body?.string(), VertrektijdenApi::class.java)

    }
}