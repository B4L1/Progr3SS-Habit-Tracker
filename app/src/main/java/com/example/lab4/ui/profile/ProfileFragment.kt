package com.example.lab4.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.request.CachePolicy
import com.example.lab4.R
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.model.ProfileResponseDto
import com.example.lab4.data.remote.HabitService
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentProfileBinding
import com.example.lab4.databinding.ItemHabitBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.recyclerview.widget.RecyclerView

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var tokenManager: TokenManager
    private var userId: Int = -1
    private val TAG = "ProfileFragment"

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
        fetchProfile()

        childFragmentManager.setFragmentResultListener("requestKey_habitCreated", viewLifecycleOwner) { _, bundle ->
            if (bundle.getBoolean("created")) {
                if (userId != -1) {
                    fetchHabits(userId)
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh profile to show updated image/username
        fetchProfile()
    }

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
            RetrofitClient.authService.logout("Bearer $token").enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }

        tokenManager.clearTokens()
        com.example.lab4.data.local.IconManager(requireContext()).clear()
        findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
    }

    private fun fetchProfile() {
        RetrofitClient.authService.getProfile().enqueue(object : Callback<ProfileResponseDto> {
            override fun onResponse(
                call: Call<ProfileResponseDto>,
                response: Response<ProfileResponseDto>
            ) {
                // Guard against view being destroyed
                if (_binding == null) return
                
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    binding.userInfoTextView.text = getString(R.string.user_info_format, profile.username, profile.email, profile.description ?: "")
                    userId = profile.id
                    
                    Log.d(TAG, "Profile URL from server: ${profile.profileImageUrl}")

                    // Load profile image
                    if (!profile.profileImageUrl.isNullOrEmpty()) {
                        var imageUrl = profile.profileImageUrl

                        // Fix for localhost URLs if running on Android Emulator/Device
                        if (imageUrl.contains("localhost")) {
                            val currentBaseUrl = RetrofitClient.BASE_URL
                            // Extract just the host (IP and port)
                            val host = currentBaseUrl.removePrefix("http://").removePrefix("https://").substringBefore("/")
                            imageUrl = imageUrl.replace("localhost:3000", host).replace("localhost", host.substringBefore(":")) 
                        }

                        // Handle URL construction
                        val finalUrl = if (imageUrl.startsWith("http")) {
                            imageUrl
                        } else {
                            val baseUrl = RetrofitClient.BASE_URL.removeSuffix("/")
                            val relativeUrl = imageUrl.removePrefix("/")
                            "$baseUrl/$relativeUrl"
                        }
                        
                        Log.d(TAG, "Loading profile image from: $finalUrl")

                        // Use current time to bust cache
                        val timestamp = System.currentTimeMillis()
                        val separator = if (finalUrl.contains("?")) "&" else "?"
                        
                        binding.profileImageView.load("$finalUrl${separator}t=$timestamp") {
                            placeholder(android.R.drawable.sym_def_app_icon)
                            error(android.R.drawable.sym_def_app_icon)
                            memoryCachePolicy(CachePolicy.DISABLED)
                            diskCachePolicy(CachePolicy.DISABLED)
                            
                            // Add Auth header in case the image is protected
                            tokenManager.getAccessToken()?.let { token ->
                                addHeader("Authorization", "Bearer $token")
                            }
                            
                            listener(
                                onError = { _, result ->
                                    Log.e(TAG, "Coil Error: ${result.throwable.message}")
                                    context?.let {
                                        Toast.makeText(it, "Image Load Error: ${result.throwable.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                onSuccess = { _, _ ->
                                    Log.d(TAG, "Image loaded successfully")
                                }
                            )
                        }
                    } else {
                        Log.d(TAG, "Profile image URL is null or empty")
                        // Toast.makeText(context, "No profile image URL found", Toast.LENGTH_SHORT).show()
                        // Reset to default if no image
                        binding.profileImageView.setImageResource(android.R.drawable.sym_def_app_icon)
                    }
                    
                    fetchHabits(profile.id)
                } else {
                    context?.let {
                        Toast.makeText(it, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ProfileResponseDto>, t: Throwable) {
                context?.let {
                    Toast.makeText(it, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun fetchHabits(userId: Int) {
        val service = RetrofitClient.createService(HabitService::class.java)
        service.getHabitsByUser(userId).enqueue(object : Callback<List<HabitResponseDto>> {
            override fun onResponse(
                call: Call<List<HabitResponseDto>>,
                response: Response<List<HabitResponseDto>>
            ) {
                // Guard against view being destroyed
                if (_binding == null) return
                
                if (response.isSuccessful && response.body() != null) {
                    setupHabitsRecyclerView(response.body()!!)
                }
            }

            override fun onFailure(call: Call<List<HabitResponseDto>>, t: Throwable) {
                // Silent fail or log
            }
        })
    }

    private fun setupHabitsRecyclerView(habits: List<HabitResponseDto>) {
        // Guard against view being destroyed  
        if (_binding == null) return
        
        val adapter = HabitAdapter(habits)
        binding.habitsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.habitsRecyclerView.adapter = adapter
    }

    class HabitAdapter(private val habits: List<HabitResponseDto>) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {
        class HabitViewHolder(val binding: ItemHabitBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
            val binding = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return HabitViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
            val habit = habits[position]
            holder.binding.habitName.text = habit.name
            holder.binding.habitGoal.text = habit.goal
        }

        override fun getItemCount() = habits.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
