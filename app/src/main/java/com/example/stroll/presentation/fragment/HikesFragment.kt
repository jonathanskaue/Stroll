package com.example.stroll.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stroll.databinding.FragmentHikesBinding
import com.example.stroll.domain.repository.RVClickListener
import com.example.stroll.other.SortType
import com.example.stroll.presentation.adapters.HikeAdapter
import com.example.stroll.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HikesFragment : BaseFragment(), RVClickListener {

    private val viewModel: MainViewModel by viewModels()

    private var _binding: FragmentHikesBinding? = null
    private val binding get() = _binding!!

    private val hikeAdapter by lazy { HikeAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentHikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toggleViewButton.setOnClickListener {
            val action = HikesFragmentDirections.actionHikesFragmentToStatisticsFragment()
            findNavController().navigate(action)
        }
        setupRecyclerView()

        when(viewModel.sortType) {
            SortType.DATE -> {
                binding.spFilter.setSelection(0)
            }
            SortType.HIKE_TIME -> {
                binding.spFilter.setSelection(1)
            }
            SortType.DISTANCE -> {
                binding.spFilter.setSelection(2)
            }
            SortType.AVG_SPEED -> {
                binding.spFilter.setSelection(3)
            }
        }

        binding.spFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {}

            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                when(pos) {
                    0 -> {
                        viewModel.sortHikes(SortType.DATE)
                        binding.rvHikes.smoothScrollToPosition( 0)
                    }
                    1 -> {
                        viewModel.sortHikes(SortType.HIKE_TIME)
                        binding.rvHikes.smoothScrollToPosition(0)
                    }
                    2 -> {
                        viewModel.sortHikes(SortType.DISTANCE)
                        binding.rvHikes.smoothScrollToPosition(0)
                    }
                    3 -> {
                        viewModel.sortHikes(SortType.AVG_SPEED)
                        binding.rvHikes.smoothScrollToPosition(0)
                    }
                }
            }
        }

        viewModel.hikes.observe(viewLifecycleOwner) {
            hikeAdapter.submitList(it)
            if (it.isEmpty()) {
                binding.constraintLayout.visibility = View.GONE
                binding.noHikes.visibility = View.VISIBLE
            } else {
                binding.constraintLayout.visibility = View.VISIBLE
                binding.noHikes.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() = binding.rvHikes.apply {
        adapter = hikeAdapter
        layoutManager = LinearLayoutManager(requireContext())
    }

    //Navigating to HikeDetailsFragment and showing the specific hike clicked on
    override fun onClick(position: Int) {
        val id = hikeAdapter.differ.currentList[position].id
        val action = id?.let { HikesFragmentDirections.actionHikesFragmentToHikeDetailsFragment(it) }
        if (action != null) {
            view?.findNavController()?.navigate(action)
        }
    }

}

