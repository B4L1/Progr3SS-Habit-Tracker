package com.example.lab4.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.request.CachePolicy
import com.example.lab4.R
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.remote.AuthService
import com.example.lab4.data.remote.HabitService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.data.repository.HabitRepository
import com.example.lab4.data.repository.ProfileRepository
import com.example.lab4.data.repository.common.UiState
import com.example.lab4.databinding.FragmentProfileBinding
import com.example.lab4.databinding.ItemHabitBinding
import com.example.lab4.ui.schedule.HabitViewModel
import com.example.lab4.ui.schedule.HabitViewModelFactory
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.data.repository.ScheduleRepository
import com.example.lab4.ui.schedule.ScheduleViewModel
import com.example.lab4.ui.schedule.ScheduleViewModelFactory

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private var userId: Int = -1
    private val TAG = "ProfileFragment"
    
    private val profileViewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(
            ProfileRepository(
                RetrofitClient.createService(AuthService::class.java)
            )
        )
    }
    
    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(
            HabitRepository(
                RetrofitClient.createService(HabitService::class.java)
            )
        )
    }

    private val scheduleViewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(
            ScheduleRepository(
                RetrofitClient.createService(com.example.lab4.data.remote.ScheduleService::class.java)
            )
        )
    }

    // Cache of today's schedules to update adapter
    private var todaySchedules: List<com.example.lab4.data.model.ScheduleResponseDto> = emptyList()
    private var currentHabits: List<HabitResponseDto> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())

        setupButtons()
        observeViewModels()
        fetchData()
        
        childFragmentManager.setFragmentResultListener("requestKey_habitCreated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("created")) {
                if (userId != -1) {
                    fetchHabits(userId)
                }
            }
        }
    }
    
    private fun fetchData() {
        profileViewModel.fetchProfile()
        
        // Fetch today's schedules for progress calculation
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayDate = dateFormat.format(java.util.Date())
        scheduleViewModel.fetchSchedules(todayDate)
    }

    override fun onResume() {
        super.onResume()
        fetchData()
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            profileViewModel.profileState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        updateProfileUI(state.data)
                    }
                    is UiState.Error -> {
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            habitViewModel.habitsState.collect { state ->
                when (state) {
                    is UiState.Success -> {
                        currentHabits = state.data
                        updateHabitsList()
                    }
                    else -> {}
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            scheduleViewModel.schedulesState.collect { state ->
                 when (state) {
                    is UiState.Success -> {
                        todaySchedules = state.data
                        updateHabitsList()
                    }
                    else -> {}
                 }
            }
        }
    }

    private fun updateHabitsList() {
        if (_binding == null) return
        if (currentHabits.isEmpty()) return 
        
        setupHabitsRecyclerView(currentHabits, todaySchedules)
    }

    // ... (rest of setupButtons, performLogout, etc - unchanged)
    private fun setupButtons() {
        binding.logoutButton.setOnClickListener {
            performLogout()
        }

        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.addHabitButton.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_createHabitFragment)
        }
    }

    private fun performLogout() {
        val token = tokenManager.getAccessToken()
        if (token != null) {
            profileViewModel.logout(token)
        }
        tokenManager.clearTokens()
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    private fun fetchProfile() {
        profileViewModel.fetchProfile()
    }
    
    private fun fetchHabits(userId: Int) {
        habitViewModel.fetchHabits()
    }

    private fun updateProfileUI(profile: ProfileResponseDto) {
        // Guard against view being destroyed
        if (_binding == null) return
        
        binding.userInfoTextView.text = getString(R.string.user_info_format, profile.username, profile.email, profile.description ?: "")
        userId = profile.id
        
        Log.d(TAG, "Profile URL from server: ${profile.profileImageUrl}")

        // Load profile image
        if (!profile.profileImageUrl.isNullOrEmpty()) {
            var imageUrl = profile.profileImageUrl

            // Fix for localhost URLs
            if (imageUrl.contains("localhost")) {
                imageUrl = imageUrl.replace("localhost", "10.52.64.147") 
            }

            val finalUrl = if (imageUrl.startsWith("http")) {
                imageUrl
            } else {
                val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                val relativeUrl = imageUrl.removePrefix("/")
                "$baseUrl/$relativeUrl"
            }
            
            val timestamp = System.currentTimeMillis()
            val separator = if (finalUrl.contains("?")) "&" else "?"
            
            binding.profileImageView.load("$finalUrl${separator}t=$timestamp") {
                placeholder(android.R.drawable.sym_def_app_icon)
                error(android.R.drawable.sym_def_app_icon)
                memoryCachePolicy(CachePolicy.DISABLED)
                diskCachePolicy(CachePolicy.DISABLED)
                tokenManager.getAccessToken()?.let { token ->
                    addHeader("Authorization", "Bearer $token")
                }
            }
        } else {
            binding.profileImageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        
        fetchHabits(profile.id)
    }

    private fun setupHabitsRecyclerView(habits: List<HabitResponseDto>, schedules: List<com.example.lab4.data.model.ScheduleResponseDto>) {
        if (_binding == null) return
        
        val adapter = HabitAdapter(habits, schedules)
        binding.habitsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.habitsRecyclerView.adapter = adapter
    }

    class HabitAdapter(
        private val habits: List<HabitResponseDto>, 
        private val schedules: List<com.example.lab4.data.model.ScheduleResponseDto>
    ) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {
        class HabitViewHolder(val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
            val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HabitViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
            val habit = habits[position]
            holder.binding.habitName.text = habit.name
            holder.binding.habitGoal.text = habit.goal
            
            // Calculate progress for today
            val habitSchedules = schedules.filter { it.habit?.id == habit.id }
            
            if (habitSchedules.isNotEmpty()) {
                val totalLogged = habitSchedules.sumOf { s -> s.progress?.sumOf { it.logged_time ?: 0 } ?: 0 }
                val totalGoal = habitSchedules.sumOf { it.duration_minutes ?: 0 }
                
                holder.binding.habitProgressBar.max = totalGoal.coerceAtLeast(1)
                holder.binding.habitProgressBar.progress = totalLogged

                // Color logic
                val context = holder.itemView.context
                val purple = context.getColor(R.color.purple_200)
                val green = context.getColor(R.color.green_completed)
                
                if (totalLogged >= totalGoal && totalGoal > 0) {
                     holder.binding.habitProgressBar.setIndicatorColor(green)
                } else {
                     holder.binding.habitProgressBar.setIndicatorColor(purple)
                }
                holder.binding.habitProgressBar.alpha = 1.0f
            } else {
                // Not used today
                holder.binding.habitProgressBar.progress = 0
                holder.binding.habitProgressBar.alpha = 0.3f // Dim it
                 holder.binding.habitProgressBar.setIndicatorColor(holder.itemView.context.getColor(android.R.color.darker_gray))
            }
        }

        override fun getItemCount() = habits.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
