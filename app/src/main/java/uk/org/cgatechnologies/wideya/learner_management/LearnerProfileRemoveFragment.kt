package uk.org.cgatechnologies.wideya.learner_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.RequiredDialogValidation
import uk.org.cgatechnologies.wideya.databinding.FragmentEntityRemoveBinding
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

private const val TAG: String = "LearnerProfileRemoveFragment"

class LearnerProfileRemoveFragment : Fragment() {

    private var _binding: FragmentEntityRemoveBinding? = null
    private val binding get() = _binding!!
    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set teacher data
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<LearnerAdmissionModel>("learner")
            val id = bundle.getInt("resId")
            // Do something with the result
            loadLearnerDetailsInView(result as LearnerAdmissionModel, listOf(id))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEntityRemoveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if(learnerManagementViewModel.isCurrentLearnerInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        setTitle()

        binding.apply {
            val inputValidations = setOf(
                RequiredDialogValidation(etEndReason, tlEndReason),
            )
            validationsArray.clear()
            validationsArray.addAll(inputValidations)
        }

        binding.apply {
            tlEndReason.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
            etEndReason.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.end_reason_learner_oid",
                        it,
                        tlEndReason.hint.toString(),
                        learnerManagementViewModel.getOptionList("end_reason_learner"),
                        childFragmentManager
                    )
                }
            }

            tlEndReasonDetail.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))

            tlEndDate.setBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
            etEndDate.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 0)
                }
            }

            ValidationUtils.addLiveValidation(validationsArray)

            removeBtn.apply {
                text = getString(R.string.remove_learner)
                setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        removeLearnerDetailsInView()
                        goBack()
                        goBack()
                    }
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        setTitle()

    }

    private fun goBack() {
        Navigation.findNavController(binding.root).navigateUp()
    }

    private fun loadLearnerDetailsInView(learner: LearnerAdmissionModel?, idList: List<Int> = listOf()) {
        binding.apply {
            //personal
            if (idList.isEmpty() || idList.contains(etEndReason.id)) etEndReason.setText(learner?.end_reason_learner_name ?: "")
            if (idList.isEmpty() || idList.contains(etEndReason.id)) etEndReason.setTag(
                R.string.idTag,
                learner?.end_reason_learner_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEndDate.id)) etEndReasonDetail.setText(
                learner?.end_reason_learner_detail ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEndDate.id)) etEndDate.setText(learner?.end_date ?: "")

        }
    }

    fun setTitle() {
        learnerManagementViewModel.apply {
            val title = "Remove Learner: ${this.currentLearner.learner_full_name}"
            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        }
    }

    private fun removeLearnerDetailsInView() {
        binding.apply {
            learnerManagementViewModel.currentLearner.apply learner@{

                this@learner.end_reason_learner_oid = etEndReason.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.end_reason_learner_name = etEndReason.text.toString()
                this@learner.end_reason_learner_detail = etEndReasonDetail.text.toString()
                this@learner.end_date = etEndDate.text.toString()

                learnerManagementViewModel.removeLearnerFromSchool(this@learner)

            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}