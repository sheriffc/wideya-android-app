package uk.org.cgatechnologies.wideya.sync_device

import android.database.Cursor
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.JsonArray
import kotlinx.coroutines.flow.Flow
import uk.org.cgatechnologies.wideya.common.data.entities.*
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup
import uk.org.cgatechnologies.wideya.learner_performance.entities.LearnerPerformance
import uk.org.cgatechnologies.wideya.school_management.entities.School
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolAcademicYear
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerAdmission
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesBiTables
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesUniTables
import uk.org.cgatechnologies.wideya.sync_device.models.TableSchema
import uk.org.cgatechnologies.wideya.sync_device.models.TableState
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherPayroll
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherTimetable


@Dao
abstract class SyncDao {

    //log
    @Insert
    abstract fun insertLogEntry(logSync: LogSync)

    @Query("""
            SELECT * FROM log_sync
            ORDER BY id DESC
                """)
    abstract fun getLogSyncList(): Flow<List<LogSync>>

    //response

    //uni
    @Upsert
    abstract suspend fun upsertDistrictOffice(districtOffice: List<DistrictOffice>)

    @Upsert
    abstract suspend fun upsertSchoolAcademicYear(schoolAcademicYear: List<SchoolAcademicYear>)

    @Upsert
    abstract suspend fun upsertTeacherPayroll(teacherPayroll: List<TeacherPayroll>)

    @Upsert
    abstract suspend fun upsertOptionList(optionList: List<OptionList>)

    @Upsert
    abstract suspend fun upsertOptionListLink(optionList: List<OptionListLink>)

    @Upsert
    abstract suspend fun upsertGeo(geo: List<Geo>)

    @Upsert
    abstract suspend fun upsertLearner(learner: List<Learner>)

    @Upsert
    abstract suspend fun upsertPerson(person: List<Person>)

    @Upsert
    abstract suspend fun upsertPersonAttendance(person: List<PersonAttendance>)

    @Upsert
    abstract suspend fun upsertPersonFingerprint(personFingerprint: List<PersonFingerprint>)

    @Upsert
    abstract suspend fun upsertPersonContact(personContact: List<PersonContact>)

    @Upsert
    abstract suspend fun upsertMediaPhoto(mediaPhoto: List<MediaPhoto>)

    @Upsert
    abstract suspend fun upsertSchool(school: List<School>)

    @Upsert
    abstract suspend fun upsertSchoolGroup(schoolGroup: List<SchoolGroup>)

    @Upsert
    abstract suspend fun upsertSchoolLearnerAdmission(schoolLearnerAdmission: List<SchoolLearnerAdmission>)

    @Upsert
    abstract suspend fun upsertSchoolLearnerEnrolment(schoolLearnerEnrolment: List<SchoolLearnerEnrolment>)

    @Upsert
    abstract suspend fun upsertTeacher(teacher: List<Teacher>)

    @Upsert
    abstract suspend fun upsertTeacherTimetable(teacherTimetable: List<TeacherTimetable>)

    @Upsert
    abstract suspend fun upsertSchoolFeeding(schoolFeeding: List<SchoolFeeding>)

    @Upsert
    abstract suspend fun upsertSchoolFeedingStock(schoolFeedingStock: List<SchoolFeedingStock>)

    @Upsert
    abstract suspend fun upsertLearnerPerformance(learnerPerformance: List<LearnerPerformance>)

    @Upsert
    abstract suspend fun upsertTableStatesBiTables(tableStatesBiTables: List<TableStatesBiTables>)

    @Upsert
    abstract suspend fun upsertTableStatesUniTables(tableStatesUniTables: List<TableStatesUniTables>)

    @Query("DELETE FROM log_sync")
    abstract fun truncateLogTable()

    //request
    @RawQuery
    abstract fun getTableState(query: SupportSQLiteQuery): List<TableState>

    @RawQuery
    abstract fun getTables(query: SupportSQLiteQuery): List<TableSchema>

    @RawQuery
    abstract fun getRecordCountToSync(query: SupportSQLiteQuery): Int

    @RawQuery
    abstract fun getRecordsToSync(query: SupportSQLiteQuery): Cursor

