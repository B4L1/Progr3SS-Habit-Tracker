package com.example.lab4.ui.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.data.model.ProgressResponseDto
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.model.UpdateScheduleDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.data.repository.ScheduleRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentEditScheduleBinding
import com.example.lab4.databinding.ItemProgressHistoryBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class EditScheduleFragment : Fragment() {
    private var _binding: FragmentEditScheduleBinding? = null
    private val binding get() = _binding!!
    private var scheduleId: Int = -1
    private val calendar = Calendar.getInstance()
    
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            ScheduleRepository(
                RetrofitClient.createService(ScheduleService::class.java)
            )
        )
    }
    
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
        observeViewModel()
        fetchScheduleDetails()

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveSchedule()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scheduleDetailState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        populateUI(state.data)
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateScheduleState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        binding.saveButton.isEnabled = false
                        binding.saveButton.text = "Saving..."
                    }
                    is UiState.Success -> {
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Save"
                        Toast.makeText(context, "Schedule updated", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateState()
                        findNavController().navigateUp()
                    }
                    is UiState.Error -> {
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Save"
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        binding.saveButton.isEnabled = true
                        binding.saveButton.text = "Save"
                    }
                }
            }
        }
    }

    private fun fetchScheduleDetails() {
        viewModel.fetchScheduleById(scheduleId)
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

        viewModel.updateSchedule(scheduleId, request)
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
            val isCompleted = item.is_completed ?: false
            val loggedTime = item.logged_time ?: 0
            
            val context = holder.itemView.context
            
            when {
                isCompleted -> {
                    holder.binding.statusIcon.setImageResource(com.example.lab4.R.drawable.ic_check)
                    holder.binding.statusIcon.background = context.getDrawable(com.example.lab4.R.drawable.bg_circle_green)
                    holder.binding.statusIcon.setColorFilter(context.getColor(com.example.lab4.R.color.white))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Completed"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(com.example.lab4.R.color.green_completed))
                }
                loggedTime > 0 -> {
                    holder.binding.statusIcon.setImageResource(com.example.lab4.R.drawable.ic_status_pending)
                    holder.binding.statusIcon.background = context.getDrawable(com.example.lab4.R.drawable.bg_icon_circle)
                    holder.binding.statusIcon.setColorFilter(context.getColor(com.example.lab4.R.color.purple_200))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Added progress"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(com.example.lab4.R.color.purple_200))
                }
                else -> {
                    holder.binding.statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    holder.binding.statusIcon.background = context.getDrawable(com.example.lab4.R.drawable.bg_circle_red)
                    holder.binding.statusIcon.setColorFilter(context.getColor(com.example.lab4.R.color.white))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Missed"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(android.R.color.holo_red_light))
                }
            }
            
            holder.binding.historyDateTextView.text = item.date
            holder.binding.historyTimeTextView.text = "${loggedTime}m"
        }

        override fun getItemCount() = history.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
