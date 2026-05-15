package com.greatingcard.nammarailubuddy

import android.app.Application
import com.greatingcard.nammarailubuddy.data.IrctcRepository

class NammaRailuApp : Application() {

    lateinit var irctcRepository: IrctcRepository
        private set

    override fun onCreate() {
        super.onCreate()
        irctcRepository = IrctcRepository(
            apiKey = BuildConfig.RAPIDAPI_KEY,
            apiHost = BuildConfig.RAPIDAPI_HOST
        )
    }

    companion object {
        fun repository(app: Application): IrctcRepository =
            (app as NammaRailuApp).irctcRepository
    }
}
