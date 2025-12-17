package com.example.lab4.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab4.R
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.repository.ScheduleRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentDayScheduleBinding
import com.example.lab4.ui.schedule.ScheduleViewModel
import com.example.lab4.ui.schedule.ScheduleViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DayScheduleFragment : Fragment() {
    private var _binding: FragmentDayScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val TAG = "DayScheduleFragment"
    private lateinit var iconManager: com.example.lab4.data.local.IconManager
    private var dateOffset: Int = 0

    // ViewModel with Factory for dependency injection
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            ScheduleRepository(
                RetrofitClient.createService(com.example.lab4.data.remote.ScheduleService::class.java)
            )
        )
    }

    companion object {
        private const val ARG_DATE_OFFSET = "date_offset"
        
        fun newInstance(offset: Int): DayScheduleFragment {
            val fragment = DayScheduleFragment()
            val args = Bundle()
            args.putInt(ARG_DATE_OFFSET, offset)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dateOffset = arguments?.getInt(ARG_DATE_OFFSET) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconManager = com.example.lab4.data.local.IconManager(requireContext())

        setupUI()
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        fetchSchedules()
    }

    private fun getTargetDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, dateOffset)
        return calendar.time
    }

    private fun setupUI() {
        val targetDate = getTargetDate()
        // Set Header Date
        val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        binding.tvDate.text = sdf.format(targetDate)
        
        // Update title based on offset
        binding.tvTitle.text = when (dateOffset) {
            0 -> "Today's plan"
            1 -> "Tomorrow's plan"
            -1 -> "Yesterday's plan"
            else -> "Day plan"
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(emptyList(), iconManager) { schedule ->
            val bundle = bundleOf("schedule_id" to schedule.id)
            findNavController().navigate(R.id.action_homeFragment_to_scheduleDetailsFragment, bundle)
        }
        binding.scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.scheduleRecyclerView.adapter = scheduleAdapter
    }

    private fun observeViewModel() {
        // Observe schedules state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.schedulesState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        // Do nothing
                    }
                    
                    is UiState.Loading -> {
                        // Could show a loading indicator here
                        Log.d(TAG, "Loading schedules...")
                    }
                    
                    is UiState.Success -> {
                        val schedules = state.data
                        Log.d(TAG, "Fetched ${schedules.size} schedules")
                        scheduleAdapter.updateData(schedules)
                        
                        // Background check for missing icons
                        lifecycleScope.launch {
                            val habits = schedules.mapNotNull { it.habit }.distinctBy { it.id }
                            iconManager.checkAndFetchIcons(habits)
                            // Refresh to show any newly fetched icons
                            withContext(Dispatchers.Main) {
                                scheduleAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                    
                    is UiState.Error -> {
                        Log.e(TAG, "Failed to fetch schedules: ${state.message}")
                        // Only show toast for today to avoid spamming touches
                        if (dateOffset == 0) {
                             Toast.makeText(context, "Failed to load schedules", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.addScheduleFab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createScheduleFragment)
        }

        // Listen for updates from parent/other fragments if needed
        childFragmentManager.setFragmentResultListener("requestKey_progressUpdated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated")) {
                fetchSchedules()
            }
        }
    }

    private fun fetchSchedules() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDateString = dateFormat.format(getTargetDate())
        
        viewModel.fetchSchedules(targetDateString)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
