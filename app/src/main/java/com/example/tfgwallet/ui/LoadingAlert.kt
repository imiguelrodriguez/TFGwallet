package com.example.tfgwallet.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import com.example.tfgwallet.R
import com.example.tfgwallet.databinding.LoadingLayoutBinding


class LoadingAlert(myActivity: Activity, text: String){
    private var activity: Activity
    private lateinit var dialog: AlertDialog
    private var text: String
    private var binding: LoadingLayoutBinding
    private var inflater : LayoutInflater

    init {
        activity = myActivity
        this.text = text
        inflater = activity.layoutInflater
        binding = LoadingLayoutBinding.bind(inflater.inflate(R.layout.loading_layout, null))
    }

    fun startAlertDialog() {
        val builder = AlertDialog.Builder(activity)
        binding.textView.text = this.text
        builder.setView(binding.root)
        builder.setCancelable(true)
        dialog = builder.create()
        dialog.show()
    }

    fun closeAlertDialog() { dialog.dismiss() }


}