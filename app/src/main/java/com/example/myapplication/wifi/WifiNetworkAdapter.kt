package com.example.myapplication.wifi

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemWifiNetworkBinding

class WifiNetworkAdapter(
    private val onNetworkClick: (String) -> Unit
) : ListAdapter<ScanResult, WifiNetworkAdapter.NetworkViewHolder>(NetworkDiffCallback()) {

    class NetworkViewHolder(
        private val binding: ItemWifiNetworkBinding,
        private val onNetworkClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(network: ScanResult) {
            binding.textNetworkName.text = network.SSID
            binding.textNetworkStrength.text = "Señal: ${network.level} dBm"
            binding.root.setOnClickListener { onNetworkClick(network.SSID) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val binding = ItemWifiNetworkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NetworkViewHolder(binding, onNetworkClick)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class NetworkDiffCallback : DiffUtil.ItemCallback<ScanResult>() {
        override fun areItemsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem.SSID == newItem.SSID
        }

        override fun areContentsTheSame(oldItem: ScanResult, newItem: ScanResult): Boolean {
            return oldItem == newItem
        }
    }
}