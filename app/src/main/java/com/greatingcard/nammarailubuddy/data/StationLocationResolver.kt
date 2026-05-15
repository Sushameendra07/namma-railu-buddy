package com.greatingcard.nammarailubuddy.data

import android.content.Context
import android.location.Geocoder
import com.greatingcard.nammarailubuddy.models.LiveTrainDetails
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class StationLocationResolver(
    private val repository: IrctcRepository
) {
    private val cache = ConcurrentHashMap<String, Pair<Double, Double>>()

    suspend fun resolveCoordinates(
        stationCode: String,
        stationName: String = "",
        context: Context? = null
    ): Pair<Double, Double>? {
        val code = stationCode.trim().uppercase()
        if (code.isBlank()) return null
        cache[code]?.let { return it }

        val fromSchedule = repository.getStationCoordinatesFromCache(code)
        if (fromSchedule != null) {
            cache[code] = fromSchedule
            return fromSchedule
        }

        when (val search = repository.searchStations(code)) {
            is ApiResult.Success -> {
                val match = search.data.firstOrNull { it.first.equals(code, ignoreCase = true) }
                val coords = match?.let { repository.lookupStationCoordinateFromSearch(it.first) }
                if (coords != null) {
                    cache[code] = coords
                    return coords
                }
            }
            else -> Unit
        }

        if (context != null && (stationName.isNotBlank() || code.length in 2..5)) {
            val geocoded = geocodeStation(context, code, stationName)
            if (geocoded != null) {
                cache[code] = geocoded
                return geocoded
            }
        }
        return null
    }

    suspend fun enrichMapLocation(details: LiveTrainDetails, context: Context?): LiveTrainDetails {
        val existingLat = details.mapLatitude ?: details.latitude
        val existingLng = details.mapLongitude ?: details.longitude
        if (existingLat != null && existingLng != null) {
            return details.copy(
                mapLatitude = existingLat,
                mapLongitude = existingLng,
                mapLocationLabel = details.mapLocationLabel.ifBlank {
                    details.activeStatusText.ifBlank { "Live train position" }
                }
            )
        }

        val stationCode = when {
            details.currentStationCode.isNotBlank() -> details.currentStationCode
            details.nextStationCode.isNotBlank() -> details.nextStationCode
            details.sourceStationCode.isNotBlank() -> details.sourceStationCode
            else -> ""
        }
        val stationName = when {
            details.nextStation.isNotBlank() -> details.nextStation
            details.currentStation.isNotBlank() -> details.currentStation
            else -> stationCode
        }

        if (stationCode.isNotBlank()) {
            val coords = resolveCoordinates(stationCode, stationName, context)
            if (coords != null) {
                return details.copy(
                    mapLatitude = coords.first,
                    mapLongitude = coords.second,
                    mapLocationLabel = getString(
                        stationName,
                        stationCode,
                        estimated = details.gpsUnavailable || details.latitude == null
                    )
                )
            }
        }

        return details
    }

    private fun getString(name: String, code: String, estimated: Boolean): String {
        val label = if (name.isNotBlank()) "$name ($code)" else code
        return if (estimated) "Near $label (station)" else label
    }

    private suspend fun geocodeStation(
        context: Context,
        code: String,
        name: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        val geocoder = Geocoder(context, Locale.getDefault())
        val queries = listOf(
            "$code railway station India",
            "${name.ifBlank { code }} railway station India",
            "$code station India"
        )
        for (query in queries) {
            val results = runCatching {
                @Suppress("DEPRECATION")
                geocoder.getFromLocationName(query, 1)?.firstOrNull()?.let { it.latitude to it.longitude }
            }.getOrNull()
            if (results != null) return@withContext results
        }
        null
    }
}
