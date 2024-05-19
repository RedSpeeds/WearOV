package com.redvirtualcreations.wearov.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.redvirtualcreations.wearov.BuildConfig
import com.redvirtualcreations.wearov.jsonObjects.VertrektijdenApi
import com.redvirtualcreations.wearov.presentation.LatLon
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

class ApiManager {
    private val gson: Gson = GsonBuilder().create()
    private val httpClient: OkHttpClient = OkHttpClient()
    private val baseUrl = "https://api.vertrektijd.info/departures/_geo/"
    private val apiKey = BuildConfig.apiKey

    fun getApiInfo(loc: LatLon): VertrektijdenApi {
        var jsonResponse = ""
        val request = Request.Builder().url("${baseUrl}+${loc.latitude}/${loc.longitude}/0.5")
            .addHeader("X-Vertrektijd-Client-Api-Key", apiKey).get().build()
        return try {
            val response = httpClient.newCall(request).execute()
            jsonResponse = response.body?.string().toString()
            jsonResponse = jsonResponse.replace(
                "\"Station_Info\":[]",
                "\"Station_Info\":{}"
            ) //FIXME Stupid fix in the event the API returns invalid data. Long term solution is finding a better API
            gson.fromJson(jsonResponse, VertrektijdenApi::class.java)
        } catch (exception: IOException) {
            VertrektijdenApi(arrayListOf(), arrayListOf(), true)
        } catch (exception: JsonSyntaxException) {
            Log.e("VertrekAPI", "Encountered malformed JSON! RAW: $jsonResponse", exception.cause)
            FirebaseCrashlytics.getInstance().recordException(exception)
            VertrektijdenApi(arrayListOf(), arrayListOf(), true)
        } catch (exception: Exception) {
            Log.e("VertrekAPI", "Encountered unknown exception!", exception)
            FirebaseCrashlytics.getInstance().recordException(exception)
            VertrektijdenApi(arrayListOf(), arrayListOf(), true)
        }
    }
}