package uk.org.cgatechnologies.wideya.teacher_management

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherPayroll
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherTimetable
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel

@Dao
abstract class TeacherManagementDao {

    //TEACHER
    @Query(
        """
            ${TeacherManagementQueries.TEACHER_MODEL}
            WHERE t.uuid = :uuid 
        """
    )
    abstract fun getTeacherModel(uuid: String): Flow<TeacherModel?>

    @Update
    abstract suspend fun updateTeacher(teacher: Teacher)

    @Insert
    abstract suspend fun insertTeacher(teacher: Teacher): Long

    @Delete
    abstract suspend fun deleteTeacher(teacher: Teacher)

    @Query(
        """
            ${TeacherManagementQueries.TEACHER_PAYROLL_MODEL}
            WHERE tp.pin IS NOT NULL
                AND tp.pin NOT IN (
                SELECT t.pin
                FROM teacher t
                WHERE t.school_uuid = :uuid
                    AND (t.deleted_at IS NULL OR t.deleted_at = '')
                    AND t.pin IS NOT NULL
            )
                AND (tp.deleted_at IS NULL OR tp.deleted_at = '')
                ORDER BY full_name
        """
    )
    abstract fun getPayrollTeacherList(uuid: String?): Flow<List<TeacherPayrollModel>>

    @Query(
        """
            ${TeacherManagementQueries.TEACHER_PAYROLL_MODEL}
            WHERE tp.pin IS NOT NULL
                AND tp.pin NOT IN (
                SELECT t.pin
                FROM teacher t
                WHERE t.school_uuid = :uuid
                    AND (t.deleted_at IS NULL OR t.deleted_at = '')
                    AND t.pin IS NOT NULL
            )
                AND (tp.deleted_at IS NULL OR tp.deleted_at = '')
                AND ( full_name LIKE '%' || :query || '%' OR pin LIKE '%' || :query || '%' )
                ORDER BY full_name
        """
    )
    abstract fun getPayrollTeacherListByQuery(uuid: String?, query: String?): Flow<List<TeacherPayrollModel>>

    @RawQuery(observedEntities = [TeacherPayroll::class])
    abstract fun getPayrollTeacherListByRawQuery(query: SupportSQLiteQuery): Flow<List<TeacherPayrollModel>>

