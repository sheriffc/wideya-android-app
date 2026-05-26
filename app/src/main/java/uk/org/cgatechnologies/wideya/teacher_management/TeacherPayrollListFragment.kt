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

private const val TAG: String = "PayrollListFragment"

class TeacherPayrollListFragment : Fragment() {

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

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        teacherManagementViewModel.setPayrollTeacherList(schoolManagementViewModel.currentSchool.uuid)

        val commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                teacherManagementViewModel.payrollTeacherList.collect { payrollTeacherList ->
                    when (payrollTeacherList) {
                        is LatestPayrollTeacherListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestPayrollTeacherListUiState.Success -> {
                            Log.d(TAG, "load list")
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.PAYROLL_TEACHER)
                            commonListAdapter.setItems(payrollTeacherList.payrollTeacherList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = payrollTeacherList.payrollTeacherList.size.toString()
                            }
                        }
                        is LatestPayrollTeacherListUiState.Error -> {
                            Log.d(TAG, "error state")
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        binding.btAdd.isVisible = false

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        teacherManagementViewModel.setPayrollTeacherListByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    teacherManagementViewModel.setPayrollTeacherListByQuery(
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