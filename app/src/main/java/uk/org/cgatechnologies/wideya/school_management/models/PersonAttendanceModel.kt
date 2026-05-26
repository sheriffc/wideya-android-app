package uk.org.cgatechnologies.wideya.school_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

@Parcelize
data class PersonAttendanceModel (
    var uuid: String? = Utils.getUuidOrdered(),
    var date: String? = "",
    var person_uuid: String = "",
    var pin: String? = "",
    var full_name: String? = null,
    var sex_oid: String? = null,
    var portrait_base64_data: String? = null,
    var portrait_display_orientation: Short? = 0,
    var entity_type_oid: String? = "",
    var academic_year: Short? = null,
    var school_name: String? = null,
    var school_uuid: String? = null,
    var school_group_uuid: String? = null,
    var school_group_name: String? = null,
    var school_group_level: String? = null,
    var admission_number: Int? = null,
    var attendance_am_status_oid: String? = null,
    var attendance_am_status_name: String? = null,
    var attendance_pm_status_oid: String? = null,
    var attendance_pm_status_name: String? = null,
    var attendance_status_oid: String? = null,
    var attendance_status_name: String? = null,
    var absent_reason_oid: String? = null,
    var absent_reason_other: String? = null,
    var absent_reason_name: String? = null,
    var teacher_role_name: String? = null,
    var biometric_method_oid: String? = null,
    var biometric_reference: String? = null,
    var submitted: Byte? = null,
    var lat: Float? = null,
    var lng: Float? = null,
    var created_at: String? = null,
    var created_by: Int? = null,
    var updated_at: String? = null,
    var updated_by: Int? = null,
    var deleted_at: String? = null,
    var deleted_by: Int? = null,
    var sync_flag: Byte = 0
) : Parcelable