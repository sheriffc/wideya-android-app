package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.ViewPagerAdapter
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolProfileBinding

private const val TAG: String = "SchoolProfileFragment"

class SchoolProfileFragment : Fragment() {

    private var _binding: FragmentSchoolProfileBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private var feedingTabEnabled = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        schoolManagementViewModel.currentDetailsMode = DetailsMode.VIEW

        Utils.cancelBackButton(activity, viewLifecycleOwner)

        // set school data flow
        setSchoolDetails()

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager

        // NOTE: if changing the tab order, you also need to update the onCreate() function
        // which dynamically reloads the tab fragments with the counts of teachers/learners/etc

        val fragmentList = listOf(
            SchoolProfileDetailsFragment(),
            SchoolTeacherListFragment(),
            SchoolGroupListFragment(),
            SchoolLearnerAdmissionListFragment(),
            SchoolFeedingFragment(),
        )

        val fragmentTitle = listOf(
            "School",
            "Teachers",
            "Classrooms",
            "Learners",
            "Feeding",
        )

        val viewPager2Adapter = ViewPagerAdapter(fragmentList, this.childFragmentManager, lifecycle)
        viewPager.adapter = viewPager2Adapter

        // prevent user from swiping between pages
        // and from tab changing when you scroll past the top or bottom
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = fragmentTitle[position]
        }.attach()

        val feedingTabIndex = fragmentTitle.indexOf("Feeding")

        // Disable the Feeding tab immediately — enabled only once school data confirms YES
        tabLayout.getTabAt(feedingTabIndex)?.view?.let {
            it.isEnabled = false
            it.alpha = 0.4f
        }

        // Observe school detail and update Feeding tab enabled state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolDetail.collect { state ->
                    if (state is LatestSchoolDetailUiState.Success) {
                        feedingTabEnabled = state.school?.receives_feeding == 1
                        tabLayout.getTabAt(feedingTabIndex)?.view?.let {
                            it.isEnabled = feedingTabEnabled
                            it.alpha = if (feedingTabEnabled) 1.0f else 0.4f
                        }
                        if (!feedingTabEnabled && viewPager.currentItem == feedingTabIndex) {
                            viewPager.setCurrentItem(0, false)
                        }
                    }
                }
            }
        }
    }

    private fun setSchoolDetails() {
        Log.d(TAG, "Check School Permission")
        val schoolUuid = Utils.getEncSharedPrefs(requireContext()).getString(Constants.PREF_SCHOOL_ID, null)
        val schoolListString =
            Utils.getEncSharedPrefs(requireContext()).getString(Constants.PREF_SCOPE_SCHOOLS, "").toString()
        val schoolList = schoolListString.split(",").map { it.trim() }

        if (schoolUuid.isNullOrEmpty() || schoolList.contains(schoolUuid).not()) {
            findNavController().navigate(
                R.id.SchoolListFragment, null, NavOptions.Builder()
                    .setPopUpTo(
                        findNavController().graph.startDestinationId, true
                    )
                    .build()
            )
        } else {
            schoolManagementViewModel.setSchoolDetail(schoolUuid)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}