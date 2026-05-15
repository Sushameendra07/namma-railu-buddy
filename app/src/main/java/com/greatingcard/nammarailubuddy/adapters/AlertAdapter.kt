package com.greatingcard.nammarailubuddy.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.greatingcard.nammarailubuddy.databinding.ItemAlertBinding
import com.greatingcard.nammarailubuddy.models.Train

class AlertAdapter(private var alertList: List<Train>) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(val binding: ItemAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val train = alertList[position]
        holder.binding.apply {
            alertTitle.text = "${train.name} • PF ${train.platform}"
            alertMessage.text = train.statusMessage.ifBlank { "Train is running ${train.delay}." }
            alertTime.text = train.lastVerified.ifBlank { "IRCTC Live" }
        }
    }

    override fun getItemCount() = alertList.size

    fun updateList(newList: List<Train>) {
        alertList = newList
        notifyDataSetChanged()
    }
}