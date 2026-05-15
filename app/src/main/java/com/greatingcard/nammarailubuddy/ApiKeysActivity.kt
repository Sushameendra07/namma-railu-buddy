package com.greatingcard.nammarailubuddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.greatingcard.nammarailubuddy.adapters.ApiKeyAdapter
import com.greatingcard.nammarailubuddy.config.ApiKeysInfo
import com.greatingcard.nammarailubuddy.databinding.ActivityApiKeysBinding
import java.io.File

/**
 * Shows every API key, where it lives on disk, and whether it is configured.
 * Edit keys in: project_root/local.properties (then Rebuild).
 */
class ApiKeysActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiKeysBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiKeysBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.txtEditHint.text = getString(R.string.api_keys_edit_hint)

        val firebaseExists = resources.getIdentifier("google_app_id", "string", packageName) != 0
        val entries = ApiKeysInfo.allEntries(File("."), firebaseJsonExists = firebaseExists)

        binding.apiKeysRecycler.layoutManager = LinearLayoutManager(this)
        binding.apiKeysRecycler.adapter = ApiKeyAdapter(entries)
        binding.btnClose.setOnClickListener { finish() }
    }
}
