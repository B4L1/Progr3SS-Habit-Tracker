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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.lab4.data.model.Content
import com.example.lab4.data.model.GeminiRequest
import com.example.lab4.data.model.Part
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                    if (categories.isNotEmpty()) {
                        setupCategorySelector()
                    } else {
                        Toast.makeText(context, "No categories available", Toast.LENGTH_SHORT).show()
                    }
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

        // Use lifecycleScope to call suspend functions
        lifecycleScope.launch {
            binding.saveButton.isEnabled = false
            Toast.makeText(context, "Analyzing habit...", Toast.LENGTH_SHORT).show()
            
            val iconName = getIconSuggestion(name, description)
            
            // Workaround: Stash icon in description since backend doesn't support 'icon' field
            val finalDescription = if (description.isEmpty()) {
                "|icon:$iconName|"
            } else {
                "$description |icon:$iconName|"
            }

            val request = CreateHabitDto(
                name = name,
                description = finalDescription,
                goal = goalString,
                categoryId = selectedCategoryId ?: categories.first().id
            )

            val service = RetrofitClient.createService(HabitService::class.java)
            service.createHabit(request).enqueue(object : Callback<HabitResponseDto> {
                override fun onResponse(
                    call: Call<HabitResponseDto>,
                    response: Response<HabitResponseDto>
                ) {
                    binding.saveButton.isEnabled = true
                    if (response.isSuccessful) {
                        val createdHabit = response.body()
                        if (createdHabit != null) {
                            val iconManager = com.example.lab4.data.local.IconManager(requireContext())
                            iconManager.saveIconForHabit(createdHabit.id, iconName)
                        }
                        
                        Toast.makeText(context, "Habit created with AI icon!", Toast.LENGTH_SHORT).show()
                        setFragmentResult("requestKey_habitCreated", bundleOf("created" to true))
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        android.util.Log.e("CreateHabit", "Error creating habit: $errorBody")
                        Toast.makeText(context, "Failed: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<HabitResponseDto>, t: Throwable) {
                    binding.saveButton.isEnabled = true
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private suspend fun getIconSuggestion(name: String, description: String): String {
        val geminiService = RetrofitClient.createGeminiService()
        val iconList = getAvailableIcons()
        
        val prompt = "Based on the activity '$name' ($description), pick the single best matching icon from this list: ${iconList.joinToString(", ")}. Return ONLY the icon name (e.g., ic_activity_run). If unsure, return ic_activity_generic."
        
        val request = GeminiRequest(listOf(Content(parts = listOf(Part(prompt)))))

        return try {
            val response = geminiService.getIconSuggestion(request)
            val candidateText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
            
            android.util.Log.d("CreateHabit", "Gemini Prompt: $prompt")
            android.util.Log.d("CreateHabit", "Gemini Response: '$candidateText'")

            // Basic validation to ensure it returns a valid icon from our list
            if (iconList.contains(candidateText)) {
                android.util.Log.d("CreateHabit", "Icon valid: $candidateText")
                withContext(Dispatchers.Main) { 
                    Toast.makeText(context, "AI chose: $candidateText", Toast.LENGTH_SHORT).show()
                }
                candidateText
            } else {
                android.util.Log.d("CreateHabit", "Icon invalid/not found ($candidateText), using generic.")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "AI chose invalid '$candidateText', using generic.", Toast.LENGTH_LONG).show()
                }
                "ic_activity_generic"
            }
        } catch (e: Exception) {
            android.util.Log.e("CreateHabit", "Gemini error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            // Fallback
            "ic_activity_generic"
        }
    }

    private fun getAvailableIcons(): List<String> {
        return listOf(
            "ic_activity_art", "ic_activity_baby", "ic_activity_bike", "ic_activity_book", 
            "ic_activity_chat", "ic_activity_clean", "ic_activity_coffee", "ic_activity_cooking", 
            "ic_activity_drink", "ic_activity_drive", "ic_activity_email", "ic_activity_fastfood", 
            "ic_activity_finance", "ic_activity_gaming", "ic_activity_garden", "ic_activity_gas", 
            "ic_activity_generic", "ic_activity_groceries", "ic_activity_gym", "ic_activity_hike", 
            "ic_activity_idea", "ic_activity_laptop", "ic_activity_laundry", "ic_activity_map", 
            "ic_activity_meds", "ic_activity_meeting", "ic_activity_movie", "ic_activity_music", 
            "ic_activity_pet", "ic_activity_photo", "ic_activity_pizza", "ic_activity_presentation", 
            "ic_activity_relax", "ic_activity_repair", "ic_activity_restaurant", "ic_activity_run", 
            "ic_activity_school", "ic_activity_shopping", "ic_activity_shower", "ic_activity_sleep", 
            "ic_activity_soccer", "ic_activity_swim", "ic_activity_transit", "ic_activity_travel", 
            "ic_activity_wakeup", "ic_activity_walk", "ic_activity_water", "ic_activity_work", 
            "ic_activity_write", "ic_activity_yoga"
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}