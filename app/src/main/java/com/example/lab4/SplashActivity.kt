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
              Log.d(TAG, "Found refresh token, validating with backend...")
              binding.loadingStatusTextView.text = "Validating session..."

              val request = com.example.lab4.data.model.RefreshTokenRequest(refreshToken)
              RetrofitClient.authService.refreshToken(request).enqueue(object : retrofit2.Callback<com.example.lab4.data.model.AuthResponseDto> {
                  override fun onResponse(
                      call: retrofit2.Call<com.example.lab4.data.model.AuthResponseDto>,
                      response: retrofit2.Response<com.example.lab4.data.model.AuthResponseDto>
                  ) {
                      if (response.isSuccessful && response.body() != null) {
                          Log.d(TAG, "Token refresh successful, navigating to Home")
                          val authResponse = response.body()!!
                          tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                          tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)
                          navigateToHome()
                      } else {
                          Log.w(TAG, "Token refresh failed (code ${response.code()}), navigating to Login")
                          tokenManager.clearTokens() // Clear invalid tokens
                          navigateToLogin()
                      }
                  }

                  override fun onFailure(call: retrofit2.Call<com.example.lab4.data.model.AuthResponseDto>, t: Throwable) {
                      Log.e(TAG, "Token refresh network error: ${t.message}", t)
                      // Decide: if network error, maybe go to login or show retry? 
                      // For now, safe default is Login so user isn't stuck.
                      navigateToLogin()
                  }
              })
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
    }
}