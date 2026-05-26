package uk.org.cgatechnologies.wideya.school_management

import androidx.annotation.WorkerThread
import uk.org.cgatechnologies.wideya.school_management.entities.School
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerAdmission
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment

class SchoolManagementRepository(private val schoolManagementDao: SchoolManagementDao) {

    //School Management
    fun getSchoolList(uuids: List<String>) = schoolManagementDao.getSchoolModelList(uuids)

    @WorkerThread
    fun getSchoolListByQuery(query: String?, uuids: List<String>) =
        schoolManagementDao.getSchoolModelListByQuery(query, uuids)

    @WorkerThread
    fun getSchoolListByRawQuery(query: String, uuids: List<String>) =
        schoolManagementDao.buildSchoolModelListByRawQuery(query, uuids)

    fun getSchool(uuid: String) = schoolManagementDao.getSchoolModel(uuid)

    @WorkerThread
    fun getSchoolModelById(uuid: String) = schoolManagementDao.getSchoolModelById(uuid)

    suspend fun insertSchool(school: School) = schoolManagementDao.insertSchool(school)

    suspend fun updateSchool(school: School) = schoolManagementDao.updateSchool(school)

    suspend fun deleteSchool(school: School) = schoolManagementDao.deleteSchool(school)

    //school learner admission
    suspend fun insertSchoolLearnerAdmission(schoolLearnerAdmission: SchoolLearnerAdmission) =
        schoolManagementDao.insertSchoolLearnerAdmission(schoolLearnerAdmission)

    suspend fun updateSchoolLearnerAdmission(schoolLearnerAdmission: SchoolLearnerAdmission) =
        schoolManagementDao.updateSchoolLearnerAdmission(schoolLearnerAdmission)

    //school learner enrolment
    suspend fun insertSchoolLearnerEnrolment(schoolLearnerEnrolment: SchoolLearnerEnrolment) =
        schoolManagementDao.insertSchoolLearnerEnrolment(schoolLearnerEnrolment)

    suspend fun updateSchoolLearnerEnrolment(schoolLearnerEnrolment: SchoolLearnerEnrolment) =
        schoolManagementDao.updateSchoolLearnerEnrolment(schoolLearnerEnrolment)

    suspend fun softDeleteSchoolLearnerEnrolment(schoolLearnerEnrolmentUuid: String, userId: Int) =
        schoolManagementDao.softDeleteSchoolLearnerEnrolment(schoolLearnerEnrolmentUuid, userId)

    suspend fun softDeleteLearnersWithoutUid(schoolUuid: String, userId: Int) {
        schoolManagementDao.softDeleteEnrolmentsWithoutLearnerId(schoolUuid, userId)
        schoolManagementDao.softDeleteAdmissionsWithoutLearnerId(schoolUuid, userId)
    }

    suspend fun closeAdmissionsAndEnrolmentsAtOtherSchools(learnerUuid: String, newSchoolUuid: String, userId: Int) {
        schoolManagementDao.closeAdmissionsAtOtherSchools(learnerUuid, newSchoolUuid, userId)
        schoolManagementDao.closeEnrolmentsAtOtherSchools(learnerUuid, newSchoolUuid, userId)
    }

    //School Teacher
    @WorkerThread
    fun getSchoolTeacherList(uuid: String?) = schoolManagementDao.getSchoolTeacherList(uuid)

    @WorkerThread
    fun getSchoolTeacherListByQuery(uuid: String?, query: String?) =
        schoolManagementDao.getSchoolTeacherListByQuery(uuid, query)

    @WorkerThread
    fun getSchoolTeacherListByRawQuery(uuid: String, query: String) =
        schoolManagementDao.buildSchoolTeacherListByRawQuery(uuid, query)

    //Learner Admission

    @WorkerThread
    fun getSchoolLearnerAdmissionList(uuid: String?) =
        schoolManagementDao.getSchoolLearnerAdmissionList(uuid)

    @WorkerThread
    fun getSchoolLearnerAdmissionListByQuery(uuid: String?, query: String?) =
        schoolManagementDao.getSchoolLearnerAdmissionListByQuery(uuid, query)

    @WorkerThread
    fun getSchoolLearnerAdmissionListByRawQuery(uuid: String, query: String) =
        schoolManagementDao.buildSchoolLearnerAdmissionListByRawQuery(uuid, query)

    //School Group

    @WorkerThread
    fun getSchoolGroupList(uuid: String?) = schoolManagementDao.getSchoolGroupList(uuid)

    @WorkerThread
    fun getSchoolGroupListByQuery(uuid: String?, query: String?) =
        schoolManagementDao.getSchoolGroupListByQuery(uuid, query)

    @WorkerThread
    fun getSchoolGroupListByRawQuery(uuid: String, query: String) =
        schoolManagementDao.buildSchoolGroupListByRawQuery(uuid, query)

    @WorkerThread
    fun getSchoolTimetableList(uuid: String?) = schoolManagementDao.getSchoolTimetableList(uuid)

    @WorkerThread
    fun getSelectedTeacherById(uuid: String) = schoolManagementDao.getSelectedTeacherById(uuid)

    companion object {
        @Volatile
        private var instance: SchoolManagementRepository? = null

        fun getInstance(schoolManagementDao: SchoolManagementDao) =
            this.instance ?: synchronized(this) {
                instance ?: SchoolManagementRepository(schoolManagementDao).also {
                    instance = it
                }
            }
    }
}