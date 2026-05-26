package uk.org.cgatechnologies.wideya.school_management

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.common.data.CommonDao
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.entities.School
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerAdmission
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel
import uk.org.cgatechnologies.wideya.school_management.models.SchoolTimetableModel
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementQueries
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel

@Dao
abstract class SchoolManagementDao {
    //SCHOOL
    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_MODEL}
            WHERE s.uuid = :uuid
                """
    )
    abstract fun getSchoolModel(uuid: String): Flow<SchoolModel?>

    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_MODEL}
            WHERE s.uuid = :uuid
                """
    )
    abstract fun getSchoolModelById(uuid: String): SchoolModel

    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_MODEL}
            WHERE s.active = 1
                AND (s.deleted_at IS NULL OR s.deleted_at = '')
                AND s.uuid IN (:uuids)
            ORDER BY education_level_display_order, s.name
        """
    )
    abstract fun getSchoolModelList(uuids: List<String>): Flow<List<SchoolModel>>

    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_MODEL}
            WHERE s.active = 1
                AND (s.deleted_at IS NULL OR s.deleted_at = '')
                AND s.uuid IN (:uuids)
                AND s.name LIKE '%' || :query || '%'
            ORDER BY education_level_display_order, s.name
        """
    )
    abstract fun getSchoolModelListByQuery(
        query: String?,
        uuids: List<String>
    ): Flow<List<SchoolModel>>

    @RawQuery(observedEntities = [School::class])
    abstract fun getSchoolModelListByRawQuery(query: SupportSQLiteQuery): Flow<List<SchoolModel>>

    fun buildSchoolModelListByRawQuery(query: String, uuids: List<String>): Flow<List<SchoolModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString = ""
        searchTerms.forEach {
            whereString += "AND ( s.name LIKE '%' || '${it}' || '%' )"
        }

        var uuidsCommaSeparatedString = uuids.joinToString { it -> "\'${it}\'" }

        val queryString = """
                    ${SchoolManagementQueries.SCHOOL_MODEL}
                    WHERE s.active = 1
                        AND (s.deleted_at IS NULL OR s.deleted_at = '')
                        AND s.uuid IN ($uuidsCommaSeparatedString)
                        $whereString
                    ORDER BY education_level_display_order, s.name
                """
        return getSchoolModelListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    @Update
    abstract suspend fun updateSchool(school: School)

    @Insert
    abstract suspend fun insertSchool(school: School): Long

    // https://developer.android.com/reference/androidx/room/Delete
    @Delete
    abstract suspend fun deleteSchool(school: School)

    //school learner admission
    @Update
    abstract suspend fun updateSchoolLearnerAdmission(schoolLearnerAdmission: SchoolLearnerAdmission)

    @Insert
    abstract suspend fun insertSchoolLearnerAdmission(schoolLearnerAdmission: SchoolLearnerAdmission): Long

    //school learner enrolment
    @Update
    abstract suspend fun updateSchoolLearnerEnrolment(schoolLearnerEnrolment: SchoolLearnerEnrolment)

    @Insert
    abstract suspend fun insertSchoolLearnerEnrolment(schoolLearnerEnrolment: SchoolLearnerEnrolment): Long

    @Delete
    abstract suspend fun deleteSchoolLearnerEnrolment(schoolLearnerEnrolment: SchoolLearnerEnrolment)

    @Query(
        """
            UPDATE school_learner_enrolment
            ${CommonDao.CommonQueries.SOFT_DELETE_SET}
            WHERE uuid = :schoolLearnerEnrolmentUuid
                """
    )
    abstract fun softDeleteSchoolLearnerEnrolment(schoolLearnerEnrolmentUuid: String, userId: Int)

    @Query(
        """
            UPDATE school_learner_admission
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                end_date = date('now'),
                end_reason_learner_oid = 'transfer',
                sync_flag = 1
            WHERE learner_uuid = :learnerUuid
                AND school_uuid != :newSchoolUuid
                AND (deleted_at IS NULL OR deleted_at = '')
        """
    )
    abstract suspend fun closeAdmissionsAtOtherSchools(learnerUuid: String, newSchoolUuid: String, userId: Int)

    @Query(
        """
            UPDATE school_learner_enrolment
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                sync_flag = 1
            WHERE learner_uuid = :learnerUuid
                AND (deleted_at IS NULL OR deleted_at = '')
                AND school_group_uuid IN (
                    SELECT uuid FROM school_group
                    WHERE school_uuid != :newSchoolUuid
                        AND (deleted_at IS NULL OR deleted_at = '')
                )
        """
    )
    abstract suspend fun closeEnrolmentsAtOtherSchools(learnerUuid: String, newSchoolUuid: String, userId: Int)

    @Query(
        """
            UPDATE school_learner_admission
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                sync_flag = 1
            WHERE school_uuid = :schoolUuid
                AND (deleted_at IS NULL OR deleted_at = '')
                AND learner_uuid IN (
                    SELECT uuid FROM learner WHERE learner_id IS NULL OR learner_id = ''
                )
        """
    )
    abstract suspend fun softDeleteAdmissionsWithoutLearnerId(schoolUuid: String, userId: Int)

    @Query(
        """
            UPDATE school_learner_enrolment
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                sync_flag = 1
            WHERE (deleted_at IS NULL OR deleted_at = '')
                AND learner_uuid IN (
                    SELECT sla.learner_uuid FROM school_learner_admission sla
                    INNER JOIN learner l ON l.uuid = sla.learner_uuid
                    WHERE sla.school_uuid = :schoolUuid
                        AND (l.learner_id IS NULL OR l.learner_id = '')
                )
        """
    )
    abstract suspend fun softDeleteEnrolmentsWithoutLearnerId(schoolUuid: String, userId: Int)

    //TEACHERS
    @Query(
        """
            ${TeacherManagementQueries.TEACHER_MODEL}
            WHERE t.school_uuid = :uuid
                AND (t.deleted_at IS NULL OR t.deleted_at = '')
            ORDER BY full_name
                """
    )
    abstract fun getSchoolTeacherList(uuid: String?): Flow<List<TeacherModel>>

    @Query(
        """
            ${TeacherManagementQueries.TEACHER_MODEL}
            WHERE t.school_uuid = :uuid 
                AND (t.deleted_at IS NULL OR t.deleted_at = '')
                AND ( full_name LIKE '%' || :query || '%' OR t.pin LIKE '%' || :query || '%' )
            ORDER BY full_name
                """
    )
    abstract fun getSchoolTeacherListByQuery(
        uuid: String?,
        query: String?
    ): Flow<List<TeacherModel>>

    @RawQuery(observedEntities = [Teacher::class])
    abstract fun getSchoolTeacherListByRawQuery(query: SupportSQLiteQuery): Flow<List<TeacherModel>>

    fun buildSchoolTeacherListByRawQuery(uuid: String, query: String): Flow<List<TeacherModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString  = ""
        searchTerms.forEach {
            whereString += "AND ( full_name LIKE '%' || '${it}' || '%' OR t.pin LIKE '%' || '${it}' || '%' )"
        }

        val queryString = """
                    ${TeacherManagementQueries.TEACHER_MODEL}
                    WHERE t.school_uuid = '$uuid' 
                        AND (t.deleted_at IS NULL OR t.deleted_at = '')
                        $whereString
                    ORDER BY full_name
                """
        return getSchoolTeacherListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    //LEARNERS
    @Query(
        """
            ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND (l.learner_id IS NOT NULL AND l.learner_id != '')
            ORDER BY learner_full_name
                """
    )
    abstract fun getSchoolLearnerAdmissionList(uuid: String?): Flow<List<LearnerAdmissionModel>>

    @Query(
        """
            ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
            WHERE sla.school_uuid = :uuid
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                AND (l.learner_id IS NOT NULL AND l.learner_id != '')
                AND ( learner_full_name LIKE '%' || :query || '%'  OR admission_number LIKE '%' || :query || '%' )
            ORDER BY learner_full_name
                """
    )
    abstract fun getSchoolLearnerAdmissionListByQuery(
        uuid: String?,
        query: String?
    ): Flow<List<LearnerAdmissionModel>>

    @RawQuery(observedEntities = [SchoolLearnerAdmission::class])
    abstract fun getSchoolLearnerAdmissionListByRawQuery(query: SupportSQLiteQuery): Flow<List<LearnerAdmissionModel>>

    fun buildSchoolLearnerAdmissionListByRawQuery(uuid: String, query: String): Flow<List<LearnerAdmissionModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString  = ""
        searchTerms.forEach {
            whereString += "AND ( learner_full_name LIKE '%' || '${it}' || '%' OR admission_number LIKE '%' || '${it}' || '%' )"
        }

        val queryString = """
                    ${SchoolManagementQueries.LEARNER_ADMISSION_MODEL}
                    WHERE sla.school_uuid = '$uuid'
                        AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
                        AND (l.learner_id IS NOT NULL AND l.learner_id != '')
                        $whereString
                    ORDER BY learner_full_name
                """
        return getSchoolLearnerAdmissionListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    //SCHOOL GROUP
    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_GROUP_MODEL}
            WHERE sg.school_uuid = :uuid
                AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                AND sg.active
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
            GROUP BY sg.uuid
            ORDER BY 
                CAST(ogl.item_extra AS SIGNED), -- order by level of schoolgroup
                sg.school_group_name
                """
    )
    abstract fun getSchoolGroupList(uuid: String?): Flow<List<SchoolGroupModel>>

    //SCHOOL GROUP

    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_GROUP_MODEL}
            WHERE sg.school_uuid = :uuid 
                AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                AND sg.active
                AND ( teacher_full_name LIKE '%' || :query || '%' OR school_group_name LIKE '%' || :query || '%' )
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
            GROUP BY sg.uuid
            ORDER BY 
                CAST(ogl.item_extra AS SIGNED), -- order by level of schoolgroup
                sg.school_group_name
                """
    )
    abstract fun getSchoolGroupListByQuery(
        uuid: String?,
        query: String?
    ): Flow<List<SchoolGroupModel>>

    @RawQuery(observedEntities = [SchoolGroup::class])
    abstract fun getSchoolGroupListByRawQuery(query: SupportSQLiteQuery): Flow<List<SchoolGroupModel>>

    fun buildSchoolGroupListByRawQuery(uuid: String, query: String): Flow<List<SchoolGroupModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString  = ""
        searchTerms.forEach {
            whereString += "AND ( teacher_full_name LIKE '%' || '${it}' || '%' OR school_group_name LIKE '%' || '${it}' || '%' OR school_group_level_name LIKE '%' || '${it}' || '%')"
        }

        val queryString = """
                    ${SchoolManagementQueries.SCHOOL_GROUP_MODEL}
                    WHERE sg.school_uuid = '$uuid' 
                        AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                        AND sg.active
                        AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                        $whereString
                    GROUP BY sg.uuid
                    ORDER BY 
                        CAST(ogl.item_extra AS SIGNED), -- order by level of schoolgroup
                        sg.school_group_name
                """
        return getSchoolGroupListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    @Query(
        """
            ${SchoolManagementQueries.SCHOOL_TIMETABLE_MODEL}
            WHERE tt.school_uuid = :uuid 
        """
    )
    abstract fun getSchoolTimetableList(uuid: String?): Flow<List<SchoolTimetableModel>>

    @Query(
        """
            ${TeacherManagementQueries.TEACHER_MODEL}
            WHERE t.uuid = :uuid 
        """
    )
    abstract fun getSelectedTeacherById(uuid: String): TeacherModel?
}