    @RawQuery
    abstract fun updateSyncFlag(query: SupportSQLiteQuery): Cursor

    // The server always echoes the effective learner_id for `learner` rows (see
    // AndroidSync ApiController/DataSync::persistLearnerRecord), whether it
    // changed or not, since the originating device otherwise has no way to learn
    // of a server-side collision correction: full-table `learner` downloads
    // exclude rows most recently synced by this device's own install id.
    @Query("UPDATE learner SET learner_id = :learnerId WHERE uuid = :uuid")
    abstract fun applyLearnerIdFromAck(uuid: String, learnerId: String)

    @Transaction
    open fun updateSyncFlagInTransaction(tableName: String, confirmedList: JsonArray): Int {
        var insertCounter = 0
        confirmedList.forEach {

            val obj = it.asJsonObject
            val pkCn: String = obj.get("pkCn").asString
            val id = obj.get("id").asString

            if (tableName == "learner" && obj.has("learner_id") && !obj.get("learner_id").isJsonNull) {
                applyLearnerIdFromAck(id, obj.get("learner_id").asString)
            }

            //updated the sync flag
            val queryString =
                "UPDATE :tableName SET sync_flag = 0 WHERE :pkCn = ':id'"
                    .replace(":tableName", tableName)
                    .replace(":pkCn", pkCn)
                    .replace(":id", id)

            val query = SimpleSQLiteQuery(queryString)
            val c = updateSyncFlag(query)
            c.moveToFirst()
            c.close()
            insertCounter += 1
        }
        return insertCounter
    }

    @RawQuery
    abstract fun walCheckpoint(query: SupportSQLiteQuery): Int
    @RawQuery
    abstract fun purgeDeletedFromTable(query: SupportSQLiteQuery): Int

    fun getSyncFlagCount(): Int{
        var recordCount = 0
        val tables = getTables(SimpleSQLiteQuery(SyncQueries.LIST_TABLES))
        tables.forEach {
            if (it.sql.contains("sync_flag")) {
                //check recs
                val queryString = SyncQueries.RECORD_COUNT_TO_SYNC.replace(":tableName", it.name)
                val recs = getRecordCountToSync(SimpleSQLiteQuery(queryString))
//                Log.d("syncdao", "${it.name} , $recs")
                recordCount += recs
            }
        }
        return recordCount
    }

