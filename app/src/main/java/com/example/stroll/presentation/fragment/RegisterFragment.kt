package com.example.stroll.presentation.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.stroll.MainActivity
import com.example.stroll.R
import com.example.stroll.databinding.FragmentRegisterBinding
import com.example.stroll.other.Constants.KEY_FIRST_TIME_TOGGLE
import com.example.stroll.other.Constants.KEY_NAME
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/*
Registers username with dependency injection and shared preferences.
Remembers username and if you previously opened the app and navigates to Home fragment if true.
Prompts you to enter a username if first time opened.
 */
@AndroidEntryPoint
class RegisterFragment : BaseFragment() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    @set:Inject
    var isFirstAppOpen = true

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!isFirstAppOpen) {
            findNavController().popBackStack()
            findNavController().navigate(R.id.action_global_homeFragment)
        }
        else{
            (activity as MainActivity).bottomNavBar.visibility = View.GONE
            binding.tvContinue.setOnClickListener {
                val success = writePersonalDataToSharedPref()
                if(success) {
                    (activity as MainActivity).bottomNavBar.visibility = View.VISIBLE
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.action_global_introductionFragment)
                } else {
                    Snackbar.make(requireView(), getString(R.string.please_enter_all_the_fields), Snackbar.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun writePersonalDataToSharedPref(): Boolean {
        val name = binding.etName.text.toString()
        if(name.isEmpty()) {
            return false
        }
        sharedPref.edit()
            .putString(KEY_NAME, name)
            .putBoolean(KEY_FIRST_TIME_TOGGLE, false)
            .apply()
        return true
    }

}