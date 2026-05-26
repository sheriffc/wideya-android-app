package uk.org.cgatechnologies.wideya.teacher_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "teacher_timetable", indices = [
    Index(value = ["teacher_uuid"]),
    Index(value = ["school_uuid"]),
    Index(value = ["school_group_uuid"]),
    Index(value = ["sync_flag"]),
])
data class TeacherTimetable(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val teacher_uuid: String,
    val school_uuid: String,
    val school_group_uuid: String? = null,
    val school_subject_oid: String? = null,
    val school_subject_other: String? = null,
    val day_of_the_week_oid: String,
    val start_time: String?,
    val end_time: String?,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
