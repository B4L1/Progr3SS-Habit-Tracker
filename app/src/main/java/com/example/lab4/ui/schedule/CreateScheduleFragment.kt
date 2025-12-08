package com.example.lab4.ui.schedule

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController
import com.example.lab4.R
import com.example.lab4.data.model.*
import com.example.lab4.data.remote.HabitService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.databinding.FragmentCreateScheduleBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CreateScheduleFragment : Fragment() {
    private var _binding: FragmentCreateScheduleBinding? = null
    private val binding get() = _binding!!
    
    private val calendar = Calendar.getInstance()
    private var durationMinutes = 30
    private var habits: List<HabitResponseDto> = emptyList()
    private var selectedHabit: HabitResponseDto? = null
    private var selectedRepeatMode = "daily"
    private var customDays = mutableListOf<Int>() // 1=Monday...7=Sunday
    private var pendingHabitId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimePicker()
        setupRepeatButtons()
        fetchHabits()
        setupGoalInput()

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveSchedule()
        }
        
        binding.createNewHabitButton.setOnClickListener {
            findNavController().navigate(R.id.action_createScheduleFragment_to_createHabitFragment)
        }

        // Use setFragmentResultListener on parentFragmentManager (since we are siblings in nav)
        parentFragmentManager.setFragmentResultListener("requestKey_habitCreated", this) { _, bundle ->
            if (bundle.getBoolean("created")) {
                pendingHabitId = bundle.getInt("habitId")
                // If ID is found, fetchHabits will find and select it
                // If bundle doesn't have ID (old behavior), pendingHabitId is 0 or null
                if (pendingHabitId == 0) pendingHabitId = null 
                fetchHabits()
            }
        }
    }

    private fun setupTimePicker() {
        updateTimeText()
        binding.timeInputLayout.setEndIconOnClickListener {
            showTimePicker()
        }
        binding.startTimeEditText.setOnClickListener {
            showTimePicker()
        }
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateTimeText()
        }, hour, minute, false).show()
    }

    private fun updateTimeText() {
        val format = SimpleDateFormat("hh:mm a", Locale.getDefault())
        binding.startTimeEditText.setText(format.format(calendar.time))
    }

    private fun setupRepeatButtons() {
        val buttons = listOf(
            binding.btnEveryDay to "daily",
            binding.btnWeekdays to "weekdays",
            binding.btnWeekends to "weekends",
            binding.btnCustom to "custom"
        )
        
        buttons.forEach { (button, mode) ->
            button.setOnClickListener {
                updateRepeatSelection(button, buttons.map { it.first })
                selectedRepeatMode = mode
                
                if (mode == "custom") {
                    showCustomDayPicker()
                }
            }
        }
        
        // precise initial selection
        updateRepeatSelection(binding.btnEveryDay, buttons.map { it.first })
    }
    
    private fun showCustomDayPicker() {
        val dayNames = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val checkedItems = BooleanArray(7) { index -> customDays.contains(index + 1) }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Days")
            .setMultiChoiceItems(dayNames, checkedItems) { _, which, isChecked ->
                val dayValue = which + 1 // 1=Monday...7=Sunday
                if (isChecked) {
                    if (!customDays.contains(dayValue)) {
                        customDays.add(dayValue)
                    }
                } else {
                    customDays.remove(dayValue)
                }
            }
            .setPositiveButton("OK") { dialog, _ ->
                customDays.sort()
                if (customDays.isEmpty()) {
                    Toast.makeText(context, "No days selected, using daily", Toast.LENGTH_SHORT).show()
                    customDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateRepeatSelection(selected: Button, allButtons: List<Button>) {
        allButtons.forEach { btn ->
            if (btn == selected) {
                btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.purple_200)
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.input_field_bg)
            }
        }
    }

    private fun setupGoalInput() {
        val units = listOf("Times", "Minutes", "Hours", "Pages", "Steps")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        binding.unitSpinner.setAdapter(adapter)
        binding.unitSpinner.setText(units[0], false)
        binding.amountEditText.setText("30")
    }

    private fun fetchHabits() {
        val service = RetrofitClient.createService(HabitService::class.java)
        service.getHabits().enqueue(object : Callback<List<HabitResponseDto>> {
            override fun onResponse(
                call: Call<List<HabitResponseDto>>,
                response: Response<List<HabitResponseDto>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    habits = response.body()!!
                    setupHabitSelector()
                    
                    // Pre-select pending habit if exists
                    pendingHabitId?.let { id ->
                        val habitToSelect = habits.find { it.id == id }
                        habitToSelect?.let {
                            binding.habitAutoComplete.setText(it.name, false)
                            selectedHabit = it
                            applyHabitDefaults(it)
                        }
                        pendingHabitId = null
                    }
                }
            }

            override fun onFailure(call: Call<List<HabitResponseDto>>, t: Throwable) {
                Toast.makeText(context, "Failed to load habits", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupHabitSelector() {
        val habitNames = habits.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, habitNames)
        binding.habitAutoComplete.setAdapter(adapter)
        
        binding.habitAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedHabit = habits[position]
            applyHabitDefaults(selectedHabit!!)
        }
    }

    private fun applyHabitDefaults(habit: HabitResponseDto) {
        habit.goal?.let { goalStr ->
            // Expected format: "30 Minutes" or "2 Hours" or "10 Times"
            val parts = goalStr.split(" ")
            if (parts.size >= 2) {
                binding.amountEditText.setText(parts[0])
                // Join remaining parts and Title Case it for matching
                val unit = parts.drop(1).joinToString(" ").trim().lowercase().replaceFirstChar { it.uppercase() }
                
                // Set text directly first (so it shows up)
                binding.unitSpinner.setText(unit, false)
                
                // We don't need to strictly search the adapter because NoFilterAdapter 
                // will show all items regardless of what is set. 
                // But we should ensure the unit is valid or perhaps select the closest match if we wanted validation.
                // For now, setting it directly is fine as long as it matches one of our strings ("Hours" etc.)
            }
        }
    }
    
    // Custom Adapter to disable filtering
    /* 
    inner class NoFilterAdapter(context: android.content.Context, val items: List<String>) : 
        ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, items) {
        
        override fun getFilter(): android.widget.Filter {
            return object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?): android.widget.Filter.FilterResults {
                    return android.widget.Filter.FilterResults().apply {
                        values = items
                        count = items.size
                    }
                }
                override fun publishResults(constraint: CharSequence?, results: android.widget.Filter.FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
    }
    */


    private fun saveSchedule() {
        if (selectedHabit == null) {
            Toast.makeText(context, "Please select a habit", Toast.LENGTH_SHORT).show()
            return
        }

        val amountStr = binding.amountEditText.text.toString()
        val amount = amountStr.toIntOrNull() ?: 30
        val unit = binding.unitSpinner.text.toString()
        
        durationMinutes = if (unit == "Hours") amount * 60 else amount
        
        // Use ISO 8601 for start_time
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val startTimeIso = isoFormat.format(calendar.time)
        
        // Use YYYY-MM-DD for date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(calendar.time)
        
        val scheduleService = RetrofitClient.createService(ScheduleService::class.java)

        if (selectedRepeatMode == "custom" || selectedRepeatMode == "none") {
            val request = CreateCustomScheduleDto(
                habitId = selectedHabit!!.id,
                date = dateString,
                start_time = startTimeIso,
                duration_minutes = durationMinutes,
                notes = null
            )
            
            scheduleService.createCustomSchedule(request).enqueue(object : Callback<ScheduleResponseDto> {
                override fun onResponse(call: Call<ScheduleResponseDto>, response: Response<ScheduleResponseDto>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Schedule created", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("CreateSchedule", "Error: $errorBody")
                        Toast.makeText(context, "Failed: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onFailure(call: Call<ScheduleResponseDto>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // Map repeat mode to daysOfWeek
            // 1=Monday, 7=Sunday. (Java Calendar: 2=Monday, 1=Sunday)
            // Spec says 1=Monday...7=Sunday.
            val daysOfWeek = when (selectedRepeatMode) {
                "daily" -> listOf(1, 2, 3, 4, 5, 6, 7)
                "weekdays" -> listOf(1, 2, 3, 4, 5)
                "weekends" -> listOf(6, 7)
                "custom" -> if (customDays.isNotEmpty()) customDays.toList() else listOf(1, 2, 3, 4, 5, 6, 7)
                else -> listOf(1, 2, 3, 4, 5, 6, 7)
            }
            
            Log.d("CreateSchedule", "Creating recurring: daysOfWeek=$daysOfWeek, start_time=$startTimeIso, habitId=${selectedHabit!!.id}")

            val request = CreateRecurringScheduleDto(
                habitId = selectedHabit!!.id,
                start_time = startTimeIso,
                daysOfWeek = daysOfWeek,
                numberOfWeeks = 4,
                duration_minutes = durationMinutes,
                notes = null
            )

            scheduleService.createRecurringSchedule(request).enqueue(object : Callback<List<ScheduleResponseDto>> {
                override fun onResponse(call: Call<List<ScheduleResponseDto>>, response: Response<List<ScheduleResponseDto>>) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Recurring schedules created", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.e("CreateSchedule", "Error: $errorBody")
                        Toast.makeText(context, "Failed: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<List<ScheduleResponseDto>>, t: Throwable) {
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
