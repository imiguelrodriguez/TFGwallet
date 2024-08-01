package com.example.tfgwallet.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tfgwallet.R

data class Step(val number: Int, val description: String)

class StepsAdapter(private val steps: List<Step>) : RecyclerView.Adapter<StepsAdapter.StepViewHolder>() {
    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val stepNumber: TextView = itemView.findViewById(R.id.stepNumber)
        val stepDescription: TextView = itemView.findViewById(R.id.stepDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        val step = steps[position]
        holder.stepNumber.text = "${step.number}."
        holder.stepDescription.text = step.description
    }

    override fun getItemCount(): Int {
        return steps.size
    }
}
