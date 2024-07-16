package com.example.tfgwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.tfgwallet.R
import com.example.tfgwallet.model.MasterKeyItem


internal class MasterKeyItemAdapter(private val masterKeys: List<MasterKeyItem>) : KeyItemAdapter (masterKeys.map { it.keyItem }) {

        inner class MasterKeyItemViewHolder(view: View) : KeyItemViewHolder(view) {
            val scHash: TextView = view.findViewById<View>(R.id.scHash) as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MasterKeyItemViewHolder {
            val itemView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.key_swipe_item, parent, false)
            return MasterKeyItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: KeyItemViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val key: MasterKeyItem = masterKeys[position]
            val masterHolder = holder as MasterKeyItemViewHolder
            masterHolder.scHash.text = key.scHash
            }
    }
