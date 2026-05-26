package uk.org.cgatechnologies.wideya.analysis

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.analysis.models.LearnerAttendanceReportModel
import uk.org.cgatechnologies.wideya.analysis.models.LearnerDisabilityModel
import uk.org.cgatechnologies.wideya.analysis.models.TeacherAttendanceReportModel
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner

@Dao
abstract class AnalysisDao {

    @Query("""
        ${AnalysisQueries.TEACHER_ATTENDANCE_REPORT}
        WHERE entity_type_oid='teacher' 
            AND school_uuid = :schoolUuid
            AND date = :date
    """)
    abstract fun getSchoolTeacherAttendanceReportByDate(schoolUuid: String, date: String) : Flow<TeacherAttendanceReportModel?>

    @Query("""
        ${AnalysisQueries.TEACHER_ATTENDANCE_CURRENT_DATE}
        WHERE t.school_uuid = :schoolUuid
             AND (t.deleted_at IS NULL OR t.deleted_at = '') 
    """)
    abstract fun getTodayTeacherAttendanceReport(schoolUuid: String) : Flow<TeacherAttendanceReportModel?>

    @Query("""
        WITH RECURSIVE dates(date) AS (
          VALUES(:startDate)
          UNION ALL
          SELECT date(date, '+1 day')
          FROM dates
          WHERE date < :endDate
        )
        SELECT 
            date attendance_date,
            report.count_teachers,
            report.count_attendance,
            report.count_present,
            report.count_absent,
            report.count_late,
            report.count_submitted
        FROM dates d
        LEFT JOIN (
            ${AnalysisQueries.TEACHER_ATTENDANCE_REPORT}
            WHERE entity_type_oid = 'teacher' 
                AND school_uuid = :schoolUuid
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
            GROUP BY date
        ) AS report ON report.attendance_date = d.date
    """)
    abstract fun getSchoolTeacherAttendanceReportByInterval(schoolUuid: String, startDate: String,endDate: String) : Flow<List<TeacherAttendanceReportModel>>

    @Query("""
        ${AnalysisQueries.LEARNER_ATTENDANCE_REPORT}
        WHERE entity_type_oid='learner' 
            AND school_uuid = :schoolUuid
            AND date = :date
    """)
    abstract fun getSchoolLearnerAttendanceReportByDate(schoolUuid: String, date: String) : Flow<LearnerAttendanceReportModel?>

    @Query("""
        ${AnalysisQueries.LEARNER_ATTENDANCE_CURRENT_DATE}
        WHERE sla.school_uuid = :schoolUuid
             AND (l.deleted_at IS NULL OR l.deleted_at = '') 
    """)
    abstract fun getTodayLearnerAttendanceReport(schoolUuid: String) : Flow<LearnerAttendanceReportModel?>

    @Query("""
        WITH RECURSIVE dates(date) AS (
          VALUES(:startDate)
          UNION ALL
          SELECT date(date, '+1 day')
          FROM dates
          WHERE date < :endDate
        )
        SELECT 
            date attendance_date,
            report.count_learners,
            report.count_attendance,
            report.count_present,
            report.count_absent,
            report.count_late,
            report.count_submitted
        FROM dates d
        LEFT JOIN (
            ${AnalysisQueries.LEARNER_ATTENDANCE_REPORT}
            WHERE entity_type_oid = 'learner' 
                AND school_uuid = :schoolUuid
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
            GROUP BY date
        ) AS report ON report.attendance_date = d.date
    """)
    abstract fun getSchoolLearnerAttendanceReportByInterval(schoolUuid: String, startDate: String,endDate: String) : Flow<List<LearnerAttendanceReportModel>>

    @Query(
        """
            ${AnalysisQueries.LEARNER_DISABILITY}
            WHERE sla.school_uuid = :schoolUuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND (disability_other_condition_id is not null 
                OR disability_vision_id is not null 
                OR disability_cognition_id is not null 
                OR disability_communication_id is not null 
                OR disability_hearing_id is not null 
                OR disability_mobility_id is not null 
                OR disability_selfcare_id is not null) 
            ORDER BY full_name
        """
    )
    abstract fun getSchoolLearnerDisabilityReport(schoolUuid: String): Flow<List<LearnerDisabilityModel>>

    @Query(
        """
            ${AnalysisQueries.LEARNER_DISABILITY}
            WHERE sla.school_uuid = :schoolUuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND (full_name LIKE '%' || :query || '%' )
                AND (disability_other_condition_id is not null 
                OR disability_vision_id is not null 
                OR disability_cognition_id is not null 
                OR disability_communication_id is not null 
                OR disability_hearing_id is not null 
                OR disability_mobility_id is not null 
                OR disability_selfcare_id is not null)  
            ORDER BY full_name
        """
    )
    abstract fun getSchoolLearnerDisabilityByQuery(schoolUuid: String, query: String?): Flow<List<LearnerDisabilityModel>>

