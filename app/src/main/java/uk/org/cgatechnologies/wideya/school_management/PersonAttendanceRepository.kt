package uk.org.cgatechnologies.wideya.school_management

import androidx.annotation.WorkerThread
import uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance
import uk.org.cgatechnologies.wideya.school_management.models.NoSchoolPersonModel

class PersonAttendanceRepository(private val personAttendanceDao: PersonAttendanceDao) {


    //person attendance
    suspend fun insertPersonAttendance(personAttendance: PersonAttendance) = personAttendanceDao.insertPersonAttendance(personAttendance)

    suspend fun insertPersonAttendanceList(personAttendanceList: List<PersonAttendance> ) = personAttendanceDao.insertPersonAttendanceList(personAttendanceList)

    suspend fun updatePersonAttendance(personAttendance: PersonAttendance) = personAttendanceDao.updatePersonAttendance(personAttendance)

    suspend fun updatePersonAttendanceList(personAttendanceList: List<PersonAttendance>) = personAttendanceDao.updatePersonAttendanceList(personAttendanceList)

    suspend fun deletePersonAttendance(personAttendance: PersonAttendance) = personAttendanceDao.deletePersonAttendance(personAttendance)

    fun softDeletePersonAttendanceForToday(personUuid: String, userId: Int) = personAttendanceDao.softDeletePersonAttendanceForToday(personUuid, userId)

    @WorkerThread
    fun getSchoolAttendanceTeacherFlowList(uuid: String?) = personAttendanceDao.getSchoolAttendanceTeacherFlowList(uuid)

    @WorkerThread
    fun getSchoolAttendanceTeacherFlowListByDate(schoolUuid: String?, date: String) = personAttendanceDao.getSchoolAttendanceTeacherFlowListByDate(schoolUuid,date)

    @WorkerThread
    fun getSchoolAttendanceTeacherList(uuid: String?) = personAttendanceDao.getSchoolAttendanceTeacherList(uuid)

    fun getSchoolTeacherAttendanceDateSummaryCountList(uuid: String?) = personAttendanceDao.getSchoolTeacherAttendanceDateSummaryCountList(uuid)

    fun getSchoolLearnerAttendanceDateSummaryCountList(uuid: String?) = personAttendanceDao.getSchoolLearnerAttendanceDateSummaryCountList(uuid)

    fun getSchoolAttendanceLearnerList(uuid: String?) = personAttendanceDao.getSchoolAttendanceLearnerList(uuid)

    fun getSchoolAttendanceLearnerFlowList(uuid: String?) = personAttendanceDao.getSchoolAttendanceLearnerFlowList(uuid)

    fun getSchoolAttendanceLearnerFlowListByDate(uuid: String?,date: String) = personAttendanceDao.getSchoolAttendanceLearnerFlowListByDate(uuid,date)

    fun getSchoolAttendanceLearnerFlowListBySchoolGroup(group: String?, school: String?) = personAttendanceDao.getSchoolAttendanceLearnerFlowListBySchoolGroup(group, school)

    fun getSchoolAttendanceLearnerFlowListByDateAndSchoolGroup(uuid: String?,date: String,schoolGroupUuid: String?) = personAttendanceDao.getSchoolAttendanceLearnerFlowListByDateAndSchoolGroup(uuid,date,schoolGroupUuid)

    fun getPersonAttendanceTableLatestDate(schoolUuid: String?) = personAttendanceDao.getPersonAttendanceTableLatestDate(schoolUuid)

    fun deletePreCreatedFutureRecords(schoolUuid: String): Int =
        personAttendanceDao.deletePreCreatedFutureRecords(schoolUuid)

    fun getSubmittedDatesFromToday(schoolUuid: String): List<String> =
        personAttendanceDao.getSubmittedDatesFromToday(schoolUuid)

    fun getSchoolLearnerPersonList(schoolUuid: String): List<NoSchoolPersonModel> =
        personAttendanceDao.getSchoolLearnerPersonList(schoolUuid)

    fun getSchoolTeacherPersonList(schoolUuid: String): List<NoSchoolPersonModel> =
        personAttendanceDao.getSchoolTeacherPersonList(schoolUuid)

    fun getAttendanceByDateAndEntityType(schoolUuid: String, date: String, entityType: String): List<PersonAttendance> =
        personAttendanceDao.getAttendanceByDateAndEntityType(schoolUuid, date, entityType)

    companion object {
        @Volatile
        private var instance: PersonAttendanceRepository? = null

        fun getInstance(personAttendanceDao: PersonAttendanceDao) =
            this.instance ?: synchronized(this) {
                instance ?: PersonAttendanceRepository(personAttendanceDao).also {
                    instance = it
                }
            }
    }
}