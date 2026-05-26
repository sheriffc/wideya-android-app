package uk.org.cgatechnologies.wideya.school_group_management

import androidx.annotation.WorkerThread
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup

class SchoolGroupManagementRepository(private val schoolGroupManagementDao: SchoolGroupManagementDao) {

    fun getSchoolGroup(uuid: String) = schoolGroupManagementDao.getSchoolGroupModel(uuid)

    fun getTeacherOptionsList(uuid: String) = schoolGroupManagementDao.getTeacherOptionsList(uuid)

    fun getSchoolGroupOptionsList(uuid: String?) = schoolGroupManagementDao.getSchoolGroupOptionsList(uuid)

    fun getSchoolGroupLearnerList(schoolGroupUuid: String, academicYear: Short) =
        schoolGroupManagementDao.getSchoolGroupLearnerList(schoolGroupUuid, academicYear)

    fun getSchoolGroupLearnerListByQuery(schoolGroupUuid: String, academicYear: Short, query: String?) =
        schoolGroupManagementDao.getSchoolGroupLearnerListByQuery(schoolGroupUuid, academicYear, query)

    fun getSchoolGroupLearnerListByRawQuery(schoolGroupUuid: String, academicYear: Short, query: String) =
        schoolGroupManagementDao.buildSchoolGroupLearnerListByRawQuery(schoolGroupUuid, academicYear, query)

    @WorkerThread
    suspend fun insertSchoolGroup(schoolGroup: SchoolGroup) = schoolGroupManagementDao.insertSchoolGroup(schoolGroup)

    @WorkerThread
    suspend fun updateSchoolGroup(schoolGroup: SchoolGroup) = schoolGroupManagementDao.updateSchoolGroup(schoolGroup)

    @WorkerThread
    suspend fun deleteSchoolGroup(schoolGroup: SchoolGroup) = schoolGroupManagementDao.deleteSchoolGroup(schoolGroup)

    @WorkerThread
    suspend fun softDeleteSchoolGroup(uuid: String, userId: Int) {
        schoolGroupManagementDao.softDeleteSchoolGroup(uuid, userId)
        schoolGroupManagementDao.softDeleteSchoolLearnerEnrolmentBySchoolGroup(uuid, userId)
    }

    companion object {
        @Volatile
        private var instance: SchoolGroupManagementRepository? = null

        fun getInstance(schoolGroupManagementDao: SchoolGroupManagementDao) =
            this.instance ?: synchronized(this) {
                instance ?: SchoolGroupManagementRepository(schoolGroupManagementDao).also {
                    instance = it
                }
            }
    }
}