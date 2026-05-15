package com.greatingcard.nammarailubuddy

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.greatingcard.nammarailubuddy.alarm.DestinationAlarmController
import com.greatingcard.nammarailubuddy.databinding.FragmentHomeBinding
import com.greatingcard.nammarailubuddy.databinding.ItemCoachBinding
import com.greatingcard.nammarailubuddy.models.LiveTrainDetails
import com.greatingcard.nammarailubuddy.ui.home.HomeViewModel
import com.greatingcard.nammarailubuddy.ui.home.HomeViewModelFactory
import com.greatingcard.nammarailubuddy.util.ApiResult
import com.greatingcard.nammarailubuddy.util.StatusBadgeHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(NammaRailuApp.repository(requireActivity().application))
    }

    private var latestLive: LiveTrainDetails? = null
    private var destinationStationCode: String? = null
    private var destinationStationName: String? = null
    private var destinationCoordinates: Pair<Double, Double>? = null
    private var scheduleStations: List<Pair<String, String>> = emptyList()

    private lateinit var alarmController: DestinationAlarmController

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    private val coachInfoMap = mapOf(
        "ENG" to "Locomotive",
        "SLR" to "Luggage / Guard",
        "GS" to "General Seating",
        "UR" to "Unreserved",
        "PC" to "Pantry Car",
        "L" to "Ladies Coach"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setContext(requireContext())
        alarmController = DestinationAlarmController(
            fragment = this,
            onStatusChanged = { status -> binding.txtAlarmStatus.text = status },
            onMessage = { msg ->
                binding.txtDataState.isVisible = true
                binding.txtDataState.text = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        )

        setupDateField()
        setupQuickActions()
        binding.btnFetch.setOnClickListener { fetchLiveStatus() }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
        binding.btnTrack.setOnClickListener { openMap() }
        binding.txtDestinationStation.setOnClickListener { showDestinationPicker() }
        binding.btnSetAlarm.setOnClickListener { saveAlarmConfig() }

        binding.txtDestinationStation.isVisible = true
        binding.editTriggerRadius.isVisible = true
        binding.btnSetAlarm.isVisible = true
        binding.txtAlarmStatus.isVisible = true
        binding.editTriggerRadius.setText(getString(R.string.radius_prefill))

        viewModel.liveStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Loading -> showLoading(true)
                is ApiResult.Success -> {
                    showLoading(false)
                    binding.btnRetry.isVisible = false
                    latestLive = result.data
                    bindLiveDetails(result.data)
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    binding.resultCard.isVisible = false
                    binding.txtDataState.isVisible = true
                    binding.txtDataState.text = result.message
                    binding.btnRetry.isVisible = result.retryable
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupDateField() {
        val today = Calendar.getInstance()
        binding.journeyDateEdit.setText(displayDateFormat.format(today.time))
        binding.journeyDateEdit.tag = dateFormat.format(today.time)
        binding.journeyDateEdit.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply { set(y, m, d) }
                    binding.journeyDateEdit.tag = dateFormat.format(cal.time)
                    binding.journeyDateEdit.setText(displayDateFormat.format(cal.time))
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun fetchLiveStatus() {
        val trainNo = binding.trainNumberEdit.text?.toString()?.trim().orEmpty()
        val date = binding.journeyDateEdit.tag as? String
            ?: binding.journeyDateEdit.text?.toString()?.trim().orEmpty()
        if (trainNo.length < 4) {
            Toast.makeText(context, getString(R.string.enter_valid_train_number), Toast.LENGTH_SHORT).show()
            return
        }
        if (date.isBlank()) {
            Toast.makeText(context, getString(R.string.select_journey_date), Toast.LENGTH_SHORT).show()
            return
        }
        binding.txtDataState.isVisible = true
        binding.txtDataState.text = getString(R.string.syncing_data)
        viewModel.trackTrain(trainNo, date)
    }

    private fun bindLiveDetails(details: LiveTrainDetails) {
        binding.resultCard.isVisible = true
        binding.txtDataState.isVisible = false
        binding.txtTrainName.text = "${details.trainName} (${details.trainNumber})"
        StatusBadgeHelper.apply(binding.txtStatusBadge, details.runStatus)
        binding.txtSource.text = getString(R.string.source_label, details.source.ifBlank { "—" })
        binding.txtDestination.text = getString(R.string.dest_label, details.destination.ifBlank { "—" })
        binding.txtPlatform.text = getString(R.string.platform_issued, details.platform)
        binding.txtEta.text = getString(R.string.eta_label, details.eta)
        binding.txtLastUpdated.text = getString(R.string.last_updated_label, details.lastUpdated)
        binding.txtCurrentStation.isVisible = true
        binding.txtCurrentStation.text = getString(
            R.string.current_station_label,
            formatStation(details.currentStation, details.currentStationCode)
        )
        binding.txtNextStation.isVisible = true
        binding.txtNextStation.text = getString(
            R.string.next_station_label,
            formatStation(details.nextStation, details.nextStationCode)
        )
        binding.txtAccuracy.text = if (details.delayMinutes > 0) {
            getString(R.string.delay_minutes_label, details.delayMinutes)
        } else {
            getString(R.string.on_time_status_label)
        }
        binding.txtCrowdStatus.text = details.activeStatusText
            .ifBlank { details.aheadDistanceText }
            .ifBlank { details.mapLocationLabel }
            .ifBlank { getString(R.string.live_irctc_feed) }

        scheduleStations = buildStationPickerList(details)
        if (destinationStationCode == null && details.destinationStationCode.isNotBlank()) {
            preselectDestination(details.destinationStationCode, details.destination)
        }

        setupCoachSequence(details.coachSequence)
        binding.progressTrain.progress = when (details.runStatus) {
            com.greatingcard.nammarailubuddy.models.TrainRunStatus.ARRIVED -> 100
            com.greatingcard.nammarailubuddy.models.TrainRunStatus.ON_TIME -> 85
            com.greatingcard.nammarailubuddy.models.TrainRunStatus.RUNNING -> 70
            com.greatingcard.nammarailubuddy.models.TrainRunStatus.DELAYED -> 50
            else -> 45
        }
    }

    private fun formatStation(name: String, code: String): String {
        return when {
            name.isNotBlank() && code.isNotBlank() -> "$name ($code)"
            name.isNotBlank() -> name
            code.isNotBlank() -> code
            else -> "—"
        }
    }

    private fun buildStationPickerList(details: LiveTrainDetails): List<Pair<String, String>> {
        val codes = details.scheduleStationCodes
        return codes.map { code ->
            code to code
        }
    }

    private fun preselectDestination(code: String, name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val coords = NammaRailuApp.repository(requireActivity().application)
                .resolveStationCoordinates(code, name, requireContext())
            destinationStationCode = code
            destinationStationName = name
            destinationCoordinates = coords
            binding.txtDestinationStation.text = formatStation(name, code)
            alarmController.setDestination(code, name, coords)
        }
    }

    private fun showDestinationPicker() {
        val options = scheduleStations.map { it.second }.ifEmpty {
            listOfNotNull(
                latestLive?.destinationStationCode?.let { formatStation(latestLive?.destination.orEmpty(), it) }
            )
        }
        if (options.isEmpty()) {
            Toast.makeText(context, getString(R.string.load_train_for_stations), Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.destination_station_hint))
            .setItems(options.toTypedArray()) { _, which ->
                val code = scheduleStations.getOrNull(which)?.first
                    ?: latestLive?.scheduleStationCodes?.getOrNull(which)
                    ?: latestLive?.destinationStationCode
                if (code.isNullOrBlank()) return@setItems
                viewLifecycleOwner.lifecycleScope.launch {
                    val repo = NammaRailuApp.repository(requireActivity().application)
                    val coords = repo.resolveStationCoordinates(code, options[which], requireContext())
                    destinationStationCode = code
                    destinationStationName = options[which]
                    destinationCoordinates = coords
                    binding.txtDestinationStation.text = options[which]
                    alarmController.setDestination(code, options[which], coords)
                }
            }
            .show()
    }

    private fun saveAlarmConfig() {
        val live = latestLive ?: run {
            Toast.makeText(context, getString(R.string.error_locate_train), Toast.LENGTH_SHORT).show()
            return
        }
        val destCode = destinationStationCode ?: live.destinationStationCode
        if (destCode.isNullOrBlank()) {
            Toast.makeText(context, getString(R.string.destination_required), Toast.LENGTH_SHORT).show()
            return
        }
        val radius = binding.editTriggerRadius.text?.toString()?.trim()?.toDoubleOrNull()
        if (radius == null || radius < 1.0 || radius > 50.0) {
            Toast.makeText(context, getString(R.string.radius_validation), Toast.LENGTH_SHORT).show()
            return
        }
        if (destinationCoordinates == null) {
            viewLifecycleOwner.lifecycleScope.launch {
                destinationCoordinates = NammaRailuApp.repository(requireActivity().application)
                    .resolveStationCoordinates(destCode, destinationStationName.orEmpty(), requireContext())
                alarmController.setDestination(destCode, destinationStationName.orEmpty(), destinationCoordinates)
                if (destinationCoordinates == null) {
                    Toast.makeText(context, getString(R.string.destination_coords_unavailable), Toast.LENGTH_LONG).show()
                    return@launch
                }
                armWithDetails(live, destCode, radius)
            }
        } else {
            armWithDetails(live, destCode, radius)
        }
    }

    private fun armWithDetails(live: LiveTrainDetails, destCode: String, radius: Double) {
        val sourceCode = live.sourceStationCode.ifBlank { live.source }
        alarmController.armAlarm(
            trainId = live.trainNumber,
            sourceStationId = sourceCode,
            destinationStationId = destCode,
            radiusKm = radius
        )
    }

    private fun setupCoachSequence(sequence: String) {
        binding.coachContainer.removeAllViews()
        if (sequence.isBlank()) {
            val empty = android.widget.TextView(requireContext()).apply {
                text = getString(R.string.coach_data_unavailable)
                setTextColor("#88FFFFFF".toColorInt())
                textSize = 11f
            }
            binding.coachContainer.addView(empty)
            return
        }
        sequence.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { coachCode ->
            val coachBinding = ItemCoachBinding.inflate(layoutInflater, binding.coachContainer, false)
            coachBinding.coachText.text = coachCode
            val (bgColor, strokeColor, textColor) = coachColors(coachCode)
            coachBinding.coachCard.setCardBackgroundColor(bgColor.toColorInt())
            coachBinding.coachCard.strokeColor = strokeColor.toColorInt()
            coachBinding.coachText.setTextColor(textColor.toColorInt())
            coachBinding.root.setOnClickListener {
                Toast.makeText(
                    context,
                    coachInfoMap[coachCode.uppercase()] ?: "Coach $coachCode",
                    Toast.LENGTH_SHORT
                ).show()
            }
            binding.coachContainer.addView(coachBinding.root)
        }
    }

    private fun coachColors(coachCode: String): Triple<String, String, String> = when {
        coachCode.contains("ENG", ignoreCase = true) -> Triple("#37474F", "#263238", "#FFFFFF")
        coachCode.contains("GS", ignoreCase = true) || coachCode.contains("UR", ignoreCase = true) ->
            Triple("#FBC02D", "#F9A825", "#000000")
        coachCode.startsWith("S", ignoreCase = true) -> Triple("#1976D2", "#1565C0", "#FFFFFF")
        coachCode.contains("B", ignoreCase = true) || coachCode.contains("A", ignoreCase = true) ->
            Triple("#D32F2F", "#B71C1C", "#FFFFFF")
        coachCode.contains("PC", ignoreCase = true) -> Triple("#388E3C", "#2E7D32", "#FFFFFF")
        coachCode.contains("L", ignoreCase = true) -> Triple("#C2185B", "#AD1457", "#FFFFFF")
        else -> Triple("#757575", "#616161", "#FFFFFF")
    }

    private fun setupQuickActions() {
        binding.actionLive.root.setOnClickListener { fetchLiveStatus() }
        binding.actionCoach.root.setOnClickListener {
            if (binding.resultCard.isVisible) {
                binding.homeScrollView.smoothScrollTo(0, binding.coachContainer.top)
            } else {
                Toast.makeText(context, getString(R.string.error_locate_train), Toast.LENGTH_SHORT).show()
            }
        }
        binding.actionPlatform.root.setOnClickListener {
            latestLive?.let {
                Toast.makeText(context, getString(R.string.platform_issued, it.platform), Toast.LENGTH_SHORT).show()
            }
        }
        binding.actionAlerts.root.setOnClickListener {
            activity?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.navigation_alerts
        }
        binding.actionLive.actionTitle.text = getString(R.string.action_live)
        binding.actionCoach.actionTitle.text = getString(R.string.action_coach)
        binding.actionPlatform.actionTitle.text = getString(R.string.action_platform)
        binding.actionAlerts.actionTitle.text = getString(R.string.action_alerts)
    }

    private fun openMap() {
        val live = latestLive ?: run {
            Toast.makeText(context, getString(R.string.error_locate_train), Toast.LENGTH_SHORT).show()
            return
        }
        val lat = live.mapLatitude ?: live.latitude
        val lng = live.mapLongitude ?: live.longitude
        if (lat == null || lng == null) {
            Toast.makeText(context, getString(R.string.map_location_unavailable), Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(requireContext(), MapActivity::class.java).apply {
            putExtra(MapActivity.EXTRA_LAT, lat)
            putExtra(MapActivity.EXTRA_LNG, lng)
            putExtra(
                MapActivity.EXTRA_TITLE,
                "${live.trainName} — ${formatStation(live.currentStation, live.currentStationCode)}"
            )
            putExtra(MapActivity.EXTRA_SUBTITLE, live.mapLocationLabel)
            destinationCoordinates?.let { (dLat, dLng) ->
                putExtra(MapActivity.EXTRA_DEST_LAT, dLat)
                putExtra(MapActivity.EXTRA_DEST_LNG, dLng)
                putExtra(
                    MapActivity.EXTRA_DEST_TITLE,
                    destinationStationName ?: destinationStationCode
                )
            }
        }
        startActivity(intent)
    }

    private fun showLoading(loading: Boolean) {
        binding.progressLoading.isVisible = loading
        binding.btnFetch.isEnabled = !loading
    }

    override fun onDestroyView() {
        alarmController.dispose()
        super.onDestroyView()
        _binding = null
    }
}
