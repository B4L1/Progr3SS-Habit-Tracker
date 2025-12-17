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
import com.example.lab4.databinding.FragmentRegisterBinding
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private val TAG = "RegisterFragment"

    // ViewModel with Factory for dependency injection
    private val viewModel: RegisterViewModel by viewModels {
        RegisterViewModelFactory(
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
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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
    }

    private fun observeViewModel() {
        // Observe registration state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.registerState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        // Do nothing
                    }
                    
                    is UiState.Loading -> {
                        binding.registerButton.isEnabled = false
                        binding.registerButton.text = "Registering..."
                    }
                    
                    is UiState.Success -> {
                        binding.registerButton.isEnabled = true
                        binding.registerButton.text = "Register"
                        
                        Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
                        
                        viewModel.resetState()
                    }
                    
                    is UiState.Error -> {
                        binding.registerButton.isEnabled = true
                        binding.registerButton.text = "Register"
                        
                        // Clear previous errors
                        binding.nameInputLayout.error = null
                        binding.emailInputLayout.error = null
                        binding.passwordInputLayout.error = null
                        binding.confirmPasswordInputLayout.error = null
                        
                        // Show error
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Registration failed: ${state.message}")
                        
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()

            // Reset errors
            binding.nameInputLayout.error = null
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null
            binding.confirmPasswordInputLayout.error = null

            // Check password match locally (UI validation)
            if (password != confirmPassword) {
                binding.passwordInputLayout.error = "Passwords do not match"
                binding.confirmPasswordInputLayout.error = "Passwords do not match"
                return@setOnClickListener
            }

            // Trigger registration via ViewModel
            viewModel.register(email, password, name)
        }

        binding.googleSignInButton.setOnClickListener {
            Toast.makeText(context, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
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
