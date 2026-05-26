package uk.org.cgatechnologies.wideya.school_group_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.children
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
import uk.org.cgatechnologies.wideya.common.interfaces.IMultiSelectorView
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.views.Views
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementViewModel
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

private const val TAG: String = "SchoolGroupLearnerListFragment"

class SchoolGroupLearnerListFragment : Fragment(), IMultiSelectorView {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()
    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()

    private val commonListAdapter by lazy {
        CommonListAdapter(true).apply {
            setLearnerManagementViewModel(learnerManagementViewModel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            //binding.progressBar.visibility = View.VISIBLE

            learnerManagementViewModel.selectedGroup =
                bundle.getStringArray("fragment_data") ?: arrayOf()
            learnerManagementViewModel.transferSelectedLearnersToSchoolGroup()
        }
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
        super.onViewCreated(view, savedInstanceState)

        setupSelectAllCheckbox()
        setupListActionButtons()
        setupMultiSelectActions()
        initStateFlowListeners()
        binding.svMultiselect.visibility = View.VISIBLE

        schoolGroupManagementViewModel.currentSchoolGroup.apply {
            schoolGroupManagementViewModel.setSchoolGroupLearnerList(
                uuid,
                academic_year ?: Utils.getAcademicYear().toShort()
            )
        }

        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        schoolGroupManagementViewModel.currentSchoolGroup.apply {
                            schoolGroupManagementViewModel.setSchoolGroupLearnerListByQuery(
                                uuid,
                                academic_year ?: Utils.getAcademicYear().toShort(), newText
                            )
                        }
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {

                    schoolGroupManagementViewModel.currentSchoolGroup.apply {
                        schoolGroupManagementViewModel.setSchoolGroupLearnerListByQuery(
                            uuid,
                            academic_year ?: Utils.getAcademicYear().toShort(),
                            query
                        )
                    }
                    return true
                }
            })
        }
    }

    fun setTitle() {
        with(schoolGroupManagementViewModel) {
            val title = ((this.currentSchoolGroup.school_group_level_name ?: "")
                    + " " + (this.currentSchoolGroup.school_group_name ?: ""))
            val subtitle = this.currentSchoolGroup.school_name

            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
            (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = subtitle
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle()
    }

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolGroupManagementViewModel.schoolGroupLearnerList.collect { learnerList ->
                    when (learnerList) {
                        is LatestSchoolGroupLearnerListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestSchoolGroupLearnerListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_GROUP_LEARNER)
                            commonListAdapter.setDiffItems(learnerList.schoolGroupLearnerList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                countTV.text = learnerList.schoolGroupLearnerList.size.toString()

                                if (learnerList.schoolGroupLearnerList.isEmpty()) {
                                    cbSelectAll.isChecked = false
                                    cbSelectAll.visibility = View.GONE
                                } else {
                                    cbSelectAll.visibility = View.VISIBLE
                                }
                            }
                        }
                        is LatestSchoolGroupLearnerListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar = Snackbar.make(
                                binding.root,
                                learnerList.exception.message.toString(),
                                5000
                            )
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                learnerManagementViewModel.multiSelectStateFlow.collect { selectedList ->
                    if (selectedList.isNotEmpty() && selectedList.first() is SchoolGroupLearnerModel) {
                        enableMultiselectActionButtons(true)
                        binding.cbSelectAll.isChecked =
                            (selectedList.size == commonListAdapter.itemCount)

                        commonListAdapter.checkSelectedItems(selectedList)
                    } else {
                        enableMultiselectActionButtons(false)
                        learnerManagementViewModel.clearMultiSelectStateFlow()
                        commonListAdapter.updateAllSelection(false)
                    }
                }
            }
        }
    }

    override fun setupSelectAllCheckbox() {
        binding.cbSelectAll.setOnCheckedChangeListener { cb, checked ->
            if (cb.isPressed) {
                commonListAdapter.updateAllSelection(checked)
            }
        }
    }

    private fun setupListActionButtons() {
        binding.btAdd.text = getString(R.string.learner_add_new)
        binding.btAssign.text = getString(R.string.learner_add_unassigned_learners)
        binding.btAssign.visibility = View.VISIBLE

        binding.btAdd.setOnClickListener {
            addNewLearner()
        }

        binding.btAssign.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("has_group", true) }
            findNavController().navigate(R.id.UnassignedLearnerListFragment, bundle)
        }
    }

    private val unassignLearnersAction: () -> Unit = {
        confirmAction(R.string.learner_remove_from_group) {
            //binding.progressBar.visibility = View.VISIBLE
            learnerManagementViewModel.removeSelectedLearnersFromSchoolGroup()
        }
    }

    private val transferLearnersAction: () -> Unit = {
        confirmAction(R.string.learner_transfer_to_other_group) {
            Utils.initSelectOneWidgetNoView(
                "learner.transfer_school_group_uuid",
                getString(R.string.classroom_assignment),
                schoolGroupManagementViewModel.getSchoolGroupOptionsList(
                    schoolGroupManagementViewModel.currentSchoolGroup.school_uuid
                ),
                childFragmentManager
            )
        }
    }

    private val actionsList = listOf(unassignLearnersAction, transferLearnersAction)

    override fun setupMultiSelectActions() {
        Log.d(TAG, "Setup MultiSelect Actions")

        val actions = resources.getStringArray(R.array.school_group_actions)
        actions.forEachIndexed { i, buttonText ->
            val actionButton = Views.createActionButton(requireContext(), buttonText)
            actionButton.setOnClickListener { actionsList[i]() }

            binding.llMultiselect.addView(actionButton)
        }
    }

    private fun enableMultiselectActionButtons(enabled: Boolean) {
        binding.llMultiselect.children.forEach {
            val button = it as AppCompatButton
            if (enabled) {
                button.isEnabled = true
                button.background.alpha = 255
            } else {
                button.isEnabled = false
                button.background.alpha = 75
            }
        }
    }

    private fun addNewLearner() {
        learnerManagementViewModel.currentDetailsMode = DetailsMode.NEW
        learnerManagementViewModel.currentLearner = LearnerAdmissionModel()
        learnerManagementViewModel.currentLearner.apply {
            school_uuid = schoolGroupManagementViewModel.currentSchoolGroup.school_uuid
            school_name = schoolGroupManagementViewModel.currentSchoolGroup.school_name
        }
        Navigation.findNavController(binding.root).navigate(
            SchoolGroupProfileFragmentDirections.actionSchoolGroupProfileFragmentToLearnerProfileDetailsFragment(
                schoolGroupManagementViewModel.currentSchoolGroup.uuid
            )
        )
    }

    private fun confirmAction(@StringRes message: Int, confirmedAction: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())

        builder.setMessage(message)
            .setTitle(R.string.confirm)

        builder.setPositiveButton(R.string.confirm) { _, _ ->
            confirmedAction()
        }
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        learnerManagementViewModel.clearMultiSelectStateFlow()
    }
}