package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.preference.PreferenceManager
import com.example.stroll.R
import com.example.stroll.databinding.FragmentMainBinding
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainFragment() : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        viewModel.addDataToRoom()
        viewModel.allData.observe(viewLifecycleOwner) { data ->
            binding.textView.text = data[0].id.toString()
        }
        binding.btnMap.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToMapFragment()
            view?.findNavController()?.navigate(action)
        }
        binding.btnIntro.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToIntroductionFragment()
            view?.findNavController()?.navigate(action)
        }
        binding.btnGraph.setOnClickListener {
            val action = MainFragmentDirections.actionMainFragmentToGraphFragment()
            view?.findNavController()?.navigate(action)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSettings()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val action = MainFragmentDirections.actionMainFragmentToSettingsFragment()
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