package uk.org.cgatechnologies.wideya.teacher_management.models

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.Parcelize

/**
 * Created by Mohamad Abuzaid on 1/5/2023.
 */

@Parcelize
data class TeacherTimetableModel(
    var uuid: String = "",
    var teacher_uuid: String = "",
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
    var sync_flag: Byte = 0,
    @Ignore
    val timeStamp: Long = System.currentTimeMillis()
) : Parcelable