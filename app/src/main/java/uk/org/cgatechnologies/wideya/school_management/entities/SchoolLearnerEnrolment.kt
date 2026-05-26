package uk.org.cgatechnologies.wideya.school_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "school_learner_enrolment", indices = [
    Index(value = ["academic_year"]),
    Index(value = ["learner_uuid"]),
    Index(value = ["school_group_uuid"]),
    Index(value = ["sync_flag"])
])
data class SchoolLearnerEnrolment(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val academic_year: Short,
    val learner_uuid: String,
    val school_group_uuid: String? = null,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
