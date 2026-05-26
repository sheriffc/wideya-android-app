package uk.org.cgatechnologies.wideya.learner_performance.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import uk.org.cgatechnologies.wideya.common.utils.Utils

@Serializable
@Entity(
    tableName = "learner_performance",
    indices = [
        Index(value = ["school_uuid"]),
        Index(value = ["school_group_uuid"]),
        Index(value = ["learner_uuid"]),
        Index(value = ["subject_oid"]),
        Index(value = ["updated_at"]),
        Index(value = ["sync_flag"]),
    ]
)
data class LearnerPerformance(
    @PrimaryKey(autoGenerate = false)
    val uuid: String = Utils.getUuidOrdered(),
    val school_uuid: String,
    val school_group_uuid: String,
    val learner_uuid: String,
    val subject_oid: String,
    val academic_year: Short,
    val term_oid: String,
    val assessment_1_score: Float? = null,
    val assessment_2_score: Float? = null,
    val max_score: Float = 100f,
    val created_at: String,
    val created_by: Int,
    val updated_at: String,
    val updated_by: Int,
    val deleted_at: String? = null,
    val deleted_by: Int? = null,
    val sync_flag: Byte = 1
)
