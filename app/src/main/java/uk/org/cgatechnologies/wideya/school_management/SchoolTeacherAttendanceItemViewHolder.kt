package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolTeacherAttendanceListItemBinding
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel

class SchoolTeacherAttendanceItemViewHolder(
    private val binding: ViewholderSchoolTeacherAttendanceListItemBinding,
    private val viewModel: SchoolManagementViewModel,
    private val fragmentManager: FragmentManager
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(attendance: PersonAttendanceModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = attendance.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                //cbSelect.isChecked = attendance.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            var isPresent = false
            var isAbsent = false
            var isLate = false

            tvTeacherName.text = attendance.full_name ?: "[Name N/A]"
            if (attendance.pin.isNullOrEmpty()) {
                tvTeacherPin.text = "PIN: N/A"
            } else {
                tvTeacherPin.text = "PIN: ${attendance.pin}"
            }

            if (attendance.absent_reason_oid != null) {
                tvAbsentReason.apply {
                    visibility = View.VISIBLE
                    text = if (attendance.absent_reason_other.isNullOrEmpty()) attendance.absent_reason_name
                    else attendance.absent_reason_other
                }
            } else {
                tvAbsentReason.apply {
                    visibility = View.GONE
                    text = ""
                }
            }

            val portraitData = attendance.portrait_base64_data
            val bitmap = if (!portraitData.isNullOrEmpty())
                Utils.decodeBase64StringToBitmap(portraitData) else null
            if (bitmap != null) {
                teacherIcon.setImageBitmap(bitmap)
                teacherIcon.colorFilter = null
            } else {
                if (attendance.sex_oid == "male") {
                    teacherIcon.setImageResource(R.drawable.person_male_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.blue_mid))
                } else if (attendance.sex_oid == "female") {
                    teacherIcon.setImageResource(R.drawable.person_female_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.red_mid))
                } else {
                    teacherIcon.setImageResource(R.drawable.persons_2_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.black))
                }
            }

            attendance.attendance_status_oid?.let {
                when (it) {
                    Constants.ATTENDANCE_PRESENT_ID -> isPresent = true
                    Constants.ATTENDANCE_ABSENT_ID -> isAbsent = true
                    Constants.ATTENDANCE_LATE_ID -> isLate = true
                }
            }

            if (!isAbsent && !attendance.biometric_method_oid.isNullOrEmpty()) {
                biometricIcon.visibility = View.VISIBLE
                when (attendance.biometric_method_oid) {
                    Constants.BIOMETRIC_METHOD_PHOTO_ID -> {
                        biometricIcon.setImageResource(R.drawable.ic_account_circle_black_24dp)
                    }
                    Constants.BIOMETRIC_METHOD_FINGERPRINT_ID -> biometricIcon.setImageResource(R.drawable.ic_fingerprint_black_24dp)
                    else -> biometricIcon.visibility = View.GONE
                }
            } else {
                biometricIcon.visibility = View.GONE
            }


            cPresent.apply {
                isEnabled = viewModel.isSelectedTeacherAttendanceCurrentDate()
                isChecked = isPresent
                setOnClickListener {
                    setTeacherAttendanceRecyclerPosition()
                    if (attendance.biometric_reference.isNullOrEmpty()) {
                        promptBiometricVerification(attendance, "present")
                        isChecked = isPresent
                    } else {
                        val attendanceID =
                            if (isChecked) Constants.ATTENDANCE_PRESENT_ID else null
                        savePersonAttendance(attendance, attendanceID)
                    }
                }
            }

            cLate.apply {
                isEnabled = viewModel.isSelectedTeacherAttendanceCurrentDate()
                isChecked = isLate
                setOnClickListener {
                    setTeacherAttendanceRecyclerPosition()
                    if (attendance.biometric_reference.isNullOrEmpty()) {
                        promptBiometricVerification(attendance, "late")
                        isChecked = isLate
                    } else {
                        val attendanceID = if (isChecked) Constants.ATTENDANCE_LATE_ID else null
                        savePersonAttendance(attendance, attendanceID)
                    }
                }
            }

            var absentTagId = ""
            attendance.absent_reason_oid?.let {
                absentTagId = it
            }


            cAbsent.apply {
                isEnabled = viewModel.isSelectedTeacherAttendanceCurrentDate()
                tag = absentTagId
                isChecked = isAbsent
                setOnClickListener {
                    setTeacherAttendanceRecyclerPosition()
                    if (isChecked) {
                        isChecked = false

                        Utils.initSelectOneWidgetChip(
                            "attendance.absent_reason_oid",
                            it,
                            "Absent Reason",
                            viewModel.getOptionList(Constants.ABSENT_REASON_TEACHER_LIST_NAME),
                            attendance,
                            fragmentManager
                        )
                    } else {
                        isChecked = false
                        savePersonAttendance(attendance, null)
                    }
                }
            }


        }
    }

    private fun promptBiometricVerification(attendance: PersonAttendanceModel, attendanceStatus: String) {
        val bundle = Bundle()
        bundle.putString("attendanceStatus", attendanceStatus)
        bundle.putParcelable("personAttendanceModel", attendance)
        binding.root.findNavController().navigate(
            R.id.action_SchoolTeacherAttendanceListFragment_to_BiometricVerificationFragment,
            bundle
        )
    }

    private fun savePersonAttendance(attendance: PersonAttendanceModel, attendanceID: String?) {
        attendance.absent_reason_oid = null
        attendance.absent_reason_name = null
        attendance.attendance_status_oid = attendanceID
        if (attendance.uuid == null) {
            viewModel.insertPersonAttendance(attendance)
        } else {
            viewModel.updatePersonAttendance(attendance)
        }
    }

    private fun setTeacherAttendanceRecyclerPosition() {
        viewModel.teacherAttendanceRecyclerPosition = layoutPosition
    }
}