package uk.org.cgatechnologies.wideya.learner_performance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import uk.org.cgatechnologies.wideya.learner_performance.entities.LearnerPerformance
import uk.org.cgatechnologies.wideya.learner_performance.models.LearnerListItemModel
import uk.org.cgatechnologies.wideya.learner_performance.models.LearnerPerformanceEntryModel
import uk.org.cgatechnologies.wideya.learner_performance.models.SchoolGroupDropdownModel

@Dao
abstract class LearnerPerformanceDao {

    @Query("""
        SELECT
            sg.uuid,
            TRIM(COALESCE(ogl.item_name, '') || ' ' || COALESCE(sg.school_group_name, '')) school_group_name,
            sg.school_group_level_oid
        FROM school_group sg
        LEFT JOIN option_list ogl
            ON ogl.item_id = sg.school_group_level_oid
            AND ogl.list_name = 'school_group_level'
        WHERE sg.school_uuid = :schoolUuid
            AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
            AND sg.academic_year = :academicYear
        ORDER BY CAST(ogl.item_extra AS INTEGER), sg.school_group_name
    """)
    abstract fun getSchoolGroupsForSchool(
        schoolUuid: String,
        academicYear: Short
    ): List<SchoolGroupDropdownModel>

    @Query("""
        SELECT ol_sel.item_id
        FROM option_list ol_sgl
        INNER JOIN option_list_link oll
            ON oll.child_list_name = 'school_group_level'
            AND ol_sgl.id = oll.child_id
        INNER JOIN option_list ol_sel
            ON ol_sel.list_name = 'school_education_level'
            AND ol_sel.id = oll.parent_id
        WHERE ol_sgl.list_name = 'school_group_level'
            AND ol_sgl.item_id = :schoolGroupLevelOid
        LIMIT 1
    """)
    abstract fun getEducationLevelForGroupLevel(schoolGroupLevelOid: String): String?

    @Query("""
        SELECT
            l.uuid learner_uuid,
            TRIM(COALESCE(p.first_name, '') || ' ' || COALESCE(p.last_name, '')) learner_name,
            sg.uuid school_group_uuid,
            TRIM(COALESCE(ogl.item_name, '') || ' ' || COALESCE(sg.school_group_name, '')) school_group_name,
            sg.school_group_level_oid
        FROM school_learner_enrolment sle
        INNER JOIN learner l ON l.uuid = sle.learner_uuid
        INNER JOIN person p ON p.uuid = l.person_uuid
        INNER JOIN school_group sg
            ON sg.uuid = sle.school_group_uuid
            AND sg.school_uuid = :schoolUuid
            AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
        LEFT JOIN option_list ogl
            ON ogl.item_id = sg.school_group_level_oid
            AND ogl.list_name = 'school_group_level'
        WHERE sle.academic_year = :academicYear
            AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
        ORDER BY CAST(ogl.item_extra AS INTEGER), sg.school_group_name, p.first_name, p.last_name
    """)
    abstract fun getAllLearnersForSchool(
        schoolUuid: String,
        academicYear: Short
    ): List<LearnerListItemModel>

    @Query("""
        SELECT
            l.uuid learner_uuid,
            TRIM(COALESCE(p.first_name, '') || ' ' || COALESCE(p.last_name, '')) learner_name,
            sg.uuid school_group_uuid,
            TRIM(COALESCE(ogl.item_name, '') || ' ' || COALESCE(sg.school_group_name, '')) school_group_name,
            sg.school_group_level_oid
        FROM school_learner_enrolment sle
        INNER JOIN learner l ON l.uuid = sle.learner_uuid
        INNER JOIN person p ON p.uuid = l.person_uuid
        INNER JOIN school_group sg
            ON sg.uuid = sle.school_group_uuid
            AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
        LEFT JOIN option_list ogl
            ON ogl.item_id = sg.school_group_level_oid
            AND ogl.list_name = 'school_group_level'
        WHERE sle.school_group_uuid = :schoolGroupUuid
            AND sle.academic_year = :academicYear
            AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
        ORDER BY p.first_name, p.last_name
    """)
    abstract fun getLearnersByGroup(
        schoolGroupUuid: String,
        academicYear: Short
    ): List<LearnerListItemModel>

    @Query("""
        SELECT
            lp.uuid existing_uuid,
            l.uuid learner_uuid,
            TRIM(COALESCE(p.first_name, '') || ' ' || COALESCE(p.last_name, '')) learner_name,
            lp.assessment_1_score,
            lp.assessment_2_score,
            COALESCE(lp.max_score, 100.0) max_score
        FROM school_learner_enrolment sle
        INNER JOIN learner l ON l.uuid = sle.learner_uuid
        INNER JOIN person p ON p.uuid = l.person_uuid
        LEFT JOIN learner_performance lp
            ON lp.learner_uuid = l.uuid
            AND lp.school_group_uuid = :schoolGroupUuid
            AND lp.subject_oid = :subjectOid
            AND lp.term_oid = :termOid
            AND lp.academic_year = :academicYear
            AND (lp.deleted_at IS NULL OR lp.deleted_at = '')
        WHERE sle.school_group_uuid = :schoolGroupUuid
            AND sle.academic_year = :academicYear
            AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
        ORDER BY p.first_name, p.last_name
    """)
    abstract fun getPerformanceEntries(
        schoolGroupUuid: String,
        subjectOid: String,
        termOid: String,
        academicYear: Short
    ): List<LearnerPerformanceEntryModel>

    @Query("""
        SELECT uuid existing_uuid, subject_oid, assessment_1_score, assessment_2_score,
               COALESCE(max_score, 100.0) max_score
        FROM learner_performance
        WHERE learner_uuid = :learnerUuid
            AND school_group_uuid = :schoolGroupUuid
            AND term_oid = :termOid
            AND academic_year = :academicYear
            AND (deleted_at IS NULL OR deleted_at = '')
    """)
    abstract fun getExistingPerformanceForLearner(
        learnerUuid: String,
        schoolGroupUuid: String,
        termOid: String,
        academicYear: Short
    ): List<ExistingPerformanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrUpdatePerformance(record: LearnerPerformance)
}
