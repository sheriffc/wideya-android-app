package uk.org.cgatechnologies.wideya.school_group_management

import androidx.room.*
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.common.data.CommonDao
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementQueries
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment

@Dao
abstract class SchoolGroupManagementDao {

    //SCHOOL GROUP
    @Query("""
            ${SchoolManagementQueries.SCHOOL_GROUP_MODEL}
            WHERE sg.uuid = :uuid 
                """)
    abstract fun getSchoolGroupModel(uuid: String): Flow<SchoolGroupModel?>

    @Query("""
            ${SchoolGroupManagementQueries.SCHOOL_GROUP_AS_OPTION_LIST}
            WHERE sg.school_uuid = :uuid
                AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
                AND sg.active
                AND say.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
            ORDER BY
                CAST(ogl.item_extra AS SIGNED), -- order by level of schoolgroup
                sg.school_group_name
                """)
    abstract fun getSchoolGroupOptionsList(uuid: String?): List<OptionList>

    @Query("""
            ${SchoolGroupManagementQueries.TEACHERS_AS_OPTION_LIST}
            WHERE t.school_uuid = :uuid
                AND (t.deleted_at IS NULL OR t.deleted_at = '')
            ORDER BY item_name
                """)
    abstract fun getTeacherOptionsList(uuid: String): List<OptionList>

    @Query("""
            ${SchoolGroupManagementQueries.SCHOOL_GROUP_LEARNER_MODEL}
            WHERE sle.school_group_uuid = :schoolGroupUuid AND sle.academic_year = :academicYear
                AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
            ORDER BY learner_full_name
                """)
    abstract fun getSchoolGroupLearnerList(schoolGroupUuid: String, academicYear: Short): Flow<List<SchoolGroupLearnerModel>>

    @Query("""
            ${SchoolGroupManagementQueries.SCHOOL_GROUP_LEARNER_MODEL}
            WHERE sle.school_group_uuid = :schoolGroupUuid AND sle.academic_year = :academicYear
                AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
                AND ( learner_full_name LIKE '%' || :query || '%' OR admission_number LIKE '%' || :query || '%' )
            ORDER BY learner_full_name
                """
    )
    abstract fun getSchoolGroupLearnerListByQuery(schoolGroupUuid: String, academicYear: Short, query: String?): Flow<List<SchoolGroupLearnerModel>>

    @RawQuery(observedEntities = [SchoolLearnerEnrolment::class])
    abstract fun getSchoolGroupLearnerListByRawQuery(query: SupportSQLiteQuery): Flow<List<SchoolGroupLearnerModel>>

    fun buildSchoolGroupLearnerListByRawQuery(schoolGroupUuid: String, academicYear: Short, query: String): Flow<List<SchoolGroupLearnerModel>>{

        val searchTerms = query.replace("""[\s]+"""," ").split(" ")

        var whereString  = ""
        searchTerms.forEach {
            whereString += "AND (learner_full_name LIKE '%' || '${it}' || '%' OR admission_number LIKE '%' || '${it}' || '%' )"
        }

        val queryString = """
            ${SchoolGroupManagementQueries.SCHOOL_GROUP_LEARNER_MODEL}
            WHERE sle.school_group_uuid = '$schoolGroupUuid' AND sle.academic_year = '$academicYear'
                AND (sle.deleted_at IS NULL OR sle.deleted_at = '')
                $whereString
                ORDER BY learner_full_name
            """
        return getSchoolGroupLearnerListByRawQuery(SimpleSQLiteQuery(queryString))
    }

    @Query("""
        SELECT MAX(IFNULL(sg.school_group_name, '')) 
        FROM school_group sg
        WHERE sg.school_uuid = :schoolUuid 
            AND sg.school_group_level_oid = :schoolGroupLevelOid
            AND sg.uuid != :schoolGroupUuid
            AND TRIM(UPPER(IFNULL(sg.school_group_name, ''))) = TRIM(UPPER(:schoolGroupName))
            AND (sg.deleted_at IS NULL OR sg.deleted_at = '')
            AND sg.active
            AND sg.academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
    """)
    abstract fun checkDuplicateSchoolGroupNameAndLevel(schoolGroupName: String, schoolUuid: String, schoolGroupLevelOid: String, schoolGroupUuid: String): String?