    class SyncQueries {
        companion object
        {
            const val LIST_TABLES = """
                                        SELECT 
                                            name, 
                                            sql, 
                                            CASE 
                                                WHEN name = 'media_photo' THEN 100 
                                                WHEN name = 'person_fingerprint' THEN 80 
                                                WHEN name = 'teacher_payroll' THEN 50
                                                ELSE 0 
                                            END as sync_order 
                                        FROM sqlite_master 
                                        WHERE type = 'table' 
                                            AND name NOT LIKE 'sqlite_%' 
                                        ORDER BY sync_order, rootpage
                                """
            const val RECORD_COUNT_TO_SYNC = "SELECT SUM(sync_flag) FROM :tableName"
            const val RECORD_COUNT_FOR_PURGE = "SELECT COUNT(*) FROM :tableName WHERE deleted_at IS NOT NULL AND NOT sync_flag"
            const val PURGE_DELETED = "DELETE FROM :tableName WHERE deleted_at IS NOT NULL AND NOT sync_flag"
            const val TABLE_STATE_UNI_UUID = """
                                        SELECT
                                            ':tableName' table_name,
                                            count(*) rec_count,
                                            max(uuid) max_pk,
                                            max(updated_at) max_updated_at
                                        FROM :tableName
                                    """
            const val TABLE_STATE_UNI_ID = """
                                        SELECT
                                            ':tableName' table_name,
                                            count(*) rec_count,
                                            max(id) max_pk,
                                            max(updated_at) max_updated_at
                                        FROM :tableName
                                    """
            const val TABLE_STATE_UNI = """
                                        SELECT 
                                            a.table_name, a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (SELECT ':tableName' table_name, COUNT(*) rec_count FROM :tableName) a
                                        LEFT JOIN (
                                            SELECT table_name, max_synced_at, max_pk 
                                            FROM table_states_uni_tables a 
                                            WHERE table_name = ':tableName'
                                        ) b ON a.table_name = b.table_name 
                                    """
            const val TABLE_STATE_NON_PAYROLL_TEACHERS = """
                                        SELECT
                                            a.table_name, a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT 'non_payroll_teachers' table_name, COUNT(*) rec_count
                                            FROM teacher_payroll
                                            WHERE pin IS NULL OR pin = ''
                                        ) a
                                        LEFT JOIN (
                                            SELECT table_name, max_synced_at, max_pk
                                            FROM table_states_uni_tables
                                            WHERE table_name = 'non_payroll_teachers'
                                        ) b ON a.table_name = b.table_name
                                    """
            const val TABLE_STATE_LEARNER = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM learner l INNER JOIN school_learner_admission sla ON l.uuid = sla.learner_uuid
                                            WHERE sla.school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_PERSON = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM (
                                                SELECT p.*
                                                FROM person p INNER JOIN learner l ON p.uuid = l.person_uuid INNER JOIN school_learner_admission sla ON l.uuid = sla.learner_uuid
                                                WHERE sla.school_uuid IN (':schoolId')
                                                UNION 
                                                SELECT p.*
                                                FROM person p INNER JOIN learner l ON p.uuid = l.guardian_person_uuid INNER JOIN school_learner_admission sla ON l.uuid = sla.learner_uuid
                                                WHERE sla.school_uuid IN (':schoolId')
                                                UNION
                                                SELECT p.*
                                                FROM person p INNER JOIN teacher t ON p.uuid = t.person_uuid 
                                                WHERE t.school_uuid IN (':schoolId')
                                            ) u 
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_PERSON_ATTENDANCE = """                                        
                                        SELECT 
                                        a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM person_attendance 
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_SCHOOL = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM school 
                                            WHERE uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_SCHOOL_GROUP = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM school_group 
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_SCHOOL_LEARNER_ADMISSION = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM school_learner_admission 
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_SCHOOL_LEARNER_ENROLMENT = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM school_learner_enrolment sle 
                                            INNER JOIN school_group sg ON sle.school_group_uuid = sg.uuid 
                                            WHERE sg.school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_TEACHER = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM teacher 
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_PERSON_FINGERPRINT = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM person_fingerprint pf
                                            INNER JOIN person p ON pf.person_uuid = p.uuid
                                            INNER JOIN teacher t ON p.uuid = t.person_uuid
                                            WHERE t.school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_MEDIA_PHOTO = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM media_photo mp
                                            INNER JOIN person p ON mp.uuid = p.portrait_uuid
                                            INNER JOIN teacher t ON p.uuid = t.person_uuid
                                            WHERE t.school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_TEACHER_TIMETABLE = """
                                        SELECT 
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name, 
                                                count(*) rec_count
                                            FROM teacher_timetable 
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk 
                                            FROM table_states_bi_tables a 
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """

            const val TABLE_STATE_SCHOOL_FEEDING = """
                                        SELECT
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name,
                                                count(*) rec_count
                                            FROM school_feeding
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk
                                            FROM table_states_bi_tables a
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_SCHOOL_FEEDING_STOCK = """
                                        SELECT
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name,
                                                count(*) rec_count
                                            FROM school_feeding_stock
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk
                                            FROM table_states_bi_tables a
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """
            const val TABLE_STATE_LEARNER_PERFORMANCE = """
                                        SELECT
                                            a.rec_count, b.max_synced_at, b.max_pk
                                        FROM (
                                            SELECT
                                                ':schoolId' school_uuid,
                                                ':tableName' table_name,
                                                count(*) rec_count
                                            FROM learner_performance
                                            WHERE school_uuid IN (':schoolId')
                                        ) a
                                        LEFT JOIN (
                                            SELECT school_uuid, table_name, max_synced_at, max_pk
                                            FROM table_states_bi_tables a
                                            WHERE table_name=':tableName' AND school_uuid=':schoolId'
                                        ) b ON a.table_name = b.table_name AND a.school_uuid = b.school_uuid
                                    """

            const val WAL_CHECKPOINT = "pragma wal_checkpoint(full)"
        }
    }

}



