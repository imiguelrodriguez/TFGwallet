package com.example.tfgwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.tfgwallet.R
import com.example.tfgwallet.model.KeyItem
import com.example.tfgwallet.model.MasterKeyItem


internal class BrowserKeyItemAdapter(keys: List<KeyItem>, private val browserTitles: List<String>) :
    KeyItemAdapter(keys) {

        inner class BrowserKeyItemViewHolder(view: View) : KeyItemViewHolder(view) {
            val browserTitle: TextView = view.findViewById<View>(R.id.browserTitle) as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrowserKeyItemViewHolder {
            val itemView: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.key_swipe_item, parent, false)
            return BrowserKeyItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: KeyItemViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val browserHolder = holder as BrowserKeyItemViewHolder
            browserHolder.browserTitle.text = browserTitles[position]
        }
    }
