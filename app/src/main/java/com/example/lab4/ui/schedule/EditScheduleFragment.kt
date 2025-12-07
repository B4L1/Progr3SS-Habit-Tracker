package com.example.lab4.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.data.model.ProgressResponseDto
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.model.UpdateScheduleDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.databinding.FragmentEditScheduleBinding
import com.example.lab4.databinding.ItemProgressHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditScheduleFragment : Fragment() {
    private var _binding: FragmentEditScheduleBinding? = null
    private val binding get() = _binding!!
    private var scheduleId: Int = -1
    private val calendar = Calendar.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getInt("schedule_id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupStartTimePicker()
        fetchScheduleDetails()

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveSchedule()
        }
    }

    private fun fetchScheduleDetails() {
        val service = RetrofitClient.createService(ScheduleService::class.java)
        service.getScheduleById(scheduleId).enqueue(object : Callback<ScheduleResponseDto> {
            override fun onResponse(
                call: Call<ScheduleResponseDto>,
                response: Response<ScheduleResponseDto>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val schedule = response.body()!!
                    populateUI(schedule)
                } else {
                    Toast.makeText(context, "Failed to load details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ScheduleResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun populateUI(schedule: ScheduleResponseDto) {
        binding.habitNameEditText.setText(schedule.habit?.name ?: "Custom Activity")
        binding.notesEditText.setText(schedule.notes ?: "")
        
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(schedule.start_time)
            if (date != null) {
                calendar.time = date
                updateStartTimeText()
            }
        } catch (e: Exception) {
            // Ignore
        }

        val totalLogged = schedule.progress?.sumOf { it.logged_time ?: 0 } ?: 0
        val goal = schedule.duration_minutes ?: 1
        val progress = (totalLogged.toFloat() / goal * 100).toInt().coerceIn(0, 100)
        binding.completionProgressBar.progress = progress
        binding.progressText.text = "$progress%"

        val historyAdapter = HistoryAdapter(schedule.progress ?: emptyList())
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = historyAdapter
    }

    private fun saveSchedule() {
        val notes = binding.notesEditText.text.toString()
        
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val startTimeIso = isoFormat.format(calendar.time)

        val request = UpdateScheduleDto(
            start_time = startTimeIso,
            notes = notes
        )

        val service = RetrofitClient.createService(ScheduleService::class.java)
        service.updateSchedule(scheduleId, request).enqueue(object : Callback<ScheduleResponseDto> {
            override fun onResponse(call: Call<ScheduleResponseDto>, response: Response<ScheduleResponseDto>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Schedule updated", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ScheduleResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupStartTimePicker() {
        updateStartTimeText()
        binding.startTimeEditText.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(Calendar.YEAR, selectedYear)
                calendar.set(Calendar.MONTH, selectedMonth)
                calendar.set(Calendar.DAY_OF_MONTH, selectedDay)
                showTimePicker()
            }, year, month, day).show()
        }
    }

    private fun showTimePicker() {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateStartTimeText()
        }, hour, minute, true).show()
    }

    private fun updateStartTimeText() {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        binding.startTimeEditText.setText(format.format(calendar.time))
    }

    inner class HistoryAdapter(private val history: List<ProgressResponseDto>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
        inner class HistoryViewHolder(val binding: ItemProgressHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = ItemProgressHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HistoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = history[position]
            holder.binding.historyDateTextView.text = item.date
            holder.binding.historyTimeTextView.text = "${item.logged_time ?: 0} mins"
        }

        override fun getItemCount() = history.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
