package com.example.lab4.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.example.lab4.R
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.AuthResponseDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentRegisterBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val TAG = "RegisterFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        tokenManager = TokenManager(requireContext())

        // Animation for input fields appearing from top to bottom
        val viewsToAnimate = listOf(
            binding.nameInputLayout,
            binding.emailInputLayout,
            binding.passwordInputLayout,
            binding.confirmPasswordInputLayout,
            binding.registerButton,
            binding.dividerText,
            binding.dividerLeft,
            binding.dividerRight,
            binding.googleSignInButton
        )

        viewsToAnimate.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = -50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(index * 50L)
                .start()
        }

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            // Reset errors
            binding.passwordInputLayout.error = null
            binding.confirmPasswordInputLayout.error = null
            binding.nameInputLayout.error = null
            binding.emailInputLayout.error = null

            var isValid = true

            if (name.isEmpty()) {
                binding.nameInputLayout.error = "Name is required"
                isValid = false
            }
            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                isValid = false
            }
            if (password.isEmpty()) {
                binding.passwordInputLayout.error = "Password is required"
                isValid = false
            }
            if (password != confirmPassword) {
                binding.passwordInputLayout.error = "Passwords do not match"
                binding.confirmPasswordInputLayout.error = "Passwords do not match"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())

            RetrofitClient.authService.register(nameBody, emailBody, passwordBody)
                .enqueue(object : Callback<AuthResponseDto> {
                    override fun onResponse(
                        call: Call<AuthResponseDto>,
                        response: Response<AuthResponseDto>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!
                            tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                            tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)
                            tokenManager.saveEmail(email) // Save email on successful registration
                            
                            Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                            
                            findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                        } else {
                            Log.e(TAG, "Registration failed: ${response.code()}")
                            Toast.makeText(context, "Registration failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponseDto>, t: Throwable) {
                        Log.e(TAG, "Registration error", t)
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        binding.loginTab.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.tabSelector to "tab_selector"
            )
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment, null, null, extras)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
