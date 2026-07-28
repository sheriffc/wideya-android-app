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

    /**
     * Highest learner_id sequence already present on this device for a school
     * (emisId) + 2-digit academic year suffix, scanned from the local `learner`
     * table (which is synced in full to every device). Used to seed the local
     * sequence counter so it can't fall behind IDs issued by other devices or a
     * previous install of this app. Returns 0 if none found.
     */
    fun getLocalMaxSequence(emisId: String, academicYearSuffix: String): Int {
        val ids = learnerManagementDao.getLearnerIdsInPrefixRange("$emisId-", "$emisId.") ?: return 0
        val prefixLen = emisId.length + 1
        var max = 0
        for (id in ids) {
            if (id.length <= prefixLen) continue
            val (yearSuffix, seq) = LearnerIdGenerator.parseSuffix(id.substring(prefixLen)) ?: continue
            if (yearSuffix == academicYearSuffix && seq > max) max = seq
        }
        return max
    }

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