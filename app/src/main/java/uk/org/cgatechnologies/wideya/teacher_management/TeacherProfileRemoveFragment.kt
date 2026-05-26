package uk.org.cgatechnologies.wideya.teacher_management

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
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.FreetextInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.RequiredDialogValidation
import uk.org.cgatechnologies.wideya.databinding.FragmentEntityRemoveBinding
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel

// TODO: data validation for all fields
private const val TAG: String = "TeacherProfileRemoveFragment"

class TeacherProfileRemoveFragment : Fragment() {

    private var _binding: FragmentEntityRemoveBinding? = null
    private val binding get() = _binding!!
    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //set teacher data
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<TeacherModel>("teacher")
            val id = bundle.getInt("resId")
            // Do something with the result
            loadTeacherDetailsInView(result as TeacherModel, listOf(id))
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

//        if(teacherManagementViewModel.isCurrentTeacherInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        setTitle()

        binding.apply {
            val inputValidations = setOf(
                RequiredDialogValidation(etEndReason, tlEndReason),
                FreetextInputValidation(etEndReasonDetail, tlEndReasonDetail, true),
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
                        "teacher.end_reason_teacher_oid",
                        it,
                        tlEndReason.hint.toString(),
                        teacherManagementViewModel.getOptionList("end_reason_teacher"),
                        childFragmentManager,
                        otherName = "disable"
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
                text = getString(R.string.remove_teacher)
                setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        removeTeacherDetailsInView()
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


    private fun loadTeacherDetailsInView(teacher: TeacherModel?, idList: List<Int> = listOf()) {
        binding.apply {
            //personal
            if (idList.isEmpty() || idList.contains(etEndReason.id)) etEndReason.setText(teacher?.end_reason_name ?: "")
            if (idList.isEmpty() || idList.contains(etEndReason.id)) etEndReason.setTag(
                R.string.idTag,
                teacher?.end_reason_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEndReasonDetail.id)) etEndReasonDetail.setText(
                teacher?.end_reason_detail ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEndDate.id)) etEndDate.setText(teacher?.end_date ?: "")

        }
    }

    fun setTitle() {
        teacherManagementViewModel.apply {
            val title = "Remove Teacher: ${this.currentTeacher.full_name}"
            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        }
    }

    private fun removeTeacherDetailsInView() {
        binding.apply {
            teacherManagementViewModel.currentTeacher.apply teacher@{

                this@teacher.end_reason_oid = etEndReason.getTag(R.string.idTag)?.toString().orEmpty()
                this@teacher.end_reason_name = etEndReason.text.toString()
                this@teacher.end_reason_detail = etEndReasonDetail.text.toString()
                this@teacher.end_date = etEndDate.text.toString()

                teacherManagementViewModel.removeTeacher(this@teacher)


            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}