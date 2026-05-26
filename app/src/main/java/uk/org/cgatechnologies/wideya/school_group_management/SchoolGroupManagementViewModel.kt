package uk.org.cgatechnologies.wideya.school_group_management

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.CommonRepository
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_group_management.models.toSchoolGroup

class SchoolGroupManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SchoolGroupManagementRepository
    private val commonRepository: CommonRepository

    lateinit var currentSchoolGroup: SchoolGroupModel
    lateinit var currentDetailsMode: DetailsMode

    private val _schoolGroupDetail = MutableStateFlow(
        LatestSchoolGroupDetailUiState.Success(
            SchoolGroupModel()
        )
    )
    val schoolGroupDetail: StateFlow<LatestSchoolGroupDetailUiState> = _schoolGroupDetail

    private val _schoolGroupLearnerList: MutableStateFlow<LatestSchoolGroupLearnerListUiState> = MutableStateFlow(
        LatestSchoolGroupLearnerListUiState.Success(emptyList())
    )
    val schoolGroupLearnerList: StateFlow<LatestSchoolGroupLearnerListUiState> = _schoolGroupLearnerList

    private var schoolGroupFlowJob: Job = Job()
    private var schoolGroupLearnerListFlowJob: Job = Job()

    init {
        Log.d(TAG, "initalise vm")
        this.repository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .schoolGroupManagementDao()
                .let { dao ->
                    SchoolGroupManagementRepository.getInstance(dao)
                }
        this.commonRepository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .commonDao()
                .let { dao ->
                    CommonRepository.getInstance(dao)
                }
    }

    fun isCurrentSchoolGroupInitialized(): Boolean {
        return this::currentSchoolGroup.isInitialized
    }

    fun setSchoolGroupDetail(uuid: String) {
        schoolGroupFlowJob.cancel()
        schoolGroupFlowJob = viewModelScope.launch {
            repository.getSchoolGroup(uuid).cancellable().collect { schoolGroup ->
                if (schoolGroup == null) {
                    _schoolGroupDetail.value = LatestSchoolGroupDetailUiState.Success(SchoolGroupModel())
                } else {
                    _schoolGroupDetail.value = LatestSchoolGroupDetailUiState.Success(schoolGroup)
                }
            }
        }
    }

    fun setSchoolGroupLearnerList(uuid: String, academicYear: Short) {
        schoolGroupLearnerListFlowJob.cancel()
        schoolGroupLearnerListFlowJob = viewModelScope.launch {
            _schoolGroupLearnerList.value = LatestSchoolGroupLearnerListUiState.Loading
            repository.getSchoolGroupLearnerList(uuid, academicYear).cancellable().collect { learnerList ->
                _schoolGroupLearnerList.value = LatestSchoolGroupLearnerListUiState.Success(learnerList)
            }
        }
    }

    fun setSchoolGroupLearnerListByQuery(uuid: String, academicYear: Short, query: String?) {
        schoolGroupLearnerListFlowJob.cancel()
        schoolGroupLearnerListFlowJob = viewModelScope.launch {
            _schoolGroupLearnerList.value = LatestSchoolGroupLearnerListUiState.Loading
            repository.getSchoolGroupLearnerListByRawQuery(uuid, academicYear, query.toString()).cancellable()
                .collect { learnerList ->
                    _schoolGroupLearnerList.value = LatestSchoolGroupLearnerListUiState.Success(learnerList)
                }
        }
    }

    fun getOptionList(listName: String): List<OptionList> {
        return commonRepository.getOptionList(listName)
    }

    fun getTeacherOptionsList(schoolUuid: String): List<OptionList> {
        return repository.getTeacherOptionsList(schoolUuid)
    }

    fun getSchoolGroupOptionsList(schoolUuid: String?): List<OptionList> {
        return repository.getSchoolGroupOptionsList(schoolUuid)
    }

    fun getSchoolGroupLevelsBySchoolEducationLevel(schoolEducationLevelItemId: String): List<OptionList> {
        return commonRepository.getSchoolGroupLevelsBySchoolEducationLevel(schoolEducationLevelItemId)
    }

    fun updateSchoolGroup(schoolGroup: SchoolGroupModel) {
        viewModelScope.launch {
            schoolGroup.apply {
                updated_at = Utils.getISODateTimeUTC()
                updated_by = Utils.getUserId(getApplication())
                sync_flag = 1
            }
            repository.updateSchoolGroup(schoolGroup.toSchoolGroup())
        }
    }

/*
fun deleteSchoolGroup(schoolGroup: SchoolGroupModel) {
    viewModelScope.launch {
        repository.deleteSchoolGroup(schoolGroupModelToSchoolGroup(schoolGroup))
        commonRepository.deletePerson(schoolGroupModelToPerson(schoolGroup))
    }
}
*/

    fun softDeleteSchoolGroup(schoolGroup: SchoolGroupModel) {
        viewModelScope.launch {
            val schoolGroupUuid = schoolGroup.uuid
            val userID = Utils.getUserId(getApplication())
            repository.softDeleteSchoolGroup(schoolGroupUuid, userID)
        }
    }

    //
    fun insertSchoolGroup(schoolGroup: SchoolGroupModel) {
        viewModelScope.launch {

            val acYear = Utils.getAcademicYear()
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            schoolGroup.apply {
                academic_year = acYear.toShort()
                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            repository.insertSchoolGroup(schoolGroup.toSchoolGroup())
        }
    }

    companion object {
        private const val TAG: String = "SchGMgmtViewModel"
    }
}

sealed class LatestSchoolGroupDetailUiState {
    data class Success(val schoolGroup: SchoolGroupModel?) : LatestSchoolGroupDetailUiState()
    data class Error(val exception: Throwable) : LatestSchoolGroupDetailUiState()
}

sealed class LatestSchoolGroupLearnerListUiState {
    object Loading : LatestSchoolGroupLearnerListUiState()

    data class Success(val schoolGroupLearnerList: List<SchoolGroupLearnerModel>) :
        LatestSchoolGroupLearnerListUiState()

    data class Error(val exception: Throwable) : LatestSchoolGroupLearnerListUiState()
}