object SchoolManagementQueries {
    const val SCHOOL_MODEL = """
            SELECT 
                s.uuid,
                s.name,
                s.school_education_level_oid education_level_oid,
                sel.item_name education_level_name,
                sel.display_order education_level_display_order,
                s.emis_id,
                s.wideya_id,
                s.payroll_sid,
                s.waec_id,
                s.fabinc_recordid,
                s.district_id,
                g_dis.name district_name,
                s.chiefdom_id,
                g_chi.name chiefdom_name,
                s.section_name section_name,
                s.town_name town_name,
                s.address,
                s.classrooms_oid,
                s.wash_oids,
                s.electricity_oids,
                s.mno_oids,
                s.learning_materials_oids,
                s.receives_feeding,
                s.district_office_uuid,
                do.name district_office_name,
                s.lat,
                s.lng,
                s.media_photo_uuid,
                s.tablet_phone_number,
                s.active,
                s.created_at,
                s.created_by,
                s.updated_at,
                s.updated_by,
                s.deleted_at,
                s.deleted_by,
                s.sync_flag
            FROM school s
            LEFT JOIN option_list sel ON sel.list_name='school_education_level' AND s.school_education_level_oid = sel.item_id AND sel.active
            LEFT JOIN geo g_chi ON s.chiefdom_id = g_chi.id AND g_chi.active
            LEFT JOIN geo g_dis ON g_chi.parent_id = g_dis.id AND g_dis.active
            LEFT JOIN district_office do ON s.district_office_uuid = do.uuid AND do.active
        """
    const val LEARNER_ADMISSION_MODEL = """
            SELECT 
                sla.uuid,
                sla.school_uuid,
                s.name school_name,
                sla.learner_uuid,
                sla.created_at learner_admission_created_at,
                sla.updated_at learner_admission_updated_at,
                sla.deleted_at learner_admission_deleted_at,
                l.learner_id,
                l.created_at learner_created_at,
                l.updated_at learner_updated_at,
                REPLACE(IFNULL(pl.last_name,'') || ', ' || IFNULL(pl.first_name,'') || ' ' || IFNULL(pl.middle_name,''),'  ',' ') learner_full_name,
                pl.first_name learner_first_name,
                pl.middle_name learner_middle_name,
                pl.last_name learner_last_name,
                pl.nin learner_nin,
                pl.date_of_birth learner_date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', pl.date_of_birth) as int) learner_age,
                pl.portrait_uuid learner_portrait_uuid,
                pl.uuid learner_person_uuid,
                pl.created_at learner_person_created_at,
                pl.updated_at learner_person_updated_at,
                pl.sex_oid learner_sex_oid,
                pl_osx.item_name learner_sex_name,
                l.guardian_person_uuid,
                REPLACE(IFNULL(pg.last_name,'') || ', ' || IFNULL(pg.first_name,'') || ' ' || IFNULL(pg.middle_name,''),'  ',' ') guardian_full_name,
                pg.first_name guardian_first_name,
                pg.middle_name guardian_middle_name,
                pg.last_name guardian_last_name,
                pg.nin guardian_nin,
                pg.date_of_birth guardian_date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', pl.date_of_birth) as int) guardian_age,
                pg.portrait_uuid guardian_portrait_uuid,
                pg.phone_1 guardian_phone_1,
                pg.phone_2 guardian_phone_2,
                pg.email guardian_email,
                pg.address guardian_address,
                pg.created_at guardian_person_created_at,
                pg.updated_at guardian_person_updated_at,
                pg.sex_oid guardian_sex_oid,
                pg_osx.item_name guardian_sex_name,
                l.language_oid_strongest language_oid_strongest,
                l_osl.item_name language_oid_strongest_name,
                l.maternal_status_oid learner_maternal_status_oid,
                l_oms.item_name learner_maternal_status_name,
                l.maternal_status_updated_at learner_maternal_status_updated_at,
                l.disability_severity_oid_vision learner_disability_severity_oid_vision,
                l_odv.item_name learner_disability_severity_oid_vision_name,
                l.disability_severity_oid_hearing learner_disability_severity_oid_hearing,
                l_odh.item_name learner_disability_severity_oid_hearing_name,
                l.disability_severity_oid_mobility learner_disability_severity_oid_mobility,
                l_odm.item_name learner_disability_severity_oid_mobility_name,
                l.disability_severity_oid_cognition learner_disability_severity_oid_cognition,
                l_odcg.item_name learner_disability_severity_oid_cognition_name,
                l.disability_severity_oid_selfcare learner_disability_severity_oid_selfcare,
                l_ods.item_name learner_disability_severity_oid_selfcare_name,
                l.disability_severity_oid_communication learner_disability_severity_oid_communication,
                l_odcm.item_name learner_disability_severity_oid_communication_name,
                l.disability_other_condition_oid learner_disability_other_condition_oid,
                l_odoc.item_name learner_disability_other_condition_name,
                l.guardian_relation_to_learner_oid,
                ogr.item_name guardian_relation_to_learner_name,
                l.guardian_relation_to_learner_other,
                sla.admission_number,
                sla.start_date,
                sla.end_date,
                sla.end_reason_learner_oid,
                oen.item_name end_reason_learner_name,
                sla.end_reason_learner_other,
                sla.end_reason_learner_detail,
                sle.uuid enrolment_uuid,
                sle.academic_year enrolment_current_academic_year,
                say.academic_year_name enrolment_current_academic_year_name,
                sg.uuid enrolment_school_group_uuid,
                sg.school_group_name enrolment_school_group_name,
                sg.school_group_level_oid enrolment_school_group_level_oid,
                sg_ogl.item_name enrolment_school_group_level_name,
                IFNULL(sg_ogl.item_name, '') || ' ' || IFNULL(sg.school_group_name, '') enrolment_school_group_concat_name,
                IFNULL(sg_ogl_prev.item_name, '') || ' ' || IFNULL(sg_prev.school_group_name, '') prev_enrolment_school_group_concat_name,
                say_prev.academic_year_name prev_enrolment_academic_year_name,
                sla.created_at,
                sla.created_by,
                sla.updated_at,
                sla.updated_by,
                sla.deleted_at,
                sla.deleted_by,
                sla.sync_flag
            FROM school_learner_admission sla 
            LEFT JOIN learner l ON sla.learner_uuid = l.uuid
            LEFT JOIN person pl ON l.person_uuid = pl.uuid
            LEFT JOIN school s ON sla.school_uuid = s.uuid 
            LEFT JOIN person pg ON l.guardian_person_uuid = pg.uuid 
            
            LEFT JOIN school_learner_enrolment sle
                ON sle.uuid = (
                    SELECT sle2.uuid FROM school_learner_enrolment sle2
                    WHERE sle2.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                    AND sle2.learner_uuid = sla.learner_uuid
                    AND (sle2.deleted_at IS NULL OR sle2.deleted_at = '')
                    AND (
                        sle2.school_group_uuid IN (SELECT uuid FROM school_group WHERE school_uuid = sla.school_uuid)
                        OR sle2.school_group_uuid IS NULL OR sle2.school_group_uuid = ''
                    )
                    ORDER BY sle2.updated_at DESC
                    LIMIT 1
                )
            LEFT JOIN school_group sg ON sle.school_group_uuid = sg.uuid
                AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                AND sg.active

            LEFT JOIN school_learner_enrolment sle_prev
                ON sle_prev.academic_year = (
                    SELECT MAX(academic_year) FROM school_academic_year
                    WHERE academic_year < (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
                )
                AND sle_prev.learner_uuid = sla.learner_uuid
                AND (sle_prev.deleted_at IS NULL OR sle_prev.deleted_at = '')
                AND sle_prev.school_group_uuid IN (
                    SELECT uuid FROM school_group WHERE school_uuid = sla.school_uuid
                )
            LEFT JOIN school_group sg_prev ON sle_prev.school_group_uuid = sg_prev.uuid
                AND (sg_prev.deleted_at IS NULL OR sg_prev.deleted_at = '')
            LEFT JOIN option_list sg_ogl_prev ON sg_ogl_prev.list_name = 'school_group_level' AND sg_prev.school_group_level_oid = sg_ogl_prev.item_id
            LEFT JOIN school_academic_year say_prev ON sle_prev.academic_year = say_prev.academic_year

            LEFT JOIN option_list pl_osx ON pl_osx.list_name = 'sex' AND pl.sex_oid = pl_osx.item_id
            LEFT JOIN option_list pg_osx ON pg_osx.list_name = 'sex' AND pg.sex_oid = pg_osx.item_id
            LEFT JOIN option_list ogr ON ogr.list_name = 'guardian_relation_to_learner' AND l.guardian_relation_to_learner_oid = ogr.item_id
            LEFT JOIN option_list oen ON oen.list_name = 'end_reason_learner' AND sla.end_reason_learner_oid = oen.item_id
            LEFT JOIN option_list sg_ogl ON sg_ogl.list_name = 'school_group_level' AND sg.school_group_level_oid = sg_ogl.item_id
            LEFT JOIN option_list l_osl ON l_osl.list_name = 'language' AND l.language_oid_strongest = l_osl.item_id
            LEFT JOIN option_list l_oms ON l_oms.list_name = 'maternal_status' AND l.maternal_status_oid = l_oms.item_id
            
            LEFT JOIN option_list l_odv ON l_odv.list_name = 'disability_severity' AND l.disability_severity_oid_vision = l_odv.item_id
            LEFT JOIN option_list l_odh ON l_odh.list_name = 'disability_severity' AND l.disability_severity_oid_hearing = l_odh.item_id
            LEFT JOIN option_list l_odm ON l_odm.list_name = 'disability_severity' AND l.disability_severity_oid_mobility = l_odm.item_id
            LEFT JOIN option_list l_odcg ON l_odcg.list_name = 'disability_severity' AND l.disability_severity_oid_cognition = l_odcg.item_id
            LEFT JOIN option_list l_ods ON l_ods.list_name = 'disability_severity' AND l.disability_severity_oid_selfcare = l_ods.item_id
            LEFT JOIN option_list l_odcm ON l_odcm.list_name = 'disability_severity' AND l.disability_severity_oid_communication = l_odcm.item_id
            LEFT JOIN option_list l_odoc ON l_odoc.list_name = 'disability_other_condition' AND l.disability_other_condition_oid = l_odoc.item_id
            
            LEFT JOIN school_academic_year say ON sle.academic_year = say.academic_year

        """
    // Same columns as LEARNER_ADMISSION_MODEL but base table is learner (no admission record yet).
    // NIN/learner_id search only; sla/enrollment/school fields are NULL.
    const val LEARNER_SEARCH_MODEL = """
            SELECT
                l.uuid,
                NULL school_uuid,
                NULL school_name,
                l.uuid learner_uuid,
                NULL learner_admission_created_at,
                NULL learner_admission_updated_at,
                NULL learner_admission_deleted_at,
                l.learner_id,
                l.created_at learner_created_at,
                l.updated_at learner_updated_at,
                REPLACE(IFNULL(pl.last_name,'') || ', ' || IFNULL(pl.first_name,'') || ' ' || IFNULL(pl.middle_name,''),'  ',' ') learner_full_name,
                pl.first_name learner_first_name,
                pl.middle_name learner_middle_name,
                pl.last_name learner_last_name,
                pl.nin learner_nin,
                pl.date_of_birth learner_date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', pl.date_of_birth) as int) learner_age,
                pl.portrait_uuid learner_portrait_uuid,
                pl.uuid learner_person_uuid,
                pl.created_at learner_person_created_at,
                pl.updated_at learner_person_updated_at,
                pl.sex_oid learner_sex_oid,
                pl_osx.item_name learner_sex_name,
                l.guardian_person_uuid,
                REPLACE(IFNULL(pg.last_name,'') || ', ' || IFNULL(pg.first_name,'') || ' ' || IFNULL(pg.middle_name,''),'  ',' ') guardian_full_name,
                pg.first_name guardian_first_name,
                pg.middle_name guardian_middle_name,
                pg.last_name guardian_last_name,
                pg.nin guardian_nin,
                pg.date_of_birth guardian_date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', pg.date_of_birth) as int) guardian_age,
                pg.portrait_uuid guardian_portrait_uuid,
                pg.phone_1 guardian_phone_1,
                pg.phone_2 guardian_phone_2,
                pg.email guardian_email,
                pg.address guardian_address,
                pg.created_at guardian_person_created_at,
                pg.updated_at guardian_person_updated_at,
                pg.sex_oid guardian_sex_oid,
                pg_osx.item_name guardian_sex_name,
                l.language_oid_strongest language_oid_strongest,
                l_osl.item_name language_oid_strongest_name,
                l.maternal_status_oid learner_maternal_status_oid,
                l_oms.item_name learner_maternal_status_name,
                l.maternal_status_updated_at learner_maternal_status_updated_at,
                l.disability_severity_oid_vision learner_disability_severity_oid_vision,
                l_odv.item_name learner_disability_severity_oid_vision_name,
                l.disability_severity_oid_hearing learner_disability_severity_oid_hearing,
                l_odh.item_name learner_disability_severity_oid_hearing_name,
                l.disability_severity_oid_mobility learner_disability_severity_oid_mobility,
                l_odm.item_name learner_disability_severity_oid_mobility_name,
                l.disability_severity_oid_cognition learner_disability_severity_oid_cognition,
                l_odcg.item_name learner_disability_severity_oid_cognition_name,
                l.disability_severity_oid_selfcare learner_disability_severity_oid_selfcare,
                l_ods.item_name learner_disability_severity_oid_selfcare_name,
                l.disability_severity_oid_communication learner_disability_severity_oid_communication,
                l_odcm.item_name learner_disability_severity_oid_communication_name,
                l.disability_other_condition_oid learner_disability_other_condition_oid,
                l_odoc.item_name learner_disability_other_condition_name,
                l.guardian_relation_to_learner_oid,
                ogr.item_name guardian_relation_to_learner_name,
                l.guardian_relation_to_learner_other,
                NULL admission_number,
                NULL start_date,
                NULL end_date,
                NULL end_reason_learner_oid,
                NULL end_reason_learner_name,
                NULL end_reason_learner_other,
                NULL end_reason_learner_detail,
                NULL enrolment_uuid,
                NULL enrolment_current_academic_year,
                NULL enrolment_current_academic_year_name,
                NULL enrolment_school_group_uuid,
                NULL enrolment_school_group_name,
                NULL enrolment_school_group_level_oid,
                NULL enrolment_school_group_level_name,
                NULL enrolment_school_group_concat_name,
                l.created_at,
                l.created_by,
                l.updated_at,
                l.updated_by,
                l.deleted_at,
                l.deleted_by,
                l.sync_flag
            FROM learner l
            LEFT JOIN person pl ON l.person_uuid = pl.uuid
            LEFT JOIN person pg ON l.guardian_person_uuid = pg.uuid
            LEFT JOIN option_list pl_osx ON pl_osx.list_name = 'sex' AND pl.sex_oid = pl_osx.item_id
            LEFT JOIN option_list pg_osx ON pg_osx.list_name = 'sex' AND pg.sex_oid = pg_osx.item_id
            LEFT JOIN option_list ogr ON ogr.list_name = 'guardian_relation_to_learner' AND l.guardian_relation_to_learner_oid = ogr.item_id
            LEFT JOIN option_list l_osl ON l_osl.list_name = 'language' AND l.language_oid_strongest = l_osl.item_id
            LEFT JOIN option_list l_oms ON l_oms.list_name = 'maternal_status' AND l.maternal_status_oid = l_oms.item_id
            LEFT JOIN option_list l_odv ON l_odv.list_name = 'disability_severity' AND l.disability_severity_oid_vision = l_odv.item_id
            LEFT JOIN option_list l_odh ON l_odh.list_name = 'disability_severity' AND l.disability_severity_oid_hearing = l_odh.item_id
            LEFT JOIN option_list l_odm ON l_odm.list_name = 'disability_severity' AND l.disability_severity_oid_mobility = l_odm.item_id
            LEFT JOIN option_list l_odcg ON l_odcg.list_name = 'disability_severity' AND l.disability_severity_oid_cognition = l_odcg.item_id
            LEFT JOIN option_list l_ods ON l_ods.list_name = 'disability_severity' AND l.disability_severity_oid_selfcare = l_ods.item_id
            LEFT JOIN option_list l_odcm ON l_odcm.list_name = 'disability_severity' AND l.disability_severity_oid_communication = l_odcm.item_id
            LEFT JOIN option_list l_odoc ON l_odoc.list_name = 'disability_other_condition' AND l.disability_other_condition_oid = l_odoc.item_id
        """

