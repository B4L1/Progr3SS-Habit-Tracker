package com.example.lab4.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.lab4.data.model.HabitResponseDto
import com.example.lab4.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.lab4.data.model.GeminiRequest
import com.example.lab4.data.model.Content
import com.example.lab4.data.model.Part

class IconManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("habit_icons", Context.MODE_PRIVATE)
    
    init {
        android.util.Log.e("IconManager", "Initialized IconManager")
    }

    fun getIconForHabit(habitId: Int): String? {
        val icon = prefs.getString("icon_$habitId", null)
        android.util.Log.e("IconManager", "getIconForHabit($habitId) returning: $icon")
        return icon
    }

    fun saveIconForHabit(habitId: Int, iconName: String) {
        val success = prefs.edit().putString("icon_$habitId", iconName).commit()
        android.util.Log.e("IconManager", "saveIconForHabit($habitId, $iconName) success: $success")
    }

    fun clear() {
        prefs.edit().clear().commit()
        android.util.Log.e("IconManager", "Cleared all icons")
    }

    suspend fun checkAndFetchIcons(habits: List<HabitResponseDto>) {
        withContext(Dispatchers.IO) {
            android.util.Log.d("IconManager", "Checking icons for ${habits.size} habits")
            val habitsWithoutIcons = habits.filter { habit ->
                if (getIconForHabit(habit.id) != null) return@filter false
                val description = habit.description ?: ""
                val hasStashed = description.contains("|icon:")
                if (hasStashed) android.util.Log.d("IconManager", "Habit ${habit.id} has stashed icon in description")
                !hasStashed
            }
            android.util.Log.d("IconManager", "Found ${habitsWithoutIcons.size} habits needing icons")


            if (habitsWithoutIcons.isEmpty()) return@withContext

            val geminiService = RetrofitClient.createGeminiService()
            val availableIcons = getAvailableIcons().joinToString(", ")

            for (habit in habitsWithoutIcons) {
                try {
                    val prompt = "Pick the best icon for activity '${habit.name}' (${habit.description ?: ""}) from: $availableIcons. Return ONLY the icon name."
                    val request = GeminiRequest(listOf(Content(parts = listOf(Part(prompt)))))
                    val response = geminiService.getIconSuggestion(request)
                    val iconName = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    
                    if (iconName != null && iconName.startsWith("ic_")) {
                         saveIconForHabit(habit.id, iconName)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getAvailableIcons(): List<String> {
        return listOf(
            "ic_activity_art", "ic_activity_baby", "ic_activity_bike", "ic_activity_book", 
            "ic_activity_chat", "ic_activity_clean", "ic_activity_coffee", "ic_activity_cooking", 
            "ic_activity_drink", "ic_activity_drive", "ic_activity_email", "ic_activity_fastfood", 
            "ic_activity_finance", "ic_activity_gaming", "ic_activity_garden", "ic_activity_gas", 
            "ic_activity_generic", "ic_activity_groceries", "ic_activity_gym", "ic_activity_hike", 
            "ic_activity_idea", "ic_activity_laptop", "ic_activity_laundry", "ic_activity_map", 
            "ic_activity_meds", "ic_activity_meeting", "ic_activity_movie", "ic_activity_music", 
            "ic_activity_pet", "ic_activity_photo", "ic_activity_pizza", "ic_activity_presentation", 
            "ic_activity_relax", "ic_activity_repair", "ic_activity_restaurant", "ic_activity_run", 
            "ic_activity_school", "ic_activity_shopping", "ic_activity_shower", "ic_activity_sleep", 
            "ic_activity_soccer", "ic_activity_swim", "ic_activity_transit", "ic_activity_travel", 
            "ic_activity_wakeup", "ic_activity_walk", "ic_activity_water", "ic_activity_work", 
            "ic_activity_write", "ic_activity_yoga"
        )
    }
}
