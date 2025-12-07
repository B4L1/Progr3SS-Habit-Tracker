package com.example.lab4.ui.ai

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.data.local.TokenManager
import com.example.lab4.data.model.ChatMessage
import com.example.lab4.data.model.ChatRequestDto
import com.example.lab4.data.model.ChatResponseDto
import com.example.lab4.data.remote.OpenAIService
import com.example.lab4.databinding.FragmentAiAssistantBinding
import com.example.lab4.databinding.ItemChatMessageBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AIAssistantFragment : Fragment() {
    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    
    private lateinit var tokenManager: TokenManager
    private var apiKey: String = ""
    private lateinit var openAIService: OpenAIService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tokenManager = TokenManager(requireContext())

        setupRecyclerView()
        
        // Check if API key is already saved
        val savedKey = tokenManager.getApiKey()
        if (!savedKey.isNullOrEmpty()) {
            apiKey = savedKey
            setupService()
        } else {
            promptForApiKey()
        }

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString().trim()
            if (messageText.isNotEmpty()) {
                if (apiKey.isEmpty()) {
                    promptForApiKey()
                } else {
                    sendMessage(messageText)
                    binding.messageEditText.text.clear()
                }
            }
        }

        // Long click to reset API Key
        binding.sendButton.setOnLongClickListener {
            promptForApiKey()
            true
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = chatAdapter
    }
    
    private fun promptForApiKey() {
        val input = EditText(context)
        input.hint = "sk-..."
        
        AlertDialog.Builder(context)
            .setTitle("Enter OpenAI API Key")
            .setMessage("This key will be saved locally.")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    // Ensure Bearer prefix
                    val finalKey = if (key.startsWith("Bearer ")) key else "Bearer $key"
                    apiKey = finalKey
                    tokenManager.saveApiKey(apiKey)
                    setupService()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun setupService() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            
        openAIService = retrofit.create(OpenAIService::class.java)
    }

    private fun sendMessage(content: String) {
        if (!::openAIService.isInitialized) {
            Toast.makeText(context, "API Key not set", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user message to UI
        val userMessage = ChatMessage(role = "user", content = content)
        messages.add(userMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)

        // Prepare API call
        val request = ChatRequestDto(
            messages = messages.toList()
        )

        openAIService.createChatCompletion(apiKey, request).enqueue(object : Callback<ChatResponseDto> {
            override fun onResponse(call: Call<ChatResponseDto>, response: Response<ChatResponseDto>) {
                if (response.isSuccessful && response.body() != null) {
                    val chatResponse = response.body()!!
                    val botMessageContent = chatResponse.choices.firstOrNull()?.message?.content
                    
                    if (botMessageContent != null) {
                        val botMessage = ChatMessage(role = "assistant", content = botMessageContent)
                        messages.add(botMessage)
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                } else {
                    when (response.code()) {
                        401 -> {
                            Toast.makeText(context, "Invalid API Key. Long press send to update.", Toast.LENGTH_LONG).show()
                            // Optionally force prompt immediately:
                            // promptForApiKey()
                        }
                        429 -> {
                            Toast.makeText(context, "Quota exceeded (429). Check OpenAI billing.", Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            Toast.makeText(context, "Failed: ${response.code()} ${response.message()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onFailure(call: Call<ChatResponseDto>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class ChatAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        inner class ChatViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChatViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val message = messages[position]
            holder.binding.messageTextView.text = message.content
            
            // Simple styling difference for user vs bot
            if (message.role == "user") {
                holder.binding.messageTextView.setBackgroundColor(resources.getColor(android.R.color.holo_blue_light, null))
            } else {
                holder.binding.messageTextView.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
        }

        override fun getItemCount() = messages.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
