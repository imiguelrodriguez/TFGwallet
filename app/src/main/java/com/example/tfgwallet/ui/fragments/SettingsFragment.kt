package com.example.tfgwallet.ui.fragments

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.example.tfgwallet.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val twoaf = findPreference<SwitchPreferenceCompat>("2af")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val user = sharedPreferences.getString("lastLoggedInUserId", null)?.substringBefore("@")
        if (user != null) {
            Log.i("user", user)
        }
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

}