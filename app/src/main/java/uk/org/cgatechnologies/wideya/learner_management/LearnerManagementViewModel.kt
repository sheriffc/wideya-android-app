package uk.org.cgatechnologies.wideya.learner_management

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.CommonRepository
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.common.utils.Extensions.addAllToFlow
import uk.org.cgatechnologies.wideya.common.utils.Extensions.addToFlow
import uk.org.cgatechnologies.wideya.common.utils.Extensions.removeFromFlow
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.learner_management.models.*
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel
import uk.org.cgatechnologies.wideya.school_management.PersonAttendanceRepository
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementRepository
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper

private const val TAG: String = "LearnerMgmtViewModel"

class LearnerManagementViewModel(application: Application) : AndroidViewModel(application),
    IViewModel.IMultiSelectorVM {

    private val repository: LearnerManagementRepository
    private val commonRepository: CommonRepository
    private val schoolManagementRepository: SchoolManagementRepository
    private val personAttendanceRepository: PersonAttendanceRepository
    private val learnerIdSequenceDao: LearnerIdSequenceDao

    lateinit var currentLearner: LearnerAdmissionModel
    lateinit var currentDetailsMode: DetailsMode

    private val _learnerDetail = MutableStateFlow(LatestLearnerDetailUiState.Success(LearnerAdmissionModel()))
    val learnerDetail: StateFlow<LatestLearnerDetailUiState> = _learnerDetail

    private val _unassignedLearnerListFlow: MutableStateFlow<UnassignedLearnerListUiState> = MutableStateFlow(
        UnassignedLearnerListUiState.Success(emptyList())
    )
    val unassignedLearnerListFlow: StateFlow<UnassignedLearnerListUiState> = _unassignedLearnerListFlow

    private val _learnerSearchResults: MutableStateFlow<LearnerSearchUiState> = MutableStateFlow(
        LearnerSearchUiState.Success(emptyList())
    )
    val learnerSearchResults: StateFlow<LearnerSearchUiState> = _learnerSearchResults

    private val _multiSelectStateFlow = MutableStateFlow(mutableSetOf<Any>())
    override val multiSelectStateFlow: StateFlow<MutableSet<Any>> = _multiSelectStateFlow

    lateinit var selectedGroup: Array<String?>

    private var learnerFlowJob: Job = Job()

    init {
        Log.d(TAG, "initalise vm")
        this.repository = AppDatabase.getDatabase(application, viewModelScope).learnerManagementDao().let { dao ->
            LearnerManagementRepository.getInstance(dao)
        }
        this.commonRepository = AppDatabase.getDatabase(application, viewModelScope).commonDao().let { dao ->
            CommonRepository.getInstance(dao)
        }
        this.schoolManagementRepository =
            AppDatabase.getDatabase(application, viewModelScope).schoolManagementDao().let { dao ->
                SchoolManagementRepository.getInstance(dao)
            }
        this.personAttendanceRepository =
            AppDatabase.getDatabase(application, viewModelScope).personAttendanceDao().let { dao ->
                PersonAttendanceRepository.getInstance(dao)
            }
        this.learnerIdSequenceDao = AppDatabase.getDatabase(application, viewModelScope).learnerIdSequenceDao()
    }

    fun isCurrentLearnerInitialized(): Boolean {
        return this::currentLearner.isInitialized
    }

    fun setLearnerDetail(uuid: String) {
        learnerFlowJob.cancel()
        learnerFlowJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getLearner(uuid).cancellable().collect { learner ->
                if (learner == null) {
                    _learnerDetail.value = LatestLearnerDetailUiState.Success(LearnerAdmissionModel())
                } else {
                    _learnerDetail.value = LatestLearnerDetailUiState.Success(learner)
                }
            }
        }
    }

    fun setUnassignedLearnerList(schoolUuid: String) {
        learnerFlowJob.cancel()
        learnerFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _unassignedLearnerListFlow.value = UnassignedLearnerListUiState.Loading
            repository.getUnassignedLearnerList(schoolUuid).cancellable().collect { learners ->
                _unassignedLearnerListFlow.value = UnassignedLearnerListUiState.Success(learners)
            }
        }
    }

    fun setLearnerSearchQuery(query: String, schoolUuid: String = "") {
        learnerFlowJob.cancel()
        learnerFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _learnerSearchResults.value = LearnerSearchUiState.Loading
            repository.searchLearnersByQuery(query, schoolUuid).cancellable().collect { learners ->
                _learnerSearchResults.value = LearnerSearchUiState.Success(learners)
            }
        }
    }

    suspend fun isDuplicateLearnerAtSchool(schoolUuid: String, excludeLearnerUuid: String, nin: String?, learnerId: String?): Boolean {
        if (schoolUuid.isEmpty()) return false
        return repository.countDuplicateLearnerAtSchool(
            schoolUuid,
            excludeLearnerUuid,
            nin.orEmpty(),
            learnerId.orEmpty()
        ) > 0
    }

    fun addExistingLearnerToSchool(learnerAdmission: LearnerAdmissionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val acYear = Utils.getAcademicYear()
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            learnerAdmission.apply {
                if (uuid.isEmpty() || uuid == learner_uuid) {
                    uuid = Utils.getUuidOrdered()
                }
                if (enrolment_uuid.isNullOrEmpty()) {
                    enrolment_uuid = Utils.getUuidOrdered()
                }
                enrolment_current_academic_year = acYear.toString()

                if (learner_id.isNullOrEmpty() && !school_uuid.isNullOrEmpty()) {
                    val school = schoolManagementRepository.getSchoolModelById(school_uuid!!)
                    val emisId = school.emis_id
                    if (!emisId.isNullOrEmpty()) {
                        val sequence = learnerIdSequenceDao.getAndIncrement(emisId, acYear.toString())
                        learner_id = LearnerIdGenerator.format(emisId, acYear, sequence)
                    }
                }

                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }

            // Close any active admissions and enrolments at other schools before enrolling here
            val learnerUuid = learnerAdmission.learner_uuid
            val newSchoolUuid = learnerAdmission.school_uuid
            if (!learnerUuid.isNullOrEmpty() && !newSchoolUuid.isNullOrEmpty()) {
                schoolManagementRepository.closeAdmissionsAndEnrolmentsAtOtherSchools(learnerUuid, newSchoolUuid, user)
            }

            // Only update the learner + person records (already exist), then create admission/enrolment
            commonRepository.updatePerson(learnerAdmission.toPerson())
            updateOrInsertLearnerGuardian(learnerAdmission)
            repository.updateLearner(learnerAdmission.toLearner())
            schoolManagementRepository.insertSchoolLearnerAdmission(learnerAdmission.toSchoolLearnerAdmission())
            schoolManagementRepository.insertSchoolLearnerEnrolment(learnerAdmission.toSchoolLearnerEnrolment())
        }
    }

    fun getOptionList(listName: String): List<OptionList> {
        return commonRepository.getOptionList(listName)
    }

    fun insertLearner(learnerAdmission: LearnerAdmissionModel) {
        viewModelScope.launch(Dispatchers.IO) {

            val acYear = Utils.getAcademicYear()
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            learnerAdmission.apply {
                // in case UUIDs needs to be generated earlier in some use cases
                if (learner_person_uuid.isNullOrEmpty()) {
                    learner_person_uuid = Utils.getUuidOrdered()
                }
                if (learner_uuid.isNullOrEmpty()) {
                    learner_uuid = Utils.getUuidOrdered()
                }
                if (uuid.isEmpty()) {
                    uuid = Utils.getUuidOrdered()
                }
                if (enrolment_uuid.isNullOrEmpty()) {
                    enrolment_uuid = Utils.getUuidOrdered()
                }

                enrolment_current_academic_year = acYear.toString()

                if (learner_id.isNullOrEmpty() && !school_uuid.isNullOrEmpty()) {
                    val school = schoolManagementRepository.getSchoolModelById(school_uuid!!)
                    val emisId = school.emis_id
                    if (!emisId.isNullOrEmpty()) {
                        val sequence = learnerIdSequenceDao.getAndIncrement(emisId, acYear.toString())
                        learner_id = LearnerIdGenerator.format(emisId, acYear, sequence)
                    }
                }

                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            commonRepository.insertPerson(learnerAdmission.toPerson())
            updateOrInsertLearnerGuardian(learnerAdmission)
            repository.insertLearner(learnerAdmission.toLearner())
            schoolManagementRepository.insertSchoolLearnerAdmission(learnerAdmission.toSchoolLearnerAdmission())
            schoolManagementRepository.insertSchoolLearnerEnrolment(learnerAdmission.toSchoolLearnerEnrolment())
        }
    }

    fun updateLearner(learner: LearnerAdmissionModel) {
        viewModelScope.launch(Dispatchers.IO) {

            learner.apply {
                updated_at = Utils.getISODateTimeUTC()
                updated_by = Utils.getUserId(getApplication())
                sync_flag = 1
            }

            commonRepository.updatePerson(learner.toPerson())
            updateOrInsertLearnerGuardian(learner)
            repository.updateLearner(learner.toLearner())
            schoolManagementRepository.updateSchoolLearnerAdmission(learner.toSchoolLearnerAdmission())
            //check if update or insert
            learner.apply {
                if (enrolment_uuid.isNullOrEmpty()) {
                    enrolment_uuid = Utils.getUuidOrdered()
                    enrolment_current_academic_year = Utils.getAcademicYear().toString()
                    schoolManagementRepository.insertSchoolLearnerEnrolment(learner.toSchoolLearnerEnrolment())
                } else {
                    schoolManagementRepository.updateSchoolLearnerEnrolment(learner.toSchoolLearnerEnrolment())
                }
            }
        }
    }

    private suspend fun updateOrInsertLearnerGuardian(learner: LearnerAdmissionModel) {
        val time = Utils.getISODateTimeUTC()
        val user = Utils.getUserId(getApplication())
        if (learner.guardian_person_uuid.isNullOrEmpty() && (!learner.guardian_first_name.isNullOrEmpty() || !learner.guardian_last_name.isNullOrEmpty() || !learner.guardian_nin.isNullOrEmpty() || !learner.guardian_portrait_uuid.isNullOrEmpty() || !learner.guardian_phone_1.isNullOrEmpty() || !learner.guardian_phone_2.isNullOrEmpty() || !learner.guardian_address.isNullOrEmpty())) {
            learner.apply {
                guardian_person_uuid = Utils.getUuidOrdered()
                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            commonRepository.insertPerson(learner.toGuardianPerson())
        } else if (!learner.guardian_person_uuid.isNullOrEmpty()) {
            learner.apply {
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            commonRepository.updatePerson(learner.toGuardianPerson())
        }
    }

    fun removeLearnerFromSchool(learnerAdmission: LearnerAdmissionModel) {
        val time = Utils.getISODateTimeUTC()
        val user = Utils.getUserId(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            learnerAdmission.apply {
                updated_at = time
                updated_by = user
                deleted_at = time
                deleted_by = user
                sync_flag = 1
            }

            val userId = Utils.getUserId(getApplication())
            val enrolmentUuid = learnerAdmission.enrolment_uuid
            val personUuid = learnerAdmission.learner_person_uuid

            schoolManagementRepository.updateSchoolLearnerAdmission(learnerAdmission.toSchoolLearnerAdmission())

            if (!enrolmentUuid.isNullOrEmpty()) {
                schoolManagementRepository.softDeleteSchoolLearnerEnrolment(enrolmentUuid, userId)
            }

            if (!personUuid.isNullOrEmpty()) {
                personAttendanceRepository.softDeletePersonAttendanceForToday(personUuid, userId)
            }
        }
    }

    fun removeSelectedLearnersFromSchoolGroup() {
        viewModelScope.launch {
            val job = launch(Dispatchers.IO) {
                _multiSelectStateFlow.value.forEach { learner ->
                    val uuid = (learner as SchoolGroupLearnerModel).learner_uuid
                    val learnerAdmission = repository.getLearnerAdmissionModel(uuid!!)

                    learnerAdmission?.let {
                        it.enrolment_school_group_uuid = null
                        it.enrolment_school_group_concat_name = null
                        it.enrolment_school_group_level_name = null
                        it.enrolment_school_group_level_oid = null
                        it.enrolment_school_group_name = null
                        updateLearner(it)
                    }
                }
            }
            job.join()

            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
            clearMultiSelectStateFlow()
        }
    }

    fun transferSelectedLearnersToSchoolGroup() {
        viewModelScope.launch {
            val job = launch(Dispatchers.IO) {
                _multiSelectStateFlow.value.forEach { learner ->
                    val uuid = (learner as SchoolGroupLearnerModel).learner_uuid
                    val learnerAdmission = repository.getLearnerAdmissionModel(uuid!!)
                    learnerAdmission?.let {
                        it.enrolment_school_group_uuid = selectedGroup[0]
                        it.enrolment_school_group_concat_name = selectedGroup[1]
                        updateLearner(it)
                    }
                }
            }
            job.join()

            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
            clearMultiSelectStateFlow()
        }
    }

    fun assignSelectedLearnersToSchoolGroup() {
        viewModelScope.launch {
            val job = launch(Dispatchers.IO) {
                _multiSelectStateFlow.value.forEach { learner ->
                    (learner as LearnerAdmissionModel).let {
                        it.enrolment_school_group_uuid = selectedGroup[0]
                        it.enrolment_school_group_concat_name = selectedGroup[1]
                        updateLearner(it)
                    }
                }
            }
            job.join()

            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
            clearMultiSelectStateFlow()
        }
    }

    override fun updateMultiSelectStateFlowByMany(items: List<Any>, selected: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            if (selected) {
                _multiSelectStateFlow.update { it.addAllToFlow(items) }
            } else {
                _multiSelectStateFlow.update { mutableSetOf() }
            }
        }
    }

    override fun updateMultiSelectStateFlow(item: Any, selected: Boolean) {
        viewModelScope.launch(Dispatchers.Default) {
            if (selected) {
                _multiSelectStateFlow.update { it.addToFlow(item) }
            } else {
                _multiSelectStateFlow.update { it.removeFromFlow(item) }
            }
        }
    }

    override fun clearMultiSelectStateFlow() {
        viewModelScope.launch(Dispatchers.Default) {
            _multiSelectStateFlow.update { mutableSetOf() }
        }
    }
}

sealed class LatestLearnerDetailUiState {
    data class Success(val learner: LearnerAdmissionModel?) : LatestLearnerDetailUiState()
    data class Error(val exception: Throwable) : LatestLearnerDetailUiState()
}

sealed class UnassignedLearnerListUiState {
    object Loading : UnassignedLearnerListUiState()

    data class Success(val learners: List<LearnerAdmissionModel>) : UnassignedLearnerListUiState()
    data class Error(val exception: Throwable) : UnassignedLearnerListUiState()
}

sealed class LearnerSearchUiState {
    object Loading : LearnerSearchUiState()
    data class Success(val learners: List<LearnerAdmissionModel>) : LearnerSearchUiState()
    data class Error(val exception: Throwable) : LearnerSearchUiState()
}
