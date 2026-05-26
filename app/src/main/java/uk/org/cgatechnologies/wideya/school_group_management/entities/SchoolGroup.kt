package uk.org.cgatechnologies.wideya.school_group_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
/*QUESTIONS
-multiple teachers per class?
-need for indicating a substitute teacher?
*/
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "school_group", indices = [
    Index(value = ["school_uuid", "academic_year", "teacher_uuid"]),
    Index(value = ["sync_flag"]),
    Index(value = ["updated_at"]),
    Index(value = ["active"]),
]
)
data class SchoolGroup(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val school_uuid: String,
    val academic_year: Short,
    val teacher_uuid: String?,
    val school_group_name: String?,
    val school_group_level_oid: String?,
    val active: Byte? = 1,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
