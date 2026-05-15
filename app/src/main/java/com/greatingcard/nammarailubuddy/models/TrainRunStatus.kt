package com.greatingcard.nammarailubuddy.models

import com.google.gson.JsonElement

enum class TrainRunStatus {
    ON_TIME,
    DELAYED,
    CANCELLED,
    ARRIVED,
    DIVERTED,
    RUNNING,
    UNKNOWN;

    companion object {
        fun fromLiveApi(
            delayMinutes: Int,
            statusCode: String,
            divertedStations: JsonElement?,
            atDestination: Boolean,
            atSource: Boolean,
            alertMessage: String
        ): TrainRunStatus {
            val alert = alertMessage.trim()
            val hasDiversion = divertedStations != null &&
                !divertedStations.isJsonNull &&
                divertedStations.isJsonArray &&
                divertedStations.asJsonArray.size() > 0
            if (hasDiversion || (alert.contains("divert", ignoreCase = true) && alert.length > 12)) {
                return DIVERTED
            }
            if (alert.contains("cancel", ignoreCase = true)) return CANCELLED
            if (atDestination) return ARRIVED
            if (atSource && delayMinutes <= 0) return ON_TIME
            if (delayMinutes > 0) return DELAYED

            return when (statusCode.uppercase()) {
                "T", "R", "RUNNING" -> RUNNING
                "D", "DELAYED" -> DELAYED
                "A", "ARRIVED" -> ARRIVED
                "C", "CANCELLED" -> CANCELLED
                else -> if (delayMinutes == 0) ON_TIME else DELAYED
            }
        }

        fun fromDelayAndFlags(delayMinutes: Int?, rawStatus: String?): TrainRunStatus {
            val status = rawStatus?.lowercase().orEmpty()
            return when {
                status.contains("cancel") -> CANCELLED
                status.contains("divert") -> DIVERTED
                status.contains("arriv") && !status.contains("depart") -> ARRIVED
                status.contains("on time") || status.contains("ontime") -> ON_TIME
                (delayMinutes ?: 0) > 0 -> DELAYED
                status.contains("delay") -> DELAYED
                status.contains("running") -> RUNNING
                else -> UNKNOWN
            }
        }
    }
}