    fun buildPayrollTeacherListByRawQuery(uuid: String, query: String): Flow<List<TeacherPayrollModel>>{

                val searchTerms = query.replace("""[\s]+"""," ").split(" ")

                var whereString  = ""
                searchTerms.forEach {
                    whereString += "AND ( full_name LIKE '%' || '${it}' || '%' OR pin LIKE '%' || '${it}' || '%' )"
                }

                val queryString = """
                    ${TeacherManagementQueries.TEACHER_PAYROLL_MODEL}
                    WHERE tp.pin IS NOT NULL
                        AND tp.pin NOT IN (
                        SELECT t.pin
                        FROM teacher t
                        WHERE t.school_uuid = '$uuid'
                            AND (t.deleted_at IS NULL OR t.deleted_at = '')
                            AND t.pin IS NOT NULL
                    )
                        AND (tp.deleted_at IS NULL OR tp.deleted_at = '')
                        $whereString
                        ORDER BY full_name
                """
        return getPayrollTeacherListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    // NON-PAYROLL TEACHERS (from teacher_payroll where pin IS NULL — synced from TSCTMIS)

    @Query(
        """
            SELECT
                tp.uuid,
                tp.first_name,
                tp.middle_name,
                tp.last_name,
                REPLACE(IFNULL(tp.last_name,'') || ', ' || IFNULL(tp.first_name,'') || ' ' || IFNULL(tp.middle_name,''),'  ',' ') full_name,
                tp.sex,
                tp.date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', tp.date_of_birth) as int) age,
                COALESCE(tp.pin, '') pin,
                tp.nin,
                tp.nassit_number,
                tp.created_at,
                tp.updated_at
            FROM teacher_payroll tp
            WHERE (tp.pin IS NULL OR tp.pin = '')
                AND (tp.deleted_at IS NULL OR tp.deleted_at = '')
            ORDER BY full_name
        """
    )
    abstract fun getNonPayrollTeacherList(): Flow<List<TeacherPayrollModel>>

    @RawQuery(observedEntities = [TeacherPayroll::class])
    abstract fun getNonPayrollTeacherListByRawQuery(query: SupportSQLiteQuery): Flow<List<TeacherPayrollModel>>

    fun buildNonPayrollTeacherListByRawQuery(query: String): Flow<List<TeacherPayrollModel>> {
        val searchTerms = query.replace("""[\s]+""", " ").split(" ")
        var whereString = ""
        searchTerms.forEach {
            whereString += "AND full_name LIKE '%' || '${it}' || '%' "
        }
        val queryString = """
            SELECT
                tp.uuid,
                tp.first_name,
                tp.middle_name,
                tp.last_name,
                REPLACE(IFNULL(tp.last_name,'') || ', ' || IFNULL(tp.first_name,'') || ' ' || IFNULL(tp.middle_name,''),'  ',' ') full_name,
                tp.sex,
                tp.date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', tp.date_of_birth) as int) age,
                COALESCE(tp.pin, '') pin,
                tp.nin,
                tp.nassit_number,
                tp.created_at,
                tp.updated_at
            FROM teacher_payroll tp
            WHERE (tp.pin IS NULL OR tp.pin = '')
                AND (tp.deleted_at IS NULL OR tp.deleted_at = '')
                $whereString
            ORDER BY full_name
        """
        return getNonPayrollTeacherListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    @Query(
        """
            ${TeacherManagementQueries.TEACHER_TIMETABLE_MODEL}
            WHERE tt.teacher_uuid = :uuid
        """
    )
    abstract fun getTeacherTimetableList(uuid: String?): Flow<List<TeacherTimetableModel>>

    @Insert
    abstract suspend fun insertTimetableEntry(timetable: TeacherTimetable): Long

    @Update
    abstract suspend fun updateTimetableEntry(timetable: TeacherTimetable)

    @Delete
    abstract suspend fun deleteTimetableEntry(timetable: TeacherTimetable)
}

object TeacherManagementQueries {
    const val TEACHER_MODEL = """
            SELECT 
                t.uuid,
                t.person_uuid,
                REPLACE(IFNULL(p.last_name,'') || ', ' || IFNULL(p.first_name,'') || ' ' || IFNULL(p.middle_name,''),'  ',' ') full_name,
                p.last_name,
                p.middle_name,
                p.first_name,
                p.sex_oid,
                osx.item_name sex_name,
                p.date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', p.date_of_birth) as int) age,
                p.nin,
                p.portrait_uuid,
                mp.base64_data portrait_base64_data,
                mp.display_orientation portrait_display_orientation,
                mp.active portrait_active,
                mp.created_at portrait_created_at,
                mp.created_by portrait_created_by,
                p.phone_1,
                p.phone_2,
                p.email,
                p.address,
                p.fp_lt_uuid,
                p.fp_li_uuid,
                p.fp_rt_uuid,
                p.fp_ri_uuid,
                p.created_at person_created_at,
                p.created_by person_created_by,
                p.updated_at person_updated_at,
                p.updated_by person_updated_by,
                p.deleted_at person_deleted_at,
                p.deleted_by person_deleted_by,
                t.school_uuid,
                s.name school_name,
                t.employment_status_oid,
                oes.item_name employment_status_name,
                t.teacher_role_oid teacher_role_oid,
                oer.item_name teacher_role_name,
                null teacher_role_other,
                t.pin,
                t.nassit_number,
                t.tsc_licence_id,
                t.start_date,
                t.end_date,
                t.end_reason_teacher_oid end_reason_oid,
                oen.item_name end_reason_name,
                t.end_reason_teacher_other end_reason_other,
                t.end_reason_teacher_detail end_reason_detail,
                t.created_at,
                t.created_by,
                t.updated_at,
                t.updated_by,
                t.deleted_at,
                t.deleted_by,
                t.sync_flag
            FROM teacher t
            LEFT JOIN person p ON t.person_uuid = p.uuid 
            LEFT JOIN school s ON t.school_uuid = s.uuid
            LEFT JOIN option_list osx ON osx.list_name = 'sex' AND p.sex_oid = osx.item_id
            LEFT JOIN option_list oes ON oes.list_name = 'employment_status' AND t.employment_status_oid = oes.item_id
            LEFT JOIN option_list oer ON oer.list_name = 'teacher_role' AND t.teacher_role_oid = oer.item_id 
            LEFT JOIN option_list oen ON oen.list_name = 'end_reason_teacher' AND t.end_reason_teacher_oid = oen.item_id
            LEFT JOIN media_photo mp ON mp.uuid = p.portrait_uuid
        """
    const val TEACHER_PAYROLL_MODEL = """
            SELECT 
                tp.uuid,
                tp.first_name,
                tp.middle_name,
                tp.last_name,
                REPLACE(IFNULL(tp.last_name,'') || ', ' || IFNULL(tp.first_name,'') || ' ' || IFNULL(tp.middle_name,''),'  ',' ') full_name,
                tp.sex,
                tp.date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', tp.date_of_birth) as int) age,
                tp.pin,
                tp.nin,
                tp.nassit_number,
                tp.created_at,
                tp.updated_at
            FROM teacher_payroll tp
        """

    const val TEACHER_TIMETABLE_MODEL = """
            SELECT
                tt.uuid,
                tt.teacher_uuid,
                tt.school_uuid,
                tt.school_group_uuid,
                tt.school_subject_oid,
                osj.item_name school_subject_name,
                tt.school_subject_other,
                tt.day_of_the_week_oid,
                otd.item_name day_of_the_week_name,
                tt.start_time,
                tt.end_time,
                tt.created_at,
                tt.created_by,
                tt.updated_at,
                tt.updated_by,
                tt.deleted_at,
                tt.deleted_by,
                tt.sync_flag
            FROM teacher_timetable tt
            LEFT JOIN option_list osj ON osj.list_name = 'school_subject' AND tt.school_subject_oid = osj.item_id
            LEFT JOIN option_list otd ON otd.list_name = 'day_of_the_week' AND tt.day_of_the_week_oid = otd.item_id
        """
}