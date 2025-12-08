package com.example.lab4.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.R
import com.example.lab4.data.model.ProgressResponseDto
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.model.UpdateScheduleDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.databinding.FragmentScheduleDetailsBinding
import com.example.lab4.databinding.ItemProgressHistoryBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScheduleDetailsFragment : Fragment() {
    private var _binding: FragmentScheduleDetailsBinding? = null
    private val binding get() = _binding!!
    private var scheduleId: Int = -1
    private var currentSchedule: ScheduleResponseDto? = null

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
        _binding = FragmentScheduleDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupButtons()
        fetchScheduleDetails()

        childFragmentManager.setFragmentResultListener("requestKey_progressUpdated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated")) {
                fetchScheduleDetails()
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.optionsMenuButton.setOnClickListener { view ->
            showOptionsMenu(view)
        }

        binding.addProgressFab.setOnClickListener {
            currentSchedule?.let { schedule ->
                val totalLogged = schedule.progress?.sumOf { it.logged_time ?: 0 } ?: 0
                val goal = schedule.duration_minutes ?: 0
                
                ProgressDialogFragment.newInstance(
                    scheduleId = schedule.id, 
                    date = schedule.date, 
                    status = schedule.status,
                    goalDuration = goal,
                    currentProgress = totalLogged
                ).show(childFragmentManager, "ProgressDialog")
            }
        }

        binding.editNotesButton.setOnClickListener {
            val bundle = Bundle().apply { putInt("schedule_id", scheduleId) }
            findNavController().navigate(R.id.action_scheduleDetailsFragment_to_editScheduleFragment, bundle)
        }
    }

    private fun showOptionsMenu(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.schedule_options_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    val bundle = Bundle().apply { putInt("schedule_id", scheduleId) }
                    findNavController().navigate(R.id.action_scheduleDetailsFragment_to_editScheduleFragment, bundle)
                    true
                }
                R.id.action_skip -> {
                    skipSchedule()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(context)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule?")
            .setPositiveButton("Yes") { _, _ ->
                deleteSchedule()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSchedule() {
        val service = RetrofitClient.createService(ScheduleService::class.java)
        service.deleteSchedule(scheduleId).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Schedule deleted", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                } else {
                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
                    currentSchedule = schedule
                    updateUI(schedule)
                } else {
                    Toast.makeText(context, "Failed to load details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ScheduleResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI(schedule: ScheduleResponseDto) {
        binding.habitNameTextView.text = schedule.habit?.name ?: "Custom Activity"
        
        // Format Time Range
        val timeRange = formatTimeRange(schedule.start_time, schedule.duration_minutes ?: 30)
        binding.timeRangeTextView.text = timeRange

        // Partners (Placeholder for now, but hide if empty)
        // Since backend doesn't support partners yet, we will just hide it as requested
        binding.lblPartner.visibility = View.GONE
        binding.partnerContainer.visibility = View.GONE

        // Calculate progress
        val totalLogged = schedule.progress?.sumOf { it.logged_time ?: 0 } ?: 0
        val goal = schedule.duration_minutes ?: 1
        val progressPercent = (totalLogged.toFloat() / goal * 100).toInt().coerceIn(0, 100)

        binding.statusProgressIndicator.progress = progressPercent
        
        val green = requireContext().getColor(R.color.green_completed)
        val red = requireContext().getColor(android.R.color.holo_red_light)
        val purple = requireContext().getColor(R.color.purple_200)
        val white = requireContext().getColor(R.color.white)

        if (schedule.status == "Completed") {
            binding.statusCheckmark.visibility = View.VISIBLE
            binding.statusCheckmark.setImageResource(R.drawable.ic_check)
            binding.statusCheckmark.background = requireContext().getDrawable(R.drawable.bg_circle_green)
            binding.statusCheckmark.imageTintList = android.content.res.ColorStateList.valueOf(white)
            binding.statusCheckmark.setPadding(4,4,4,4) // Ensure checkmark is smaller than circle

            binding.statusProgressIndicator.setIndicatorColor(green)
            binding.statusLabelSmall.text = "Completed"
            binding.statusLabelSmall.setTextColor(green)
        } else if (schedule.status == "Skipped") {
            binding.statusCheckmark.visibility = View.VISIBLE
            binding.statusCheckmark.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.statusCheckmark.background = requireContext().getDrawable(R.drawable.bg_circle_red)
            binding.statusCheckmark.imageTintList = android.content.res.ColorStateList.valueOf(white)
            binding.statusCheckmark.setPadding(4,4,4,4)

            binding.statusProgressIndicator.setIndicatorColor(red)
            binding.statusLabelSmall.text = "Skipped"
            binding.statusLabelSmall.setTextColor(red)
        } else {
            binding.statusCheckmark.visibility = View.INVISIBLE
            binding.statusProgressIndicator.setIndicatorColor(purple)
            
            binding.statusLabelSmall.text = schedule.status
            binding.statusLabelSmall.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }

        // Goal parsing - duration_minutes is stored in minutes, 
        // but for display we want to respect the habit's original unit/goal if possible.
        // The backend stores 'goal' string e.g. "2 Times" or "30 Minutes"
        // And 'duration_minutes' as the calculated time.
        
        val habitGoal = schedule.habit?.goal ?: "${schedule.duration_minutes} Minutes"
        val parts = habitGoal.split(" ")
        val amount = parts.getOrNull(0)?.toIntOrNull() ?: schedule.duration_minutes ?: 0
        val unit = parts.getOrNull(1) ?: "Minutes"
        
        // Compact unit for progress text e.g. "2m" or "2x"
        val shortUnit = when(unit.lowercase()) {
             "minutes", "minute" -> "m"
             "hours", "hour" -> "h"
             "times", "time" -> "x"
             "pages", "page" -> "p"
             "steps", "step" -> "st"
             else -> unit.take(1)
         }
        
        // Update labels
        binding.valDuration.text = "$amount$shortUnit" // was Duration, now Goal
        binding.progressText.text = "$totalLogged / $amount$shortUnit"
        
        // Repeat
        binding.valRepeat.text = if (schedule.is_custom) "Once" else "Recurring"

        binding.notesContent.text = if (schedule.notes.isNullOrEmpty()) "No notes added." else schedule.notes

        // Setup History
        val historyAdapter = HistoryAdapter(schedule.progress ?: emptyList(), schedule.habit?.name ?: "Habit")
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = historyAdapter
    }

    private fun skipSchedule() {
        val service = RetrofitClient.createService(ScheduleService::class.java)
        val updateDto = UpdateScheduleDto(status = "Skipped", notes = currentSchedule?.notes)
        
        service.updateSchedule(scheduleId, updateDto).enqueue(object : Callback<ScheduleResponseDto> {
            override fun onResponse(call: Call<ScheduleResponseDto>, response: Response<ScheduleResponseDto>) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "Schedule Skipped", Toast.LENGTH_SHORT).show()
                    fetchScheduleDetails()
                } else {
                    Toast.makeText(context, "Failed to skip", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ScheduleResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun formatTimeRange(startTimeIso: String, durationMinutes: Int): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val startDate = inputFormat.parse(startTimeIso) ?: return ""
            
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            val startStr = outputFormat.format(startDate)
            
            val calendar = java.util.Calendar.getInstance()
            calendar.time = startDate
            calendar.add(java.util.Calendar.MINUTE, durationMinutes)
            val endStr = outputFormat.format(calendar.time)
            
            return "$startStr - $endStr"
        } catch (e: Exception) {
            return ""
        }
    }

    inner class HistoryAdapter(
        private val history: List<ProgressResponseDto>,
        private val habitName: String
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
        
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
                    // Completed
                    holder.binding.statusIcon.setImageResource(R.drawable.ic_check)
                    holder.binding.statusIcon.background = context.getDrawable(R.drawable.bg_circle_green)
                    holder.binding.statusIcon.setColorFilter(context.getColor(R.color.white))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Completed $habitName"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(R.color.green_completed))
                }
                loggedTime > 0 -> {
                    // In Progress (has logged time but not marked completed)
                    holder.binding.statusIcon.setImageResource(R.drawable.ic_status_pending) // Or a "partial" icon
                    holder.binding.statusIcon.background = context.getDrawable(R.drawable.bg_icon_circle)
                    holder.binding.statusIcon.setColorFilter(context.getColor(R.color.purple_200))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Added progress to $habitName"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(R.color.purple_200))
                }
                else -> {
                    // Missed (no logged time and not completed)
                    holder.binding.statusIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    holder.binding.statusIcon.background = context.getDrawable(R.drawable.bg_circle_red)
                    holder.binding.statusIcon.setColorFilter(context.getColor(R.color.white))
                    holder.binding.statusIcon.setPadding(4,4,4,4)
                    
                    holder.binding.historyTitleTextView.text = "Missed $habitName"
                    holder.binding.historyTimeTextView.setTextColor(context.getColor(android.R.color.holo_red_light))
                }
            }

             try {
                 holder.binding.historyDateTextView.text = item.date 
             } catch (e: Exception) {
                 holder.binding.historyDateTextView.text = item.date
             }

            holder.binding.historyTimeTextView.text = "${loggedTime}m"
        }

        override fun getItemCount() = history.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
