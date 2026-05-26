package uk.org.cgatechnologies.wideya.school_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by Mohamad Abuzaid on 1/20/2023.
 */

@Parcelize
data class SchoolTimetableModel(
    var uuid: String = "",
    var teacher_uuid: String = "",
    var teacher_full_name: String? = null,
    var teacher_first_name: String? = null,
    var teacher_middle_name: String? = null,
    var teacher_last_name: String? = null,
    var school_uuid: String? = null,
    var school_group_uuid: String? = null,
    var school_subject_oid: String? = null, //school_subject_oid
    var school_subject_name: String? = null,
    var school_subject_other: String? = null,
    var day_of_the_week_oid: String? = null,
    var day_of_the_week_name: String? = null,
    var start_time: String? = null, //00:00 HH:MM
    var end_time: String? = null, //00:00 HH:MM
    var created_at: String = "",
    var created_by: Int = 0,
    var updated_at: String = "",
    var updated_by: Int = 0,
    var deleted_at: String? = null,
    var deleted_by: Int? = null,
    var sync_flag: Byte = 0
) : Parcelable