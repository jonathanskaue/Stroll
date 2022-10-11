package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
class IntroductionFragment() : BaseFragment() {

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
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.settings -> {
                        val action = IntroductionFragmentDirections.actionIntroductionFragmentToSettingsFragment()
                        view.findNavController().navigate(action)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }
}