package com.example.stroll.presentation.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import com.example.stroll.R
import com.example.stroll.presentation.viewmodel.MainViewModel
//All other fragments will extend BaseFragment
abstract class BaseFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    private fun loadSettings() {
        val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val dark_mode = sp?.getBoolean("dark_mode", false)
        val markers = sp?.getBoolean("marker_map", true)
        val heatMap = sp?.getBoolean("heat_map", false)
        val mountains = sp?.getBoolean("poi_mountain", true)
        val fishing = sp?.getBoolean("poi_fishing", true)
        val attractions = sp?.getBoolean("poi_attraction", true)
        val camping = sp?.getBoolean("poi_camping", true)
        val canoe = sp?.getBoolean("poi_canoe", true)
        val misc = sp?.getBoolean("poi_misc", true)
        val startingPos = sp?.getBoolean("poi_starting", false)

        if (dark_mode == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        if (heatMap!!) {
            viewModel.isHeatMap()
        }
        if (!heatMap) {
            viewModel.isNotHeatMap()
        }
        if (markers!!) {
            viewModel.isMarker()
        }
        if (!markers) {
            viewModel.isNotMarker()
        }
        if (mountains!!) {
            viewModel.isMountain()
        }
        if (!mountains) {
            viewModel.isNotMountain()
        }
        if (fishing!!) {
            viewModel.isFishing()
        }
        if (!fishing) {
            viewModel.isNotFishing()
        }
        if (attractions!!) {
            viewModel.isAttraction()
        }
        if (!attractions) {
            viewModel.isNotAttraction()
        }
        if (camping!!) {
            viewModel.isCamping()
        }
        if (!camping) {
            viewModel.isNotCamping()
        }
        if (canoe!!) {
            viewModel.isCanoe()
        }
        if (!canoe) {
            viewModel.isNotCanoe()
        }
        if (misc!!) {
            viewModel.isMisc()
        }
        if (!misc) {
            viewModel.isNotMisc()
        }
        if (startingPos!!) {
            viewModel.isStartingPos()
        }
        if (!startingPos) {
            viewModel.isNotStartingPos()
        }
    }

}