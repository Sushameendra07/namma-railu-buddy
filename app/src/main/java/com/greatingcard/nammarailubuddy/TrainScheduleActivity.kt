package com.greatingcard.nammarailubuddy

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.greatingcard.nammarailubuddy.databinding.ActivityRailServiceBinding
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch

class TrainScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRailServiceBinding
    private lateinit var trainEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRailServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.serviceTitle.text = getString(R.string.train_schedule_title)
        binding.btnSubmit.text = getString(R.string.view_schedule)
        trainEdit = EditText(this).apply { hint = getString(R.string.train_number_hint) }
        binding.inputContainer.addView(trainEdit)
        binding.btnSubmit.setOnClickListener { loadSchedule() }
    }

    private fun loadSchedule() {
        val trainNo = trainEdit.text?.toString()?.trim().orEmpty()
        if (trainNo.length < 4) {
            Toast.makeText(this, getString(R.string.enter_valid_train_number), Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.isVisible = true
        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            when (val result = NammaRailuApp.repository(application).getTrainSchedule(trainNo)) {
                is ApiResult.Success -> binding.resultText.text = result.data
                is ApiResult.Error -> {
                    binding.resultText.text = result.message
                    Toast.makeText(this@TrainScheduleActivity, result.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
            binding.progressBar.isVisible = false
            binding.btnSubmit.isEnabled = true
        }
    }
}
