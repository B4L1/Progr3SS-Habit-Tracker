package com.example.lab4

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.ActivitySplashBinding
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    private val TAG = "SplashActivity"
    private lateinit var binding: ActivitySplashBinding
    private val SPLASH_TIME_OUT: Long = 2000
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "onCreate: SplashActivity created.")

        // Initialize RetrofitClient
        RetrofitClient.init(applicationContext)

        // Set colored title "Progr3SS" with blue '3'
        val titleText = "Progr3SS"
        val spannableString = android.text.SpannableString(titleText)
        val colorBlue = android.graphics.Color.parseColor("#03a9fc")
        spannableString.setSpan(
             android.text.style.ForegroundColorSpan(colorBlue),
             5, 6,
             android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.titleTextView.text = spannableString

        // Simulate loading
        simulateLoading()
    }

    private fun simulateLoading() {
        val totalTime = 2000L
        val interval = 20L
        val steps = totalTime / interval
        var currentStep = 0
        
        val handler = Handler(Looper.getMainLooper())
        
        val runnable = object : Runnable {
            override fun run() {
                if (currentStep <= steps) {
                    val progress = (currentStep.toFloat() / steps * 100).toInt()
                    binding.progressBar.progress = progress
                    
                    if (progress < 30) {
                        binding.loadingStatusTextView.text = "Initializing..."
                    } else if (progress < 60) {
                        binding.loadingStatusTextView.text = "Loading resources..."
                    } else if (progress < 90) {
                        binding.loadingStatusTextView.text = "Checking session..."
                    } else {
                        binding.loadingStatusTextView.text = "Starting..."
                    }
                    
                    currentStep++
                    handler.postDelayed(this, interval)
                } else {
                    checkAuthAndNavigate()
                }
            }
        }
        handler.post(runnable)
    }

    private fun checkAuthAndNavigate() {
         tokenManager = TokenManager(this)
         val refreshToken = tokenManager.getRefreshToken()
         
         if (!refreshToken.isNullOrEmpty()) {
             Log.d(TAG, "Found refresh token, navigating to Home (Optimistic Auth)")
             navigateToHome()
         } else {
             Log.d(TAG, "No refresh token found, navigating to Login")
             navigateToLogin()
         }
    }

    private fun navigateToHome() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        intent.putExtra("DESTINATION", "HOME")
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: SplashActivity started.")
        checkIconsInBackground()
    }
    
    private fun checkIconsInBackground() {
        // user requested to load icons on splash screen for those that don't have them
        // We run this in a global scope or separate scope so it continues even if Splash finishes.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Check if we have token first
                val tm = com.example.lab4.data.local.TokenManager(applicationContext)
                if (tm.getAccessToken() != null) {
                    val habitService = com.example.lab4.data.remote.RetrofitClient.createService(com.example.lab4.data.remote.HabitService::class.java)
                    val habitRepository = com.example.lab4.data.repository.HabitRepository(habitService)
                    
                    val result = habitRepository.getHabits()
                    result.onSuccess { habits ->
                        val iconManager = com.example.lab4.data.local.IconManager(applicationContext)
                        iconManager.checkAndFetchIcons(habits)
                    }.onFailure {
                        Log.e(TAG, "Failed to fetch habits for icon check: ${it.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in background icon check", e)
            }
        }
    }
}