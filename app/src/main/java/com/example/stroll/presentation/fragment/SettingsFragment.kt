package com.example.stroll.presentation.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.example.stroll.MainActivity
import com.example.stroll.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as MainActivity).bottomNavBar.visibility = View.GONE

    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        loadSettings()
        return super.onPreferenceTreeClick(preference)
    }

    private fun loadSettings() {
        val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val dark_mode = sp?.getBoolean("dark_mode", false)
        val markers = sp?.getBoolean("marker_map", true)
        findPreference<PreferenceCategory>("map_poi")?.isVisible = markers!!

        if (dark_mode == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroy() {
        (activity as MainActivity).bottomNavBar.visibility = View.VISIBLE
        super.onDestroy()
    }
}