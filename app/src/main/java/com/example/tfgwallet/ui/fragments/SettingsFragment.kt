package com.example.tfgwallet.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.tfgwallet.R

private const val ARG_PARAM1 = "user"
class SettingsFragment : PreferenceFragmentCompat() {
    private var user: String = ""
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        arguments?.let {
            user = it.getString(ARG_PARAM1).toString()
        }

        val twoaf = findPreference<SwitchPreferenceCompat>("2af")
        val userPreferences = context?.getSharedPreferences("user_$user", Context.MODE_PRIVATE)
        twoaf?.isChecked = userPreferences?.getBoolean("isTwoFactorAuthEnabled", false) == true

        twoaf?.setOnPreferenceChangeListener { _, newValue ->
            val newValueBoolean = newValue as Boolean

            if (newValueBoolean) {
                userPreferences?.edit()?.putBoolean("isTwoFactorAuthEnabled", true)?.apply()
                Log.i("2af", userPreferences?.all.toString())
                Toast.makeText(requireContext(), "Switch turned ON", Toast.LENGTH_SHORT).show()
            } else {
                userPreferences?.edit()?.putBoolean("isTwoFactorAuthEnabled", false)?.apply()
                Toast.makeText(requireContext(), "Switch turned OFF", Toast.LENGTH_SHORT).show()
            }
            true // Return true to update the preference value
        }

    }
    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }
}