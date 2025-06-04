package com.example.exoself.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.exoself.MainViewModel
import com.example.exoself.R
import com.example.exoself.databinding.FragmentHomeBinding
import com.google.android.material.tabs.TabLayoutMediator

private val TAB_TITLES = arrayOf(
    R.string.tab_current, R.string.tab_saved, R.string.tab_future, R.string.tab_done
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getAuthState().observe(viewLifecycleOwner) { isUserSignedOut ->
            if (!isUserSignedOut) {
                val viewPager = binding.viewPager.apply {
                    adapter = DeckPagerAdapter(requireActivity())
                    isUserInputEnabled = false
                }
                val tabLayout = binding.tabs
                TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                    tab.text = context?.resources?.getString(TAB_TITLES[position])
                }.attach()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}