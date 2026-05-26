package uk.org.cgatechnologies.wideya.analysis

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.school_management.LatestSchoolDetailUiState
import uk.org.cgatechnologies.wideya.school_management.LatestSchoolLearnerAdmissionListUiState
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel

class LearnerDisabilitySummaryFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val analysisViewModel by activityViewModels<AnalysisViewModel> ()
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun bindData(view: View) {
        analysisViewModel.getSchoolLearnerDisabilityReport(schoolManagementViewModel.currentSchool.uuid)

        val commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        binding.btAdd.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                analysisViewModel.learnerDisabilityReportList.collect { item ->
                    when (item) {
                        is LatestLearnerDisabilityReportListListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestLearnerDisabilityReportListListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.ANALYSIS_LEARNER_DISABILITY)
                            commonListAdapter.setItems(item.learnerDisabilityReportList)
                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = item.learnerDisabilityReportList.size.toString()
                            }
                        }
                        is LatestLearnerDisabilityReportListListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar =
                                Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        analysisViewModel.getSchoolLearnerDisabilityByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                        Log.d("learnerDisability", "onQueryTextChange: "+newText)
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    analysisViewModel.getSchoolLearnerDisabilityByQuery(
                        schoolManagementViewModel.currentSchool.uuid,
                        query
                    )
                    Log.d("learnerDisability", "onQueryTextChange: "+query)
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