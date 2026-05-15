package com.greatingcard.nammarailubuddy

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.greatingcard.nammarailubuddy.adapters.TrainAdapter
import com.greatingcard.nammarailubuddy.databinding.FragmentTrainsBinding
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrainsFragment : Fragment() {

    private var _binding: FragmentTrainsBinding? = null
    private val binding get() = _binding!!
    private lateinit var trainAdapter: TrainAdapter
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTrainsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        trainAdapter = TrainAdapter(emptyList())
        binding.trainsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.trainsRecyclerView.adapter = trainAdapter

        val today = Calendar.getInstance()
        binding.journeyDateEdit.setText(displayDateFormat.format(today.time))
        binding.journeyDateEdit.tag = apiDateFormat.format(today.time)
        binding.journeyDateEdit.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply { set(y, m, d) }
                    binding.journeyDateEdit.tag = apiDateFormat.format(cal.time)
                    binding.journeyDateEdit.setText(displayDateFormat.format(cal.time))
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSearchTrains.setOnClickListener { searchTrains() }
    }

    private fun searchTrains() {
        val from = binding.fromStationEdit.text?.toString()?.trim().orEmpty()
        val to = binding.toStationEdit.text?.toString()?.trim().orEmpty()
        val date = binding.journeyDateEdit.tag as? String ?: return
        if (from.length < 2 || to.length < 2) {
            binding.trainsErrorText.isVisible = true
            binding.trainsErrorText.text = getString(R.string.enter_station_codes)
            return
        }

        binding.trainsProgress.isVisible = true
        binding.trainsErrorText.isVisible = false
        binding.btnSearchTrains.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            val repo = NammaRailuApp.repository(requireActivity().application)
            when (val result = repo.searchTrainsBetween(from, to, date)) {
                is ApiResult.Success -> {
                    trainAdapter.updateList(result.data)
                    if (result.data.isEmpty()) {
                        binding.trainsErrorText.isVisible = true
                        binding.trainsErrorText.text = getString(R.string.no_trains_found_api)
                    }
                }
                is ApiResult.Error -> {
                    trainAdapter.updateList(emptyList())
                    binding.trainsErrorText.isVisible = true
                    binding.trainsErrorText.text = result.message
                }
                else -> Unit
            }
            binding.trainsProgress.isVisible = false
            binding.btnSearchTrains.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
