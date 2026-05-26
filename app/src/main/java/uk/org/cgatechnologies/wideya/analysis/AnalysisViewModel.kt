package uk.org.cgatechnologies.wideya.analysis

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.highsoft.highcharts.common.hichartsclasses.HIOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.analysis.models.LearnerAttendanceReportModel
import uk.org.cgatechnologies.wideya.analysis.models.LearnerDisabilityModel
import uk.org.cgatechnologies.wideya.analysis.models.TeacherAttendanceReportModel
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import java.time.LocalDate

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {
    val TAG: String = "AnalysisViewModel"

    private val repository: AnalysisRepository

    lateinit var selectedDate: LocalDate

    private var teacherAttendanceReportJob: Job = Job()
    private var teacherAttendanceWeeklyReportJob: Job = Job()
    private var learnerDisabilityReportJob: Job = Job()

    private var learnerAttendanceReportJob: Job = Job()
    private var learnerAttendanceWeeklyReportJob: Job = Job()

    private val _teacherAttendanceReport = MutableStateFlow(
        LatestTeacherAttendanceReportUiState.Success(
            TeacherAttendanceReportModel()
        )
    )
    val teacherAttendanceReport: StateFlow<LatestTeacherAttendanceReportUiState> =
        _teacherAttendanceReport

    private val _teacherAttendanceReportList =
        MutableStateFlow(LatestTeacherAttendanceReportListListUiState.Success(emptyList()))
    val teacherAttendanceReportList: StateFlow<LatestTeacherAttendanceReportListListUiState> =
        _teacherAttendanceReportList

    private val _learnerAttendanceReport = MutableStateFlow(
        LatestLearnerAttendanceReportUiState.Success(
            LearnerAttendanceReportModel()
        )
    )
    val learnerAttendanceReport: StateFlow<LatestLearnerAttendanceReportUiState> = _learnerAttendanceReport

    private val _learnerAttendanceReportList =
        MutableStateFlow(LatestLearnerAttendanceReportListListUiState.Success(emptyList()))
    val learnerAttendanceReportList: StateFlow<LatestLearnerAttendanceReportListListUiState> =
        _learnerAttendanceReportList

    private val _learnerDisabilityReportList: MutableStateFlow<LatestLearnerDisabilityReportListListUiState> =
        MutableStateFlow(LatestLearnerDisabilityReportListListUiState.Success(emptyList()))

    val learnerDisabilityReportList: StateFlow<LatestLearnerDisabilityReportListListUiState> =
        _learnerDisabilityReportList

    init {
        Log.d(TAG, "initalise vm")
        this.repository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .analysisDao()
                .let { dao ->
                    AnalysisRepository.getInstance(dao)
                }
    }

    fun setTeacherAttendanceReport(schoolUuid: String) {
        teacherAttendanceReportJob.cancel()
        teacherAttendanceReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getTodayTeacherAttendanceReport(schoolUuid).collect { teacherDetail ->
                if (teacherDetail == null) {
                    _teacherAttendanceReport.value = LatestTeacherAttendanceReportUiState.Success(
                        TeacherAttendanceReportModel()
                    )
                } else {
                    _teacherAttendanceReport.value =
                        LatestTeacherAttendanceReportUiState.Success(teacherDetail)
                }

            }
        }
    }

    fun getSchoolTeacherAttendanceReportByDate(schoolUuid: String, date: String) {
        teacherAttendanceReportJob.cancel()
        teacherAttendanceReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getSchoolTeacherAttendanceReportByDate(schoolUuid, date)
                .collect { teacherDetail ->
                    if (teacherDetail == null) {
                        _teacherAttendanceReport.value =
                            LatestTeacherAttendanceReportUiState.Success(
                                TeacherAttendanceReportModel()
                            )
                    } else {
                        _teacherAttendanceReport.value =
                            LatestTeacherAttendanceReportUiState.Success(teacherDetail)
                    }
                }
        }
    }

    fun getSchoolTeacherAttendanceByInterval(
        schoolUuid: String,
        startDate: String,
        endDate: String
    ) {
        teacherAttendanceWeeklyReportJob.cancel()
        teacherAttendanceWeeklyReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getSchoolTeacherAttendanceReportByInterval(schoolUuid, startDate, endDate)
                .collect { teacherAttendanceReportList ->
                    Log.d(TAG, "teacher week data set")
                    _teacherAttendanceReportList.value =
                        LatestTeacherAttendanceReportListListUiState.Success(
                            teacherAttendanceReportList
                        )
                }
        }
    }

    fun setLearnerAttendanceReport(schoolUuid: String) {
        learnerAttendanceReportJob.cancel()
        learnerAttendanceReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getTodayLearnerAttendanceReport(schoolUuid).collect { learnerDetail ->
                if (learnerDetail == null) {
                    _learnerAttendanceReport.value = LatestLearnerAttendanceReportUiState.Success(
                        LearnerAttendanceReportModel()
                    )
                } else {
                    _learnerAttendanceReport.value = LatestLearnerAttendanceReportUiState.Success(learnerDetail)
                }

            }
        }
    }

    fun getSchoolLearnerAttendanceReportByDate(schoolUuid: String, date: String) {
        learnerAttendanceReportJob.cancel()
        learnerAttendanceReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getSchoolLearnerAttendanceReportByDate(schoolUuid, date).collect { learnerDetail ->
                if (learnerDetail == null) {
                    _learnerAttendanceReport.value = LatestLearnerAttendanceReportUiState.Success(
                        LearnerAttendanceReportModel()
                    )
                } else {
                    _learnerAttendanceReport.value = LatestLearnerAttendanceReportUiState.Success(learnerDetail)
                }
            }
        }
    }

    fun getSchoolLearnerAttendanceByInterval(schoolUuid: String, startDate: String, endDate: String) {
        learnerAttendanceWeeklyReportJob.cancel()
        learnerAttendanceWeeklyReportJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getSchoolLearnerAttendanceReportByInterval(schoolUuid, startDate, endDate)
                .collect { learnerAttendanceReportList ->
                    _learnerAttendanceReportList.value =
                        LatestLearnerAttendanceReportListListUiState.Success(learnerAttendanceReportList)
                }
        }
    }

    fun getSchoolLearnerDisabilityReport(schoolUuid: String) {
        learnerDisabilityReportJob.cancel()
        learnerDisabilityReportJob = viewModelScope.launch(Dispatchers.IO) {
            _learnerDisabilityReportList.value = LatestLearnerDisabilityReportListListUiState.Loading
            repository.getSchoolLearnerDisabilityReport(schoolUuid).collect { learnerReport ->
                _learnerDisabilityReportList.value =
                    LatestLearnerDisabilityReportListListUiState.Success(learnerReport)
            }
        }
    }

    fun getSchoolLearnerDisabilityByQuery(schoolUuid: String, query: String?) {
        learnerDisabilityReportJob.cancel()
        learnerDisabilityReportJob = viewModelScope.launch(Dispatchers.IO) {
            _learnerDisabilityReportList.value = LatestLearnerDisabilityReportListListUiState.Loading
            repository.getSchoolLearnerDisabilityByRawQuery(schoolUuid, query.toString())
                .collect { learnerReport ->
                    _learnerDisabilityReportList.value =
                        LatestLearnerDisabilityReportListListUiState.Success(learnerReport)
                }
        }
    }

    fun isSelectedDateInitialized(): Boolean {
        var initStatus = false
        if (this::selectedDate.isInitialized) {
            initStatus = true
        }
        return initStatus
    }
}

