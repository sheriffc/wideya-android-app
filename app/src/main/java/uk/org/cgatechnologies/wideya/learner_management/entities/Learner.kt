package uk.org.cgatechnologies.wideya.learner_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

/*QUESTIONS
-should guardian info be assigned directly to a learner, or should it be left to admission
*/
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(tableName = "learner", indices = [
    Index(value = ["person_uuid"]),
    Index(value = ["updated_at"]),
    Index(value = ["sync_flag"]),
    Index(value = ["guardian_person_uuid"]),
    Index(value = ["learner_id"]),
])
data class Learner(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val person_uuid: String,
    val learner_id: String? = null,
    val language_oid_strongest: String?,
    val maternal_status_oid: String?,
    val maternal_status_updated_at: String? = null,
    val disability_severity_oid_vision: String?,
    val disability_severity_oid_hearing: String?,
    val disability_severity_oid_mobility: String?,
    val disability_severity_oid_cognition: String?,
    val disability_severity_oid_selfcare: String?,
    val disability_severity_oid_communication: String?,
    val disability_other_condition_oid: String?,
    val guardian_person_uuid: String?,
    val guardian_relation_to_learner_oid: String?,
    val guardian_relation_to_learner_other: String?,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 0
)
