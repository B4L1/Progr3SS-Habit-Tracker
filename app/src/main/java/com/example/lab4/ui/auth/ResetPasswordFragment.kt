package com.example.lab4.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.repository.AuthRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentResetPasswordBinding
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {
    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ResetPasswordViewModel by viewModels {
        ResetPasswordViewModelFactory(
            AuthRepository(
                RetrofitClient.authService,
                TokenManager(requireContext())
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModel()

        binding.resetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                return@setOnClickListener
            }
            
            // Clear error
            binding.emailInputLayout.error = null

            viewModel.resetPassword(email)
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.resetState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.resetButton.isEnabled = false
                        binding.resetButton.text = "Sending..."
                    }
                    is UiState.Success -> {
                        binding.resetButton.isEnabled = true
                        binding.resetButton.text = "Send Reset Link"
                        Toast.makeText(context, "New password sent to email", Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                        findNavController().navigateUp()
                    }
                    is UiState.Error -> {
                        binding.resetButton.isEnabled = true
                        binding.resetButton.text = "Send Reset Link"
                        Toast.makeText(context, "Failed: ${state.message}", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        binding.resetButton.isEnabled = true
                        binding.resetButton.text = "Send Reset Link"
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
