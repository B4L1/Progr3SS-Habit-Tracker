package com.example.lab4.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.lab4.data.model.ResetPasswordRequest
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentResetPasswordBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordFragment : Fragment() {
    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.resetButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                return@setOnClickListener
            }

            val request = ResetPasswordRequest(email)
            RetrofitClient.authService.resetPassword(request).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "New password sent to email", Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    } else {
                        // Show more detailed error message
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(context, "Failed: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    Toast.makeText(context, "Connection Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
