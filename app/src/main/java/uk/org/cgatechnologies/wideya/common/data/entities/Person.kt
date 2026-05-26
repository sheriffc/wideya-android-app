package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "person", indices = [
    Index(value = ["nin"]),
    Index(value = ["updated_at"]),
    Index(value = ["sync_flag"]),
    Index(value = ["portrait_uuid"]),
])
data class Person(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val last_name: String?,
    val middle_name: String?,
    val first_name: String?,
    val sex_oid: String?,
    val date_of_birth: String?,
    val nin: String?,
    val portrait_uuid: String?,
    val phone_1: String?,
    val phone_2: String?,
    val email: String?,
    val address: String?,
    val fp_lt_uuid: String?,
    val fp_li_uuid: String?,
    val fp_rt_uuid: String?,
    val fp_ri_uuid: String?,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
