package com.example.lab4.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.lab4.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
    }

    private fun setupViewPager() {
        // Start from a large number reasonably in the middle to allow scrolling left/right
        // Let's say index 500 is "Today", so 0 is 500 days ago, etc.
        val initialPosition = Int.MAX_VALUE / 2
        
        val adapter = DayPagerAdapter(this)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(initialPosition, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class DayPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        // Virtually infinite scrolling
        override fun getItemCount(): Int = Int.MAX_VALUE

        override fun createFragment(position: Int): Fragment {
            // Calculate offset from the middle
            val offset = position - (Int.MAX_VALUE / 2)
            return DayScheduleFragment.newInstance(offset)
        }
    }
}
