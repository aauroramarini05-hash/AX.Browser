package com.xdust.auryxbrowser.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter used by [MainActivity] to display multiple WebView fragments in a
 * ViewPager2. Each tab corresponds to a [WebViewFragment].
 */
class TabAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val fragments: MutableList<Fragment> = mutableListOf()

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    /**
     * Returns the fragment at the given position if it exists.
     */
    fun getFragmentAt(position: Int): Fragment? = fragments.getOrNull(position)

    /**
     * Adds a new [WebViewFragment] to the end of the tab list.
     */
    fun addTab(fragment: Fragment) {
        fragments.add(fragment)
        notifyItemInserted(fragments.lastIndex)
    }

    /**
     * Removes the fragment at the given index. If the index is out of bounds
     * nothing will happen.
     */
    fun removeTab(index: Int) {
        if (index < 0 || index >= fragments.size) return
        fragments.removeAt(index)
        notifyItemRemoved(index)
    }
}