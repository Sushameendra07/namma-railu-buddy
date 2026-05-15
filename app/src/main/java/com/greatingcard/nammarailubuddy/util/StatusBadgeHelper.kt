package com.greatingcard.nammarailubuddy.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.TextView
import com.greatingcard.nammarailubuddy.R
import com.greatingcard.nammarailubuddy.models.TrainRunStatus

object StatusBadgeHelper {

    fun apply(badge: TextView, status: TrainRunStatus) {
        val (label, bg, fg) = when (status) {
            TrainRunStatus.ON_TIME -> Triple(R.string.status_on_time_badge, "#FFD600", "#000000")
            TrainRunStatus.DELAYED -> Triple(R.string.status_delayed_badge, "#C62828", "#FFFFFF")
            TrainRunStatus.CANCELLED -> Triple(R.string.status_cancelled_badge, "#424242", "#FFFFFF")
            TrainRunStatus.ARRIVED -> Triple(R.string.status_arrived_badge, "#1565C0", "#FFFFFF")
            TrainRunStatus.DIVERTED -> Triple(R.string.status_diverted_badge, "#6A1B9A", "#FFFFFF")
            TrainRunStatus.RUNNING -> Triple(R.string.status_running_badge, "#2E7D32", "#FFFFFF")
            TrainRunStatus.UNKNOWN -> Triple(R.string.status_running_badge, "#757575", "#FFFFFF")
        }
        badge.setText(label)
        badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bg))
        badge.setTextColor(Color.parseColor(fg))
    }
}
