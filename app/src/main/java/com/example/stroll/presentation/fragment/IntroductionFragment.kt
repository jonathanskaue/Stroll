package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.viewpager.widget.ViewPager
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.databinding.FragmentIntroductionBinding
import com.example.stroll.presentation.adapters.ViewPagerAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class IntroductionFragment : BaseFragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentIntroductionBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPager: ViewPager
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var dotsIndicator: SpringDotsIndicator
    private lateinit var imageList: List<Int>
    private lateinit var headingList: List<String>
    private lateinit var bodyList: List<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as MainActivity).bottomNavBar.visibility = View.GONE
        _binding = FragmentIntroductionBinding.inflate(inflater, container, false)

        viewPager = binding.idViewPager
        dotsIndicator = binding.springDotsIndicator

        imageList = ArrayList()
        imageList = imageList + R.drawable.allura___in_the_park
        imageList = imageList + R.drawable.allura___giant_phone
        imageList = imageList + R.drawable.allura___ui_windows

        headingList = ArrayList()
        headingList = headingList + getString(R.string.start_a_hike)
        headingList = headingList + getString(R.string.augmented_reality)
        headingList = headingList + getString(R.string.recorded_hikes)

        bodyList = ArrayList()
        bodyList = bodyList + getString(R.string.start_a_hike_by_starting_point_and_destination)
        bodyList = bodyList + getString(R.string.see_your_favourite_destinations_in_ar)
        bodyList = bodyList + getString(R.string.see_all_your_previous_hikes_and_stats)

        viewPagerAdapter = ViewPagerAdapter(viewModel, view, requireContext(), imageList, headingList, bodyList)
        viewPager.adapter = viewPagerAdapter
        dotsIndicator.attachTo(viewPager)

        lifecycleScope.launch {
            viewModel.isStarted.collect {
                if (viewModel.isStarted.value) {
                    view?.findNavController()?.popBackStack()
                    view?.findNavController()?.navigate(R.id.action_global_homeFragment)
                }
            }
        }
        return binding.root
    }

    override fun onStop() {
        super.onStop()
        (activity as MainActivity).bottomNavBar.visibility = View.VISIBLE
    }
}