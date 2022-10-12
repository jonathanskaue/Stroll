package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil.setContentView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.databinding.FragmentIntroductionBinding
import com.example.stroll.presentation.adapters.ViewPagerAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

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
        (activity as AppCompatActivity).supportActionBar?.hide()
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        viewPager = binding.idViewPager
        dotsIndicator = binding.springDotsIndicator

        imageList = ArrayList()
        imageList = imageList + R.drawable.allura___in_the_park
        imageList = imageList + R.drawable.allura___giant_phone
        imageList = imageList + R.drawable.allura___ui_windows

        headingList = ArrayList()
        headingList = headingList + "Start a hike!"
        headingList = headingList + "Augmented Reality"
        headingList = headingList + "Recorded hikes"

        bodyList = ArrayList()
        bodyList = bodyList + "Start a hike by either creating a starting point and destination \n or by just start strolling!"
        bodyList = bodyList + "See your favorite destinations in AR"
        bodyList = bodyList + "See all your previous hikes and stats!"

        viewPagerAdapter = ViewPagerAdapter(viewModel, view, requireContext(), imageList, headingList, bodyList)
        viewPager.adapter = viewPagerAdapter
        dotsIndicator.attachTo(viewPager)

        lifecycleScope.launch {
            viewModel.isStarted.collect {
                if (viewModel.isStarted.value) {
                    (activity as MainActivity).loadFragment(MainFragment())
                    /*val action = IntroductionFragmentDirections.actionIntroductionFragmentToMainFragment()
                    view?.findNavController()?.navigate(action)*/
                }
            }
        }




        return binding.root

    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).supportActionBar?.show()
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