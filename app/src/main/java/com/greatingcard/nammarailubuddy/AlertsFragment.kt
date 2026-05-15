package com.greatingcard.nammarailubuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.greatingcard.nammarailubuddy.adapters.AlertAdapter
import com.greatingcard.nammarailubuddy.databinding.FragmentAlertsBinding
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private lateinit var alertAdapter: AlertAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        alertAdapter = AlertAdapter(emptyList())
        binding.alertsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.alertsRecyclerView.adapter = alertAdapter
        binding.btnLoadStation.setOnClickListener { loadLiveStation() }
        loadLiveStation()
    }

    private fun loadLiveStation() {
        val code = binding.stationCodeEdit.text?.toString()?.trim().orEmpty()
        if (code.length < 2) {
            binding.emptyStateAlerts.isVisible = true
            binding.emptyStateAlerts.text = getString(R.string.enter_station_codes)
            return
        }

        binding.alertsProgress.isVisible = true
        binding.emptyStateAlerts.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            val repo = NammaRailuApp.repository(requireActivity().application)
            when (val result = repo.getLiveStation(code)) {
                is ApiResult.Success -> {
                    alertAdapter.updateList(result.data)
                    binding.emptyStateAlerts.isVisible = result.data.isEmpty()
                    if (result.data.isEmpty()) {
                        binding.emptyStateAlerts.text = getString(R.string.no_delay_alerts)
                    }
                }
                is ApiResult.Error -> {
                    alertAdapter.updateList(emptyList())
                    binding.emptyStateAlerts.isVisible = true
                    binding.emptyStateAlerts.text = result.message
                }
                else -> Unit
            }
            binding.alertsProgress.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
