package com.example.tfgwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.tfgwallet.R
import com.example.tfgwallet.model.KeyItem


internal open class KeyItemAdapter(private val keys: List<KeyItem>) :
    RecyclerView.Adapter<KeyItemAdapter.KeyItemViewHolder>() {

    open inner class KeyItemViewHolder(view: View) : ViewHolder(view) {
        val privateKey: TextView = view.findViewById<View>(R.id.privKey) as TextView
        val publicKey: TextView = view.findViewById<View>(R.id.pubKey) as TextView
        val chainCode: TextView = view.findViewById<View>(R.id.chainCode) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyItemViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.key_swipe_item, parent, false)
        return KeyItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: KeyItemViewHolder, position: Int) {
        val key: KeyItem = keys[position]
        holder.privateKey.text = key.privKey
        holder.publicKey.text = key.pubKey
        holder.chainCode.text = key.chainCode
    }

    override fun getItemCount(): Int {
        return keys.size
    }
}