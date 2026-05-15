package com.greatingcard.nammarailubuddy.models

data class Train(
    val number: String = "",
    val name: String = "",
    val platform: String = "",
    val delay: String = "",
    val destination: String = "",
    val eta: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val source: String = "",
    val platformVotes: Int = 0,
    val coachPosition: String = "",
    val statusMessage: String = "",
    val lastVerified: String = "",
    val crowdDensity: String = "",
    val runStatus: TrainRunStatus = TrainRunStatus.UNKNOWN
)
