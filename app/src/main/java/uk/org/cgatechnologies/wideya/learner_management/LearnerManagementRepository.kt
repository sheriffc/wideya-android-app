package uk.org.cgatechnologies.wideya.learner_management

import androidx.annotation.WorkerThread
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner

class LearnerManagementRepository (private val learnerManagementDao: LearnerManagementDao) {

    fun getLearner(uuid: String) = learnerManagementDao.getLearnerModel(uuid)
    fun getLearnerAdmissionModel(uuid: String) = learnerManagementDao.getLearnerAdmissionModel(uuid)
    fun getUnassignedLearnerList(schoolUuid: String) = learnerManagementDao.getUnassignedLearnerList(schoolUuid)
    fun searchLearnersByQuery(query: String, schoolUuid: String) = learnerManagementDao.searchLearnersByQuery(query, schoolUuid)
    suspend fun countDuplicateLearnerAtSchool(schoolUuid: String, excludeLearnerUuid: String, nin: String, learnerId: String) =
        learnerManagementDao.countDuplicateLearnerAtSchool(schoolUuid, excludeLearnerUuid, nin, learnerId)

    @WorkerThread
    suspend fun insertLearner(learner: Learner) = learnerManagementDao.insertLearner(learner)
    @WorkerThread
    suspend fun updateLearner(learner: Learner) = learnerManagementDao.updateLearner(learner)
    @WorkerThread
    suspend fun deleteLearner(learner: Learner) = learnerManagementDao.deleteLearner(learner)

    fun checkAdmissionNumberExists(admissionNumber: String,school: String,learner: String?) = learnerManagementDao.checkAdmissionNumberExists(admissionNumber,school,learner)

    companion object {
        @Volatile
        private var instance: LearnerManagementRepository? = null

        fun getInstance(learnerManagementDao: LearnerManagementDao) =
            this.instance ?: synchronized(this) {
                instance ?: LearnerManagementRepository(learnerManagementDao).also {
                    instance = it
                }
            }
    }
}