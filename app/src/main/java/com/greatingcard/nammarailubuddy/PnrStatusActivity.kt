package com.greatingcard.nammarailubuddy

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.greatingcard.nammarailubuddy.databinding.ActivityRailServiceBinding
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch

class PnrStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRailServiceBinding
    private lateinit var pnrEdit: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRailServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.serviceTitle.text = getString(R.string.pnr_status_title)
        binding.btnSubmit.text = getString(R.string.check_pnr)
        pnrEdit = EditText(this).apply { hint = getString(R.string.pnr_hint) }
        binding.inputContainer.addView(pnrEdit)
        binding.btnSubmit.setOnClickListener { checkPnr() }
    }

    private fun checkPnr() {
        val pnr = pnrEdit.text?.toString()?.trim().orEmpty()
        if (pnr.length != 10) {
            Toast.makeText(this, getString(R.string.pnr_hint), Toast.LENGTH_SHORT).show()
            return
        }
        binding.progressBar.isVisible = true
        binding.btnSubmit.isEnabled = false
        lifecycleScope.launch {
            when (val result = NammaRailuApp.repository(application).getPnrStatus(pnr)) {
                is ApiResult.Success -> binding.resultText.text = result.data
                is ApiResult.Error -> {
                    binding.resultText.text = result.message
                    Toast.makeText(this@PnrStatusActivity, result.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
            binding.progressBar.isVisible = false
            binding.btnSubmit.isEnabled = true
        }
    }
}
