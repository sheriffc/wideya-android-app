package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementViewModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel

private const val TAG: String = "SchoolTeacherListFragment"

class SchoolTeacherListFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("binding", "TeacherList onCreate() called")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("binding", "TeacherList onViewCreated() called")
        super.onViewCreated(view, savedInstanceState)

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolDetail.collect { item ->
                    when (item) {
                        is LatestSchoolDetailUiState.Success -> {
                            item.school?.let {
                                bindData(view)
                            }
                        }
                        is LatestSchoolDetailUiState.Error -> {
                            Snackbar.make(binding.root, item.exception.message.toString(), 5000).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindData(view: View){
        setTitle()

        schoolManagementViewModel.setSchoolTeacherList(schoolManagementViewModel.currentSchool.uuid)

        val commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolTeacherList.collect { teacherList ->
                    when (teacherList) {
                        is LatestSchoolTeacherListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestSchoolTeacherListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_TEACHER)
                            commonListAdapter.setItems(teacherList.teacherList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = teacherList.teacherList.size.toString()
                            }
                        }
                        is LatestSchoolTeacherListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar = Snackbar.make(binding.root, teacherList.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        binding.btAdd.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())

            builder.setMessage("Choose whether to add a payroll teacher (search for them on the payroll list) or a non-payroll teacher (enter their details from empty)")
                .setTitle("Add Teacher to School")

            builder.setNeutralButton("Cancel") { _, _ ->
                // User cancelled the dialog
            }

            builder.setPositiveButton("Non-Payroll") { _, _ ->
                teacherManagementViewModel.currentDetailsMode = DetailsMode.NEW
                teacherManagementViewModel.currentTeacher = TeacherModel()
                teacherManagementViewModel.currentTeacher.apply {
                    school_uuid = schoolManagementViewModel.currentSchool.uuid
                    school_name = schoolManagementViewModel.currentSchool.name
                }
                Navigation.findNavController(binding.root).navigate(
                    SchoolProfileFragmentDirections.actionSchoolProfileFragmentToTeacherNonPayrollListFragment()
                )
            }

            builder.setNegativeButton("Payroll") { _, _ ->
                teacherManagementViewModel.currentDetailsMode = DetailsMode.NEW
                teacherManagementViewModel.currentTeacher = TeacherModel()
                teacherManagementViewModel.currentTeacher.apply {
                    school_uuid = schoolManagementViewModel.currentSchool.uuid
                    school_name = schoolManagementViewModel.currentSchool.name
                }
                Navigation.findNavController(binding.root)
                    .navigate(SchoolProfileFragmentDirections.actionSchoolProfileFragmentToTeacherPayrollListFragment())
            }

            val dialog = builder.create()

            dialog.show()
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        schoolManagementViewModel.setSchoolTeacherListByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    schoolManagementViewModel.setSchoolTeacherListByQuery(
                        schoolManagementViewModel.currentSchool.uuid,
                        query
                    )
                    return true
                }
            })
        }
    }

    fun setTitle() {
        schoolManagementViewModel.apply {
            (requireActivity() as AppCompatActivity).supportActionBar?.apply {
                title = schoolManagementViewModel.currentSchool.name
                subtitle = null
            }
        }
    }

    override fun onDestroyView() {
        Log.d("binding", "TeacherList onDestroyView() called")
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        Log.d("binding", "TeacherList onDestroy() called")
        super.onDestroy()
    }

}
