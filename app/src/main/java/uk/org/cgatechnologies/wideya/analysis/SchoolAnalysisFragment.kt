package uk.org.cgatechnologies.wideya.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import uk.org.cgatechnologies.wideya.common.adapters.ViewPagerAdapter
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolAnalysisBinding

private const val TAG = "school_analysis"

class SchoolAnalysisFragment : Fragment() {

    private var _binding: FragmentSchoolAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabLayout: TabLayout
    protected lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager

        val fragmentList = listOf(
            TeachersAnalysisSummaryFragment(),
            LearnersAnalysisSummaryFragment(),
            LearnerDisabilitySummaryFragment()
        )

        val fragmentTitle = listOf(
            "Teacher Attendance",
            "Learner Attendance",
            "Learner Needs"
        )

        val viewPager2Adapter = ViewPagerAdapter(fragmentList,this.childFragmentManager, lifecycle)
        viewPager.adapter = viewPager2Adapter

        // prevent user from swiping between pages
        // and from tab changing when you scroll past the top or bottom
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = fragmentTitle.get(position)
        }.attach()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}