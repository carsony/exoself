package com.example.exoself.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

const val NUM_TABS = 4

class DeckPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun createFragment(pos: Int): Fragment = TasksFragment.newInstance(pos)
    override fun getItemCount(): Int = NUM_TABS
}