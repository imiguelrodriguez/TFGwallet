package com.example.tfgwallet.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.tfgwallet.ui.fragments.HomeFragment
import com.example.tfgwallet.ui.fragments.ProfileFragment
import com.example.tfgwallet.R
import com.example.tfgwallet.ui.fragments.SettingsFragment
import com.example.tfgwallet.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        replaceFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> replaceFragment(HomeFragment())
                R.id.settings -> replaceFragment(SettingsFragment())
                R.id.profile-> replaceFragment(ProfileFragment())
                else -> {
                    true
                }
            }

        }

    }
    private fun replaceFragment(fragment: Fragment): Boolean {
        val fragmentManager: FragmentManager  = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frameLayout, fragment)
        fragmentTransaction.commit()
        return true
    }
}