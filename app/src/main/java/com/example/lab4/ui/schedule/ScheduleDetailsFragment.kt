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

        // Calculate progress
        val totalLogged = schedule.progress?.sumOf { it.logged_time ?: 0 } ?: 0
        val goal = schedule.duration_minutes ?: 1
        val progressPercent = (totalLogged.toFloat() / goal * 100).toInt().coerceIn(0, 100)

        binding.statusProgressIndicator.progress = progressPercent
        
        val green = requireContext().getColor(R.color.green_completed)
        val purple = requireContext().getColor(R.color.purple_200)
        val white = requireContext().getColor(R.color.white)

        if (schedule.status == "Completed") {
            binding.statusCheckmark.visibility = View.VISIBLE
            binding.statusCheckmark.setImageResource(R.drawable.ic_check)
            binding.statusCheckmark.setColorFilter(white)
            binding.statusProgressIndicator.setIndicatorColor(green)
            
            binding.statusLabelSmall.text = "Completed"
            binding.statusLabelSmall.setTextColor(green)
        } else {
            binding.statusCheckmark.visibility = View.INVISIBLE
            binding.statusProgressIndicator.setIndicatorColor(purple)
            
            binding.statusLabelSmall.text = schedule.status
            binding.statusLabelSmall.setTextColor(requireContext().getColor(android.R.color.darker_gray))
        }

        binding.valDuration.text = "${schedule.duration_minutes ?: 30}m"
        
        // Repeat
        binding.valRepeat.text = if (schedule.is_custom) "Once" else "Recurring"

        binding.notesContent.text = if (schedule.notes.isNullOrEmpty()) "No notes added." else schedule.notes

        // Setup History
        val historyAdapter = HistoryAdapter(schedule.progress ?: emptyList(), schedule.habit?.name ?: "Habit")
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = historyAdapter
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
            val isCompleted = item.is_completed ?: true
            
            if (isCompleted) {
                holder.binding.statusIcon.setImageResource(android.R.drawable.checkbox_on_background)
                holder.binding.statusIcon.setColorFilter(holder.itemView.context.getColor(R.color.green_completed))
                holder.binding.historyTitleTextView.text = "Completed $habitName"
                holder.binding.historyTimeTextView.setTextColor(holder.itemView.context.getColor(R.color.green_completed))
            } else {
                holder.binding.statusIcon.setImageResource(android.R.drawable.ic_delete)
                holder.binding.statusIcon.setColorFilter(holder.itemView.context.getColor(android.R.color.holo_red_light))
                holder.binding.historyTitleTextView.text = "Missed $habitName"
                holder.binding.historyTimeTextView.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_light))
            }

             try {
                 holder.binding.historyDateTextView.text = item.date 
             } catch (e: Exception) {
                 holder.binding.historyDateTextView.text = item.date
             }

            holder.binding.historyTimeTextView.text = "${item.logged_time ?: 0}m"
        }

        override fun getItemCount() = history.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
