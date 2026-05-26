package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementViewModel

private const val TAG: String = "SchoolLearnerAdmissionListFragment"

class SchoolLearnerAdmissionListFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()

    private lateinit var commonListAdapter: CommonListAdapter

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
        setTitle()

        initStateFlowListeners()
        setupListActionButtons()

        schoolManagementViewModel.removeLearnersWithoutUid(schoolManagementViewModel.currentSchool.uuid)
        schoolManagementViewModel.setSchoolLearnerAdmissionList(schoolManagementViewModel.currentSchool.uuid)

        commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        schoolManagementViewModel.setSchoolLearnerAdmissionListByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    schoolManagementViewModel.setSchoolLearnerAdmissionListByQuery(
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

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolLearnerAdmissionList.collect { learnerAdmissionList ->
                    when (learnerAdmissionList) {
                        is LatestSchoolLearnerAdmissionListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestSchoolLearnerAdmissionListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_LEARNER)
                            commonListAdapter.setItems(learnerAdmissionList.learnerAdmissionList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = learnerAdmissionList.learnerAdmissionList.size.toString()
                            }
                        }
                        is LatestSchoolLearnerAdmissionListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar =
                                Snackbar.make(binding.root, learnerAdmissionList.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun setupListActionButtons() {
        binding.btAdd.text = getString(R.string.learner_add_new)
        binding.btAssign.text = getString(R.string.learner_unassigned_learners)
        binding.btAssign.visibility = View.VISIBLE

        binding.btAdd.setOnClickListener {
            addNewLearner()
        }

        binding.btAssign.setOnClickListener {
            val bundle = Bundle().apply { putString("school_uuid", schoolManagementViewModel.currentSchool.uuid) }
            findNavController().navigate(R.id.UnassignedLearnerListFragment, bundle)
        }
    }

    private fun addNewLearner() {
        Navigation.findNavController(binding.root).navigate(
            SchoolProfileFragmentDirections.actionSchoolProfileFragmentToLearnerSearchFragment()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}