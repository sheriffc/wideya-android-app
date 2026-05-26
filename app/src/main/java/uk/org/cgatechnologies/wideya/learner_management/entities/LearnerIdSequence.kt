package uk.org.cgatechnologies.wideya.learner_management.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "learner_id_sequence",
    indices = [Index(value = ["emis_id", "academic_year"], unique = true)]
)
data class LearnerIdSequence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val emis_id: String,
    val academic_year: String,
    val last_sequence: Int = 0
)
