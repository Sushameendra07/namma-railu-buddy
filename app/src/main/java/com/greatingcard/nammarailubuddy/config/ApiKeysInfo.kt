package com.greatingcard.nammarailubuddy.config

import com.greatingcard.nammarailubuddy.BuildConfig
import java.io.File

/**
 * Single place in code that describes every API key and where it is stored.
 */
object ApiKeysInfo {

    data class KeyEntry(
        val name: String,
        val purpose: String,
        val storageLocation: String,
        val isConfigured: Boolean,
        val maskedValue: String,
        val usedBy: List<String>
    )

    fun allEntries(projectRoot: File, firebaseJsonExists: Boolean): List<KeyEntry> {
        val rapid = BuildConfig.RAPIDAPI_KEY
        return listOf(
            KeyEntry(
                name = "RAPIDAPI_KEY",
                purpose = "IRCTC live trains, PNR, seats, station board",
                storageLocation = "local.properties → RAPIDAPI_KEY",
                isConfigured = rapid.isNotBlank(),
                maskedValue = mask(rapid),
                usedBy = listOf(
                    "Home — live train status",
                    "Trains — between stations",
                    "Alerts — live station",
                    "PNR / Schedule / Seat screens"
                )
            ),
            KeyEntry(
                name = "RAPIDAPI_HOST",
                purpose = "RapidAPI server hostname",
                storageLocation = "local.properties → RAPIDAPI_HOST",
                isConfigured = BuildConfig.RAPIDAPI_HOST.isNotBlank(),
                maskedValue = BuildConfig.RAPIDAPI_HOST,
                usedBy = listOf("All IRCTC API calls (irctc1.p.rapidapi.com)")
            ),
            KeyEntry(
                name = "MAPS_API_KEY",
                purpose = "Google Maps — train location map",
                storageLocation = "local.properties → MAPS_API_KEY",
                isConfigured = BuildConfig.MAPS_API_KEY.isNotBlank(),
                maskedValue = mask(BuildConfig.MAPS_API_KEY),
                usedBy = listOf("MapActivity")
            ),
            KeyEntry(
                name = "Firebase (google-services.json)",
                purpose = "Email login, Google sign-in, session",
                storageLocation = "app/google-services.json",
                isConfigured = firebaseJsonExists,
                maskedValue = if (firebaseJsonExists) "Present (see JSON file)" else "Missing",
                usedBy = listOf("LoginActivity", "RegisterActivity", "ProfileFragment")
            )
        )
    }

    fun localPropertiesPath(projectRoot: File): String =
        File(projectRoot, "local.properties").absolutePath

    private fun mask(value: String): String = when {
        value.isBlank() -> "Not set — add to local.properties"
        value.length <= 8 -> "****"
        else -> "${value.take(4)}…${value.takeLast(4)} (${value.length} chars)"
    }
}
