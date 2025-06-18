package com.example.myapplication.mqtt

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemMqttSubscriptionBinding
import java.text.SimpleDateFormat
import java.util.*

class MqttSubscriptionsAdapter(
    private val subscriptions: List<MqttSubscription>,
    private val onItemClick: (MqttSubscription) -> Unit
) : RecyclerView.Adapter<MqttSubscriptionsAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemMqttSubscriptionBinding) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(subscription: MqttSubscription) {
            binding.tvTopic.text = subscription.topic
            binding.tvQos.text = "QoS: ${subscription.qos}"
            
            subscription.lastMessage?.let { message ->
                binding.tvLastMessage.text = "Último mensaje: $message"
                binding.tvLastMessage.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvLastMessage.visibility = android.view.View.GONE
            }
            
            subscription.timestamp?.let { timestamp ->
                binding.tvTimestamp.text = "Recibido: ${dateFormat.format(timestamp)}"
                binding.tvTimestamp.visibility = android.view.View.VISIBLE
            } ?: run {
                binding.tvTimestamp.visibility = android.view.View.GONE
            }
            
            binding.root.setOnClickListener {
                onItemClick(subscription)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMqttSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(subscriptions[position])
    }

    override fun getItemCount(): Int = subscriptions.size
} 