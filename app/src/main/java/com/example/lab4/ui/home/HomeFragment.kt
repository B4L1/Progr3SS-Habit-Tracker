package com.example.lab4.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lab4.R
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.remote.ScheduleService
import com.example.lab4.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val TAG = "HomeFragment"
    private lateinit var iconManager: com.example.lab4.data.local.IconManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        iconManager = com.example.lab4.data.local.IconManager(requireContext())

        // Set Header Date
        val sdf = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date())

        setupRecyclerView()

        binding.addScheduleFab.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createScheduleFragment)
        }

        childFragmentManager.setFragmentResultListener("requestKey_progressUpdated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("updated")) {
                fetchSchedules()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        fetchSchedules()
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(emptyList(), iconManager) { schedule ->
            val bundle = bundleOf("schedule_id" to schedule.id)
            findNavController().navigate(R.id.action_homeFragment_to_scheduleDetailsFragment, bundle)
        }
        binding.scheduleRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.scheduleRecyclerView.adapter = scheduleAdapter
    }

    private fun fetchSchedules() {
        val service = RetrofitClient.createService(ScheduleService::class.java)
        service.getSchedules(null).enqueue(object : Callback<List<ScheduleResponseDto>> {
            override fun onResponse(
                call: Call<List<ScheduleResponseDto>>,
                response: Response<List<ScheduleResponseDto>>
            ) {
                if (response.isSuccessful && response.body() != null) {
                    val schedules = response.body()!!
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
                } else {
                    Log.e(TAG, "Failed to fetch schedules: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<ScheduleResponseDto>>, t: Throwable) {
                Log.e(TAG, "Error fetching schedules", t)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
