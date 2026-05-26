package uk.org.cgatechnologies.wideya.common.data

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import uk.org.cgatechnologies.wideya.common.data.entities.*
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.data.entities.Person
import uk.org.cgatechnologies.wideya.common.data.entities.PersonFingerprint
import uk.org.cgatechnologies.wideya.common.data.entities.PersonFingerprintA
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolAcademicYear

@Dao
abstract class CommonDao {

    //PERSON ENTITY OPERATIONS

    @Update
    abstract suspend fun updatePerson(person: Person)

    @Insert
    abstract suspend fun insertPerson(person: Person): Long

    @Delete
    abstract suspend fun deletePerson(person: Person)

    @Query(
        """
            UPDATE person
            ${CommonQueries.SOFT_DELETE_SET}
            WHERE uuid = :personUuid
                """
    )
    abstract fun softDeletePerson(personUuid: String, userId: Int)

    //PERSON FINGERPRINT ENTITY OPERATIONS

    @Insert
    abstract suspend fun insertPersonFingerprint(personFingerprint: PersonFingerprint)

    @Query("""
        UPDATE person_fingerprint
        ${CommonQueries.SOFT_DELETE_SET}
        WHERE person_uuid = :personUuid
        """)
    abstract suspend fun softDeletePersonFingerprintForPerson(personUuid: String, userId: Int)

    //MEDIA PHOTO ENTITY OPERATIONS

    @Insert
    abstract suspend fun insertMediaPhoto(mediaPhoto: MediaPhoto)

    @Update
    abstract suspend fun updateMediaPhoto(mediaPhoto: MediaPhoto)

    @Query(
        """
            UPDATE media_photo
            ${CommonQueries.SOFT_DELETE_SET}
            WHERE ref_uuid = :personUuid
                """
    )
    abstract fun softDeleteMediaPhotoForPersonUuid(personUuid: String, userId: Int)

    //QUERIES

    @RawQuery
    abstract fun getRecordCount(query: SupportSQLiteQuery): Int

    @Query("""
            SELECT * 
            FROM option_list 
            WHERE list_name=:listName 
                AND active 
            ORDER BY display_order
                """)
    abstract fun getOptionList(listName: String): List<OptionList>

    @Query("""
            SELECT 
                ol.*
            FROM option_list ol 
            LEFT JOIN option_list pol ON ol.parent_id = pol.id 
            WHERE ol.list_name = :listName 
                AND pol.item_id=:parentOid 
                AND ol.active
            ORDER BY ol.display_order
                """)
    abstract fun getOptionListForParentOid(listName: String, parentOid: String): List<OptionList>

    @Query(""" 
            SELECT 
                0 id,
                NULL parent_id,
                '' list_name,
                do.name item_name,
                do.uuid item_id,
                NULL item_extra,
                NULL item_asc_fabinc_recordid,
                NULL display_order,
                1 active,
                '' created_at,
                NULL updated_at,
                NULL deleted_at
            FROM district_office do
            WHERE active
                """)
    abstract fun getDistrictOfficesList(): List<OptionList>

    @Query("""
            SELECT
                ol_gl.*
            FROM option_list ol_gl
            INNER JOIN option_list_link oll
                ON oll.child_list_name = 'school_group_level'
                    AND oll.parent_list_name = 'school_education_level'
                    AND ol_gl.id = oll.child_id
            INNER JOIN option_list ol_sel
                ON ol_sel.list_name = 'school_education_level'
                    AND ol_sel.id = oll.parent_id
                    AND ol_sel.item_id = :schoolEducationLevelItemId
            WHERE ol_gl.list_name = 'school_group_level'
                AND ol_gl.active
            ORDER BY ol_gl.display_order, ol_gl.item_name
                """)
    abstract fun getSchoolGroupLevelsBySchoolEducationLevel(schoolEducationLevelItemId: String): List<OptionList>

    @Query(""" 
            SELECT 
                ol_ss.*
            FROM option_list ol_ss
            INNER JOIN option_list ol_rel 
                ON ol_rel.list_name = 'rudimentary_education_level'       
                    AND ol_ss.parent_id = ol_rel.id
            INNER JOIN option_list_link oll 
                ON oll.child_list_name = ol_rel.list_name
                    AND oll.parent_list_name = 'school_education_level' 
                    AND ol_rel.id = oll.child_id
            INNER JOIN option_list ol_sel 
                ON ol_sel.list_name = oll.parent_list_name
                    AND ol_sel.id = oll.parent_id 
                    AND ol_sel.item_id = :schoolEducationLevelItemId 
            WHERE ol_ss.list_name='school_subject' 
                AND ol_ss.active 
            ORDER BY ol_rel.display_order, ol_ss.display_order, ol_ss.item_name
                """)
    abstract fun getSchoolSubjectsBySchoolEducationLevel(schoolEducationLevelItemId: String): List<OptionList>

    @Query("""
        SELECT school_education_level_oid 
        FROM school 
        WHERE uuid=:schoolUuid
    """)
    abstract fun getEducationLevelForSchoolId(schoolUuid:String): String?

    // TODO: we probably want to think more carefully about this; this is a critical function and could go wrong if not careful
    @Query("""
        SELECT * 
        FROM school_academic_year 
        WHERE academic_year = (SELECT MAX(academic_year) FROM school_academic_year WHERE active)
    """)
    abstract fun getAcademicYear(): SchoolAcademicYear?

    @Query("""
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_li_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_lt_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_ri_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_rt_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
    """)
    abstract fun getAllPersonFingerprints(): List<PersonFingerprintA>

    @Query("""
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_li_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
            AND pf.person_uuid = :personUuid
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_lt_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
            AND pf.person_uuid = :personUuid
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_ri_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
            AND pf.person_uuid = :personUuid
        UNION
        SELECT
            pf.uuid,
            pf.person_uuid,
            pf.finger_position_oid,
            pf.fp_a_cbor,
            pf.fp_a_nfiq,
            pf.updated_at
        FROM person_fingerprint pf
        INNER JOIN person p ON pf.uuid = p.fp_rt_uuid
        WHERE (pf.deleted_at IS NULL OR pf.deleted_at = '')
            AND pf.person_uuid = :personUuid
    """)
    abstract fun getPersonFingerprints(personUuid: String): List<PersonFingerprintA>

    object CommonQueries {
        const val SOFT_DELETE_SET = """
            SET deleted_at = CURRENT_TIMESTAMP,
                deleted_by = :userId,
                updated_at = CURRENT_TIMESTAMP,
                updated_by = :userId,
                sync_flag = 1
            """
    }
}

