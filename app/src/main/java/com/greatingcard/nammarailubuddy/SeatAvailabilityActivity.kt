package com.greatingcard.nammarailubuddy

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.greatingcard.nammarailubuddy.databinding.ActivityRailServiceBinding
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SeatAvailabilityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRailServiceBinding
    private lateinit var trainEdit: EditText
    private lateinit var fromEdit: EditText
    private lateinit var toEdit: EditText
    private lateinit var dateEdit: EditText
    private lateinit var classEdit: EditText
    private lateinit var quotaEdit: EditText
    private var apiDate: String = ""
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRailServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.serviceTitle.text = getString(R.string.seat_availability_title)
        binding.btnSubmit.text = getString(R.string.check_seats)

        trainEdit = EditText(this).apply { hint = getString(R.string.train_number_hint) }
        fromEdit = EditText(this).apply { hint = getString(R.string.from_station_code) }
        toEdit = EditText(this).apply { hint = getString(R.string.to_station_code) }
        dateEdit = EditText(this).apply {
            hint = getString(R.string.journey_date_hint)
            isFocusable = false
            isClickable = true
        }
        classEdit = EditText(this).apply { hint = getString(R.string.class_code_hint) }
        quotaEdit = EditText(this).apply {
            hint = getString(R.string.quota_hint)
            setText("GN")
        }
        listOf(trainEdit, fromEdit, toEdit, dateEdit, classEdit, quotaEdit).forEach {
            binding.inputContainer.addView(it)
        }

        val today = Calendar.getInstance()
        apiDate = apiDateFormat.format(today.time)
        dateEdit.setText(apiDate)
        dateEdit.setOnClickListener {
            DatePickerDialog(
                this,
                { _, y, m, d ->
                    val cal = Calendar.getInstance().apply { set(y, m, d) }
                    apiDate = apiDateFormat.format(cal.time)
                    dateEdit.setText(apiDate)
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSubmit.setOnClickListener { checkSeats() }
    }

    private fun checkSeats() {
        val trainNo = trainEdit.text?.toString()?.trim().orEmpty()
        val from = fromEdit.text?.toString()?.trim().orEmpty()
        val to = toEdit.text?.toString()?.trim().orEmpty()
        val cls = classEdit.text?.toString()?.trim().orEmpty()
        val quota = quotaEdit.text?.toString()?.trim().orEmpty().ifBlank { "GN" }
        if (trainNo.length < 4 || from.length < 2 || to.length < 2 || cls.isBlank()) {
            Toast.makeText(this, getString(R.string.enter_station_codes), Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.isVisible = true
        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            when (
                val result = NammaRailuApp.repository(application).checkSeatAvailability(
                    trainNo, from, to, apiDate, cls, quota
                )
            ) {
                is ApiResult.Success -> binding.resultText.text = result.data
                is ApiResult.Error -> {
                    binding.resultText.text = result.message
                    Toast.makeText(this@SeatAvailabilityActivity, result.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
            binding.progressBar.isVisible = false
            binding.btnSubmit.isEnabled = true
        }
    }
}
