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

class IconManager(private val context: Context) {
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
            
            // First: Restore icons from description "stash" if missing locally
            habits.forEach { habit ->
                if (getIconForHabit(habit.id) == null) {
                    val description = habit.description ?: ""
                    if (description.contains("|icon:")) {
                        try {
                            val startIndex = description.indexOf("|icon:") + "|icon:".length
                            val endIndex = description.indexOf("|", startIndex)
                            if (endIndex > startIndex) {
                                val stashedIcon = description.substring(startIndex, endIndex)
                                if (stashedIcon.isNotEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        saveIconForHabit(habit.id, stashedIcon)
                                        android.util.Log.d("IconManager", "Restored stashed icon $stashedIcon for ${habit.name}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("IconManager", "Failed to parse stashed icon for ${habit.name}")
                        }
                    }
                }
            }

            val habitsWithoutIcons = habits.filter { habit ->
                val currentIcon = getIconForHabit(habit.id)
                // We need an icon if it's missing OR if it's the generic default
                val needsIcon = currentIcon == null || currentIcon == "ic_activity_generic"
                
                if (!needsIcon) return@filter false
                
                // Note: The restoration loop above puts stashed icons into SharedPreferences.
                // So 'currentIcon' reflects the stash.
                // If the stash was generic, we still want to try AI again.
                
                true
            }
            android.util.Log.d("IconManager", "Found ${habitsWithoutIcons.size} habits needing icons")


            if (habitsWithoutIcons.isEmpty()) return@withContext

            val geminiService = RetrofitClient.createGeminiService()
            val availableIcons = getAvailableIcons().joinToString(", ")

            for (habit in habitsWithoutIcons) {
                try {
                    val prompt = "Pick the best icon for activity '${habit.name}' (${habit.description ?: ""}) from: $availableIcons. Return ONLY the icon name."
                    android.util.Log.d("IconManager", "Sending prompt to Gemini: $prompt")
                    
                    val request = GeminiRequest(listOf(Content(parts = listOf(Part(prompt)))))
                    val response = geminiService.getIconSuggestion(request)
                    val iconName = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    
                    android.util.Log.d("IconManager", "Gemini response: $iconName")

                    withContext(Dispatchers.Main) {
                        if (iconName != null && iconName.startsWith("ic_")) {
                             android.widget.Toast.makeText(context, "AI Analysis: Suggested $iconName for ${habit.name}", android.widget.Toast.LENGTH_LONG).show()
                             saveIconForHabit(habit.id, iconName)
                        } else {
                             android.widget.Toast.makeText(context, "AI Analysis: Could not match icon for ${habit.name} (Got: $iconName)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("IconManager", "Error calling Gemini", e)
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "AI Analysis Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
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
