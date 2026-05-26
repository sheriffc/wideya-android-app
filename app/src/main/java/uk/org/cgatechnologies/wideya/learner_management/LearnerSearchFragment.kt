package uk.org.cgatechnologies.wideya.learner_management

import android.os.Bundle
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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

private const val TAG: String = "LearnerSearchFragment"

class LearnerSearchFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()
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

        val commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(view.context)
            adapter = commonListAdapter
        }

        commonListAdapter.setType(CommonListAdapter.LEARNER_SEARCH)

        val currentSchoolUuid = schoolManagementViewModel.currentSchool.uuid
        learnerManagementViewModel.setLearnerSearchQuery("", currentSchoolUuid)

        commonListAdapter.onLearnerSearchItemClick = { learner ->
            learnerManagementViewModel.currentDetailsMode = DetailsMode.FROM_SEARCH
            learnerManagementViewModel.currentLearner = learner.apply {
                school_uuid = schoolManagementViewModel.currentSchool.uuid
                school_name = schoolManagementViewModel.currentSchool.name
            }
            Navigation.findNavController(binding.root).navigate(
                LearnerSearchFragmentDirections.actionLearnerSearchFragmentToLearnerProfileDetailsFragment(null)
            )
        }

        binding.countTV.isVisible = false
        binding.countLabel.isVisible = false

        binding.btAdd.isVisible = true
        binding.btAdd.text = requireContext().getString(R.string.new_learner)
        binding.btAdd.setOnClickListener {
            learnerManagementViewModel.currentDetailsMode = DetailsMode.NEW
            learnerManagementViewModel.currentLearner = LearnerAdmissionModel().apply {
                school_uuid = schoolManagementViewModel.currentSchool.uuid
                school_name = schoolManagementViewModel.currentSchool.name
            }
            Navigation.findNavController(binding.root).navigate(
                LearnerSearchFragmentDirections.actionLearnerSearchFragmentToLearnerProfileDetailsFragment(null)
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                learnerManagementViewModel.learnerSearchResults.collect { state ->
                    when (state) {
                        is LearnerSearchUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LearnerSearchUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            commonListAdapter.setItems(state.learners)
                        }
                        is LearnerSearchUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }

        with(binding.svCommonSearchBar) {
            setOnQueryTextListener(object : OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        learnerManagementViewModel.setLearnerSearchQuery(newText, currentSchoolUuid)
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    learnerManagementViewModel.setLearnerSearchQuery(query, currentSchoolUuid)
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
