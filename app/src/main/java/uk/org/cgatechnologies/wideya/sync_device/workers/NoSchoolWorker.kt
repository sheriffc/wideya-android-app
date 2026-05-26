package uk.org.cgatechnologies.wideya.sync_device.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.school_management.PersonAttendanceRepository
import uk.org.cgatechnologies.wideya.school_management.models.NoSchoolPersonModel

class NoSchoolWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val schoolUuid  = inputData.getString(KEY_SCHOOL_UUID)  ?: return Result.failure()
        val dateStr     = inputData.getString(KEY_DATE)         ?: return Result.failure()
        val reasonOid   = inputData.getString(KEY_REASON_OID)   ?: return Result.failure()
        val reasonOther = inputData.getString(KEY_REASON_OTHER)

        val db   = AppDatabase.getInstance() ?: return Result.failure()
        val repo = PersonAttendanceRepository.getInstance(db.personAttendanceDao())

        withContext(Dispatchers.IO) {
            val acYear = Utils.getAcademicYear().toShort()
            val time   = Utils.getISODateTimeUTC()
            val user   = Utils.getUserId(applicationContext)
            val lat    = Utils.getLastKnownLat(applicationContext)
            val lng    = Utils.getLastKnownLng(applicationContext)

            val learners = repo.getSchoolLearnerPersonList(schoolUuid)
            val teachers = repo.getSchoolTeacherPersonList(schoolUuid)

            applyNoSchool(
                repo, learners, Constants.LEARNER_ENTITY_ID, schoolUuid, dateStr, acYear,
                reasonOid, reasonOther, lat, lng, time, user,
                amStatus = Constants.ATTENDANCE_ABSENT_ID, pmStatus = Constants.ATTENDANCE_ABSENT_ID,
                statusOid = null
            )
            applyNoSchool(
                repo, teachers, Constants.TEACHER_ENTITY_ID, schoolUuid, dateStr, acYear,
                reasonOid, reasonOther, lat, lng, time, user,
                amStatus = null, pmStatus = null,
                statusOid = Constants.ATTENDANCE_ABSENT_ID
            )
        }

        WorkerHelper.initUniqueUploadWorkRequest(applicationContext)
        return Result.success()
    }

    private suspend fun applyNoSchool(
        repo: PersonAttendanceRepository,
        persons: List<NoSchoolPersonModel>,
        entityType: String,
        schoolUuid: String,
        dateStr: String,
        acYear: Short,
        reasonOid: String,
        reasonOther: String?,
        lat: Float?,
        lng: Float?,
        time: String,
        user: Int,
        amStatus: String?,
        pmStatus: String?,
        statusOid: String?
    ) {
        if (persons.isEmpty()) return

        val existing = repo.getAttendanceByDateAndEntityType(schoolUuid, dateStr, entityType)
            .associateBy { it.person_uuid }

        val toInsert = mutableListOf<PersonAttendance>()
        val toUpdate = mutableListOf<PersonAttendance>()

        for (person in persons) {
            val prev = existing[person.person_uuid]
            val record = PersonAttendance(
                uuid                     = prev?.uuid ?: Utils.getUuidOrdered(),
                date                     = dateStr,
                person_uuid              = person.person_uuid,
                entity_type_oid          = entityType,
                academic_year            = acYear,
                school_uuid              = schoolUuid,
                school_group_uuid        = person.school_group_uuid,
                attendance_am_status_oid = amStatus,
                attendance_pm_status_oid = pmStatus,
                attendance_status_oid    = statusOid,
                absent_reason_oid        = reasonOid,
                absent_reason_other      = reasonOther,
                lat                      = lat,
                lng                      = lng,
                biometric_method_oid     = null,
                biometric_reference      = null,
                submitted                = 1,
                created_at               = prev?.created_at ?: time,
                created_by               = prev?.created_by ?: user,
                updated_at               = time,
                updated_by               = user,
                deleted_at               = null,
                deleted_by               = null,
                sync_flag                = 1
            )
            if (prev != null) toUpdate.add(record) else toInsert.add(record)
        }

        if (toInsert.isNotEmpty()) repo.insertPersonAttendanceList(toInsert)
        if (toUpdate.isNotEmpty()) repo.updatePersonAttendanceList(toUpdate)
    }

    companion object {
        const val KEY_SCHOOL_UUID  = "school_uuid"
        const val KEY_DATE         = "date"
        const val KEY_REASON_OID   = "reason_oid"
        const val KEY_REASON_OTHER = "reason_other"
    }
}
