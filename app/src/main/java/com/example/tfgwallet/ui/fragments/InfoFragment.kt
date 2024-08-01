package com.example.tfgwallet.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tfgwallet.databinding.FragmentInfoBinding
import com.example.tfgwallet.ui.Step
import com.example.tfgwallet.ui.StepsAdapter


/**
 * A simple [Fragment] subclass.
 * Use the [InfoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class InfoFragment : Fragment() {
    // TODO: Rename and change types of parameters

    private lateinit var binding: FragmentInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentInfoBinding.inflate(layoutInflater)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding.recyclerViewSteps.layoutManager = LinearLayoutManager(requireContext())

        val steps = listOf(
            Step(1, "Please, scan the QR code to register the plugin in the app."),
            Step(2, "You will find all the keys in the keys section."),
            Step(3, "You can enable/disable 2AF at anytime in the settings section.")
        )

        val stepsAdapter = StepsAdapter(steps)
        binding.recyclerViewSteps.adapter = stepsAdapter
        return binding.root
    }

}