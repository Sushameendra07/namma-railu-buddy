package com.greatingcard.nammarailubuddy.data

import android.content.Context
import com.google.gson.JsonObject
import com.greatingcard.nammarailubuddy.models.LiveTrainDetails
import com.greatingcard.nammarailubuddy.models.Train
import com.greatingcard.nammarailubuddy.network.IrctcApi
import com.greatingcard.nammarailubuddy.network.IrctcClient
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class IrctcRepository(
    apiKey: String,
    apiHost: String
) {
    private val api: IrctcApi = IrctcClient.create(apiKey, apiHost)
    private val scheduleCoordinateCache = ConcurrentHashMap<String, Pair<Double, Double>>()
    private val locationResolver = StationLocationResolver(this)

    val isConfigured: Boolean = apiKey.isNotBlank()

    suspend fun getLiveTrainStatus(trainNo: String, date: String, context: Context? = null): ApiResult<LiveTrainDetails> =
        safeCall {
            val normalized = trainNo.trim()
            val response = fetchLiveTrainStatusJson(normalized, date)
            val data = IrctcJsonMapper.unwrapData(response)
            scheduleCoordinateCache.putAll(IrctcJsonMapper.parseRouteStationCoordinates(data))
            var details = IrctcJsonMapper.parseLiveTrainStatus(response)
            val routeCodes = IrctcJsonMapper.parseRouteStationCodes(data)
            if (routeCodes.isNotEmpty()) {
                details = details.copy(scheduleStationCodes = routeCodes)
            }
            runCatching {
                val schedule = api.getTrainSchedule(normalized)
                scheduleCoordinateCache.putAll(IrctcJsonMapper.parseScheduleStationCoordinates(schedule))
                details = IrctcJsonMapper.enrichFromSchedule(details, schedule)
                if (details.scheduleStationCodes.isEmpty()) {
                    details = details.copy(
                        scheduleStationCodes = IrctcJsonMapper.parseScheduleStationCodes(schedule)
                    )
                }
            }
            locationResolver.enrichMapLocation(details, context)
        }

    suspend fun getTrainScheduleJson(trainNo: String): ApiResult<JsonObject> = safeCall {
        api.getTrainSchedule(trainNo.trim())
    }

    fun getStationCoordinatesFromCache(stationCode: String): Pair<Double, Double>? =
        scheduleCoordinateCache[stationCode.trim().uppercase()]

    fun cacheStationCoordinates(map: Map<String, Pair<Double, Double>>) {
        scheduleCoordinateCache.putAll(map)
    }

    fun lookupStationCoordinateFromSearch(code: String): Pair<Double, Double>? =
        scheduleCoordinateCache[code.uppercase()]

    suspend fun resolveStationCoordinates(
        stationCode: String,
        stationName: String = "",
        context: Context? = null
    ): Pair<Double, Double>? = locationResolver.resolveCoordinates(stationCode, stationName, context)

    suspend fun searchTrainsBetween(
        fromCode: String,
        toCode: String,
        date: String
    ): ApiResult<List<Train>> = safeCall {
        val response = api.trainBetweenStations(
            fromStationCode = fromCode.trim().uppercase(),
            toStationCode = toCode.trim().uppercase(),
            date = date
        )
        IrctcJsonMapper.parseTrainList(response)
    }

    suspend fun searchStations(query: String): ApiResult<List<Pair<String, String>>> = safeCall {
        val response = api.searchStation(query.trim())
        IrctcJsonMapper.parseStationSuggestions(response)
    }

    suspend fun getLiveStation(stationCode: String): ApiResult<List<Train>> = safeCall {
        val response = api.getLiveStation(stationCode.trim().uppercase())
        IrctcJsonMapper.parseLiveStationAlerts(response).ifEmpty {
            IrctcJsonMapper.parseTrainList(response)
        }
    }

    suspend fun getPnrStatus(pnr: String): ApiResult<String> = safeCall {
        val response = api.getPnrStatus(pnr.trim())
        IrctcJsonMapper.formatPnrSummary(response)
    }

    suspend fun checkSeatAvailability(
        trainNo: String,
        from: String,
        to: String,
        date: String,
        classCode: String,
        quota: String
    ): ApiResult<String> = safeCall {
        val response = api.checkSeatAvailability(
            trainNo = trainNo.trim(),
            fromStnCode = from.trim().uppercase(),
            toStnCode = to.trim().uppercase(),
            date = date,
            classCode = classCode.trim().uppercase(),
            quota = quota.trim().uppercase()
        )
        IrctcJsonMapper.formatSeatAvailability(response)
    }

    suspend fun getTrainSchedule(trainNo: String): ApiResult<String> = safeCall {
        val response = api.getTrainSchedule(trainNo.trim())
        IrctcJsonMapper.formatTrainSchedule(response)
    }

    suspend fun searchTrain(query: String): ApiResult<List<Train>> = safeCall {
        val response = api.searchTrain(query.trim())
        IrctcJsonMapper.parseTrainList(response)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext ApiResult.Error(
                "RAPIDAPI_KEY not configured. Add your RapidAPI key to local.properties.",
                retryable = false
            )
        }
        try {
            val data = block()
            ApiResult.Success(data)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty()
            val friendlyBody = extractApiMessage(body)
            ApiResult.Error(
                message = when (e.code()) {
                    401, 403 -> "Invalid RapidAPI key or subscription. $friendlyBody"
                    404 -> "Live status not found for this train or date. Try another date."
                    429 -> if (friendlyBody.contains("quota", ignoreCase = true)) {
                        "RapidAPI monthly quota exceeded. Upgrade your plan at rapidapi.com or try again later."
                    } else {
                        "Too many requests. Please wait a moment and retry."
                    }
                    else -> friendlyBody.ifBlank { "Server error ${e.code()}" }
                },
                retryable = e.code() == 429 || e.code() >= 500
            )
        } catch (e: IOException) {
            ApiResult.Error("Network error: ${e.message ?: "check connection"}")
        } catch (e: IllegalStateException) {
            ApiResult.Error(e.message ?: "No data returned from API")
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unexpected error")
        }
    }

    /** Only uses `/api/v1/liveTrainStatus` (getLiveTrainStatus does not exist on this API). */
    private suspend fun fetchLiveTrainStatusJson(trainNo: String, date: String): JsonObject {
        val dayIndex = dayIndexFromDate(date)
        var lastError: HttpException? = null

        val attempts: List<suspend () -> JsonObject> = listOf(
            { api.liveTrainStatus(trainNo = trainNo, date = date, startDay = null) },
            { api.liveTrainStatus(trainNo = trainNo, date = null, startDay = dayIndex) },
            { api.liveTrainStatus(trainNo = trainNo, date = date, startDay = dayIndex) }
        )

        for (attempt in attempts) {
            try {
                return attempt()
            } catch (e: HttpException) {
                lastError = e
                if (e.code() == 429 || e.code() == 401 || e.code() == 403) throw e
            }
        }
        throw lastError ?: IllegalStateException("Could not fetch live train status")
    }

    private fun extractApiMessage(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            json.get("message")?.asString ?: body
        }.getOrDefault(body)
    }

    private fun dayIndexFromDate(date: String): Int {
        return runCatching {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val target = fmt.parse(date) ?: return@runCatching 1
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val targetCal = Calendar.getInstance().apply { time = target }
            val diff = ((targetCal.timeInMillis - today.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
            (diff + 1).coerceIn(1, 3)
        }.getOrDefault(1)
    }
}
