package uk.org.cgatechnologies.wideya.school_management

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolLearnerAttendanceListItemBinding
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel

class SchoolLearnerAttendanceItemViewHolder(
    private val binding: ViewholderSchoolLearnerAttendanceListItemBinding,
    private val viewModel: SchoolManagementViewModel
) : RecyclerView.ViewHolder(binding.root) {
    private val presentAmSymbol = "\\"
    private val presentPmSymbol = "/"
    private val absentSymbol = "O"
    private val learnerBgColorState = R.color.chip_background_state_list_learner
    private val learnerBgColorTransaparent = R.color.chip_background_state_list_transparent


    fun bind(attendance: PersonAttendanceModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = attendance.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                //cbSelect.isChecked = attendance.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            tvLearnerName.text = (attendance.full_name ?: "NAME NOT FOUND")
            if (!attendance.school_group_uuid.isNullOrEmpty()) {
                ((attendance.school_group_level?.plus(", ") ?: "") +
                        (attendance.school_group_name ?: "")).also { tvSchoolGroup.text = it }
                tvSchoolGroup.setTextColor(ContextCompat.getColor(binding.tvSchoolGroup.context, R.color.black))
            } else {
                tvSchoolGroup.text = tvSchoolGroup.context.getString(R.string.assign_learner_to_class)
                tvSchoolGroup.setTextColor(
                    ContextCompat.getColor(
                        binding.tvSchoolGroup.context,
                        R.color.md_theme_light_error
                    )
                )
            }

            attendance.admission_number?.let {
                "${attendance.admission_number.toString()}   (Admission Num)".also { tvLearnerIdentifier.text = it }
            } ?: kotlin.run {
                tvLearnerIdentifier.text = ""
            }

            if (attendance.sex_oid.equals("male")) {
                learnerIcon.setImageResource(R.drawable.learner_male_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.blue_mid))
            } else if (attendance.sex_oid.equals("female")) {
                learnerIcon.setImageResource(R.drawable.learner_female_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.red_mid))
            } else {
                learnerIcon.setImageResource(R.drawable.learners_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.black))
            }

            var amText = " "
            var amBgColor: Int = learnerBgColorState
            var amIsChecked = false

            var pmText = " "
            var pmBgColor: Int = learnerBgColorState
            var pmIsChecked = false

            //am attendance
            when (attendance.attendance_am_status_oid) {
                Constants.ATTENDANCE_PRESENT_ID -> {
                    amText = presentAmSymbol
                    amIsChecked = true
                }
                Constants.ATTENDANCE_ABSENT_ID -> amText = absentSymbol
                else -> amBgColor = learnerBgColorTransaparent
            }

            amChip.apply {
                text = amText
                setChipBackgroundColorResource(amBgColor)
                isChecked = amIsChecked
                isEnabled = viewModel.isSelectedLearnerAttendanceCurrentDate()
                setOnClickListener {
                    attendance.attendance_am_status_oid = attendanceSelected(attendance.attendance_am_status_oid)
                    val attendanceStatus = attendanceButtonDisplay(attendance.attendance_am_status_oid, true)
                    text = attendanceStatus.symbol
                    setChipBackgroundColorResource(attendanceStatus.bgColor)
                    isChecked = attendanceStatus.isChecked
                    savePersonAttendance(attendance)
                }
            }

            //pm attendance
            when (attendance.attendance_pm_status_oid) {
                Constants.ATTENDANCE_PRESENT_ID -> {
                    pmText = presentPmSymbol
                    pmIsChecked = true
                }
                Constants.ATTENDANCE_ABSENT_ID -> pmText = absentSymbol
                else -> pmBgColor = learnerBgColorTransaparent
            }

            pmChip.apply {
                text = pmText
                setChipBackgroundColorResource(pmBgColor)
                isChecked = pmIsChecked
                isEnabled = viewModel.isSelectedLearnerAttendanceCurrentDate()
                setOnClickListener {
                    attendance.attendance_pm_status_oid = attendanceSelected(attendance.attendance_pm_status_oid)
                    val attendanceStatus = attendanceButtonDisplay(attendance.attendance_pm_status_oid, false)
                    text = attendanceStatus.symbol
                    setChipBackgroundColorResource(attendanceStatus.bgColor)
                    isChecked = attendanceStatus.isChecked
                    savePersonAttendance(attendance)
                }
            }

        }

    }

    private fun attendanceSelected(attendance: String?): String? {
        val attendanceSelected: String? = when (attendance) {
            null -> Constants.ATTENDANCE_PRESENT_ID
            Constants.ATTENDANCE_PRESENT_ID -> Constants.ATTENDANCE_ABSENT_ID
            Constants.ATTENDANCE_ABSENT_ID -> null
            else -> null
        }
        return attendanceSelected
    }

    private fun savePersonAttendance(attendance: PersonAttendanceModel) {
        if (attendance.uuid == null) {
            viewModel.insertPersonAttendance(attendance)
        } else {
            viewModel.updatePersonAttendance(attendance)
        }
    }

    private fun attendanceButtonDisplay(attendance: String?, isAm: Boolean): AttendanceStatus {
        val attendanceSymbol: String?
        val attendanceBgColor: Int
        val attendanceChecked: Boolean

        when (attendance) {
            null -> {
                attendanceSymbol = " "
                attendanceBgColor = learnerBgColorTransaparent
                attendanceChecked = false
            }
            Constants.ATTENDANCE_PRESENT_ID -> {
                attendanceSymbol = if (isAm) presentAmSymbol else presentPmSymbol
                attendanceBgColor = learnerBgColorState
                attendanceChecked = true
            }
            Constants.ATTENDANCE_ABSENT_ID -> {
                attendanceSymbol = absentSymbol
                attendanceBgColor = learnerBgColorState
                attendanceChecked = false
            }
            else -> {
                attendanceSymbol = null
                attendanceBgColor = learnerBgColorTransaparent
                attendanceChecked = false
            }
        }

        return AttendanceStatus(attendanceSymbol, attendanceBgColor, attendanceChecked)
    }

    data class AttendanceStatus(
        val symbol: String?,
        val bgColor: Int,
        val isChecked: Boolean
    )
}