package uk.org.cgatechnologies.wideya.teacher_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "teacher", indices = [
    Index(value = ["school_uuid"]),
    Index(value = ["school_uuid","updated_at"]),
    Index(value = ["person_uuid"]),
    Index(value = ["sync_flag"]),
])
data class Teacher(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val person_uuid: String,
    val school_uuid: String?,
    val employment_status_oid: String?,
    val teacher_role_oid: String?,
    val pin: String?,
    val nassit_number: String?,
    val tsc_licence_id: String?,
    val start_date: String?,
    val end_date: String?,
    val end_reason_teacher_oid: String?,
    val end_reason_teacher_other: String?,
    val end_reason_teacher_detail: String?,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String?,
    val deleted_by: Int?,
    val sync_flag: Byte = 0
)
