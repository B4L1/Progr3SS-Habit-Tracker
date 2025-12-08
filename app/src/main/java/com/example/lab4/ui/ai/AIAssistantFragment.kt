package com.example.lab4.ui.ai

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lab4.data.model.ChatMessage
import com.example.lab4.data.model.Content
import com.example.lab4.data.model.GeminiRequest
import com.example.lab4.data.model.Part
import com.example.lab4.data.remote.RetrofitClient
import com.example.lab4.databinding.FragmentAiAssistantBinding
import com.example.lab4.databinding.ItemChatMessageBinding
import kotlinx.coroutines.launch

class AIAssistantFragment : Fragment() {
    private var _binding: FragmentAiAssistantBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val TAG = "AIAssistantFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiAssistantBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.messageInput.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun sendMessage(userText: String) {
        // UI Update
        messages.add(ChatMessage("user", userText))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)

        lifecycleScope.launch {
            try {
                // Prepare History
                val contents = messages.map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(msg.content))
                    )
                }

                val request = GeminiRequest(contents)
                val service = RetrofitClient.createGeminiService()
                val response = service.generateContent(request)
                
                val botText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (botText != null) {
                    messages.add(ChatMessage("assistant", botText))
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                } else {
                     // Log mismatch or error
                     Log.e(TAG, "No candidates/content in response: $response")
                     Toast.makeText(context, "No response from AI", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class ChatAdapter(private val chats: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
        inner class ChatViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ChatViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val chat = chats[position]
            holder.binding.messageTextView.text = chat.content
            
            // Layout params manipulation for alignment
            val params = holder.binding.messageTextView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            
            if (chat.role == "user") {
                holder.binding.messageTextView.setBackgroundColor(0xFFBB86FC.toInt()) // Purple 200
                holder.binding.messageTextView.setTextColor(0xFF000000.toInt())
                
                // Align Right
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.horizontalBias = 1.0f
            } else {
                holder.binding.messageTextView.setBackgroundColor(0xFF333333.toInt()) // Dark Gray
                holder.binding.messageTextView.setTextColor(0xFFFFFFFF.toInt())
                
                // Align Left
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.horizontalBias = 0.0f
            }
            holder.binding.messageTextView.layoutParams = params
        }

        override fun getItemCount() = chats.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
