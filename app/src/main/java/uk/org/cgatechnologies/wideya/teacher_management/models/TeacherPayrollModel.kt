package uk.org.cgatechnologies.wideya.teacher_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TeacherPayrollModel(
    val uuid: String,
    var first_name: String,
    var middle_name: String? = null,
    var last_name: String,
    var full_name: String,
    var sex: String? = null,
    var date_of_birth: String? = null,
    var age: Int? = null,
    var pin: String,
    var nin: String? = null,
    var nassit_number: String? = null,
    var created_at: String,
    var updated_at: String? = null,
) : Parcelable