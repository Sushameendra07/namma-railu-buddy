package com.greatingcard.nammarailubuddy.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greatingcard.nammarailubuddy.data.IrctcRepository
import com.greatingcard.nammarailubuddy.models.LiveTrainDetails
import com.greatingcard.nammarailubuddy.util.ApiResult
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: IrctcRepository) : ViewModel() {

    private val _liveStatus = MutableLiveData<ApiResult<LiveTrainDetails>>()
    val liveStatus: LiveData<ApiResult<LiveTrainDetails>> = _liveStatus

    private var lastTrainNo: String = ""
    private var lastDate: String = ""
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun trackTrain(trainNo: String, date: String) {
        lastTrainNo = trainNo
        lastDate = date
        _liveStatus.value = ApiResult.Loading
        viewModelScope.launch {
            _liveStatus.value = repository.getLiveTrainStatus(trainNo, date, appContext)
        }
    }

    fun retry() {
        if (lastTrainNo.isNotBlank() && lastDate.isNotBlank()) {
            trackTrain(lastTrainNo, lastDate)
        }
    }
}