    const val SCHOOL_GROUP_MODEL = """
            SELECT 
                sg.uuid,
                sg.school_uuid,
                s.name school_name,
                s.school_education_level_oid,
                sg.academic_year,
                say.academic_year_name,
                sg.school_group_name,
                sg.school_group_level_oid,
                ogl.item_name school_group_level_name,
                sg.teacher_uuid,
                t.pin teacher_pin,
                REPLACE(IFNULL(pt.first_name,'') || ' ' || IFNULL(pt.middle_name,'') || ' ' || IFNULL(pt.last_name,''),'  ',' ') teacher_full_name,
                pt.first_name teacher_first_name,
                pt.middle_name teacher_middle_name,
                pt.last_name teacher_last_name,
                COUNT(sle.uuid) learner_count,
                0 display_order,
                sg.active,
                sg.created_at,
                sg.created_by,
                sg.updated_at,
                sg.updated_by,
                sg.deleted_at,
                sg.deleted_by,
                sg.sync_flag
            FROM school_group sg 
            LEFT JOIN school s ON sg.school_uuid = s.uuid
            LEFT JOIN school_academic_year say ON sg.academic_year = say.academic_year
            LEFT JOIN option_list ogl ON ogl.list_name = 'school_group_level' AND sg.school_group_level_oid = ogl.item_id
            LEFT JOIN teacher t ON t.uuid = sg.teacher_uuid
            LEFT JOIN person pt ON t.person_uuid = pt.uuid 
            LEFT JOIN school_learner_enrolment sle 
                ON sg.uuid = sle.school_group_uuid 
                AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
        """

    const val SCHOOL_TIMETABLE_MODEL = """
            SELECT
                tt.uuid,
                tt.teacher_uuid,
                REPLACE(IFNULL(p.last_name,'') || ', ' || IFNULL(p.first_name,'') || ' ' || IFNULL(p.middle_name,''),'  ',' ') teacher_full_name,       
                p.first_name teacher_first_name,
                p.middle_name teacher_middle_name,
                p.last_name teacher_last_name,
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
            LEFT JOIN teacher tch ON tt.teacher_uuid = tch.uuid 
            LEFT JOIN person p ON tch.person_uuid = p.uuid
            LEFT JOIN option_list osj ON osj.list_name = 'school_subject' AND tt.school_subject_oid = osj.item_id
            LEFT JOIN option_list otd ON otd.list_name = 'day_of_the_week' AND tt.day_of_the_week_oid = otd.item_id
        """
}