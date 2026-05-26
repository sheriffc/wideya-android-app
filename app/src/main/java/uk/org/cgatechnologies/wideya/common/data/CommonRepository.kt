package uk.org.cgatechnologies.wideya.common.data

import androidx.annotation.WorkerThread
import uk.org.cgatechnologies.wideya.common.data.entities.MediaPhoto
import uk.org.cgatechnologies.wideya.common.data.entities.Person
import uk.org.cgatechnologies.wideya.common.data.entities.PersonFingerprint

class CommonRepository (private val commonDao: CommonDao) {

    fun getOptionList(listName: String) = commonDao.getOptionList(listName)

    fun getOptionListForParentOid(listName: String, parentOid: String ) = commonDao.getOptionListForParentOid(listName, parentOid)
    fun getDistrictOfficesList() = commonDao.getDistrictOfficesList()

    fun getSchoolGroupLevelsBySchoolEducationLevel(schoolEducationLevelItemId: String) = commonDao.getSchoolGroupLevelsBySchoolEducationLevel(schoolEducationLevelItemId)

    fun getSchoolSubjectsBySchoolEducationLevel(schoolEducationLevelItemId: String ) = commonDao.getSchoolSubjectsBySchoolEducationLevel(schoolEducationLevelItemId)

    fun getEducationLevelForSchoolId(schoolUuid: String) = commonDao.getEducationLevelForSchoolId(schoolUuid)

//    fun getCurrentAcademicYear() = commonDao.get

    //PERSON
    @WorkerThread
    suspend fun insertPerson(person: Person) = commonDao.insertPerson(person)
    @WorkerThread
    suspend fun updatePerson(person: Person) = commonDao.updatePerson(person)
    @WorkerThread
    suspend fun deletePerson(person: Person) = commonDao.deletePerson(person)

    //PERSON FINGERPRINT
    @WorkerThread
    suspend fun insertPersonFingerprint(personFingerprint: PersonFingerprint) = commonDao.insertPersonFingerprint(personFingerprint)
    @WorkerThread
    suspend fun softDeletePersonFingerprintForPerson(personUuid: String, userId: Int) = commonDao.softDeletePersonFingerprintForPerson(personUuid, userId)

    fun getAllPersonFingerprints() = commonDao.getAllPersonFingerprints()

    fun getPersonFingerprints(personUuid: String) = commonDao.getPersonFingerprints(personUuid)

    //MEDIA PHOTO
    @WorkerThread
    suspend fun insertMediaPhoto(mediaPhoto: MediaPhoto) = commonDao.insertMediaPhoto(mediaPhoto)

    @WorkerThread
    suspend fun updateMediaPhoto(mediaPhoto: MediaPhoto) = commonDao.updateMediaPhoto(mediaPhoto)

    @WorkerThread
    suspend fun softDeleteMediaPhotoForPerson(personUuid: String, userId: Int) = commonDao
        .softDeleteMediaPhotoForPersonUuid(personUuid, userId)

    companion object {
        @Volatile
        private var instance: CommonRepository? = null

        fun getInstance(commonDao: CommonDao) =
            this.instance ?: synchronized(this) {
                instance ?: CommonRepository(commonDao).also {
                    instance = it
                }
            }
    }
}