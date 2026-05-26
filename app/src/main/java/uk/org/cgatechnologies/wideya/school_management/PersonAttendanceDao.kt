package uk.org.cgatechnologies.wideya.school_management

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.common.data.CommonDao
import uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance
import uk.org.cgatechnologies.wideya.school_management.models.AttendanceDateSummaryCountModel
import uk.org.cgatechnologies.wideya.school_management.models.NoSchoolPersonModel
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel

@Dao
abstract class PersonAttendanceDao {
    //PERSON ATTENDANCE
    @Insert
    abstract suspend fun insertPersonAttendance(personAttendance: PersonAttendance): Long

    @Insert
    abstract suspend fun insertPersonAttendanceList(personAttendanceList: List<PersonAttendance>)

    @Update
    abstract suspend fun updatePersonAttendance(personAttendance: PersonAttendance)

    @Update
    abstract suspend fun updatePersonAttendanceList(personAttendanceList: List<PersonAttendance>)

    @Delete
    abstract suspend fun deletePersonAttendance(personAttendance: PersonAttendance)

    @Query(
        """
            ${PersonAttendanceQueries.TEACHER_ATTENDANCE_MODEL}
            WHERE t.school_uuid = :uuid
                AND (t.deleted_at IS NULL OR t.deleted_at = '')
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceTeacherFlowList(uuid: String?): Flow<List<PersonAttendanceModel>>

    @Query(
        """
            ${PersonAttendanceQueries.TEACHER_ATTENDANCE_DATE_MODEL}
            WHERE t.school_uuid = :schoolUuid
                AND date = :date
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceTeacherFlowListByDate(
        schoolUuid: String?,
        date: String
    ): Flow<List<PersonAttendanceModel>>

    @Query(
        """
            ${PersonAttendanceQueries.TEACHER_ATTENDANCE_MODEL}
            WHERE t.school_uuid = :uuid
                AND (t.deleted_at IS NULL OR t.deleted_at = '')
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceTeacherList(uuid: String?): List<PersonAttendanceModel>

    @Query(
        """
            ${PersonAttendanceQueries.TEACHER_ATTENDANCE_DATE_COUNT_SUMMARY_MODEL}
            WHERE pa.school_uuid = :uuid
                AND pa.entity_type_oid = 'teacher'
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
                AND pa.attendance_status_oid is not null
            GROUP BY date,submitted
                """
    )
    abstract fun getSchoolTeacherAttendanceDateSummaryCountList(uuid: String?): List<AttendanceDateSummaryCountModel>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_DATE_COUNT_SUMMARY_MODEL}
            WHERE pa.school_uuid = :uuid
                AND pa.entity_type_oid = 'learner'
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
                AND pa.attendance_am_status_oid is not null
                AND pa.attendance_pm_status_oid is not null
            GROUP BY date,submitted
                """
    )
    abstract fun getSchoolLearnerAttendanceDateSummaryCountList(uuid: String?): List<AttendanceDateSummaryCountModel>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceLearnerFlowList(uuid: String?): Flow<List<PersonAttendanceModel>>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_DATE_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                AND pa.date = :date
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceLearnerFlowListByDate(
        uuid: String?,
        date: String
    ): Flow<List<PersonAttendanceModel>>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_DATE_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (pa.deleted_at IS NULL OR pa.deleted_at = '')
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                AND pa.date = :date
                AND ( pa.school_group_uuid = :schoolGroupUuid  )
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceLearnerFlowListByDateAndSchoolGroup(
        uuid: String?,
        date: String,
        schoolGroupUuid: String?
    ): Flow<List<PersonAttendanceModel>>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceLearnerList(uuid: String?): List<PersonAttendanceModel>

    @Query(
        """
            ${PersonAttendanceQueries.LEARNER_ATTENDANCE_MODEL}
            WHERE sla.school_uuid = :school
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                AND sg.uuid = :group
            ORDER BY full_name
                """
    )
    abstract fun getSchoolAttendanceLearnerFlowListBySchoolGroup(
        group: String?,
        school: String?
    ): Flow<List<PersonAttendanceModel>>

    @Query(
        """
        SELECT MAX(date) 
        FROM person_attendance 
        WHERE school_uuid = :schoolUuid
    """
    )
    abstract fun getPersonAttendanceTableLatestDate(schoolUuid: String?): String?

    // ── No-School helpers ────────────────────────────────────────────────────

    /**
     * Deletes records that were pre-created for a future date (created_at date < attendance date).
     * This cleans up records wrongly created ahead of time by the No School feature before the
     * WorkManager-deferred approach was in place.
     */
    @Query(
        """
        DELETE FROM person_attendance
        WHERE school_uuid = :schoolUuid
            AND (deleted_at IS NULL OR deleted_at = '')
            AND date >= STRFTIME('%Y-%m-%d', datetime('now'))
            AND SUBSTR(created_at, 1, 10) < date
        """
    )
    abstract fun deletePreCreatedFutureRecords(schoolUuid: String): Int

    /** Returns distinct dates (yyyy-MM-dd) >= today that have at least one submitted record. */
    @Query(
        """
        SELECT DISTINCT date FROM person_attendance
        WHERE school_uuid = :schoolUuid
            AND submitted = 1
            AND (deleted_at IS NULL OR deleted_at = '')
            AND date >= STRFTIME('%Y-%m-%d', datetime('now'))
        """
    )
    abstract fun getSubmittedDatesFromToday(schoolUuid: String): List<String>

    // ── No-School bulk helpers ───────────────────────────────────────────────

    @Query(
        """
        SELECT l.person_uuid, sla.school_uuid, sle.school_group_uuid
        FROM school_learner_admission sla
        JOIN learner l ON l.uuid = sla.learner_uuid
        LEFT JOIN school_learner_enrolment sle ON sle.learner_uuid = sla.learner_uuid
            AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
        WHERE sla.school_uuid = :schoolUuid
            AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
        GROUP BY l.person_uuid
        """
    )
    abstract fun getSchoolLearnerPersonList(schoolUuid: String): List<NoSchoolPersonModel>

    @Query(
        """
        SELECT t.person_uuid, t.school_uuid, null AS school_group_uuid
        FROM teacher t
        WHERE t.school_uuid = :schoolUuid
            AND (t.deleted_at IS NULL OR t.deleted_at = '')
        """
    )
    abstract fun getSchoolTeacherPersonList(schoolUuid: String): List<NoSchoolPersonModel>

    @Query(
        """
        SELECT * FROM person_attendance
        WHERE school_uuid = :schoolUuid
            AND date = :date
            AND entity_type_oid = :entityType
            AND (deleted_at IS NULL OR deleted_at = '')
        """
    )
    abstract fun getAttendanceByDateAndEntityType(
        schoolUuid: String,
        date: String,
        entityType: String
    ): List<PersonAttendance>

    // ── TODO: refactor this to use the regular update() command and set the values in the view model, including deleted_by etc
    @Query(
        """
        UPDATE person_attendance 
        ${CommonDao.CommonQueries.SOFT_DELETE_SET}
        WHERE person_uuid = :personUuid 
            AND date = DATE()
    """
    )
    abstract fun softDeletePersonAttendanceForToday(personUuid: String, userId: Int)

    object PersonAttendanceQueries {
        const val TEACHER_ATTENDANCE_MODEL = """
            SELECT 
                pa.uuid,
                t.person_uuid,
                t.pin,
                REPLACE(IFNULL(pt.last_name,'') || ', ' || IFNULL(pt.first_name,'') || ' ' || IFNULL(pt.middle_name,''),'  ',' ') full_name,
                pt.sex_oid,
                mp.base64_data portrait_base64_data,
                mp.display_orientation portrait_display_orientation,
                pa.date,
                pa.academic_year,
                'teacher' entity_type_oid,
                t.school_uuid,
                '' school_group_level,
                '' school_group_name,
                '' school_name,
                pa.attendance_am_status_oid,
                '' attendance_am_status_name,
                pa.attendance_pm_status_oid,
                '' attendance_pm_status_name,
                pa.attendance_status_oid,
                ola.item_name attendance_status_name,
                pa.absent_reason_oid,
                art.item_name absent_reason_name,
                pa.absent_reason_other,
                ol.item_name teacher_role_name,
                pa.lat,
                pa.lng,
                pa.biometric_method_oid,
                pa.biometric_reference,
                pa.submitted,
                pa.created_at,
                pa.created_by,
                pa.updated_at,
                pa.updated_by,
                pa.deleted_at,
                pa.deleted_by,
                pa.sync_flag
            FROM teacher t
            LEFT JOIN person pt ON t.person_uuid = pt.uuid
            LEFT JOIN (
                SELECT * FROM person_attendance temp_pa
                WHERE temp_pa.date = STRFTIME('%Y-%m-%d',datetime('now'))
                    AND (temp_pa.deleted_at IS NULL OR temp_pa.deleted_at = '')
            ) AS pa ON pa.person_uuid = pt.uuid
            LEFT JOIN option_list ol ON ol.list_name = 'teacher_role' AND t.teacher_role_oid = ol.item_id
            LEFT JOIN option_list ola ON ola.list_name ='attendance_status' AND ola.item_id = pa.attendance_status_oid
            LEFT JOIN option_list art ON art.list_name ='absent_reason_teacher' AND art.item_id = pa.absent_reason_oid
            LEFT JOIN media_photo mp ON mp.uuid = pt.portrait_uuid
        """
        const val TEACHER_ATTENDANCE_DATE_MODEL = """
            SELECT
                pa.uuid,
                t.person_uuid,
                t.pin,
                REPLACE(IFNULL(pt.last_name,'') || ', ' || IFNULL(pt.first_name,'') || ' ' || IFNULL(pt.middle_name,''),'  ',' ') full_name,
                pt.sex_oid,
                mp.base64_data portrait_base64_data,
                mp.display_orientation portrait_display_orientation,
                pa.date,
                pa.academic_year,
                'teacher' entity_type_oid,
                t.school_uuid,
                '' school_group_level,
                '' school_group_name,
                '' school_name,
                pa.attendance_am_status_oid,
                '' attendance_am_status_name,
                pa.attendance_pm_status_oid,
                '' attendance_pm_status_name,
                pa.attendance_status_oid,
                ola.item_name attendance_status_name,
                pa.absent_reason_oid,
                art.item_name absent_reason_name,
                pa.absent_reason_other,
                ol.item_name teacher_role_name,
                pa.lat,
                pa.lng,
                pa.biometric_method_oid,
                pa.biometric_reference,
                pa.submitted,
                pa.created_at,
                pa.created_by,
                pa.updated_at,
                pa.updated_by,
                pa.deleted_at,
                pa.deleted_by,
                pa.sync_flag
            FROM person_attendance pa 
            LEFT JOIN teacher t ON pa.person_uuid = t.person_uuid
            LEFT JOIN person pt ON t.person_uuid = pt.uuid
            LEFT JOIN option_list ol ON ol.list_name = 'teacher_role' AND t.teacher_role_oid = ol.item_id
            LEFT JOIN option_list ola ON ola.list_name ='attendance_status' AND ola.item_id = pa.attendance_status_oid
            LEFT JOIN option_list art ON art.list_name ='absent_reason_teacher' AND art.item_id = pa.absent_reason_oid
            LEFT JOIN media_photo mp ON mp.uuid = pt.portrait_uuid
        """

        const val LEARNER_ATTENDANCE_MODEL = """
            SELECT     
                pa.uuid,
                l.person_uuid,
                '' pin,
                REPLACE(IFNULL(pl.last_name,'') || ', ' || IFNULL(pl.first_name,'') || ' ' || IFNULL(pl.middle_name,''),'  ',' ') full_name,
                pl.sex_oid,
                pa.date,
                pa.academic_year,
                'learner' entity_type_oid,
                sla.school_uuid,
                s.name school_name,
                sle.school_group_uuid,
                sg.school_group_name,
                lg.item_name school_group_level,
                sla.admission_number,
                pa.attendance_am_status_oid,
                ola.item_name attendance_am_status_name,
                pa.attendance_pm_status_oid,
                olp.item_name attendance_pm_status_name,
                pa.attendance_status_oid,
                ols.item_name attendance_status_name,
                pa.absent_reason_oid,
                '' absent_reason_name,
                pa.absent_reason_other,
                '' teacher_role_name,
                pa.lat,
                pa.lng,
                pa.biometric_method_oid,
                pa.biometric_reference,
                pa.submitted,
                pa.created_at,
                pa.created_by,
                pa.updated_at,
                pa.updated_by,
                pa.deleted_at,
                pa.deleted_by,
                pa.sync_flag
            FROM school_learner_admission sla
            LEFT JOIN learner l ON l.uuid = sla.learner_uuid
            LEFT JOIN person pl ON l.person_uuid = pl.uuid
            LEFT JOIN (
                SELECT * FROM person_attendance temp_pa
                WHERE temp_pa.date = STRFTIME('%Y-%m-%d',datetime('now'))
                    AND (temp_pa.deleted_at IS NULL OR temp_pa.deleted_at = '')
            ) AS pa ON pa.person_uuid = pl.uuid
            LEFT JOIN option_list ols ON ols.list_name ='attendance_status' AND ols.item_id = pa.attendance_status_oid
            LEFT JOIN option_list ola ON ola.list_name ='attendance_status' AND ola.item_id = pa.attendance_am_status_oid
            LEFT JOIN option_list olp ON olp.list_name ='attendance_status' AND olp.item_id = pa.attendance_pm_status_oid
            LEFT JOIN school s ON s.uuid = sla.school_uuid
            LEFT JOIN school_learner_enrolment sle ON sle.learner_uuid = sla.learner_uuid
                AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
            LEFT JOIN school_group sg ON sg.uuid = sle.school_group_uuid
                AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                AND sg.active
            LEFT JOIN option_list lg ON lg.item_id = sg.school_group_level_oid AND lg.list_name = 'school_group_level'
            LEFT JOIN school_academic_year say ON sg.academic_year = say.academic_year
        """
        const val LEARNER_ATTENDANCE_DATE_MODEL = """
            SELECT     
                pa.uuid,
                l.person_uuid,
                '' pin,
                REPLACE(IFNULL(pl.last_name,'') || ', ' || IFNULL(pl.first_name,'') || ' ' || IFNULL(pl.middle_name,''),'  ',' ') full_name,
                pl.sex_oid,
                pa.date,
                pa.academic_year,
                'learner' entity_type_oid,
                sla.school_uuid,
                s.name school_name,
                sle.school_group_uuid,
                sg.school_group_name,
                lg.item_name school_group_level,
                sla.admission_number,
                pa.attendance_am_status_oid,
                '' attendance_am_status_name,
                pa.attendance_pm_status_oid,
                '' attendance_pm_status_name,
                pa.attendance_status_oid,
                ola.item_name attendance_status_name,
                pa.absent_reason_oid,
                '' absent_reason_name,
                pa.absent_reason_other,
                '' teacher_role_name,
                pa.lat,
                pa.lng,
                pa.biometric_method_oid,
                pa.biometric_reference,
                pa.submitted,
                pa.created_at,
                pa.created_by,
                pa.updated_at,
                pa.updated_by,
                pa.deleted_at,
                pa.deleted_by,
                pa.sync_flag
            FROM person_attendance pa
            LEFT JOIN person pl ON pa.person_uuid = pl.uuid
            LEFT JOIN learner l ON l.person_uuid = pl.uuid
            LEFT JOIN school_learner_admission sla ON l.uuid = sla.learner_uuid
            LEFT JOIN option_list ola ON ola.list_name ='attendance_status' AND ola.item_id = pa.attendance_status_oid
            LEFT JOIN school s ON s.uuid = sla.school_uuid
            LEFT JOIN school_learner_enrolment sle ON sle.learner_uuid = sla.learner_uuid
            LEFT JOIN school_group sg ON sg.uuid = pa.school_group_uuid
            LEFT JOIN option_list lg ON lg.item_id = sg.school_group_level_oid AND lg.list_name = 'school_group_level'
            LEFT JOIN school_academic_year say ON sg.academic_year = say.academic_year
            """

        const val TEACHER_ATTENDANCE_DATE_COUNT_SUMMARY_MODEL = """
            SELECT 
                pa.date,
                COUNT(pa.attendance_status_oid) count_recorded_attendance, 
                COUNT(pa.submitted) count_submitted
            FROM person_attendance pa
         """

        const val LEARNER_ATTENDANCE_DATE_COUNT_SUMMARY_MODEL = """
            SELECT 
                date,
                COUNT(pa.attendance_pm_status_oid) count_recorded_attendance, 
                COUNT(pa.submitted) count_submitted
            FROM person_attendance pa
          """

    }
}