package uk.org.cgatechnologies.wideya.teacher_management

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
import uk.org.cgatechnologies.wideya.databinding.FragmentTeacherProfileBinding

class TeacherProfileFragment : Fragment() {

    private var _binding: FragmentTeacherProfileBinding? = null
    private val binding get() = _binding!!

    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set teacher data flow
        val bundle = requireArguments()
        val args = TeacherProfileFragmentArgs.fromBundle(bundle)
        teacherManagementViewModel.currentTeacher = args.teacher
        teacherManagementViewModel.setTeacherDetail(args.teacher.uuid)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        teacherManagementViewModel.currentDetailsMode = DetailsMode.VIEW

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
            TeacherProfileDetailsFragment(),
            TeacherTimetableFragment()
        )

        val fragmentTitle = listOf(
            "Details",
            "Timetable"
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
            title = teacherManagementViewModel.currentTeacher.full_name
            subtitle = teacherManagementViewModel.currentTeacher.school_name
        }
    }

    override fun onDestroyView() {
//        Log.d("binding", "TeacherProfile onDestroyView() called")
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG: String = "TeacherProfileFragment"
    }
}