package com.example.lab4.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.lab4.data.model.CreateHabitDto
import com.example.lab4.data.model.HabitCategoryResponseDto
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.remote.HabitService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentCreateHabitBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CreateHabitFragment : Fragment() {
    private var _binding: FragmentCreateHabitBinding? = null
    private val binding get() = _binding!!
    
    private var categories: List<HabitCategoryResponseDto> = emptyList()
    private var selectedCategoryId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateHabitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchCategories()
        setupGoalInputs()

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            createHabit()
        }
    }

    private fun setupGoalInputs() {
        val units = listOf("Times", "Minutes", "Hours", "Pages", "Steps")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.goalUnitSpinner.setAdapter(adapter)
        binding.goalUnitSpinner.setText(units[0], false) // Default to Times
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
                    setupCategorySelector()
                } else {
                    Toast.makeText(context, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<List<HabitCategoryResponseDto>>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupCategorySelector() {
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.categoryAutoComplete.setAdapter(adapter)
        
        binding.categoryAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = categories[position].id
        }
    }

    private fun createHabit() {
        val name = binding.nameEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        
        // Combine Goal Amount and Unit
        val amount = binding.goalAmountEditText.text.toString().trim()
        val unit = binding.goalUnitSpinner.text.toString().trim()
        
        binding.nameInputLayout.error = null
        binding.goalAmountInputLayout.error = null

        var isValid = true
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            isValid = false
        }
        if (amount.isEmpty()) {
            binding.goalAmountInputLayout.error = "Required"
            isValid = false
        }
        
        if (categories.isEmpty()) {
            Toast.makeText(context, "No categories available", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        if (!isValid) return
        
        val goalString = "$amount $unit"

        val request = CreateHabitDto(
            name = name,
            description = if (description.isEmpty()) null else description,
            goal = goalString,
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
                    findNavController().navigateUp()
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