package com.example.lab4

import android.app.Application
import com.example.lab4.data.remote.RetrofitClient

class Lab4Application : Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
    }
}
