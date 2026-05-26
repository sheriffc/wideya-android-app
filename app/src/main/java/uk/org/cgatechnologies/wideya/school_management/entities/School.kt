package uk.org.cgatechnologies.wideya.school_management.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
//import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Parcelize
@Entity(tableName = "school", indices = [
    Index(value = ["district_office_uuid"]),
    Index(value = ["active"]),
    Index(value = ["updated_at"]),
    Index(value = ["sync_flag"]),
    Index(value = ["media_photo_uuid"]),
])
data class School(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val name: String,
    val school_education_level_oid: String?,
    val emis_id: String?,
    val wideya_id: String?,
    val payroll_sid: String?,
    val waec_id: String?,
    val fabinc_recordid: Int?,
    val district_id: String?,
    val chiefdom_id: String?,
    val section_name: String?,
    val town_name: String?,
    val address: String?,
    val classrooms_oid: String? = null,
    val wash_oids: String? = null,
    val electricity_oids: String? = null,
    val mno_oids: String? = null,
    val learning_materials_oids: String? = null,
    val receives_feeding: Int? = null,
    val district_office_uuid: String?,
    val lat: Float?,
    val lng: Float?,
    val media_photo_uuid: String?,
    val tablet_phone_number: String?,
    val active: Byte?,
    val created_at: String = Utils.getISODateTimeUTC(),
    val created_by: Int = 0,
    val updated_at: String = Utils.getISODateTimeUTC(),
    val updated_by: Int = 0,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
) : Parcelable
