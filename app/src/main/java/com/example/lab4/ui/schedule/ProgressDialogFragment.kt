package com.example.lab4.ui.schedule

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.lab4.data.model.CreateProgressDto
import com.example.lab4.data.model.UpdateScheduleDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.data.repository.ScheduleRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.DialogProgressBinding
import kotlinx.coroutines.launch

class ProgressDialogFragment : DialogFragment() {
    private var _binding: DialogProgressBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            ScheduleRepository(
                RetrofitClient.createService(ScheduleService::class.java)
            )
        )
    }

    private var scheduleId: Int = -1
    private var date: String = ""
    private var status: String = "Planned"
    private var existingNotes: String? = null
    private var existingLoggedTime: Int? = null
    private var goalDuration: Int = 0
    private var currentProgress: Int = 0

    companion object {
        const val ARG_SCHEDULE_ID = "schedule_id"
        const val ARG_DATE = "date"
        const val ARG_STATUS = "status"
        const val ARG_NOTES = "notes"
        const val ARG_LOGGED_TIME = "logged_time"
        const val ARG_GOAL_DURATION = "goal_duration"
        const val ARG_CURRENT_PROGRESS = "current_progress"

        fun newInstance(
            scheduleId: Int, 
            date: String, 
            status: String,
            notes: String? = null,
            loggedTime: Int? = null,
            goalDuration: Int = 0,
            currentProgress: Int = 0
        ): ProgressDialogFragment {
            val fragment = ProgressDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SCHEDULE_ID, scheduleId)
            args.putString(ARG_DATE, date)
            args.putString(ARG_STATUS, status)
            args.putString(ARG_NOTES, notes)
            if (loggedTime != null) {
                args.putInt(ARG_LOGGED_TIME, loggedTime)
            }
            args.putInt(ARG_GOAL_DURATION, goalDuration)
            args.putInt(ARG_CURRENT_PROGRESS, currentProgress)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            scheduleId = it.getInt(ARG_SCHEDULE_ID)
            date = it.getString(ARG_DATE) ?: ""
            status = it.getString(ARG_STATUS) ?: "Planned"
            existingNotes = it.getString(ARG_NOTES)
            if (it.containsKey(ARG_LOGGED_TIME)) {
                existingLoggedTime = it.getInt(ARG_LOGGED_TIME)
            }
            goalDuration = it.getInt(ARG_GOAL_DURATION)
            currentProgress = it.getInt(ARG_CURRENT_PROGRESS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        observeViewModel()

        binding.completedCheckBox.isChecked = (status == "Completed")
        
        if (!existingNotes.isNullOrEmpty()) {
            binding.notesEditText.setText(existingNotes)
        }
        if (existingLoggedTime != null) {
            binding.loggedTimeEditText.setText(existingLoggedTime.toString())
        }

        binding.loggedTimeEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val logged = s.toString().toIntOrNull() ?: 0
                val effectiveCurrent = currentProgress - (existingLoggedTime ?: 0)
                if (goalDuration > 0 && (effectiveCurrent + logged) >= goalDuration) {
                    binding.completedCheckBox.isChecked = true
                }
            }
        })

        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            saveProgress()
        }

        binding.deleteButton.setOnClickListener {
            confirmDelete()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteScheduleState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        Toast.makeText(context, "Schedule deleted", Toast.LENGTH_SHORT).show()
                        setFragmentResult("requestKey_progressUpdated", bundleOf("updated" to true))
                        dismiss()
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.progressState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        Toast.makeText(context, "Progress updated!", Toast.LENGTH_SHORT).show()
                        setFragmentResult("requestKey_progressUpdated", bundleOf("updated" to true))
                        dismiss()
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        // Also listen for update errors if updateAndCreateProgress fails at update stage
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateScheduleState.collect { state ->
                if (state is UiState.Error) {
                    Toast.makeText(context, "Update Failed: ${state.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteSchedule() {
        viewModel.deleteSchedule(scheduleId)
    }

    private fun saveProgress() {
        val notes = binding.notesEditText.text.toString()
        val loggedTimeStr = binding.loggedTimeEditText.text.toString()
        val loggedTime = if (loggedTimeStr.isNotEmpty()) loggedTimeStr.toInt() else 0
        val isCompleted = binding.completedCheckBox.isChecked

        val newStatus = if (isCompleted) "Completed" else "Planned"
        
        val updateRequest = UpdateScheduleDto(
            status = newStatus,
            notes = if (notes.isNotEmpty()) notes else null
        )
        
        val progressRequest = CreateProgressDto(
            scheduleId = scheduleId,
            date = date,
            logged_time = if (loggedTime > 0) loggedTime else null,
            notes = if (notes.isNotEmpty()) notes else null,
            is_completed = isCompleted
        )

        viewModel.updateAndCreateProgress(scheduleId, updateRequest, progressRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
