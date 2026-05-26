package uk.org.cgatechnologies.wideya.school_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "school_learner_admission", indices = [
    Index(value = ["school_uuid", "learner_uuid"]),
    Index(value = ["school_uuid", "updated_at"]),
    Index(value = ["sync_flag"]),
])
data class SchoolLearnerAdmission(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val school_uuid: String,
    val learner_uuid: String,
    val admission_number: Int? = null,
    val start_date: String? = null,
    val end_date: String? = null,
    val end_reason_learner_oid: String? = null,
    val end_reason_learner_other: String? = null,
    val end_reason_learner_detail: String? = null,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
