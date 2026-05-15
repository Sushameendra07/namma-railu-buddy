package com.greatingcard.nammarailubuddy.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.greatingcard.nammarailubuddy.models.LiveTrainDetails
import com.greatingcard.nammarailubuddy.models.Train
import com.greatingcard.nammarailubuddy.models.TrainRunStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IrctcJsonMapper {

    private fun JsonElement?.asObject(): JsonObject? =
        if (this != null && isJsonObject) asJsonObject else null

    private fun JsonElement?.asArray(): JsonArray? =
        if (this != null && isJsonArray) asJsonArray else null

    private fun JsonObject?.str(vararg keys: String): String {
        if (this == null) return ""
        for (key in keys) {
            val el = get(key) ?: continue
            if (!el.isJsonNull) {
                val v = when {
                    el.isJsonPrimitive -> el.asString
                    else -> el.toString()
                }
                if (v.isNotBlank() && v != "null") return v.trim('"')
            }
        }
        return ""
    }

    private fun JsonObject?.int(vararg keys: String): Int {
        if (this == null) return 0
        for (key in keys) {
            val el = get(key) ?: continue
            if (!el.isJsonNull && el.isJsonPrimitive) {
                return runCatching { el.asInt }.getOrElse {
                    runCatching { el.asString.toInt() }.getOrDefault(0)
                }
            }
        }
        return 0
    }

    private fun JsonObject?.double(vararg keys: String): Double? {
        if (this == null) return null
        for (key in keys) {
            val el = get(key) ?: continue
            if (!el.isJsonNull && el.isJsonPrimitive) {
                return runCatching { el.asDouble }.getOrElse {
                    runCatching { el.asString.toDouble() }.getOrNull()
                }
            }
        }
        return null
    }

    fun unwrapData(root: JsonObject): JsonObject {
        val status = root.str("status", "success")
        if (status.equals("false", ignoreCase = true) || status == "0") {
            val msg = root.str("message", "error", "msg")
            throw IllegalStateException(msg.ifBlank { "API returned unsuccessful status" })
        }
        return root.get("data")?.asObject()
            ?: root.get("body")?.asObject()
            ?: root
    }

    fun parseLiveTrainStatus(root: JsonObject): LiveTrainDetails {
        val data = unwrapData(root)
        val trainNumber = data.str("train_number", "trainNumber", "train_no", "trainNo")
        val trainName = data.str("train_name", "trainName", "title")

        val currentName = data.str("current_station_name", "currentStationName")
        val currentCode = data.str("current_station_code", "currentStationCode")

        val nextStopInfo = data.get("next_stoppage_info")?.asObject()
        var nextName = nextStopInfo?.str("next_stoppage").orEmpty()
        var nextCode = data.str("next_station_code", "nextStationCode")
        var nextPlatform = 0

        val upcoming = data.get("upcoming_stations")?.asArray()
        if (upcoming != null && upcoming.size() > 0) {
            val nextSt = upcoming[0].asObject()
            if (nextSt != null) {
                if (nextCode.isBlank()) nextCode = nextSt.str("station_code", "stationCode")
                if (nextName.isBlank()) nextName = nextSt.str("station_name", "stationName")
                nextPlatform = nextSt.int("platform_number", "platform")
            }
        }

        val sourceName = data.str("source_stn_name", "source")
        val destName = data.str("dest_stn_name", "destination")
        val sourceCode = data.str("source", "source_station_code").uppercase()
        val destCode = data.str("destination", "destination_station_code").uppercase()

        var platform = data.str("platform_number", "platform")
        if (platform.isBlank() || platform == "0") {
            platform = if (nextPlatform > 0) nextPlatform.toString() else "—"
        }

        val delayMinutes = data.int("delay", "delay_in_mins", "delayInMins", "late_by")
        val statusCode = data.str("status", "train_status")
        val atDstn = data.get("at_dstn")?.asJsonPrimitive?.asBoolean == true
        val atSrc = data.get("at_src")?.asJsonPrimitive?.asBoolean == true
        val alertMsg = data.str("new_alert_msg", "new_message")

        val runStatus = TrainRunStatus.fromLiveApi(
            delayMinutes = delayMinutes,
            statusCode = statusCode,
            divertedStations = data.get("diverted_stations"),
            atDestination = atDstn,
            atSource = atSrc,
            alertMessage = alertMsg
        )

        val eta = data.str("eta", "etd", "cur_stn_sta").ifBlank {
            upcoming?.get(0)?.asObject()?.str("eta", "sta").orEmpty()
        }
        val departure = data.str("std", "cur_stn_std", "etd")
        val arrival = data.str("sta", "cur_stn_sta")

        val coords = parseLiveCoordinates(data)
        val gpsFlag = data.get("gps_unable")?.asJsonPrimitive?.asBoolean == true
        val gpsUnavailable = coords == null && gpsFlag

        val bubble = data.get("bubble_message")?.asObject()
        val activeStatus = listOfNotNull(
            bubble?.str("readable_message", "message"),
            data.str("ahead_distance_text"),
            data.str("status_as_of")
        ).firstOrNull { it.isNotBlank() }.orEmpty()

        val statusAsOf = data.str("status_as_of", "update_time")
        val route = buildRouteSummary(data)
        val coaches = parseCoachSequence(data)
        val updated = statusAsOf.ifBlank {
            SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
        }

        val mapLabel = when {
            activeStatus.isNotBlank() -> activeStatus
            coords != null && !gpsUnavailable -> "Live train position"
            currentName.isNotBlank() -> "Near $currentName ($currentCode)"
            else -> "Live IRCTC"
        }

        return LiveTrainDetails(
            trainNumber = trainNumber,
            trainName = trainName.ifBlank { "Train $trainNumber" },
            source = sourceName.ifBlank { sourceCode },
            destination = destName.ifBlank { destCode },
            sourceStationCode = sourceCode.take(5),
            destinationStationCode = destCode.take(5),
            currentStation = currentName,
            currentStationCode = currentCode.take(5),
            nextStation = nextName,
            nextStationCode = nextCode.take(5),
            platform = platform,
            delayMinutes = delayMinutes,
            runStatus = runStatus,
            eta = eta.ifBlank { "—" },
            departureTime = departure,
            arrivalTime = arrival,
            lastUpdated = updated,
            latitude = coords?.first,
            longitude = coords?.second,
            mapLatitude = coords?.first,
            mapLongitude = coords?.second,
            mapLocationLabel = mapLabel,
            gpsUnavailable = gpsUnavailable,
            routeSummary = route,
            coachSequence = coaches,
            activeStatusText = activeStatus,
            aheadDistanceText = data.str("ahead_distance_text"),
            statusAsOf = statusAsOf,
            rawJson = data.toString()
        )
    }

    fun parseRouteStationCoordinates(data: JsonObject): Map<String, Pair<Double, Double>> {
        val map = mutableMapOf<String, Pair<Double, Double>>()
        listOf("upcoming_stations", "previous_stations").forEach { key ->
            data.get(key)?.asArray()?.forEach { el ->
                val st = el.asObject() ?: return@forEach
                val code = st.str("station_code", "stationCode").uppercase()
                val lat = st.double("station_lat", "latitude", "lat")
                val lng = st.double("station_lng", "longitude", "lng")
                if (code.isNotBlank() && lat != null && lng != null) {
                    map[code] = lat to lng
                }
            }
        }
        return map
    }

    fun parseRouteStationCodes(data: JsonObject): List<String> {
        val codes = mutableListOf<String>()
        listOf("previous_stations", "upcoming_stations").forEach { key ->
            data.get(key)?.asArray()?.forEach { el ->
                el.asObject()?.str("station_code", "stationCode")?.uppercase()?.let {
                    if (it.isNotBlank()) codes.add(it)
                }
            }
        }
        return codes.distinct()
    }

    /** IRCTC live API: cur_stn_lat/lng or travelling_from_lat_lng [lat, lng]. */
    private fun parseLiveCoordinates(data: JsonObject): Pair<Double, Double>? {
        val travelling = data.get("travelling_from_lat_lng")?.asArray()
        if (travelling != null && travelling.size() >= 2) {
            val lat = runCatching { travelling[0].asDouble }.getOrNull()
            val lng = runCatching { travelling[1].asDouble }.getOrNull()
            if (lat != null && lng != null && (lat != 0.0 || lng != 0.0)) return lat to lng
        }
        val lat = data.double("cur_stn_lat", "latitude", "lat", "train_lat")
        val lng = data.double("cur_stn_lng", "longitude", "lng", "lon", "train_lng")
        if (lat != null && lng != null) return lat to lng
        return null
    }

    fun enrichFromSchedule(details: LiveTrainDetails, scheduleRoot: JsonObject): LiveTrainDetails {
        val data = unwrapData(scheduleRoot)
        val coachFromSchedule = parseCoachSequence(data)
        val stationCodes = parseScheduleStationCodes(scheduleRoot)
        return details.copy(
            coachSequence = coachFromSchedule.ifBlank { details.coachSequence },
            scheduleStationCodes = stationCodes.ifEmpty { details.scheduleStationCodes }
        )
    }

    fun parseScheduleStationCoordinates(scheduleRoot: JsonObject): Map<String, Pair<Double, Double>> {
        val data = unwrapData(scheduleRoot)
        val route = data.get("route")?.asArray() ?: data.get("stations")?.asArray() ?: return emptyMap()
        val map = mutableMapOf<String, Pair<Double, Double>>()
        route.forEach { el ->
            val st = el.asObject() ?: return@forEach
            val code = st.str("station_code", "stationCode", "stnCode", "code").uppercase()
            val lat = st.double("latitude", "lat", "stn_lat")
            val lng = st.double("longitude", "lng", "lon", "stn_lng")
            if (code.isNotBlank() && lat != null && lng != null) {
                map[code] = lat to lng
            }
        }
        return map
    }

    fun parseScheduleStationCodes(scheduleRoot: JsonObject): List<String> {
        val data = unwrapData(scheduleRoot)
        val route = data.get("route")?.asArray() ?: data.get("stations")?.asArray() ?: return emptyList()
        return route.mapNotNull { el ->
            el.asObject()?.str("station_code", "stationCode", "stnCode", "code")?.uppercase()?.takeIf { it.isNotBlank() }
        }
    }

    fun parseCoachSequence(data: JsonObject): String {
        val direct = data.str(
            "coach_position", "coachPosition", "coach_sequence", "coachSequence",
            "coaches", "rake", "rake_type", "rakeType", "coach_composition", "coachComposition"
        )
        if (direct.isNotBlank()) return normalizeCoachList(direct)

        val rakeArray = data.get("rake")?.asArray()
            ?: data.get("coaches")?.asArray()
            ?: data.get("coach_list")?.asArray()
        if (rakeArray != null && rakeArray.size() > 0) {
            val codes = mutableListOf<String>()
            rakeArray.forEach { el ->
                when {
                    el.isJsonPrimitive -> codes.add(el.asString.trim())
                    el.isJsonObject -> {
                        val o = el.asObject()
                        val code = o.str("coach", "coach_name", "coachName", "code", "type")
                        if (code.isNotBlank()) codes.add(code)
                    }
                }
            }
            if (codes.isNotEmpty()) return codes.joinToString(",")
        }
        return ""
    }

    private fun normalizeCoachList(raw: String): String {
        return raw.replace("|", ",").replace("/", ",")
            .split(",", " ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(",")
    }

    private fun buildRouteSummary(data: JsonObject): String {
        val updates = data.get("updates")?.asArray()
            ?: data.get("route")?.asArray()
            ?: data.get("stations")?.asArray()
        if (updates == null || updates.size() == 0) return ""
        val parts = mutableListOf<String>()
        val limit = minOf(updates.size(), 5)
        for (i in 0 until limit) {
            val st = updates[i].asObject() ?: continue
            val name = st.str("station_name", "stationName", "name", "code")
            val delay = st.int("delay", "delay_in_mins")
            if (name.isNotBlank()) {
                parts.add(if (delay > 0) "$name (+${delay}m)" else name)
            }
        }
        return parts.joinToString(" → ")
    }

    fun parseTrainList(root: JsonObject): List<Train> {
        val data = unwrapData(root)
        val array = data.get("trains")?.asArray()
            ?: data.get("data")?.asArray()
            ?: data.get("train_list")?.asArray()
            ?: if (data.has("train_number") || data.has("trainNumber")) JsonArray().apply { add(data) } else null
        if (array == null) return emptyList()
        return array.mapNotNull { el ->
            val obj = el.asObject() ?: return@mapNotNull null
            parseTrainItem(obj)
        }
    }

    fun parseTrainItem(obj: JsonObject): Train {
        val number = obj.str("train_number", "trainNumber", "train_no", "trainNo")
        val name = obj.str("train_name", "trainName", "name")
        val from = obj.str("from_stn_name", "from", "source", "from_station_name")
        val to = obj.str("to_stn_name", "to", "destination", "to_station_name")
        val platform = obj.str("platform", "platform_number", "pf")
        val delayMin = obj.int("delay", "delay_in_mins", "late_by")
        val statusText = obj.str("status", "train_status", "journey_status")
        val runStatus = TrainRunStatus.fromDelayAndFlags(delayMin, statusText)
        val delayLabel = when (runStatus) {
            TrainRunStatus.CANCELLED -> "Cancelled"
            TrainRunStatus.DIVERTED -> "Diverted"
            TrainRunStatus.ARRIVED -> "Arrived"
            TrainRunStatus.ON_TIME -> "On Time"
            TrainRunStatus.DELAYED -> if (delayMin > 0) "Delayed ${delayMin}m" else "Delayed"
            else -> statusText.ifBlank { "Running" }
        }
        return Train(
            number = number,
            name = name.ifBlank { number },
            platform = platform.ifBlank { "—" },
            delay = delayLabel,
            destination = to,
            source = from,
            eta = obj.str("arrival_time", "eta", "arrivalTime").ifBlank { "—" },
            runStatus = runStatus,
            statusMessage = obj.str("remark", "statusMessage", "info").ifBlank { delayLabel },
            lastVerified = "IRCTC Live"
        )
    }

    fun parseStationSuggestions(root: JsonObject): List<Pair<String, String>> {
        val data = unwrapData(root)
        val array = data.get("stations")?.asArray()
            ?: data.get("data")?.asArray()
        if (array == null) return emptyList()
        return array.mapNotNull { el ->
            val obj = el.asObject() ?: return@mapNotNull null
            val code = obj.str("station_code", "stationCode", "code", "stnCode")
            val name = obj.str("station_name", "stationName", "name")
            if (code.isBlank()) null else code to name.ifBlank { code }
        }
    }

    fun parseLiveStationAlerts(root: JsonObject): List<Train> {
        val data = unwrapData(root)
        val array = data.get("trains")?.asArray()
            ?: data.get("data")?.asArray()
            ?: data.get("train_list")?.asArray()
        if (array == null) return emptyList()
        return array.mapNotNull { el ->
            val obj = el.asObject() ?: return@mapNotNull null
            val train = parseTrainItem(obj)
            val noteworthy = train.runStatus == TrainRunStatus.DELAYED ||
                train.runStatus == TrainRunStatus.CANCELLED ||
                train.runStatus == TrainRunStatus.DIVERTED ||
                train.delay.contains("delay", ignoreCase = true)
            if (noteworthy) train else null
        }
    }

    fun formatPnrSummary(root: JsonObject): String {
        val data = unwrapData(root)
        val pnr = data.str("pnrNumber", "pnr", "PNR")
        val train = data.str("trainName", "train_name")
        val trainNo = data.str("trainNo", "train_number")
        val from = data.str("from", "boardingPoint", "from_station")
        val to = data.str("to", "reservationUpto", "to_station")
        val date = data.str("doj", "dateOfJourney", "journeyDate")
        val chart = data.str("chartStatus", "ChartPrepared", "chart_prepared")
        val passengers = data.get("passengerList")?.asArray()
            ?: data.get("PassengerStatus")?.asArray()
            ?: data.get("passengers")?.asArray()
        val sb = StringBuilder()
        sb.appendLine("PNR: $pnr")
        sb.appendLine("Train: $train ($trainNo)")
        sb.appendLine("Route: $from → $to")
        sb.appendLine("Date: $date")
        sb.appendLine("Chart: ${chart.ifBlank { "—" }}")
        passengers?.forEachIndexed { index, el ->
            val p = el.asObject() ?: return@forEachIndexed
            val berth = p.str("bookingStatus", "BookingStatus", "currentStatus", "CurrentStatus")
            val coach = p.str("coach", "Coach")
            sb.appendLine("Passenger ${index + 1}: $berth ${if (coach.isNotBlank()) "($coach)" else ""}")
        }
        return sb.toString().trim()
    }

    fun formatSeatAvailability(root: JsonObject): String {
        val data = unwrapData(root)
        val availability = data.str("availability", "availablity", "status")
        val fare = data.str("fare", "totalFare", "total_fare")
        val cls = data.str("class", "classCode", "class_type")
        val quota = data.str("quota")
        val train = data.str("trainName", "train_name")
        return buildString {
            appendLine(train.ifBlank { "Seat availability" })
            appendLine("Class: ${cls.ifBlank { "—" }} | Quota: ${quota.ifBlank { "GN" }}")
            appendLine("Status: ${availability.ifBlank { data.toString() }}")
            if (fare.isNotBlank()) appendLine("Fare: ₹$fare")
        }.trim()
    }

    fun formatTrainSchedule(root: JsonObject): String {
        val data = unwrapData(root)
        val trainName = data.str("train_name", "trainName")
        val trainNo = data.str("train_number", "trainNumber", "trainNo")
        val route = data.get("route")?.asArray()
            ?: data.get("stations")?.asArray()
        val sb = StringBuilder()
        sb.appendLine("$trainName ($trainNo)")
        route?.forEach { el ->
            val st = el.asObject() ?: return@forEach
            val name = st.str("station_name", "stationName", "stnName")
            val code = st.str("station_code", "stationCode", "stnCode")
            val arr = st.str("sta", "arrivalTime", "arrival_time")
            val dep = st.str("std", "departureTime", "departure_time")
            val halt = st.str("halt", "haltTime")
            sb.appendLine("• $name ($code)  Arr: ${arr.ifBlank { "—" }}  Dep: ${dep.ifBlank { "—" }}  Halt: ${halt.ifBlank { "—" }}")
        }
        return sb.toString().trim().ifBlank { data.toString() }
    }
}
