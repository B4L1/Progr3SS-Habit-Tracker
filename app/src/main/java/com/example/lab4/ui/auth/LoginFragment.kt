package com.example.lab4.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.example.lab4.R
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.repository.AuthRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val TAG = "LoginFragment"

    // ViewModel with Factory for dependency injection
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(
            AuthRepository(
                RetrofitClient.authService,
                TokenManager(requireContext())
            )
        )
    }

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

        tokenManager = TokenManager(requireContext())

        setupUI()
        observeViewModel()
        setupClickListeners()
    }

    private fun setupUI() {
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

        // Observe saved email from ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.savedEmail.collect { savedEmail ->
                if (!savedEmail.isNullOrEmpty()) {
                    binding.emailEditText.setText(savedEmail)
                } else {
                    // DEBUG: Pre-fill for testing
                    binding.emailEditText.setText("test@example.com")
                    binding.passwordEditText.setText("password123")
                }
            }
        }

        // DEBUG: Check for saved tokens
        if (viewModel.hasRefreshToken()) {
            Log.d(TAG, "Found existing Refresh Token")
            Toast.makeText(context, "Saved Session Found (Token exists)", Toast.LENGTH_SHORT).show()
        } else {
            Log.d(TAG, "No existing Refresh Token found.")
        }
    }

    private fun observeViewModel() {
        // Observe login state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        // Do nothing
                    }
                    
                    is UiState.Loading -> {
                        binding.loginButton.isEnabled = false
                        binding.loginButton.text = "Logging in..."
                    }
                    
                    is UiState.Success -> {
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = "Login"
                        
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                        
                        viewModel.resetState()
                    }
                    
                    is UiState.Error -> {
                        binding.loginButton.isEnabled = true
                        binding.loginButton.text = "Login"
                        
                        // Clear previous errors
                        binding.emailInputLayout.error = null
                        binding.passwordInputLayout.error = null
                        
                        // Show error
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Login failed: ${state.message}")
                        
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            // Clear previous errors
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            // Trigger login via ViewModel
            viewModel.login(email, password)
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
