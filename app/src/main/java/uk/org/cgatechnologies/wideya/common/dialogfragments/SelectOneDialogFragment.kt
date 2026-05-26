package uk.org.cgatechnologies.wideya.common.dialogfragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.DialogFragmentSelectOneBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementViewModel
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementViewModel
import java.util.*
import kotlin.properties.Delegates

class SelectOneDialogFragment : DialogFragment() {
    private var _binding: DialogFragmentSelectOneBinding? = null
    private val binding get() = _binding!!
    private lateinit var propertyName: String
    private lateinit var otherName: String
    private lateinit var currentName: String
    private lateinit var currentId: String
    private var viewId by Delegates.notNull<Int>()
    private lateinit var includeClearButton: String // TODO: this should be Boolean but not allowed with lateinit...

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()
    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()
    lateinit var personAttendanceModel: PersonAttendanceModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        _binding = DialogFragmentSelectOneBinding.inflate(layoutInflater)
        binding.etOther.isEnabled = false
        binding.etOther.visibility = View.GONE

        if (arguments != null) {
            val mArgs = arguments
            binding.tvTitle.text = mArgs?.getString("title") ?: ""
            propertyName = mArgs?.getString("propertyName") ?: ""
            otherName = mArgs?.getString("otherName") ?: ""
            currentName = mArgs?.getString("currentName") ?: ""
            currentId = mArgs?.getString("currentId") ?: ""
            viewId = mArgs?.getInt("viewId") ?: 0
            includeClearButton = mArgs?.getString("includeClearButton") ?: "yes"

            propertyName.let {
                if (it == "attendance.absent_reason_oid") {
                    personAttendanceModel = mArgs?.getParcelable("item")!!
                }
            }


            val listOfOptions: ArrayList<OptionList> = mArgs?.getParcelableArrayList("list")!!

            var preChecked = -1

            listOfOptions.forEachIndexed { index, element ->
                val button = RadioButton(requireContext())
                val buttonLayoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
                buttonLayoutParams.setMargins(0, 8, 0, 8)
                button.layoutParams = buttonLayoutParams
                button.id = index
                button.setTag(R.string.idTag, element.item_id)
                button.text = element.item_name
                button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

                if (element.item_id.equals(currentId)) {
                    preChecked = index
                    if (otherName != "disable" &&
                        element.item_name?.trim()?.lowercase()?.startsWith("other") == true
                    ) {
                        binding.etOther.setText(otherName)
                        binding.etOther.isEnabled = true
                        binding.etOther.visibility = View.VISIBLE
                    }
                }
                binding.radioGroup.addView(button)
            }
            binding.radioGroup.check(preChecked)
        }

        binding.radioGroup.setOnCheckedChangeListener { group, id ->
            val rb: RadioButton = group.findViewById(id)
            if (otherName != "disable" &&
                rb.text.toString().trim().lowercase().startsWith("other")
            ) {
                binding.etOther.isEnabled = true
                binding.etOther.visibility = View.VISIBLE
            } else {
                binding.etOther.isEnabled = false
                binding.etOther.visibility = View.GONE
            }
        }

        binding.btnNegative.setOnClickListener {
            dismiss()
        }

        if (includeClearButton === "yes") {
            binding.btnClear.setOnClickListener {
                Log.d(TAG, "clear answer")
                saveAnswer(true)
                dismiss()
            }
        } else {
            binding.btnClear.isVisible = false
        }

        binding.btnPositive.setOnClickListener {
            Log.d(TAG, "save answer")
            saveAnswer(false)
            dismiss()
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
        return builder.setView(binding.root).create()
    }