    @Update
    abstract suspend fun updateSchoolGroup(schoolGroup: SchoolGroup)

    @Insert
    abstract suspend fun insertSchoolGroup(schoolGroup: SchoolGroup): Long

    @Delete
    abstract suspend fun deleteSchoolGroup(schoolGroup: SchoolGroup)

    @Query("""
        UPDATE school_group
        ${CommonDao.CommonQueries.SOFT_DELETE_SET}
        WHERE uuid = :uuid
           """)
    abstract fun softDeleteSchoolGroup(uuid: String, userId: Int)

    @Query("""
        UPDATE school_learner_enrolment
        SET school_group_uuid = NULL,
            updated_at = CURRENT_TIMESTAMP,
            updated_by = :userId,
            sync_flag = 1
        WHERE school_group_uuid = :schoolGroupUuid
            """)
    abstract fun softDeleteSchoolLearnerEnrolmentBySchoolGroup(schoolGroupUuid: String, userId: Int)

}

class SchoolGroupManagementQueries {
    companion object {
        const val TEACHERS_AS_OPTION_LIST = """
            SELECT
                '' id,
                '' parent_id,
                '' list_name,
                REPLACE(IFNULL(p.last_name,'') || ', ' || IFNULL(p.first_name,'') || ' ' || IFNULL(p.middle_name,'') || ' - ' || IFNULL(t.pin,''),'  ',' ') item_name,
                t.uuid item_id,
                '' item_extra,
                '' display_order,
                '' active,
                '' created_at,
                '' updated_at
            FROM teacher t 
            INNER JOIN person p ON t.person_uuid = p.uuid
        """
        const val SCHOOL_GROUP_LEARNER_MODEL = """
            SELECT 
                sle.uuid,
                sle.academic_year,
                say.academic_year_name,
                sle.learner_uuid,
                REPLACE(IFNULL(pl.last_name,'') || ', ' || IFNULL(pl.first_name,'') || ' ' || IFNULL(pl.middle_name,''),'  ',' ') learner_full_name,
                pl.nin learner_nin,
                pl.date_of_birth learner_date_of_birth,
                cast(strftime('%Y.%m%d', 'now') - strftime('%Y.%m%d', pl.date_of_birth) as int) learner_age,
                pl.portrait_uuid learner_portrait_uuid,
                pl.created_at learner_person_created_at,
                pl.updated_at learner_person_updated_at,
                pl.sex_oid learner_sex_oid,
                pl_osx.item_name learner_sex_name,
                sla.admission_number admission_number,
                sle.school_group_uuid,
                0 checked,
                sle.created_at,
                sle.updated_at
            FROM school_learner_enrolment sle 
            LEFT JOIN school_group sg ON sle.school_group_uuid = sg.uuid 
            LEFT JOIN school_learner_admission sla 
                ON sg.school_uuid = sla.school_uuid 
                AND sle.learner_uuid = sla.learner_uuid 
                AND (sla.deleted_at IS NULL OR sla.deleted_at = '')
            LEFT JOIN learner l ON sle.learner_uuid = l.uuid 
            LEFT JOIN person pl ON l.person_uuid = pl.uuid 
            LEFT JOIN option_list pl_osx ON pl_osx.list_name = 'sex' AND pl.sex_oid = pl_osx.item_id
            LEFT JOIN school_academic_year say ON sle.academic_year = say.academic_year
        """

        const val SCHOOL_GROUP_AS_OPTION_LIST = """
            SELECT 
                '' id,
                '' parent_id,
                '' list_name,
                sg.uuid item_id,
                IFNULL(ogl.item_name, '') || ' ' || IFNULL(sg.school_group_name,'') item_name, 
                '' item_extra,
                '' display_order,
                '' active,
                '' created_at,
                '' updated_at
            FROM school_group sg
            LEFT JOIN option_list ogl ON ogl.item_id = sg.school_group_level_oid AND ogl.list_name = 'school_group_level'
            LEFT JOIN school_academic_year say ON sg.academic_year = say.academic_year
        """
    }
}