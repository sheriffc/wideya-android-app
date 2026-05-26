package uk.org.cgatechnologies.wideya.teacher_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel

private const val TAG: String = "NonPayrollListFragment"

class TeacherNonPayrollListFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        teacherManagementViewModel.setNonPayrollTeacherList(schoolManagementViewModel.currentSchool.uuid)

        val commonListAdapter = CommonListAdapter()
        commonListAdapter.onPayrollTeacherItemClick = { teacher ->
            findNavController().navigate(
                TeacherNonPayrollListFragmentDirections
                    .actionTeacherNonPayrollListFragmentToTeacherProfileDetailsFragment(teacher)
            )
        }
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                teacherManagementViewModel.nonPayrollTeacherList.collect { state ->
                    when (state) {
                        is LatestNonPayrollTeacherListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestNonPayrollTeacherListUiState.Success -> {
                            Log.d(TAG, "load list")
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.PAYROLL_TEACHER)
                            commonListAdapter.setItems(state.nonPayrollTeacherList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                countTV.text = state.nonPayrollTeacherList.size.toString()
                            }
                        }
                        is LatestNonPayrollTeacherListUiState.Error -> {
                            Log.d(TAG, "error state")
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        // "Add blank" — lets the user enter a non-payroll teacher not in the TSCTMIS list
        binding.btAdd.isVisible = true
        binding.btAdd.setOnClickListener {
            findNavController().navigate(
                TeacherNonPayrollListFragmentDirections
                    .actionTeacherNonPayrollListFragmentToTeacherProfileDetailsFragment(
                        TeacherPayrollModel(
                            uuid = "", first_name = "", middle_name = "", last_name = "",
                            full_name = "", sex = "", date_of_birth = "", age = null,
                            pin = "", nin = "", nassit_number = "", created_at = "", updated_at = ""
                        )
                    )
            )
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        teacherManagementViewModel.setNonPayrollTeacherListByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    teacherManagementViewModel.setNonPayrollTeacherListByQuery(
                        schoolManagementViewModel.currentSchool.uuid,
                        query
                    )
                    return true
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