    private fun saveAnswer(wipeField: Boolean = false) {
        val radioButtonID: Int = binding.radioGroup.checkedRadioButtonId
        val radioButton = binding.radioGroup.findViewById<View>(radioButtonID) as RadioButton?
        if (radioButton == null) {
            dismiss()
            return
        }

        val selectedText: String?
        val selectedTag: String?

        if (wipeField) {
            selectedText = null
            selectedTag = null
        } else {
            selectedText = radioButton.text as String
            selectedTag = radioButton.getTag(R.string.idTag) as String
        }

        var otherText = ""
        if (otherName != "disable" &&
            selectedText?.trim { it <= ' ' }?.lowercase()?.startsWith("other") == true
        ) {
            otherText = binding.etOther.text.toString()
        }

        when (propertyName) {
            //SCHOOL
            "school.school_education_level_oid" -> {
                schoolManagementViewModel.currentSchool.education_level_oid = selectedTag
                schoolManagementViewModel.currentSchool.education_level_name = selectedText
                sendBundleHomeSchool(R.id.etEducationLevel)
            }
            "school.district_office_uuid" -> {
                schoolManagementViewModel.currentSchool.district_office_uuid = selectedTag
                schoolManagementViewModel.currentSchool.district_office_name = selectedText
                sendBundleHomeSchool(R.id.etDistrictOffice)
            }
            //TEACHER
            "teacher.sex_oid" -> {
                teacherManagementViewModel.currentTeacher.sex_oid = selectedTag
                teacherManagementViewModel.currentTeacher.sex_name = selectedText
                sendBundleHomeTeacher(viewId)
            }
            "teacher.employment_status_oid" -> {
                teacherManagementViewModel.currentTeacher.employment_status_oid = selectedTag
                teacherManagementViewModel.currentTeacher.employment_status_name = selectedText
                sendBundleHomeTeacher(viewId)
            }
            "teacher.teacher_role_oid" -> {
                teacherManagementViewModel.currentTeacher.teacher_role_oid = selectedTag
                teacherManagementViewModel.currentTeacher.teacher_role_name = selectedText
                teacherManagementViewModel.currentTeacher.teacher_role_other = otherText
                sendBundleHomeTeacher(viewId)
            }
            "teacher.end_reason_teacher_oid" -> {
                teacherManagementViewModel.currentTeacher.end_reason_oid = selectedTag
                teacherManagementViewModel.currentTeacher.end_reason_name = selectedText
                sendBundleHomeTeacher(viewId)
            }
            "school_group.school_group_level_oid" -> {
                schoolGroupManagementViewModel.currentSchoolGroup.school_group_level_oid = selectedTag
                schoolGroupManagementViewModel.currentSchoolGroup.school_group_level_name = selectedText
                sendBundleHomeSchoolGroup(viewId)
            }
            "school_group.teacher_uuid" -> {
                schoolGroupManagementViewModel.currentSchoolGroup.teacher_uuid = selectedTag
                selectedText?.split(" - ")?.apply {
                    schoolGroupManagementViewModel.currentSchoolGroup.teacher_full_name = first()
                    schoolGroupManagementViewModel.currentSchoolGroup.teacher_pin = last()
                } ?: kotlin.run {
                    schoolGroupManagementViewModel.currentSchoolGroup.teacher_full_name = null
                    schoolGroupManagementViewModel.currentSchoolGroup.teacher_pin = null
                }

                sendBundleHomeSchoolGroup(viewId)

            }
            "attendance.absent_reason_oid" -> {
                personAttendanceModel.attendance_status_oid = Constants.ATTENDANCE_ABSENT_ID
                personAttendanceModel.absent_reason_oid = selectedTag
                personAttendanceModel.absent_reason_other = otherText
                if (personAttendanceModel.uuid == null) {
                    schoolManagementViewModel.insertPersonAttendance(personAttendanceModel)
                } else {
                    schoolManagementViewModel.updatePersonAttendance(personAttendanceModel)
                }
            }
            //TIMETABLE
            "timetable.school_subject_oid" -> {
                teacherManagementViewModel.currentTimetableEntry.school_subject_oid = selectedTag
                teacherManagementViewModel.currentTimetableEntry.school_subject_name = selectedText
                teacherManagementViewModel.currentTimetableEntry.school_subject_other = otherText
                sendBundleHomeTimetable(viewId)
            }
            "timetable.day_of_the_week_oid" -> {
                teacherManagementViewModel.currentTimetableEntry.day_of_the_week_oid = selectedTag
                teacherManagementViewModel.currentTimetableEntry.day_of_the_week_name = selectedText
                sendBundleHomeTimetable(viewId)
            }
            //LEARNER
            "learner.sex_oid" -> {
                learnerManagementViewModel.currentLearner.learner_sex_oid = selectedTag
                learnerManagementViewModel.currentLearner.learner_sex_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.end_reason_learner_oid" -> {
                learnerManagementViewModel.currentLearner.end_reason_learner_oid = selectedTag
                learnerManagementViewModel.currentLearner.end_reason_learner_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.enrolment_school_group_uuid" -> {
                learnerManagementViewModel.currentLearner.enrolment_school_group_uuid = selectedTag
                learnerManagementViewModel.currentLearner.enrolment_school_group_concat_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.transfer_school_group_uuid" -> {
                sendBundleFragmentData(arrayOf(selectedTag, selectedText))
            }
            "learner.language_oid_strongest" -> {
                learnerManagementViewModel.currentLearner.language_oid_strongest = selectedTag
                learnerManagementViewModel.currentLearner.language_oid_strongest_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_maternal_status_oid" -> {
                learnerManagementViewModel.currentLearner.learner_maternal_status_oid = selectedTag
                learnerManagementViewModel.currentLearner.learner_maternal_status_name = selectedText
                learnerManagementViewModel.currentLearner.learner_maternal_status_updated_at = Utils.getISODateTimeUTC()
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_vision" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_vision = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_vision_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_hearing" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_hearing = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_hearing_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_mobility" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_mobility = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_mobility_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_cognition" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_cognition = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_cognition_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_selfcare" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_selfcare = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_selfcare_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_severity_oid_communication" -> {
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_communication = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_severity_oid_communication_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.learner_disability_other_condition_oid" -> {
                learnerManagementViewModel.currentLearner.learner_disability_other_condition_oid = selectedTag
                learnerManagementViewModel.currentLearner.learner_disability_other_condition_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.guardian_relation_to_learner_oid" -> {
                learnerManagementViewModel.currentLearner.guardian_relation_to_learner_oid = selectedTag
                learnerManagementViewModel.currentLearner.guardian_relation_to_learner_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            "learner.guardian_sex_oid" -> {
                learnerManagementViewModel.currentLearner.guardian_sex_oid = selectedTag
                learnerManagementViewModel.currentLearner.guardian_sex_name = selectedText
                sendBundleHomeLearner(viewId)
            }
            else -> return
        }
    }

    private fun sendBundleHomeSchool(id: Int) {
        //https://developer.android.com/guide/fragments/communicate
        val bundle = Bundle()
//        val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
        bundle.putParcelable("school", schoolManagementViewModel.currentSchool)
        bundle.putInt("resId", id)
        setFragmentResult("requestKey", bundle)
    }

    private fun sendBundleHomeTeacher(id: Int) {
        val bundle = Bundle()
//        val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
        bundle.putParcelable("teacher", teacherManagementViewModel.currentTeacher)
        bundle.putInt("resId", id)
        setFragmentResult("requestKey", bundle)
    }

    private fun sendBundleHomeTimetable(id: Int) {
        val bundle = Bundle()
        bundle.putParcelable("timetable", teacherManagementViewModel.currentTimetableEntry)
        bundle.putInt("resId", id)
        setFragmentResult("requestKey", bundle)
    }

    private fun sendBundleHomeSchoolGroup(id: Int) {
        val bundle = Bundle()
        bundle.putParcelable("schoolGroup", schoolGroupManagementViewModel.currentSchoolGroup)
        bundle.putInt("resId", id)
        setFragmentResult("requestKey", bundle)
    }

    private fun sendBundleHomeLearner(id: Int) {
        val bundle = Bundle()
        bundle.putParcelable("learner", learnerManagementViewModel.currentLearner)
        bundle.putInt("resId", id)
        setFragmentResult("requestKey", bundle)
    }

    private fun sendBundleFragmentData(data: Array<String?>) {
        val bundle = Bundle()
        bundle.putStringArray("fragment_data", data)
        setFragmentResult("requestKey", bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SelectOneWidget"
    }
}