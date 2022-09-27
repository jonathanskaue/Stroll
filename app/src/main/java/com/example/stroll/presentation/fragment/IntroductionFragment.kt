package com.example.stroll.presentation.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.example.stroll.R
import com.example.stroll.databinding.FragmentIntroductionBinding
import com.example.stroll.databinding.FragmentMainBinding
import com.example.stroll.databinding.FragmentMapBinding
import com.example.stroll.presentation.adapters.ViewPagerAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroductionFragment() : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentIntroductionBinding? = null
    private val binding get() = _binding!!

    lateinit var viewPager: ViewPager
    lateinit var viewPagerAdapter: ViewPagerAdapter
    lateinit var imageList: List<Int>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)

        viewPager = binding.idViewPager

        imageList = ArrayList<Int>()
        imageList = imageList + R.drawable.skjermbilde_kart
        imageList = imageList + R.drawable.skjermbilde_dualkartkamera
        imageList = imageList + R.drawable.skjermbilde_records

        viewPagerAdapter = ViewPagerAdapter(requireContext(), imageList)
        viewPager.adapter = viewPagerAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    fun loadSettings() {
        val sp = context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
        val dark_mode = sp?.getBoolean("dark_mode", false)

        if (dark_mode == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}