package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "person_attendance", indices = [
    Index(value = ["person_uuid"]),
    Index(value = ["date", "person_uuid", "entity_type_oid", "academic_year", "school_uuid", "school_group_uuid"]),
    Index(value = ["school_uuid", "updated_at"]), //for sync
])
data class PersonAttendance(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val date: String,
    val person_uuid: String,
    val entity_type_oid: String,
    val academic_year: Short?,
    val school_uuid: String?,
    val school_group_uuid: String?,
    val attendance_am_status_oid: String?,
    val attendance_pm_status_oid: String?,
    val attendance_status_oid: String?,
    val absent_reason_oid: String?,
    val absent_reason_other: String?,
    val lat: Float?,
    val lng: Float?,
    val biometric_method_oid: String?,
    val biometric_reference: String?, //can be either reference to the fp position if fp or media_uuid if photo
    val submitted: Byte? = 0,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
