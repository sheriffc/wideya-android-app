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

    /**
     * Like [getAndIncrement], but first floors the counter to at least
     * [localScanMax] — the highest sequence number already found in this
     * device's own (fully-synced) `learner` table for this school+year. This
     * covers two cases the plain local counter can't: a fresh install/reinstall
     * (the counter resets to 0 but the downloaded learner data hasn't), and a
     * device that has downloaded learners another device created since this
     * counter last advanced. Never decreases the counter.
     */
    @Transaction
    open fun getAndIncrementSeeded(emisId: String, academicYear: String, localScanMax: Int): Int {
        val existing = getSequence(emisId, academicYear)
        val baseline = maxOf(existing?.last_sequence ?: 0, localScanMax)
        val next = baseline + 1
        if (existing == null) {
            insertSequence(LearnerIdSequence(emis_id = emisId, academic_year = academicYear, last_sequence = next))
        } else {
            updateSequence(existing.copy(last_sequence = next))
        }
        return next
    }
}
