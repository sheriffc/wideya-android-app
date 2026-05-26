package uk.org.cgatechnologies.wideya.school_group_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import uk.org.cgatechnologies.wideya.common.adapters.ViewPagerAdapter
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolGroupProfileBinding

class SchoolGroupProfileFragment : Fragment() {

    private var _binding: FragmentSchoolGroupProfileBinding? = null
    private val binding get() = _binding!!

    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set schoolGroup data flow
        val bundle = requireArguments()
        val args = SchoolGroupProfileFragmentArgs.fromBundle(bundle)
        schoolGroupManagementViewModel.currentSchoolGroup = args.schoolGroup
        schoolGroupManagementViewModel.setSchoolGroupDetail(args.schoolGroup.uuid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolGroupProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        schoolGroupManagementViewModel.currentDetailsMode = DetailsMode.VIEW

        //show delete button
        //https://stackoverflow.com/questions/71917856/sethasoptionsmenuboolean-unit-is-deprecated-deprecated-in-java
        //https://developer.android.com/jetpack/androidx/releases/activity#1.4.0-alpha01

//        val menuHost: MenuHost = requireActivity()
//        menuHost.addMenuProvider(object : MenuProvider {
//            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
//                menuInflater.inflate(R.menu.menu_main, menu)
//                menu.findItem(R.id.menuDeleteItem).isVisible = true
//            }
//            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
//                return when (menuItem.itemId) {
//                    R.id.menuDeleteItem ->  {
//                        confirmDelete()
//                        true
//                    }
//                    else -> {
//                        false
//                    }
//                }
//            }
//        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        tabLayout = binding.tabLayout
        viewPager = binding.viewPager

        val fragmentList = listOf(
            SchoolGroupProfileDetailsFragment(),
            SchoolGroupLearnerListFragment(),
        )

        val fragmentTitle = listOf(
            "Details",
            "Learners",
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
            title = schoolGroupManagementViewModel.currentSchoolGroup.school_name
            subtitle =
                ((schoolGroupManagementViewModel.currentSchoolGroup.school_group_level_name ?: "")
                        + " " + (schoolGroupManagementViewModel.currentSchoolGroup.school_group_name ?: ""))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun confirmDelete() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setMessage("Delete this schoolGroup?")
            .setTitle("Confirmation")

        builder.setPositiveButton("OK") { _, _ ->
            //delete
//            schoolGroupManagementViewModel.deleteSchoolGroup(schoolGroupManagementViewModel.currentSchoolGroup)
            Navigation.findNavController(binding.root).navigateUp()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // User cancelled the dialog
        }
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        private const val TAG: String = "SchoolGroupProfileFragment"
    }
}