package com.example.exoself.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.exoself.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val settingsViewModel: SettingsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsViewModel.getAuthState().observe(viewLifecycleOwner) { isUserSignedOut ->
            if (!isUserSignedOut) {
                settingsViewModel.profileLiveData.observe(viewLifecycleOwner) {
                    binding.timerDurationEt.hint = it.timerDuration.toString()
                }
            }
        }

        binding.submitBtn.setOnClickListener {
            if (binding.timerDurationEt.text.toString().isNotEmpty()) {
                submitButtonHandler()
                findNavController().popBackStack()
            }
        }
        binding.cancelBtn.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun submitButtonHandler() {
        settingsViewModel.setTimerDuration(binding.timerDurationEt.text.toString().toInt())
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}