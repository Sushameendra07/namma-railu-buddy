package com.greatingcard.nammarailubuddy.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.greatingcard.nammarailubuddy.R

class DestinationAlarmController(
    private val fragment: Fragment,
    private val onStatusChanged: (String) -> Unit,
    private val onMessage: (String) -> Unit
) {
    private val firebaseDbUrl =
        "https://nammarailubuddy-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val notificationChannelId = "destination_alarm_channel"
    private val alarmCheckIntervalMs = 20_000L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var destinationCoordinates: Pair<Double, Double>? = null
    private var destinationLabel: String = ""
    private var triggerRadiusKm = 5.0
    private var alarmTriggered = false
    private var activeAlarmId: String? = null
    private var activeAlarmUserId: String? = null
    private var armedTrainId: String = ""

    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val notificationPermissionLauncher: ActivityResultLauncher<String>

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(fragment.requireContext())
        ensureNotificationChannel()

        locationPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) startLocationPolling() else {
                onMessage(fragment.getString(R.string.location_permission_required))
                onStatusChanged(fragment.getString(R.string.alarm_status_disarmed))
            }
        }

        notificationPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                onMessage(fragment.getString(R.string.notification_permission_required))
            }
        }
    }

    fun setDestination(stationCode: String, stationName: String, coordinates: Pair<Double, Double>?) {
        destinationLabel = if (stationName.isNotBlank()) "$stationName ($stationCode)" else stationCode
        destinationCoordinates = coordinates
    }

    fun armAlarm(
        trainId: String,
        sourceStationId: String,
        destinationStationId: String,
        radiusKm: Double
    ) {
        if (destinationCoordinates == null) {
            onMessage(fragment.getString(R.string.destination_coords_unavailable))
            return
        }
        triggerRadiusKm = radiusKm
        alarmTriggered = false
        armedTrainId = trainId
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "demoUser"
        val alarmId = "${trainId}_${System.currentTimeMillis()}"
        val payload = mapOf(
            "trainId" to trainId,
            "sourceStationId" to sourceStationId,
            "destinationStationId" to destinationStationId,
            "triggerKm" to triggerRadiusKm,
            "state" to "armed",
            "createdAt" to ServerValue.TIMESTAMP
        )
        FirebaseDatabase.getInstance(firebaseDbUrl).reference
            .child("alarmSessions")
            .child(uid)
            .child(alarmId)
            .setValue(payload)
            .addOnSuccessListener {
                activeAlarmId = alarmId
                activeAlarmUserId = uid
                onMessage(fragment.getString(R.string.alarm_saved))
                onStatusChanged(fragment.getString(R.string.alarm_status_armed))
                maybeRequestNotificationPermission()
                requestLocationPermissionAndStart()
            }
            .addOnFailureListener {
                onMessage(fragment.getString(R.string.alarm_save_failed))
            }
    }

    fun dispose() {
        stopLocationPolling()
        alarmTriggered = false
    }

    private fun requestLocationPermissionAndStart() {
        val ctx = fragment.context ?: return
        val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            startLocationPolling()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationPolling() {
        if (alarmTriggered) return
        val destination = destinationCoordinates ?: return
        if (!isLocationPermissionGranted()) return

        stopLocationPolling()
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, alarmCheckIntervalMs)
            .setMinUpdateIntervalMillis(alarmCheckIntervalMs)
            .build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val current = result.lastLocation ?: return
                checkDestinationDistance(current, destination)
            }
        }
        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )
        onMessage(fragment.getString(R.string.distance_check_running))
    }

    private fun stopLocationPolling() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun checkDestinationDistance(current: Location, destination: Pair<Double, Double>) {
        if (alarmTriggered) return
        val results = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            destination.first,
            destination.second,
            results
        )
        val distanceKm = results.firstOrNull()?.div(1000.0) ?: return
        if (distanceKm <= triggerRadiusKm) {
            triggerDestinationAlarm(distanceKm)
        }
    }

    private fun triggerDestinationAlarm(distanceKm: Double) {
        if (alarmTriggered) return
        alarmTriggered = true
        stopLocationPolling()
        onStatusChanged(fragment.getString(R.string.alarm_status_triggered))
        onMessage(
            fragment.getString(R.string.alarm_trigger_text, distanceKm, destinationLabel)
        )
        sendAlarmNotification(distanceKm)
        vibratePhone()
        writeAlarmTriggeredState()
    }

    private fun sendAlarmNotification(distanceKm: Double) {
        val ctx = fragment.context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(ctx, notificationChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(fragment.getString(R.string.alarm_trigger_title))
            .setContentText(fragment.getString(R.string.alarm_trigger_text, distanceKm, destinationLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        NotificationManagerCompat.from(ctx)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun vibratePhone() {
        val ctx = fragment.context ?: return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        val pattern = longArrayOf(0, 500, 250, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun writeAlarmTriggeredState() {
        val uid = activeAlarmUserId ?: return
        val alarmId = activeAlarmId ?: return
        FirebaseDatabase.getInstance(firebaseDbUrl).reference
            .child("alarmSessions")
            .child(uid)
            .child(alarmId)
            .child("state")
            .setValue("triggered")
    }

    private fun maybeRequestNotificationPermission() {
        val ctx = fragment.context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        val ctx = fragment.context ?: return false
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun ensureNotificationChannel() {
        val ctx = fragment.context ?: return
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannelCompat.Builder(
            notificationChannelId,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName("Destination alarm")
            .setDescription("Alerts when you are near destination station")
            .setVibrationEnabled(true)
            .setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        NotificationManagerCompat.from(ctx).createNotificationChannel(channel)
    }
}
