package com.example.lab4.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.R
import com.example.lab4.data.model.ScheduleResponseDto
import com.example.lab4.databinding.ItemHeaderBinding
import com.example.lab4.databinding.ItemScheduleBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ScheduleAdapter(
    private var schedules: List<ScheduleResponseDto>,
    private val onItemClick: (ScheduleResponseDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ListItem> = emptyList()

    init {
        processSchedules()
    }

    sealed class ListItem {
        data class Header(val title: String, val iconRes: Int) : ListItem()
        data class Item(val schedule: ScheduleResponseDto) : ListItem()
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
    }

    private fun processSchedules() {
        val sorted = schedules.sortedBy { it.start_time }
        val morning = mutableListOf<ScheduleResponseDto>()
        val afternoon = mutableListOf<ScheduleResponseDto>()
        val night = mutableListOf<ScheduleResponseDto>()

        for (s in sorted) {
            val hour = getHour(s.start_time)
            when (hour) {
                in 6..11 -> morning.add(s)
                in 12..17 -> afternoon.add(s)
                else -> night.add(s)
            }
        }

        val newItems = mutableListOf<ListItem>()
        if (morning.isNotEmpty()) {
            newItems.add(ListItem.Header("Morning", R.drawable.ic_status_pending)) // Need sun icon, using placeholder
            newItems.addAll(morning.map { ListItem.Item(it) })
        }
        if (afternoon.isNotEmpty()) {
            newItems.add(ListItem.Header("Afternoon", R.drawable.ic_status_pending)) // Need cloud/sun icon
            newItems.addAll(afternoon.map { ListItem.Item(it) })
        }
        if (night.isNotEmpty()) {
            newItems.add(ListItem.Header("Night", R.drawable.ic_status_pending)) // Need moon icon
            newItems.addAll(night.map { ListItem.Item(it) })
        }
        items = newItems
    }

    private fun getHour(isoTime: String): Int {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(isoTime)
            val cal = java.util.Calendar.getInstance()
            if (date != null) {
                cal.time = date
                // Convert to local time
                cal.timeZone = TimeZone.getDefault()
                cal.get(java.util.Calendar.HOUR_OF_DAY)
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    inner class HeaderViewHolder(val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ItemViewHolder(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val binding = ItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.Item -> TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> {
                val h = holder as HeaderViewHolder
                h.binding.headerTextView.text = item.title
                // Set icon color based on section? For now use white/yellow
                if (item.title == "Morning" || item.title == "Afternoon") {
                    h.binding.iconImageView.setColorFilter(h.itemView.context.getColor(R.color.yellow_sun))
                } else {
                    h.binding.iconImageView.setColorFilter(h.itemView.context.getColor(R.color.purple_moon))
                }
                // Ideally load different icons for sun/moon
            }
            is ListItem.Item -> {
                val h = holder as ItemViewHolder
                val schedule = item.schedule
                with(h.binding) {
                    habitNameTextView.text = schedule.habit?.name ?: "Custom Activity"
                    
                    // Format time
                     try {
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        val date = inputFormat.parse(schedule.start_time)
                        
                        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        outputFormat.timeZone = TimeZone.getDefault()
                        
                        timeTextView.text = if (date != null) outputFormat.format(date) else schedule.start_time
                    } catch (e: Exception) {
                        timeTextView.text = schedule.start_time
                    }

                    // Status Icon
                    if (schedule.status == "completed") {
                        statusImageView.setImageResource(R.drawable.ic_status_completed)
                    } else {
                        statusImageView.setImageResource(R.drawable.ic_status_pending)
                    }

                    root.setOnClickListener {
                        onItemClick(schedule)
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newSchedules: List<ScheduleResponseDto>) {
        schedules = newSchedules
        processSchedules()
        notifyDataSetChanged()
    }
}
