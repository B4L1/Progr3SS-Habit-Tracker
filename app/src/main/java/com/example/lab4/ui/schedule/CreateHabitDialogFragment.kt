package com.example.lab4.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.lab4.data.model.CreateHabitDto
import com.example.lab4.data.model.HabitCategoryResponseDto
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.remote.HabitService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.DialogCreateHabitBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateHabitDialogFragment : DialogFragment() {
    private var _binding: DialogCreateHabitBinding? = null
    private val binding get() = _binding!!
    
    private var categories: List<HabitCategoryResponseDto> = emptyList()
    private var selectedCategoryId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchCategories()

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            createHabit()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun fetchCategories() {
        val service = RetrofitClient.createService(HabitService::class.java)
        service.getHabitCategories().enqueue(object : Callback<List<HabitCategoryResponseDto>> {
            override fun onResponse(
                call: Call<List<HabitCategoryResponseDto>>,
                response: Response<List<HabitCategoryResponseDto>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    categories = response.body()!!
                    setupCategorySpinner()
                } else {
                    Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<HabitCategoryResponseDto>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun createHabit() {
        val name = binding.nameEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        val goal = binding.goalEditText.text.toString().trim()
        
        binding.nameInputLayout.error = null
        binding.goalInputLayout.error = null

        var isValid = true
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        }
        if (goal.isEmpty()) {
            binding.goalInputLayout.error = "Goal is required"
            isValid = false
        }
        
        if (categories.isEmpty()) {
            Toast.makeText(context, "No categories available", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (!isValid) return
        
        val selectedPosition = binding.categorySpinner.selectedItemPosition
        if (selectedPosition != -1) {
            selectedCategoryId = categories[selectedPosition].id
        }

        val request = CreateHabitDto(
            name = name,
            description = if (description.isEmpty()) null else description,
            goal = goal,
            categoryId = selectedCategoryId ?: categories.first().id
        )

        val service = RetrofitClient.createService(HabitService::class.java)
        service.createHabit(request).enqueue(object : Callback<HabitResponseDto> {
            override fun onResponse(
                call: Call<HabitResponseDto>,
                response: Response<HabitResponseDto>
            ) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Habit created!", Toast.LENGTH_SHORT).show()
                    setFragmentResult("requestKey_habitCreated", bundleOf("created" to true))
                    dismiss()
                } else {
                    Toast.makeText(context, "Failed to create habit: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<HabitResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}