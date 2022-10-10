package com.example.stroll.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.example.stroll.R
import com.example.stroll.databinding.FragmentIntroductionBinding
import com.example.stroll.presentation.adapters.ViewPagerAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroductionFragment() : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentIntroductionBinding? = null
    private val binding get() = _binding!!

    lateinit var viewPager: ViewPager
    lateinit var viewPagerAdapter: ViewPagerAdapter
    lateinit var dotsIndicator: SpringDotsIndicator
    lateinit var imageList: List<Int>
    lateinit var headingList: List<String>
    lateinit var bodyList: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        viewPager = binding.idViewPager
        dotsIndicator = binding.springDotsIndicator

        imageList = ArrayList()
        imageList = imageList + R.drawable.mapicon
        imageList = imageList + R.drawable.cameraicon
        imageList = imageList + R.drawable.recordicon

        headingList = ArrayList()
        headingList = headingList + "nigga"
        headingList = headingList + "gringo"
        headingList = headingList + "n-word"

        bodyList = ArrayList()
        bodyList = bodyList + "niggasaurus rex"
        bodyList = headingList + "gringo"
        bodyList = headingList + "n-word"


        viewPagerAdapter = ViewPagerAdapter(requireContext(), imageList, headingList, bodyList)
        viewPager.adapter = viewPagerAdapter
        dotsIndicator.attachTo(viewPager)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = IntroductionFragmentDirections.actionIntroductionFragmentToSettingsFragment()
                view?.findNavController()?.navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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