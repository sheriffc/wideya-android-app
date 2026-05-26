package uk.org.cgatechnologies.wideya.school_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "school_feeding",
    indices = [
        Index(value = ["school_uuid"]),
        Index(value = ["sync_flag"]),
        Index(value = ["updated_at"])
    ]
)
data class SchoolFeeding(
    @PrimaryKey(autoGenerate = false) val uuid: String,
    val school_uuid: String,
    val receives_feeding: Int,       // 0 = No, 1 = Yes
    val supply_period_oid: String?,  // first_term | second_term | third_term
    val received_at: String?,        // yyyy-MM-dd
    val supplied_by_oid: String?,    // gosl | plan | wfp | crs | other
    val supplied_by_other: String?,
    val qty_rice: Int?,
    val qty_beans: Int?,
    val qty_gari: Int?,
    val qty_veg_oil: Int?,
    val qty_salt: Int?,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Int = 1
)