sealed class LatestTeacherAttendanceReportUiState {
    data class Success(val teacherAttendanceReport: TeacherAttendanceReportModel?) :
        LatestTeacherAttendanceReportUiState()

    data class Error(val exception: Throwable) : LatestTeacherAttendanceReportUiState()
}

sealed class LatestTeacherAttendanceReportListListUiState {
    data class Success(val teacherAttendanceReportList: List<TeacherAttendanceReportModel>) :
        LatestTeacherAttendanceReportListListUiState()

    data class Error(val exception: Throwable) : LatestTeacherAttendanceReportListListUiState()
}

sealed class LatestLearnerAttendanceReportUiState {
    data class Success(val learnerAttendanceReport: LearnerAttendanceReportModel?) :
        LatestLearnerAttendanceReportUiState()

    data class Error(val exception: Throwable) : LatestLearnerAttendanceReportUiState()
}

sealed class LatestLearnerAttendanceReportListListUiState {
    data class Success(val learnerAttendanceReportList: List<LearnerAttendanceReportModel>) :
        LatestLearnerAttendanceReportListListUiState()

    data class Error(val exception: Throwable) : LatestLearnerAttendanceReportListListUiState()
}

sealed class LatestLearnerDisabilityReportListListUiState {
    object Loading : LatestLearnerDisabilityReportListListUiState()
    data class Success(val learnerDisabilityReportList: List<LearnerDisabilityModel>) :
        LatestLearnerDisabilityReportListListUiState()

    data class Error(val exception: Throwable) : LatestLearnerDisabilityReportListListUiState()
}