package com.example.shareplate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<ChatMessage>()

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageCard: CardView = view.findViewById(R.id.messageCard)
        val messageText: TextView = view.findViewById(R.id.messageText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.text

        // Set message appearance based on sender
        with(holder.messageCard) {
            if (message.isUser) {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 48 * context.resources.displayMetrics.density.toInt()
                    marginEnd = 8 * context.resources.displayMetrics.density.toInt()
                }
                setCardBackgroundColor(context.getColor(R.color.button_green))
            } else {
                layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                    marginStart = 8 * context.resources.displayMetrics.density.toInt()
                    marginEnd = 48 * context.resources.displayMetrics.density.toInt()
                }
                setCardBackgroundColor(context.getColor(R.color.gray_light))
            }
        }

        // Set text color based on sender
        holder.messageText.setTextColor(
            holder.itemView.context.getColor(
                if (message.isUser) R.color.white else R.color.black
            )
        )
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }
} 