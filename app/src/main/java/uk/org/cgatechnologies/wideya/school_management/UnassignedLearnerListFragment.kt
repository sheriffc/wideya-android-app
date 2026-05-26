package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.interfaces.IMultiSelectorView
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.views.Views
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementViewModel
import uk.org.cgatechnologies.wideya.learner_management.UnassignedLearnerListUiState
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

/**
 * Created by Mohamad Abuzaid on 02/02/2023.
 */

private const val TAG: String = "UnassignedLearnerListFragment"

class UnassignedLearnerListFragment : Fragment(), IMultiSelectorView {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private lateinit var args: UnassignedLearnerListFragmentArgs

    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
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
            learnerManagementViewModel.selectedGroup =
                bundle.getStringArray("fragment_data") ?: arrayOf()
            learnerManagementViewModel.assignSelectedLearnersToSchoolGroup()
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

        val bundle = requireArguments()
        args = UnassignedLearnerListFragmentArgs.fromBundle(bundle)

        setupSelectAllCheckbox()
        setupMultiSelectActions()
        initStateFlowListeners()

        binding.svMultiselect.visibility = View.VISIBLE
        binding.btAdd.visibility = View.GONE
        binding.svCommonSearchBar.visibility = View.INVISIBLE
        binding.tvPreText.visibility = View.VISIBLE
        binding.tvPreText.text = getString(R.string.unassigned_learners_fragment_explanation_text)

        learnerManagementViewModel.setUnassignedLearnerList(schoolManagementViewModel.currentSchool.uuid)

        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }
    }

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                learnerManagementViewModel.unassignedLearnerListFlow.collect { learners ->
                    when (learners) {
                        is UnassignedLearnerListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is UnassignedLearnerListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_LEARNER)
                            commonListAdapter.setDiffItems(learners.learners)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                countTV.text = learners.learners.size.toString()

                                if (learners.learners.isEmpty()) {
                                    cbSelectAll.isChecked = false
                                    cbSelectAll.visibility = View.GONE
                                } else {
                                    cbSelectAll.visibility = View.VISIBLE
                                }
                            }
                        }
                        is UnassignedLearnerListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar = Snackbar.make(
                                binding.root,
                                learners.exception.message.toString(),
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
                    if (selectedList.isNotEmpty() && selectedList.first() is LearnerAdmissionModel) {
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

    private val assignLearners: () -> Unit = {
        if (args.hasGroup) {
            confirmAction(R.string.learner_assign_to_current_group) {
                //binding.progressBar.visibility = View.VISIBLE

                learnerManagementViewModel.selectedGroup = arrayOf(
                    schoolGroupManagementViewModel.currentSchoolGroup.uuid,
                    schoolGroupManagementViewModel.currentSchoolGroup.school_group_name
                )
                learnerManagementViewModel.assignSelectedLearnersToSchoolGroup()

                findNavController().popBackStack()
            }

        } else {
            confirmAction(R.string.learner_assign_to_other_group) {
                Utils.initSelectOneWidgetNoView(
                    "learner.transfer_school_group_uuid",
                    getString(R.string.classroom_assignment),
                    schoolGroupManagementViewModel.getSchoolGroupOptionsList(args.schoolUuid),
                    childFragmentManager
                )
            }
        }
    }

    private val actionsList = listOf(assignLearners)
    override fun setupMultiSelectActions() {
        Log.d(TAG, "Setup MultiSelect Actions")

        val actions = resources.getStringArray(R.array.unassigned_learners_actions)
        actions.forEachIndexed { i, a ->
            val actionButton = Views.createActionButton(requireContext(), a)
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