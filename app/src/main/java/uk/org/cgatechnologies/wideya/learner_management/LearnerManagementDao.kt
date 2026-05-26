package uk.org.cgatechnologies.wideya.learner_management

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementQueries
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

@Dao
abstract class LearnerManagementDao {

    //LEARNER
    @Query(
        """
        ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
            WHERE sla.uuid = :uuid 
        """
    )
    abstract fun getLearnerModel(uuid: String): Flow<LearnerAdmissionModel?>

    @Query(
        """
         ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
            WHERE sla.learner_uuid = :learnerUuid 
         """
    )
    abstract fun getLearnerAdmissionModel(learnerUuid: String): LearnerAdmissionModel?

    @Query(
        """
        ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
            WHERE s.uuid = :schoolUuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND (sg.uuid IS NULL OR sg.uuid = '')
                ORDER BY learner_full_name
        """
    )
    abstract fun getUnassignedLearnerList(schoolUuid: String): Flow<List<LearnerAdmissionModel>>

    @Query(
        """
        ${SchoolManagementQueries.LEARNER_SEARCH_MODEL}
            WHERE (l.deleted_at IS NULL OR l.deleted_at = '')
                AND (pl.nin LIKE '%' || :query || '%'
                    OR l.learner_id LIKE '%' || :query || '%'
                    OR :query = '')
                AND (:schoolUuid = '' OR l.uuid NOT IN (
                    SELECT learner_uuid
                    FROM school_learner_admission
                    WHERE school_uuid = :schoolUuid
                        AND (deleted_at IS NULL OR deleted_at = '')
                ))
            ORDER BY learner_full_name
            LIMIT 100
        """
    )
    abstract fun searchLearnersByQuery(query: String, schoolUuid: String): Flow<List<LearnerAdmissionModel>>

    @Query("""
        SELECT COUNT(*)
        FROM school_learner_admission sla
        INNER JOIN learner l ON l.uuid = sla.learner_uuid
        INNER JOIN person p ON p.uuid = l.person_uuid
        WHERE sla.school_uuid = :schoolUuid
            AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
            AND sla.learner_uuid != :excludeLearnerUuid
            AND (
                (:nin != '' AND p.nin IS NOT NULL AND p.nin != '' AND p.nin = :nin)
                OR (:learnerId != '' AND l.learner_id IS NOT NULL AND l.learner_id != '' AND l.learner_id = :learnerId)
            )
    """)
    abstract suspend fun countDuplicateLearnerAtSchool(
        schoolUuid: String,
        excludeLearnerUuid: String,
        nin: String,
        learnerId: String
    ): Int

    @Update
    abstract suspend fun updateLearner(learner: Learner)

    @Insert
    abstract suspend fun insertLearner(learner: Learner): Long

    @Delete
    abstract suspend fun deleteLearner(learner: Learner)

    @Query(
        """
            SELECT MAX(admission_number) 
            FROM school_learner_admission sla
            WHERE admission_number = :admissionNumber 
                AND school_uuid = :schoolUuid
                AND learner_uuid != :learnerUuid
                AND (deleted_at IS NULL OR deleted_at = '') 
        """
    )
    abstract fun checkAdmissionNumberExists(admissionNumber: String, schoolUuid: String, learnerUuid: String?): String?

}