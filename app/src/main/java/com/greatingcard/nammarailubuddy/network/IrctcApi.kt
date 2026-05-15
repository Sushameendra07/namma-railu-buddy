package com.greatingcard.nammarailubuddy.network

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * IRCTC RapidAPI (irctc1.p.rapidapi.com) — live railway data.
 * @see <a href="https://rapidapi.com/IRCTCAPI/api/irctc1">RapidAPI IRCTC</a>
 */
interface IrctcApi {

    /** Primary live status endpoint (RapidAPI irctc1). */
    @GET("api/v1/liveTrainStatus")
    suspend fun liveTrainStatus(
        @Query("trainNo") trainNo: String,
        @Query("date") date: String? = null,
        @Query("startDay") startDay: Int? = null
    ): JsonObject

    @GET("api/v3/trainBetweenStations")
    suspend fun trainBetweenStations(
        @Query("fromStationCode") fromStationCode: String,
        @Query("toStationCode") toStationCode: String,
        @Query("date") date: String
    ): JsonObject

    @GET("api/v1/searchStation")
    suspend fun searchStation(
        @Query("query") query: String
    ): JsonObject

    @GET("api/v1/searchTrain")
    suspend fun searchTrain(
        @Query("query") query: String
    ): JsonObject

    @GET("api/v1/getTrainSchedule")
    suspend fun getTrainSchedule(
        @Query("trainNo") trainNo: String
    ): JsonObject

    @GET("api/v3/getPNRStatus")
    suspend fun getPnrStatus(
        @Query("pnrNumber") pnrNumber: String
    ): JsonObject

    @GET("api/v3/checkSeatAvailability")
    suspend fun checkSeatAvailability(
        @Query("trainNo") trainNo: String,
        @Query("fromStnCode") fromStnCode: String,
        @Query("toStnCode") toStnCode: String,
        @Query("date") date: String,
        @Query("classCode") classCode: String,
        @Query("quota") quota: String = "GN"
    ): JsonObject

    @GET("api/v1/getTrainsByStation")
    suspend fun getTrainsByStation(
        @Query("stationCode") stationCode: String
    ): JsonObject

    @GET("api/v1/getLiveStation")
    suspend fun getLiveStation(
        @Query("stationCode") stationCode: String,
        @Query("hours") hours: Int = 2
    ): JsonObject
}
