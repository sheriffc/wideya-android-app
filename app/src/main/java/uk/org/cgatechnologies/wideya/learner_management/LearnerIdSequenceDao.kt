package uk.org.cgatechnologies.wideya.learner_management

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import uk.org.cgatechnologies.wideya.learner_management.entities.LearnerIdSequence

@Dao
abstract class LearnerIdSequenceDao {

    @Query("SELECT * FROM learner_id_sequence WHERE emis_id = :emisId AND academic_year = :academicYear LIMIT 1")
    abstract fun getSequence(emisId: String, academicYear: String): LearnerIdSequence?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insertSequence(sequence: LearnerIdSequence): Long

    @Update
    abstract fun updateSequence(sequence: LearnerIdSequence)

    @Transaction
    open fun getAndIncrement(emisId: String, academicYear: String): Int {
        val existing = getSequence(emisId, academicYear)
        return if (existing == null) {
            insertSequence(LearnerIdSequence(emis_id = emisId, academic_year = academicYear, last_sequence = 1))
            1
        } else {
            val next = existing.last_sequence + 1
            updateSequence(existing.copy(last_sequence = next))
            next
        }
    }
}
