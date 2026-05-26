package uk.org.cgatechnologies.wideya.common.adapters

import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(private val listFragment: List<Fragment>, fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return listFragment.size
    }

    override fun createFragment(position: Int): Fragment {
        Log.d("viewpageradapter", "creating fragment ${position}")
        return listFragment[position]
    }

//    override fun getItemId(position: Int): Long {
//        return listFragment[position].id.toLong()
//    }
//
//    override fun containsItem(itemId: Long): Boolean = listFragment.any { it.id.toLong() == itemId }

}