    @RawQuery(observedEntities = [Learner::class])
    abstract fun getSchoolLearnerDisabilityByRawQuery(query: SupportSQLiteQuery): Flow<List<LearnerDisabilityModel>>

    fun buildSchoolLearnerDisabilityByRawQuery(schoolUuid: String, query: String): Flow<List<LearnerDisabilityModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString  = ""
        searchTerms.forEach {
            whereString += "AND ( full_name LIKE '%' || '${it}' || '%' OR sla.admission_number LIKE '%' || '${it}' || '%')"
        }

        val queryString = """
                    ${AnalysisQueries.LEARNER_DISABILITY}
                    WHERE sla.school_uuid = '$schoolUuid'
                    AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                    $whereString
                    AND (disability_other_condition_id is not null 
                    OR disability_vision_id is not null 
                    OR disability_cognition_id is not null 
                    OR disability_communication_id is not null 
                    OR disability_hearing_id is not null 
                    OR disability_mobility_id is not null 
                    OR disability_selfcare_id is not null)  
                    ORDER BY full_name
                """
        return getSchoolLearnerDisabilityByRawQuery(SimpleSQLiteQuery(queryString))
    }
}

class AnalysisQueries {
    companion object {
        const val TEACHER_ATTENDANCE_CURRENT_DATE ="""
            SELECT 
                COUNT(t.uuid) count_teachers,
                COUNT(attendance_status_oid) count_attendance,
                COUNT(pa1.present) count_present,
                COUNT(pa2.absent) count_absent,
                COUNT(pa3.late) count_late,
                COUNT(pa.submitted) count_submitted,
                pa.date attendance_date
            FROM teacher t
            LEFT JOIN person pt ON t.person_uuid = pt.uuid
            LEFT JOIN (
                SELECT * FROM person_attendance temp_pa
                WHERE temp_pa.date = STRFTIME('%Y-%m-%d',datetime('now'))
                    AND (temp_pa.deleted_at IS NULL OR temp_pa.deleted_at = '')
             ) AS pa ON pa.person_uuid = pt.uuid
            LEFT JOIN (
                SELECT 
                    uuid present_uuid,
                    attendance_status_oid present
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'present' 
            ) AS pa1 ON pa1.present_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid absent_uuid,
                    attendance_status_oid absent
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'absent' 
            ) AS pa2 ON pa2.absent_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid late_uuid,
                    attendance_status_oid late
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'late' 
            ) AS pa3 ON pa3.late_uuid = pa.uuid
        """


        const val TEACHER_ATTENDANCE_REPORT = """
            SELECT 
                COUNT(uuid) count_teachers,
                COUNT(attendance_status_oid) count_attendance,
                COUNT(pa1.present) count_present,
                COUNT(pa2.absent) count_absent,
                COUNT(pa3.late) count_late,
                COUNT(submitted) count_submitted,
                date attendance_date
            FROM person_attendance pa
            LEFT JOIN (
                SELECT 
                    uuid present_uuid,
                    attendance_status_oid present
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'present' 
            ) AS pa1 ON pa1.present_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid absent_uuid,
                    attendance_status_oid absent
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'absent' 
            ) AS pa2 ON pa2.absent_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid late_uuid,
                    attendance_status_oid late
                FROM person_attendance 
                WHERE entity_type_oid = 'teacher'
                AND attendance_status_oid = 'late' 
            ) AS pa3 ON pa3.late_uuid = pa.uuid 
        """

        const val LEARNER_ATTENDANCE_CURRENT_DATE ="""
            SELECT 
                COUNT(l.uuid) count_learners,
                COUNT(attendance_am_status_oid || attendance_pm_status_oid) count_attendance,
                COUNT(pa1.present) count_present,
                COUNT(pa2.absent) count_absent,
                COUNT(pa3.late) count_late,
                COUNT(pa.submitted) count_submitted,
                pa.date attendance_date
            FROM learner l
            INNER JOIN person pt ON l.person_uuid = pt.uuid
            INNER JOIN school_learner_admission sla ON l.uuid = sla.learner_uuid
            LEFT JOIN (
                SELECT * FROM person_attendance temp_pa
                WHERE temp_pa.date = STRFTIME('%Y-%m-%d',datetime('now'))
                    AND (temp_pa.deleted_at IS NULL OR temp_pa.deleted_at = '')
             ) AS pa ON pa.person_uuid = pt.uuid
            LEFT JOIN (
                SELECT 
                    uuid present_uuid,
                    1 present
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND attendance_status_oid = 'present' 
            ) AS pa1 ON pa1.present_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid absent_uuid,
                    1 absent
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND attendance_status_oid = 'absent' 
            ) AS pa2 ON pa2.absent_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid late_uuid,
                    1 late
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND attendance_status_oid = 'late' 
            ) AS pa3 ON pa3.late_uuid = pa.uuid
        """


        const val LEARNER_ATTENDANCE_REPORT = """
            SELECT 
                COUNT(uuid) count_learners,
                COUNT(attendance_am_status_oid || attendance_pm_status_oid) count_attendance,
                COUNT(pa1.present) count_present,
                COUNT(pa2.absent) count_absent,
                COUNT(pa3.late) count_late,
                COUNT(submitted) count_submitted,
                date attendance_date
            FROM person_attendance pa
            LEFT JOIN (
                SELECT 
                    uuid present_uuid,
                    1 present
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND attendance_am_status_oid = 'present' AND attendance_pm_status_oid = 'present'
            ) AS pa1 ON pa1.present_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid absent_uuid,
                    1 absent
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND attendance_am_status_oid = 'absent' AND attendance_pm_status_oid = 'absent'
            ) AS pa2 ON pa2.absent_uuid = pa.uuid
            LEFT JOIN (
                SELECT 
                    uuid late_uuid,
                    1 late
                FROM person_attendance 
                WHERE entity_type_oid = 'learner'
                AND (
                    (attendance_am_status_oid = 'absent' AND attendance_pm_status_oid = 'present')
                    OR (attendance_am_status_oid = 'present' AND attendance_pm_status_oid = 'absent')
                    )
            ) AS pa3 ON pa3.late_uuid = pa.uuid 
        """

        const val LEARNER_DISABILITY = """
            SELECT 
                 l.uuid learner_uuid,
                 REPLACE(IFNULL(p.last_name,'') || ', ' || IFNULL(p.first_name,'') || ' ' || IFNULL(p.middle_name,''),'  ',' ') full_name,
                 sla.admission_number,
                 p.sex_oid learner_sex_oid,
                 CASE
                  WHEN disability_severity_oid_vision  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_vision
                 END as disability_vision_id, 
                 olv.item_name disability_vision,
                 CASE
                  WHEN disability_severity_oid_cognition  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_cognition
                 END as disability_cognition_id, 
                 olc.item_name disability_cognition,
                 CASE
                  WHEN disability_severity_oid_communication  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_communication
                 END as disability_communication_id, 
                 olco.item_name disability_communication,
                 CASE
                  WHEN disability_severity_oid_hearing  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_hearing
                 END as disability_hearing_id, 
                 olh.item_name disability_hearing,
                 CASE
                  WHEN disability_severity_oid_mobility  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_mobility
                 END as disability_mobility_id, 
                 olm.item_name disability_mobility,
                 CASE
                  WHEN disability_severity_oid_selfcare  in ('','0_no_difficulty') 
                     THEN null ELSE disability_severity_oid_selfcare
                 END as disability_selfcare_id, 
                 ols.item_name disability_selfcare,
                 CASE
                  WHEN disability_other_condition_oid  in ('','none') 
                     THEN null ELSE disability_other_condition_oid
                 END as disability_other_condition_id, 
                 olo.item_name disability_other_condition
            FROM learner l
            LEFT JOIN person p ON p.uuid = l.person_uuid
            LEFT JOIN school_learner_admission sla ON sla.learner_uuid = l.uuid
            LEFT JOIN option_list olv ON olv.item_id = l.disability_severity_oid_vision
                AND olv.list_name = 'disability_severity'
            LEFT JOIN option_list olc ON olc.item_id = l.disability_severity_oid_cognition
                AND olc.list_name = 'disability_severity'
            LEFT JOIN option_list olco ON olco.item_id = l.disability_severity_oid_communication
                AND olco.list_name = 'disability_severity'
            LEFT JOIN option_list olh ON olh.item_id = l.disability_severity_oid_hearing
                AND olh.list_name = 'disability_severity'
            LEFT JOIN option_list olm ON olm.item_id = l.disability_severity_oid_mobility
                AND olm.list_name = 'disability_severity'
            LEFT JOIN option_list ols ON ols.item_id = l.disability_severity_oid_selfcare
                AND ols.list_name = 'disability_severity'
            LEFT JOIN option_list olo ON olo.item_id = l.disability_other_condition_oid
                AND olo.list_name = 'disability_other_condition'
        """


    }
}