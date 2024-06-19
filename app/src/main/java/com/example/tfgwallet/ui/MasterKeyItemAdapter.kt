package com.example.tfgwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgwallet.R
import com.example.tfgwallet.model.MasterKeyItem


internal class MasterKeyItemAdapter(keys: List<MasterKeyItem>) :
    RecyclerView.Adapter<MasterKeyItemAdapter.MasterKeyItemViewHolder>() {
    private val keys: List<MasterKeyItem>

    inner class MasterKeyItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val privateKey: TextView
        val publicKey: TextView
        val chainCode: TextView
        val scHash: TextView

        init {
            privateKey = view.findViewById<View>(R.id.privKey) as TextView
            publicKey = view.findViewById<View>(R.id.pubKey) as TextView
            chainCode = view.findViewById<View>(R.id.chainCode) as TextView
            scHash = view.findViewById<View>(R.id.scHash) as TextView
        }

    }

    init {
        this.keys = keys
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MasterKeyItemViewHolder {
        val itemView: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.key_swipe_item, parent, false)
        return MasterKeyItemViewHolder(itemView)
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun onBindViewHolder(holder: MasterKeyItemViewHolder, position: Int) {
        val key: MasterKeyItem = keys[position]
        holder.privateKey.text = key.privKey
        holder.publicKey.text = key.pubKey
        holder.chainCode.text = key.chainCode
        holder.scHash.text = key.scHash
    }

    override fun getItemCount(): Int {
        return keys.size
    }
}