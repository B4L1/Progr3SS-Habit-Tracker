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
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("IconManager", "Failed to parse stashed icon for ${habit.name}")
                        }
                    }
                }
            }

            // Identify habits that still need icons (missing or generic)
            val habitsWithoutIcons = habits.filter { habit ->
                val currentIcon = getIconForHabit(habit.id)
                // Re-analyze if it's null OR generic (assuming user wants specific icons)
                // But user said: "the others should not be sent... if they have an icon that's not the default one"
                // So if it IS default (generic), we DO send it.
                val needsIcon = currentIcon == null || currentIcon == "ic_activity_generic"
                needsIcon
            }
            
            android.util.Log.d("IconManager", "Found ${habitsWithoutIcons.size} habits needing icons")

            if (habitsWithoutIcons.isEmpty()) return@withContext

            // BATCH REQUEST
            val geminiService = RetrofitClient.createGeminiService()
            val availableIcons = getAvailableIcons().joinToString(", ")
            
            // Construct Batch Prompt
            val activityList = habitsWithoutIcons.joinToString("; ") { "ID ${it.id}: '${it.name}' (${it.description ?: ""})" }
            val prompt = """
                I have a list of activities. For EACH activity, verify if it matches one of the following icons exactly:
                [$availableIcons]
                
                Using the ID provided, return a JSON array mapping IDs to the best matching icon name. 
                Format: [{"id": 123, "icon": "ic_activity_example"}, ...]
                If no good match found for an activity, use "ic_activity_generic".
                
                Activities:
                $activityList
                
                Return ONLY the JSON.
            """.trimIndent()
            
            try {
                android.util.Log.d("IconManager", "Sending BATCH prompt to Gemini...")
                val request = GeminiRequest(listOf(Content(parts = listOf(Part(prompt)))))
                val response = geminiService.getIconSuggestion(request)
                var responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
                
                android.util.Log.d("IconManager", "Gemini Batch Response: $responseText")
                
                // Sanitization (sometimes Gemini adds ```json ... ```)
                if (responseText.startsWith("```")) {
                    responseText = responseText.replace("```json", "").replace("```", "").trim()
                }
                
                // Parse JSON (Simple manual parsing to avoid adding Gson/Moshi dependency if not ready, strictly speaking we should use a library but regex is safer for simple task)
                // or just use manual string parsing since the format is strict.
                // Regex for: {"id": (\d+), "icon": "([^"]+)"}
                val regex = Regex("""\"id\"\s*:\s*(\d+),\s*\"icon\"\s*:\s*\"([^\"]+)\"""")
                val matches = regex.findAll(responseText)
                
                var count = 0
                matches.forEach { match ->
                    val habitId = match.groupValues[1].toIntOrNull()
                    val iconName = match.groupValues[2]
                    
                    if (habitId != null && iconName.startsWith("ic_")) {
                         withContext(Dispatchers.Main) {
                             saveIconForHabit(habitId, iconName)
                         }
                         count++
                    }
                }
                 withContext(Dispatchers.Main) {
                      android.widget.Toast.makeText(context, "AI Batch Analysis: Updated $count icons", android.widget.Toast.LENGTH_SHORT).show()
                 }
                
            } catch (e: Exception) {
                android.util.Log.e("IconManager", "Error calling Gemini Batch", e)
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
