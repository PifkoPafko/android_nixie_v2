package com.example.android_nixie_v2

import android.os.Handler
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class WifiAdapter(private val handler : Handler) : RecyclerView.Adapter<WifiAdapter.WifiViewHolder>() {

    var enable : Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.wifi_list_item, parent, false)
        return WifiViewHolder(itemView)
    }

    override fun onBindViewHolder(holder : WifiViewHolder, position: Int) {
        holder.wifiCardNameText.text = WifiSet.wifiList[holder.absoluteAdapterPosition].ssid
        holder.wifiCardAuthText.text = authModetoString(WifiSet.wifiList[holder.absoluteAdapterPosition].authMode)
        holder.wifiCardRssiText.text = String.format("RSSI: %d", WifiSet.wifiList[holder.absoluteAdapterPosition].rssi)

        holder.wifiCard.setOnClickListener {
            val connectMsg = Message()
            connectMsg.arg1 = 3
            connectMsg.arg2 = holder.absoluteAdapterPosition
            handler.sendMessage(connectMsg)
        }

        setEnable(holder)
    }

    override fun getItemCount(): Int {
        return WifiSet.wifiList.size
    }

    class WifiViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val wifiCard = itemView.findViewById<MaterialCardView?>(R.id.wifiCard)
        val wifiCardNameText = itemView.findViewById<android.widget.TextView?>(R.id.wifiCardNameText)
        val wifiCardAuthText = itemView.findViewById<android.widget.TextView?>(R.id.wifiCardAuthText)
        val wifiCardRssiText = itemView.findViewById<android.widget.TextView?>(R.id.wifiCardRssiText)
    }

    private fun setEnable(holder : WifiViewHolder) {
        holder.wifiCard.isEnabled = enable
    }
}

object WifiSet {
    val wifiList : ArrayList<Wifi> = arrayListOf()
}

