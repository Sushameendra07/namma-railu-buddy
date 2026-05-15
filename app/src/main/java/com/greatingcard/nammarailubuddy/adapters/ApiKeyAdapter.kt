package com.greatingcard.nammarailubuddy.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.greatingcard.nammarailubuddy.config.ApiKeysInfo
import com.greatingcard.nammarailubuddy.databinding.ItemApiKeyBinding

class ApiKeyAdapter(
    private val items: List<ApiKeysInfo.KeyEntry>
) : RecyclerView.Adapter<ApiKeyAdapter.Holder>() {

    class Holder(val binding: ItemApiKeyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemApiKeyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            keyName.text = item.name
            keyPurpose.text = item.purpose
            keyLocation.text = "📁 ${item.storageLocation}"
            keyMasked.text = "Value: ${item.maskedValue}"
            keyUsedBy.text = "Used by:\n• ${item.usedBy.joinToString("\n• ")}"
            if (item.isConfigured) {
                keyStatus.text = "OK"
                keyStatus.setBackgroundColor(Color.parseColor("#2E7D32"))
                keyStatus.setTextColor(Color.WHITE)
            } else {
                keyStatus.text = "MISSING"
                keyStatus.setBackgroundColor(Color.parseColor("#C62828"))
                keyStatus.setTextColor(Color.WHITE)
            }
        }
    }

    override fun getItemCount() = items.size
}
