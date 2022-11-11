package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.stroll.databinding.FragmentHomeBinding


import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.example.stroll.databinding.FragmentHikeDetailsBinding
import com.example.stroll.other.Utility
import com.example.stroll.presentation.viewmodel.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.round
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HikeDetailsFragment : BaseFragment() {

    @set:Inject
    var name = ""

    val args: HikeDetailsFragmentArgs by navArgs()

    private val viewModel: StatisticsViewModel by viewModels()

    private var _binding: FragmentHikeDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        _binding = FragmentHikeDetailsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("args", "onViewCreated: $args")

    }
}