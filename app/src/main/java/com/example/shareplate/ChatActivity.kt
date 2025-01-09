package com.example.shareplate

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var backButton: ImageView
    private lateinit var chatAdapter: ChatAdapter
    private val chatbot = SharePlateChatbot()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        sendWelcomeMessage()
    }

    private fun initializeViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageInput.text.clear()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun sendWelcomeMessage() {
        lifecycleScope.launch {
            val welcomeMessage = "Hi! I'm your SharePlate Assistant. I can help you with donations, requests, and navigating the app. What would you like help with?"
            chatAdapter.addMessage(ChatMessage(welcomeMessage, false))
        }
    }

    private fun sendMessage(message: String) {
        lifecycleScope.launch {
            // Add user message
            chatAdapter.addMessage(ChatMessage(message, true))
            messagesRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)

            // Get bot response
            val response = chatbot.sendMessage(message)
            chatAdapter.addMessage(ChatMessage(response, false))
            messagesRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
        }
    }
} 