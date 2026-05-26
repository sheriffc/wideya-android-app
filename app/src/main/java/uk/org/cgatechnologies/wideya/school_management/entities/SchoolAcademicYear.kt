package uk.org.cgatechnologies.wideya.school_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "school_academic_year", indices = [
    Index(value = ["academic_year"]),
    Index(value = ["active"]),
    Index(value = ["updated_at"]),
])
data class SchoolAcademicYear(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val academic_year_name: String,
    val academic_year: Int,
    val date_from: String,
    val date_to: String? = null,
    val active: Byte? = null,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
)
