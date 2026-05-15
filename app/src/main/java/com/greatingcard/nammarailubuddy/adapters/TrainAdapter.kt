package com.greatingcard.nammarailubuddy.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.greatingcard.nammarailubuddy.databinding.ItemTrainBinding
import com.greatingcard.nammarailubuddy.models.Train
import com.greatingcard.nammarailubuddy.util.StatusBadgeHelper

class TrainAdapter(private var trainList: List<Train>) : RecyclerView.Adapter<TrainAdapter.TrainViewHolder>() {

    class TrainViewHolder(val binding: ItemTrainBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainViewHolder {
        val binding = ItemTrainBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainViewHolder, position: Int) {
        val train = trainList[position]
        holder.binding.apply {
            val displayName = if (train.number.isNotBlank()) "${train.name} (${train.number})" else train.name
            trainName.text = displayName
            trainRoute.text = "${train.source} ➔ ${train.destination}"
            platformText.text = "PF ${train.platform}"
            etaText.text = "ETA: ${train.eta}"
            StatusBadgeHelper.apply(delayStatus, train.runStatus)
            if (train.statusMessage.isNotBlank()) {
                etaText.text = "${etaText.text} • ${train.statusMessage}"
            }
        }
    }

    override fun getItemCount() = trainList.size

    fun updateList(newList: List<Train>) {
        trainList = newList
        notifyDataSetChanged()
    }
}
