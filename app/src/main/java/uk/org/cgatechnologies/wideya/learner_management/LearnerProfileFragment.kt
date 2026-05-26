package uk.org.cgatechnologies.wideya.learner_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import uk.org.cgatechnologies.wideya.common.adapters.ViewPagerAdapter
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnerProfileBinding

class LearnerProfileFragment : Fragment() {

    private var _binding: FragmentLearnerProfileBinding? = null
    private val binding get() = _binding!!

    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set learner data flow
        val bundle = requireArguments()
        val args = LearnerProfileFragmentArgs.fromBundle(bundle)
        learnerManagementViewModel.currentLearner = args.learner
        learnerManagementViewModel.setLearnerDetail(args.learner.uuid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        learnerManagementViewModel.currentDetailsMode = DetailsMode.VIEW

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager

        val fragmentList = listOf(
            LearnerProfileDetailsFragment(),
        )

        val fragmentTitle = listOf(
            "Details",
        )

        val viewPager2Adapter = ViewPagerAdapter(fragmentList, this.childFragmentManager, lifecycle)
        viewPager.adapter = viewPager2Adapter

        // prevent user from swiping between pages
        // and from tab changing when you scroll past the top or bottom
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = fragmentTitle[position]
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = learnerManagementViewModel.currentLearner.learner_full_name
            subtitle = learnerManagementViewModel.currentLearner.school_name
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}