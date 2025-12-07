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
import com.example.lab4.data.model.SignInDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val TAG = "LoginFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
            binding.emailInputLayout,
            binding.passwordInputLayout,
            binding.forgotPasswordTextView,
            binding.loginButton,
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

        // Pre-fill email if available
        val savedEmail = tokenManager.getEmail()
        if (!savedEmail.isNullOrEmpty()) {
            binding.emailEditText.setText(savedEmail)
        }

        // DEBUG: Check for saved tokens
        val existingRefreshToken = tokenManager.getRefreshToken()
        if (!existingRefreshToken.isNullOrEmpty()) {
            Log.d(TAG, "Found existing Refresh Token: $existingRefreshToken")
            Toast.makeText(context, "Saved Session Found (Token exists)", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "No existing Refresh Token found.")
        }

        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.passwordInputLayout.error = "Password is required"
                return@setOnClickListener
            }

            val signInDto = SignInDto(email, password)

            RetrofitClient.authService.login(signInDto)
                .enqueue(object : Callback<AuthResponseDto> {
                    override fun onResponse(
                        call: Call<AuthResponseDto>,
                        response: Response<AuthResponseDto>
                    ) {
                        if (response.isSuccessful && response.body() != null) {
                            val authResponse = response.body()!!
                            tokenManager.saveAccessToken(authResponse.tokens.accessToken)
                            tokenManager.saveRefreshToken(authResponse.tokens.refreshToken)
                            tokenManager.saveEmail(email) // Save email on successful login

                            Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()

                            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        } else {
                            Log.e(TAG, "Login failed: ${response.code()}")
                            Toast.makeText(context, "Login failed: ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponseDto>, t: Throwable) {
                        Log.e(TAG, "Login error", t)
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        binding.googleSignInButton.setOnClickListener {
             Toast.makeText(context, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.registerTab.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.tabSelector to "tab_selector"
            )
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment, null, null, extras)
        }

        binding.forgotPasswordTextView.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_resetPasswordFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
