package uk.org.cgatechnologies.wideya.school_management

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.data.CommonRepository
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.common.utils.Extensions.rotateBitmap
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.data.AttendanceCompletenessData
import uk.org.cgatechnologies.wideya.school_management.data.AttendanceStatus
import uk.org.cgatechnologies.wideya.school_management.models.*
import uk.org.cgatechnologies.wideya.school_management.models.mappers.toPersonAttendance
import uk.org.cgatechnologies.wideya.school_management.models.mappers.toSchool
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

private const val TAG: String = "SchoolMgmtViewModel"

class SchoolManagementViewModel(private val application: Application) :
    AndroidViewModel(application), IViewModel
.ICameraViewModel {

    private val repository: SchoolManagementRepository
    private val commonRepository: CommonRepository
    private val personAttendanceRepository: PersonAttendanceRepository
    private lateinit var schoolFeedingDao: SchoolFeedingDao
    private lateinit var schoolFeedingStockDao: SchoolFeedingStockDao

    lateinit var currentSchool: SchoolModel
    lateinit var currentDetailsMode: DetailsMode
    lateinit var selectedGroupId: String
    lateinit var selectedTeacherAttendanceDate: DisplayAttendanceDateModel
    lateinit var selectedLearnerAttendanceDate: DisplayAttendanceDateModel
    private lateinit var invalidDeviceDate: String
    var incorrectTeacherAttendanceDate: String? = null
    var incorrectLearnerAttendanceDate: String? = null
    var teacherAttendanceRecyclerPosition: Int = 0

    override var portraitBinariesDelete: Boolean = false
    override var tempPhotoPath: String? = null
    private var currentTeacherPortrait: Bitmap? = null
    override var currentPortraitThumbnail: Bitmap? = null
    override var currentPortraitOrientation: Int = 0

    private var personTeacherAttendanceModelList: List<PersonAttendanceModel> = mutableListOf()
    private var personLearnerAttendanceModelList: List<PersonAttendanceModel> = mutableListOf()

    private var schoolFlowJob: Job = Job()
    private var schoolListFlowJob: Job = Job()
    private var teacherListFlowJob: Job = Job()
    private var schoolLearnerAdmissionListFlowJob: Job = Job()
    private var schoolGroupListFlowJob: Job = Job()
    private var attendanceSchoolGroupOptionListFlowJob: Job = Job()

    private var insertBlankTeachersAttendanceJob: Job? = null
    private var insertBlankLearnersAttendanceJob: Job? = null

    private var teachersAttendanceListJob: Job = Job()
    private var learnersAttendanceListJob: Job = Job()

    private var timetableListFlowJob: Job = Job()

    //https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
    private val _schoolList: MutableStateFlow<LatestSchoolListUiState> =
        MutableStateFlow(LatestSchoolListUiState.Success(emptyList()))
    val schoolList: StateFlow<LatestSchoolListUiState> = _schoolList

    private val _schoolDetail = MutableStateFlow(LatestSchoolDetailUiState.Success(null))
    val schoolDetail: StateFlow<LatestSchoolDetailUiState> = _schoolDetail

    private val _schoolTeacherList: MutableStateFlow<LatestSchoolTeacherListUiState> =
        MutableStateFlow(LatestSchoolTeacherListUiState.Success(emptyList()))
    val schoolTeacherList: StateFlow<LatestSchoolTeacherListUiState> = _schoolTeacherList

    private val _schoolLearnerAdmissionList: MutableStateFlow<LatestSchoolLearnerAdmissionListUiState> =
        MutableStateFlow(LatestSchoolLearnerAdmissionListUiState.Success(emptyList()))
    val schoolLearnerAdmissionList: StateFlow<LatestSchoolLearnerAdmissionListUiState> =
        _schoolLearnerAdmissionList

    private val _schoolGroupList: MutableStateFlow<LatestSchoolGroupListUiState> =
        MutableStateFlow(LatestSchoolGroupListUiState.Success(emptyList()))
    val schoolGroupList: StateFlow<LatestSchoolGroupListUiState> = _schoolGroupList

    private val _attendanceSchoolGroupList: MutableStateFlow<LatestSchoolAttendanceGroupListUiState> =
        MutableStateFlow(LatestSchoolAttendanceGroupListUiState.Success(emptyList()))
    val attendanceSchoolGroupList: StateFlow<LatestSchoolAttendanceGroupListUiState> =
        _attendanceSchoolGroupList

    private val _schoolTeacherAttendanceList: MutableStateFlow<LatestSchoolTeacherAttendanceListUiState> =
        MutableStateFlow(LatestSchoolTeacherAttendanceListUiState.Success(emptyList()))
    val schoolTeacherAttendanceList: StateFlow<LatestSchoolTeacherAttendanceListUiState> =
        _schoolTeacherAttendanceList

    private val _schoolLearnerAttendanceList: MutableStateFlow<LatestSchoolLearnerAttendanceListUiState> =
        MutableStateFlow(LatestSchoolLearnerAttendanceListUiState.Success(emptyList()))
    val schoolLearnerAttendanceList: StateFlow<LatestSchoolLearnerAttendanceListUiState> =
        _schoolLearnerAttendanceList

    private val _timetableListFlow: MutableStateFlow<SchoolTimetableListUiState> =
        MutableStateFlow(SchoolTimetableListUiState.Success(emptyList()))
    val timetableListFlow: StateFlow<SchoolTimetableListUiState> = _timetableListFlow

    private val _cameraPhoto = MutableStateFlow(LatestCameraPhotoUiState.Success(null))
    val cameraPhoto: StateFlow<LatestCameraPhotoUiState> = _cameraPhoto

    init {
        Log.d(TAG, "initalise vm")

        this.repository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .schoolManagementDao()
                .let { dao ->
                    SchoolManagementRepository.getInstance(dao)
                }
        this.commonRepository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .commonDao()
                .let { dao ->
                    CommonRepository.getInstance(dao)
                }
        this.personAttendanceRepository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .personAttendanceDao()
                .let { dao ->
                    PersonAttendanceRepository.getInstance(dao)
                }
        this.schoolFeedingDao =
            AppDatabase.getDatabase(application, viewModelScope).schoolFeedingDao()
        this.schoolFeedingStockDao =
            AppDatabase.getDatabase(application, viewModelScope).schoolFeedingStockDao()
    }

    fun isCurrentSchoolInitialized(): Boolean {
        return this::currentSchool.isInitialized
    }

    fun setSchoolList(uuids: List<String>) {
        schoolListFlowJob.cancel()
        schoolListFlowJob = viewModelScope.launch {
            _schoolList.value = LatestSchoolListUiState.Loading
            repository.getSchoolList(uuids).cancellable().collect { schoolList ->
                _schoolList.value = LatestSchoolListUiState.Success(schoolList)
            }
        }
    }

    fun setSchoolListByQuery(query: String?, uuids: List<String>) {
        schoolListFlowJob.cancel()
        schoolListFlowJob = viewModelScope.launch {
            _schoolList.value = LatestSchoolListUiState.Loading
            repository.getSchoolListByRawQuery(query.toString(), uuids).cancellable()
                .collect { schoolList ->
                    _schoolList.value = LatestSchoolListUiState.Success(schoolList)
                }
        }
    }

    fun setSchoolDetail(uuid: String) {
        schoolFlowJob.cancel()
        schoolFlowJob = viewModelScope.launch {
            repository.getSchool(uuid).cancellable().collect { school ->
                if (school == null) {
                    currentSchool = SchoolModel()
                    _schoolDetail.value = LatestSchoolDetailUiState.Success(currentSchool)
                } else {
                    currentSchool = school
                    _schoolDetail.value = LatestSchoolDetailUiState.Success(school)
                }
            }
        }
    }

    fun insertSchool(school: SchoolModel) {
        viewModelScope.launch {
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())
            school.apply {
                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            repository.insertSchool(school.toSchool())
        }
    }

    fun updateSchool(school: SchoolModel) {
        viewModelScope.launch {
            school.apply {
                updated_at = Utils.getISODateTimeUTC()
                updated_by = Utils.getUserId(getApplication())
                sync_flag = 1
                repository.updateSchool(school.toSchool())
            }
        }
    }

    fun insertBlankPersonsAttendanceListForToday(entityType: String) {
        val existingJob: Job? = when (entityType) {
            Constants.TEACHER_ENTITY_ID -> {
                insertBlankTeachersAttendanceJob
            }
            Constants.LEARNER_ENTITY_ID -> {
                insertBlankLearnersAttendanceJob
            }
            else -> return // throw an error instead?
        }

        // prevent running this insertion job multiple times concurrently
        if (existingJob != null && existingJob.isActive) {
            return
        }

        when (entityType) {
            Constants.TEACHER_ENTITY_ID -> {
                insertBlankTeachersAttendanceJob =
                    launchJobBlankPersonsAttendanceForToday(entityType)
            }
            Constants.LEARNER_ENTITY_ID -> {
                insertBlankLearnersAttendanceJob =
                    launchJobBlankPersonsAttendanceForToday(entityType)
            }
            else -> return
        }
    }

    private fun launchJobBlankPersonsAttendanceForToday(entityType: String): Job {
        return viewModelScope.launch(Dispatchers.Default) {

            val schoolUuid = currentSchool.uuid
            val acYear = Utils.getAcademicYear()

            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            val latVal = Utils.getLastKnownLat(getApplication())
            val lngVal = Utils.getLastKnownLng(getApplication())

            val dateVal = Utils.getISODateUTC()

            var personAttendanceModelList = listOf<PersonAttendanceModel>()
            val personAttendanceList = mutableListOf<PersonAttendance>()

            when (entityType) {
                Constants.TEACHER_ENTITY_ID -> personAttendanceModelList =
                    personAttendanceRepository.getSchoolAttendanceTeacherList(schoolUuid)
                Constants.LEARNER_ENTITY_ID -> personAttendanceModelList =
                    personAttendanceRepository.getSchoolAttendanceLearnerList(schoolUuid)
            }
            val startTime1 = System.currentTimeMillis()
            for (personAttendanceModel in personAttendanceModelList) {
                if (personAttendanceModel.uuid.isNullOrEmpty()) {
                    personAttendanceModel.apply {
//                            val currentDay: Calendar = Calendar.getInstance()
//                            currentDay.add(Calendar.DAY_OF_MONTH, -0)
                        academic_year = acYear.toShort()
                        uuid = Utils.getUuidOrdered()
                        date = dateVal
                        lat = latVal
                        lng = lngVal
                        created_at = time
                        created_by = user
                        updated_at = time
                        updated_by = user
                        sync_flag = 1
                    }
                    personAttendanceList.add(personAttendanceModel.toPersonAttendance(getApplication()))
                }
            }
            Log.d(
                "timer",
                "type:${entityType}, ${System.currentTimeMillis() - startTime1}ms elapsed for record " +
                        "generation"
            )
            val startTime2 = System.currentTimeMillis()
            personAttendanceRepository.insertPersonAttendanceList(personAttendanceList)
            Log.d(
                "timer",
                "type:${entityType}, ${System.currentTimeMillis() - startTime2}ms elapsed for insert"
            )
        }
    }

    fun savePersonAttendance(personAttendanceModel: PersonAttendanceModel, storageDirPath: String) {
        if (personAttendanceModel.uuid == null) {
            insertPersonAttendance(personAttendanceModel)
        } else {
            updatePersonAttendance(personAttendanceModel)
            if (personAttendanceModel.biometric_method_oid == Constants.BIOMETRIC_METHOD_PHOTO_ID) {
                viewModelScope.launch {
                    val job: Job = launch(Dispatchers.IO) {
                        saveTempPhotoAsAttendance(
                            personAttendanceModel.uuid ?: "z_${UUID.randomUUID()}"
                        )
                    }

                    job.join()

                    deletePhotoCache(storageDirPath)
                }
            }
        }
    }

    fun insertPersonAttendance(personAttendanceModel: PersonAttendanceModel) {
        viewModelScope.launch(Dispatchers.IO) {

            val acYear = Utils.getAcademicYear()
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            personAttendanceModel.apply {
                academic_year = acYear.toShort()
                uuid = Utils.getUuidOrdered()
                date = Utils.getISODateUTC()
                lat = Utils.getLastKnownLat(getApplication())
                lng = Utils.getLastKnownLng(getApplication())
                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            personAttendanceRepository.insertPersonAttendance(
                personAttendanceModel.toPersonAttendance(getApplication())
            )
        }
    }

    fun updatePersonAttendance(personAttendanceModel: PersonAttendanceModel) {
        viewModelScope.launch(Dispatchers.IO) {
            // this method needs to be highly optimised to avoid laggy erratic behaviour
            //     in learner attendance status buttons; this is why
            //     parameters are being fetched from SharedPrefs (5ms) instead of EncSharedPrefs (100ms)
            personAttendanceModel.apply {
                Utils.getSharedPrefsState(getApplication()).apply {
                    academic_year = Utils.getAcademicYear().toShort()
                    lat = getString(Constants.PREF_LOCATION_LAT, "")?.toFloatOrNull()
                    lng = getString(Constants.PREF_LOCATION_LNG, "")?.toFloatOrNull()
                    updated_at = Utils.getISODateTimeUTC()
                    updated_by = getString(Constants.PREF_USER_ID, "-1")?.toIntOrNull()
                    sync_flag = 1
                }
            }
            personAttendanceRepository.updatePersonAttendance(
                personAttendanceModel.toPersonAttendance(getApplication())
            )
        }
    }

    fun bulkSetLearnerAttendanceSession(isAm: Boolean, status: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val time  = Utils.getISODateTimeUTC()
            val user  = Utils.getUserId(getApplication())
            val acYear = Utils.getAcademicYear().toShort()
            val lat   = Utils.getLastKnownLat(getApplication())
            val lng   = Utils.getLastKnownLng(getApplication())

            val toInsert = mutableListOf<PersonAttendance>()
            val toUpdate = mutableListOf<PersonAttendance>()

            for (model in personLearnerAttendanceModelList) {
                if (isAm) model.attendance_am_status_oid = status
                else model.attendance_pm_status_oid = status

                if (model.uuid == null) {
                    model.apply {
                        academic_year = acYear
                        uuid          = Utils.getUuidOrdered()
                        date          = Utils.getISODateUTC()
                        this.lat      = lat
                        this.lng      = lng
                        created_at    = time
                        created_by    = user
                        updated_at    = time
                        updated_by    = user
                        sync_flag     = 1
                    }
                    toInsert.add(model.toPersonAttendance(getApplication()))
                } else {
                    model.apply {
                        academic_year = acYear
                        this.lat      = lat
                        this.lng      = lng
                        updated_at    = time
                        updated_by    = user
                        sync_flag     = 1
                    }
                    toUpdate.add(model.toPersonAttendance(getApplication()))
                }
            }

            if (toInsert.isNotEmpty()) personAttendanceRepository.insertPersonAttendanceList(toInsert)
            if (toUpdate.isNotEmpty()) personAttendanceRepository.updatePersonAttendanceList(toUpdate)
        }
    }

    fun submitPersonAttendanceList(entityType: String) {
        val personAttendanceList = mutableListOf<PersonAttendance>()
        viewModelScope.launch(Dispatchers.IO) {
            var selectedAttendanceList = listOf<PersonAttendanceModel>()
            when (entityType) {
                Constants.TEACHER_ENTITY_ID -> selectedAttendanceList =
                    personTeacherAttendanceModelList
                Constants.LEARNER_ENTITY_ID -> selectedAttendanceList =
                    personLearnerAttendanceModelList
            }
            for (personAttendanceModel in selectedAttendanceList) {
                personAttendanceModel.apply {
                    submitted = 1
                    updated_at = Utils.getISODateTimeUTC()
                    updated_by = Utils.getUserId(getApplication())
                    sync_flag = 1
                }
                personAttendanceList.add(personAttendanceModel.toPersonAttendance(getApplication()))
            }
            personAttendanceRepository.updatePersonAttendanceList(personAttendanceList)
        }
    }

    fun isSchoolGroupSelectedInitialized(): Boolean {
        var initStatus = false
        if (this::selectedGroupId.isInitialized) {
            initStatus = true
        }
        return initStatus
    }

    fun isTeacherAttendanceDateSelectInitialized(): Boolean {
        var initStatus = false
        if (this::selectedTeacherAttendanceDate.isInitialized) {
            initStatus = true
        }
        return initStatus
    }

    fun isLearnerAttendanceDateSelectInitialized(): Boolean {
        var initStatus = false
        if (this::selectedLearnerAttendanceDate.isInitialized) {
            initStatus = true
        }
        return initStatus
    }

    fun isIncorrectTeacherAttendanceDateDialogInitialized(): Boolean {
        var initStatus = false
        if (!this.incorrectTeacherAttendanceDate.isNullOrEmpty()) {
            initStatus = true
        }
        return initStatus
    }

    fun isIncorrectLearnerAttendanceDateDialogInitialized(): Boolean {
        var initStatus = false
        if (!this.incorrectLearnerAttendanceDate.isNullOrEmpty()) {
            initStatus = true
        }
        return initStatus
    }

    fun saveSchoolIdUserPreferences(context: Context, uuid: String) {
        Utils.getEncSharedPrefs(context).edit()
            .putString(Constants.PREF_SCHOOL_ID, uuid)
            .apply()
    }

    fun setSchoolTeacherList(uuid: String?) {
        teacherListFlowJob.cancel()
        teacherListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolTeacherList.value = LatestSchoolTeacherListUiState.Loading
            repository.getSchoolTeacherList(uuid).cancellable().collect { teacherList ->
                _schoolTeacherList.value = LatestSchoolTeacherListUiState.Success(teacherList)
            }
        }
    }

    fun setSchoolTeacherListByQuery(uuid: String?, query: String?) {
        teacherListFlowJob.cancel()
        teacherListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolTeacherList.value = LatestSchoolTeacherListUiState.Loading
            repository.getSchoolTeacherListByRawQuery(uuid.toString(), query.toString())
                .cancellable()
                .collect { teacherList ->
                    _schoolTeacherList.value = LatestSchoolTeacherListUiState.Success(teacherList)
                }
        }
    }

    fun removeLearnersWithoutUid(schoolUuid: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = Utils.getUserId(getApplication())
            repository.softDeleteLearnersWithoutUid(schoolUuid, userId)
        }
    }

    fun setSchoolLearnerAdmissionList(uuid: String?) {
        schoolLearnerAdmissionListFlowJob.cancel()
        schoolLearnerAdmissionListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAdmissionList.value = LatestSchoolLearnerAdmissionListUiState.Loading
            repository.getSchoolLearnerAdmissionList(uuid).cancellable()
                .collect { learnerAdmissionList ->
                    _schoolLearnerAdmissionList.value =
                        LatestSchoolLearnerAdmissionListUiState.Success(learnerAdmissionList)
                }
        }
    }

    fun setSchoolLearnerAdmissionListByQuery(uuid: String?, query: String?) {
        schoolLearnerAdmissionListFlowJob.cancel()
        schoolLearnerAdmissionListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAdmissionList.value = LatestSchoolLearnerAdmissionListUiState.Loading
            repository.getSchoolLearnerAdmissionListByRawQuery(uuid.toString(), query.toString())
                .cancellable()
                .collect { learnerAdmissionList ->
                    _schoolLearnerAdmissionList.value =
                        LatestSchoolLearnerAdmissionListUiState.Success(learnerAdmissionList)
                }
        }
    }

    fun setSchoolGroupList(uuid: String?) {
        schoolGroupListFlowJob.cancel()
        schoolGroupListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolGroupList.value = LatestSchoolGroupListUiState.Loading
            repository.getSchoolGroupList(uuid).cancellable().collect { schoolGroupList ->
                _schoolGroupList.value = LatestSchoolGroupListUiState.Success(schoolGroupList)
            }
        }
    }

    fun setSchoolGroupListByQuery(uuid: String?, query: String?) {
        schoolGroupListFlowJob.cancel()
        schoolGroupListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolGroupList.value = LatestSchoolGroupListUiState.Loading
            repository.getSchoolGroupListByRawQuery(uuid.toString(), query.toString()).cancellable()
                .collect { schoolGroupList ->
                    _schoolGroupList.value = LatestSchoolGroupListUiState.Success(schoolGroupList)
                }
        }
    }

    fun setSchoolTeacherAttendanceList(uuid: String?) {
        teachersAttendanceListJob.cancel()
        teachersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolTeacherAttendanceList.emit(LatestSchoolTeacherAttendanceListUiState.Loading)
            personAttendanceRepository.getSchoolAttendanceTeacherFlowList(uuid)
                .collect { schoolTeacherAttendanceList ->
                    Log.d(TAG, "Update Teacher Attendance List")
                    _schoolTeacherAttendanceList.emit(
                        LatestSchoolTeacherAttendanceListUiState.Success(schoolTeacherAttendanceList)
                    )
                    personTeacherAttendanceModelList = schoolTeacherAttendanceList
                }
        }
    }

    fun setSchoolTeacherAttendanceListByDate(schoolUuid: String?, date: String) {
        teachersAttendanceListJob.cancel()
        teachersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolTeacherAttendanceList.emit(LatestSchoolTeacherAttendanceListUiState.Loading)
            personAttendanceRepository.getSchoolAttendanceTeacherFlowListByDate(schoolUuid, date)
                .collect { schoolTeacherAttendanceList ->
                    Log.d(TAG, "ByDate: Update Teacher Attendance List - By Date")
                    _schoolTeacherAttendanceList.emit(
                        LatestSchoolTeacherAttendanceListUiState.Success(schoolTeacherAttendanceList)
                    )
                    personTeacherAttendanceModelList = schoolTeacherAttendanceList
                }
        }
    }

    fun setSchoolLearnerAttendanceList(uuid: String?) {
        learnersAttendanceListJob.cancel()
        learnersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAttendanceList.value = LatestSchoolLearnerAttendanceListUiState.Loading
            personAttendanceRepository.getSchoolAttendanceLearnerFlowList(uuid)
                .collect { schoolLearnerAttendanceList ->
                    _schoolLearnerAttendanceList.value =
                        LatestSchoolLearnerAttendanceListUiState.Success(schoolLearnerAttendanceList)
                    personLearnerAttendanceModelList = schoolLearnerAttendanceList
                }

        }
    }

    fun setSchoolLearnerAttendanceListByDate(uuid: String?, date: String) {
        learnersAttendanceListJob.cancel()
        learnersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAttendanceList.value = LatestSchoolLearnerAttendanceListUiState.Loading
            personAttendanceRepository.getSchoolAttendanceLearnerFlowListByDate(uuid, date)
                .collect { schoolLearnerAttendanceList ->
                    _schoolLearnerAttendanceList.value =
                        LatestSchoolLearnerAttendanceListUiState.Success(schoolLearnerAttendanceList)
                }

        }
    }

    fun setSchoolLearnerAttendanceListBySchoolGroup(group: String?, school: String?) {
        learnersAttendanceListJob.cancel()
        learnersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAttendanceList.value = LatestSchoolLearnerAttendanceListUiState.Loading
            personAttendanceRepository.getSchoolAttendanceLearnerFlowListBySchoolGroup(group, school)
                .collect { learnerAttendanceList ->
                    _schoolLearnerAttendanceList.value =
                        LatestSchoolLearnerAttendanceListUiState.Success(learnerAttendanceList)
                    personLearnerAttendanceModelList = learnerAttendanceList
                }
        }
    }

    fun setSchoolLearnerAttendanceListByDateAndSchoolGroup(
        uuid: String?,
        date: String,
        schoolGroupUuid: String?
    ) {
        learnersAttendanceListJob.cancel()
        learnersAttendanceListJob = viewModelScope.launch(Dispatchers.IO) {
            _schoolLearnerAttendanceList.value = LatestSchoolLearnerAttendanceListUiState.Loading
            personAttendanceRepository.getSchoolAttendanceLearnerFlowListByDateAndSchoolGroup(
                uuid,
                date,
                schoolGroupUuid
            )
                .collect { schoolLearnerAttendanceList ->
                    _schoolLearnerAttendanceList.value =
                        LatestSchoolLearnerAttendanceListUiState.Success(schoolLearnerAttendanceList)
                }

        }
    }

    fun setLearnerAttendanceSchoolGroupOptionList(uuid: String?) {
        attendanceSchoolGroupOptionListFlowJob.cancel()
        attendanceSchoolGroupOptionListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _attendanceSchoolGroupList.value = LatestSchoolAttendanceGroupListUiState.Loading
            repository.getSchoolGroupList(uuid).cancellable().collect { schoolGroupList ->
                _attendanceSchoolGroupList.value =
                    LatestSchoolAttendanceGroupListUiState.Success(schoolGroupList)
            }
        }
    }

    fun getAttendanceDateList(uuid: String, entityType: String): List<DisplayAttendanceDateModel> {
        val datesList = ArrayList<DisplayAttendanceDateModel>()
        val readableDatePattern = "E, dd MMM yyyy"
        val datePattern = "yyyy-MM-dd"
        var attendanceDateList = listOf<AttendanceDateSummaryCountModel>()
        when (entityType) {
            Constants.TEACHER_ENTITY_ID -> attendanceDateList =
                personAttendanceRepository.getSchoolTeacherAttendanceDateSummaryCountList(uuid)
            Constants.LEARNER_ENTITY_ID -> attendanceDateList =
                personAttendanceRepository.getSchoolLearnerAttendanceDateSummaryCountList(uuid)
        }

        val dateRange: IntRange = 0..29 //30 days range
        val dateFormat: DateFormat = SimpleDateFormat(datePattern, Locale.ENGLISH)
        val readableFormat: DateFormat = SimpleDateFormat(readableDatePattern, Locale.ENGLISH)
        val currentDay: Calendar = Calendar.getInstance()
        currentDay.add(Calendar.DAY_OF_MONTH, -0)
        for (i in dateRange) {
            val cal: Calendar = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, -i)
            val currentDate = dateFormat.format(currentDay.time)
            var attendanceStatus: AttendanceStatus = AttendanceStatus.NOT_DONE

            if (dateFormat.format(cal.time).equals(currentDate)) {
                attendanceStatus = AttendanceStatus.TODAY
            }

            val attendanceListFilter =
                attendanceDateList.find { it.date == dateFormat.format(cal.time) }

            attendanceListFilter?.let {
                if (it.count_submitted > 0) {
                    attendanceStatus = AttendanceStatus.SUBMITTED
                } else if (it.count_recorded_attendance > 0 && attendanceStatus != AttendanceStatus.TODAY) {
                    attendanceStatus = AttendanceStatus.UNSUBMITTED
                }
            }

            datesList.add(
                DisplayAttendanceDateModel(
                    readableFormat.format(cal.time),
                    attendanceStatus,
                    dateFormat.format(cal.time)
                )
            )
        }
        return datesList
    }

    fun getSelectedTeacherByIdAsync(uuid: String) = viewModelScope.async(Dispatchers.IO) {
        repository.getSelectedTeacherById(uuid)
    }

    fun getSchoolTimetableList(uuid: String) {
        timetableListFlowJob.cancel()
        timetableListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _timetableListFlow.value = SchoolTimetableListUiState.Loading
            repository.getSchoolTimetableList(uuid).cancellable().collect { timetableList ->
                if (timetableList.isEmpty()) {
                    _timetableListFlow.value = SchoolTimetableListUiState.Success(emptyList())
                } else {
                    _timetableListFlow.value = SchoolTimetableListUiState.Success(timetableList)
                }
            }
        }
    }

    fun isSelectedTeacherAttendanceCurrentDate(): Boolean {
        var isCurrentDate = true
        if (isTeacherAttendanceDateSelectInitialized()) {
            isCurrentDate = (selectedTeacherAttendanceDate.date == LocalDate.now().toString())
        }
        return isCurrentDate
    }

    fun isSelectedLearnerAttendanceCurrentDate(): Boolean {
        var isCurrentDate = true
        if (isLearnerAttendanceDateSelectInitialized()) {
            isCurrentDate = (selectedLearnerAttendanceDate.date == LocalDate.now().toString())
        }
        return isCurrentDate
    }

    fun calculateTeacherAttendanceCompleteness(teacherAttendanceList: List<PersonAttendanceModel>): AttendanceCompletenessData {
        val attendanceCompletenessData = AttendanceCompletenessData()
        if (teacherAttendanceList.isNotEmpty()) {
            attendanceCompletenessData.apply {
                countAllPersons = teacherAttendanceList.size
                countMarkedAttendance = teacherAttendanceList.filter {
                    !it.attendance_status_oid.isNullOrEmpty()
                }.size
                if (countAllPersons == 0) {
                    percentComplete = 0
                } else {
                    percentComplete = countMarkedAttendance * 100 / countAllPersons
                }
            }
        }
        return attendanceCompletenessData
    }

    fun calculateLearnerAttendanceCompleteness(learnerAttendanceList: List<PersonAttendanceModel>): AttendanceCompletenessData {
        val attendanceCompletenessData = AttendanceCompletenessData()
        if (learnerAttendanceList.isNotEmpty()) {
            attendanceCompletenessData.apply {
                countAllPersons = learnerAttendanceList.size
                countMarkedAttendance = learnerAttendanceList.filter {
                    !it.attendance_am_status_oid.isNullOrEmpty() && !it.attendance_pm_status_oid.isNullOrEmpty()
                }.size
                if (countAllPersons == 0) {
                    percentComplete = 0
                } else {
                    percentComplete = countMarkedAttendance * 100 / countAllPersons
                }
            }
        }
        return attendanceCompletenessData
    }

    fun isDeviceDateInvalid(): Boolean {
        personAttendanceRepository.deletePreCreatedFutureRecords(currentSchool.uuid)
        var isDeviceDateInvalid = false
        val maxAttendanceDate =
            personAttendanceRepository.getPersonAttendanceTableLatestDate(currentSchool.uuid)
        if (!maxAttendanceDate.isNullOrEmpty() && maxAttendanceDate > LocalDate.now().toString()) {
            invalidDeviceDate = String()
            isDeviceDateInvalid = true
        }
        return isDeviceDateInvalid
    }

    fun isInvalidDeviceDateInitialized(): Boolean {
        var initStatus = false
        if (this::invalidDeviceDate.isInitialized) {
            initStatus = true
        }
        return initStatus
    }

    fun isTeacherAttendanceListSubmitted(personAttendanceList: List<PersonAttendanceModel>): Boolean {
        val attendanceListFilter =
            personAttendanceList.filter { it.submitted != null && it.submitted!! >= 1 }
        return attendanceListFilter.isNotEmpty()
    }

    fun isLearnerAttendanceListSubmitted(personAttendanceList: List<PersonAttendanceModel>): Boolean {
        val attendanceListFilter =
            personAttendanceList.filter { it.submitted != null && it.submitted!! >= 1 }
        return attendanceListFilter.isNotEmpty() && attendanceListFilter.size == personAttendanceList.size
    }

    fun processTempPhoto() {
        if (tempPhotoPath.isNullOrEmpty()) return
        val ATTENDANCE_PORTRAIT_MAX_PX = 600
        val THUMB_MAX_PX = 320

        //resize
        currentTeacherPortrait =
            Utils.decodeScaledBitmap(
                Uri.fromFile(File(tempPhotoPath!!)),
                getApplication(),
                ATTENDANCE_PORTRAIT_MAX_PX
            )

        //fix orientation
        currentPortraitOrientation = Utils.getOrientation(tempPhotoPath ?: "")

        if (currentPortraitOrientation > 0) {
            if (currentTeacherPortrait != null) {
                currentTeacherPortrait =
                    currentTeacherPortrait!!.rotateBitmap(currentPortraitOrientation)
                currentPortraitOrientation = 0
            }
        }

        //re-save the temp file with new res
        currentTeacherPortrait?.let {
            try {
                val outputStream = FileOutputStream(tempPhotoPath)
                it.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()

                currentPortraitThumbnail =
                    Utils.decodeScaledBitmap(
                        Uri.fromFile(tempPhotoPath?.let { it1 -> File(it1) }),
                        getApplication(),
                        THUMB_MAX_PX
                    )
            } catch (ex: Exception) {
                Log.d(TAG, ex.message.toString())
            }
        }
    }

    override fun getPortraitBinaries(): String? = null

    fun deleteTempFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tempPhotoPath?.let {
                    File(it).delete()
                }
            } catch (e: Exception) {
                Log.d(TAG, e.message.toString())
            }
        }
    }

    fun deletePhotoCacheInViewModelScope(storageDirPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            deletePhotoCache(storageDirPath)
        }
    }

    override fun deletePhotoCache(storageDirPath: String) {
        val storageDir = File(storageDirPath)
        try {
            if (storageDir.isDirectory) {
                val children: Array<String>? = storageDir.list()
                children?.forEach {
                    File(storageDir, it).delete()
                }
            }

            tempPhotoPath?.let { File(it).delete() }
            tempPhotoPath = null
            currentTeacherPortrait = null
            currentPortraitThumbnail = null
            setLatestCameraPhoto(null)
        } catch (e: Exception) {
            Log.d(TAG, e.message.toString())
        }
    }

    override fun setLatestCameraPhoto(photo: PhotoModel?) {
        viewModelScope.launch(Dispatchers.Default) {
            _cameraPhoto.update { cp -> cp.copy(photo = photo) }
        }
    }

    private fun saveTempPhotoAsAttendance(attendanceUuid: String) {

        //save photo and delete temp
        val storageDir = File(Utils.getAttendancePhotoPath(application))
        if (storageDir.exists().not()) storageDir.mkdirs()

        tempPhotoPath?.let { tp ->
            val sourceFile = File(tp)

            if (sourceFile.exists()) {
                if (sourceFile.length() > 0) {
                    val targetFile = File(storageDir, "${attendanceUuid}.jpg")
                    if (sourceFile.renameTo(targetFile)) {
                        Log.d(TAG, "saved")
                    } else {
                        Log.d(TAG, "not saved")
                    }
                }
            }
        }
    }

    fun getOptionList(listName: String): List<OptionList> {
        return commonRepository.getOptionList(listName)
    }

    /** Returns UTC-midnight epoch millis for every date >= today that has submitted attendance. */
    fun getBlockedDatesMillisForNoSchool(): Set<Long> {
        return personAttendanceRepository
            .getSubmittedDatesFromToday(currentSchool.uuid)
            .mapNotNull { dateStr ->
                runCatching {
                    java.time.LocalDate.parse(dateStr)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }.toHashSet()
    }

    fun markNoSchoolDays(
        reasonOid: String,
        reasonOther: String?,
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val schoolUuid = currentSchool.uuid
            val acYear    = Utils.getAcademicYear().toShort()
            val time      = Utils.getISODateTimeUTC()
            val user      = Utils.getUserId(getApplication())
            val lat       = Utils.getLastKnownLat(getApplication())
            val lng       = Utils.getLastKnownLng(getApplication())

            val learners = personAttendanceRepository.getSchoolLearnerPersonList(schoolUuid)
            val teachers = personAttendanceRepository.getSchoolTeacherPersonList(schoolUuid)

            var current = java.time.LocalDate.parse(startDate)
            val end     = java.time.LocalDate.parse(endDate)
            val today   = java.time.LocalDate.now()
            var processedToday = false

            while (!current.isAfter(end)) {
                val dateStr = current.toString()

                if (!current.isAfter(today)) {
                    markEntityNoSchool(
                        persons      = learners,
                        entityType   = Constants.LEARNER_ENTITY_ID,
                        schoolUuid   = schoolUuid,
                        dateStr      = dateStr,
                        acYear       = acYear,
                        reasonOid    = reasonOid,
                        reasonOther  = reasonOther,
                        lat          = lat,
                        lng          = lng,
                        time         = time,
                        user         = user,
                        amStatus     = Constants.ATTENDANCE_ABSENT_ID,
                        pmStatus     = Constants.ATTENDANCE_ABSENT_ID,
                        statusOid    = null
                    )
                    markEntityNoSchool(
                        persons      = teachers,
                        entityType   = Constants.TEACHER_ENTITY_ID,
                        schoolUuid   = schoolUuid,
                        dateStr      = dateStr,
                        acYear       = acYear,
                        reasonOid    = reasonOid,
                        reasonOther  = reasonOther,
                        lat          = lat,
                        lng          = lng,
                        time         = time,
                        user         = user,
                        amStatus     = null,
                        pmStatus     = null,
                        statusOid    = Constants.ATTENDANCE_ABSENT_ID
                    )
                    processedToday = true
                } else {
                    WorkerHelper.scheduleNoSchoolWorker(
                        getApplication(), schoolUuid, dateStr, reasonOid, reasonOther
                    )
                }

                current = current.plusDays(1)
            }

            if (processedToday) WorkerHelper.initUniqueUploadWorkRequest(getApplication())
        }
    }

    private suspend fun markEntityNoSchool(
        persons: List<uk.org.cgatechnologies.wideya.school_management.models.NoSchoolPersonModel>,
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

        val existing = personAttendanceRepository
            .getAttendanceByDateAndEntityType(schoolUuid, dateStr, entityType)
            .associateBy { it.person_uuid }

        val toInsert = mutableListOf<uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance>()
        val toUpdate = mutableListOf<uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance>()

        for (person in persons) {
            val prev = existing[person.person_uuid]
            val record = uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance(
                uuid                    = prev?.uuid ?: Utils.getUuidOrdered(),
                date                    = dateStr,
                person_uuid             = person.person_uuid,
                entity_type_oid         = entityType,
                academic_year           = acYear,
                school_uuid             = schoolUuid,
                school_group_uuid       = person.school_group_uuid,
                attendance_am_status_oid = amStatus,
                attendance_pm_status_oid = pmStatus,
                attendance_status_oid   = statusOid,
                absent_reason_oid       = reasonOid,
                absent_reason_other     = reasonOther,
                lat                     = lat,
                lng                     = lng,
                biometric_method_oid    = null,
                biometric_reference     = null,
                submitted               = 1,
                created_at              = prev?.created_at ?: time,
                created_by              = prev?.created_by ?: user,
                updated_at              = time,
                updated_by              = user,
                deleted_at              = null,
                deleted_by              = null,
                sync_flag               = 1
            )
            if (prev != null) toUpdate.add(record) else toInsert.add(record)
        }

        if (toInsert.isNotEmpty()) personAttendanceRepository.insertPersonAttendanceList(toInsert)
        if (toUpdate.isNotEmpty()) personAttendanceRepository.updatePersonAttendanceList(toUpdate)
    }

    // ── School Feeding ───────────────────────────────────────────────────────

    fun getSchoolFeeding(): SchoolFeeding? =
        schoolFeedingDao.getLatestBySchool(currentSchool.uuid)

    fun saveSchoolFeeding(record: SchoolFeeding) {
        viewModelScope.launch(Dispatchers.IO) {
            schoolFeedingDao.insert(record)
            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
        }
    }

    fun getSchoolFeedingStock(month: String): SchoolFeedingStock? =
        schoolFeedingStockDao.getBySchoolAndMonth(currentSchool.uuid, month)

    fun saveSchoolFeedingStock(record: SchoolFeedingStock) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = schoolFeedingStockDao.getBySchoolAndMonth(record.school_uuid, record.stock_month)
            if (existing == null) {
                schoolFeedingStockDao.insert(record)
            } else {
                schoolFeedingStockDao.update(
                    record.copy(
                        uuid = existing.uuid,
                        created_at = existing.created_at,
                        created_by = existing.created_by
                    )
                )
            }
            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
        }
    }
}

sealed class LatestSchoolListUiState {
    object Loading : LatestSchoolListUiState()

    data class Success(val schoolList: List<SchoolModel>) : LatestSchoolListUiState()

    data class Error(val exception: Throwable) : LatestSchoolListUiState()
}

sealed class LatestSchoolDetailUiState {
    data class Success(val school: SchoolModel?) : LatestSchoolDetailUiState()
    data class Error(val exception: Throwable) : LatestSchoolDetailUiState()
}

sealed class LatestSchoolTeacherListUiState {
    object Loading : LatestSchoolTeacherListUiState()
    data class Success(val teacherList: List<TeacherModel>) : LatestSchoolTeacherListUiState()
    data class Error(val exception: Throwable) : LatestSchoolTeacherListUiState()
}

sealed class SelectedTeacherModelUiState {
    data class Success(val teacherModel: TeacherModel?) : SelectedTeacherModelUiState()
    data class Error(val exception: Throwable) : SelectedTeacherModelUiState()
}

sealed class LatestSchoolLearnerAdmissionListUiState {
    object Loading : LatestSchoolLearnerAdmissionListUiState()

    data class Success(val learnerAdmissionList: List<LearnerAdmissionModel>) :
        LatestSchoolLearnerAdmissionListUiState()

    data class Error(val exception: Throwable) : LatestSchoolLearnerAdmissionListUiState()
}

sealed class LatestSchoolGroupListUiState {
    object Loading : LatestSchoolGroupListUiState()
    data class Success(val schoolGroupList: List<SchoolGroupModel>) : LatestSchoolGroupListUiState()
    data class Error(val exception: Throwable) : LatestSchoolGroupListUiState()
}

sealed class LatestSchoolAttendanceGroupListUiState {
    object Loading : LatestSchoolAttendanceGroupListUiState()

    data class Success(val schoolAttendanceGroupList: List<SchoolGroupModel>) :
        LatestSchoolAttendanceGroupListUiState()

    data class Error(val exception: Throwable) : LatestSchoolAttendanceGroupListUiState()
}

sealed class LatestSchoolTeacherAttendanceListUiState {
    object Loading : LatestSchoolTeacherAttendanceListUiState()

    data class Success(val teacherAttendanceList: List<PersonAttendanceModel>) :
        LatestSchoolTeacherAttendanceListUiState()

    data class Error(val exception: Throwable) : LatestSchoolTeacherAttendanceListUiState()
}

sealed class LatestSchoolLearnerAttendanceListUiState {
    object Loading : LatestSchoolLearnerAttendanceListUiState()

    data class Success(val learnerAttendanceList: List<PersonAttendanceModel>) :
        LatestSchoolLearnerAttendanceListUiState()

    data class Error(val exception: Throwable) : LatestSchoolLearnerAttendanceListUiState()
}

sealed class SchoolTimetableListUiState {
    object Loading : SchoolTimetableListUiState()

    data class Success(val timetableList: List<SchoolTimetableModel>) : SchoolTimetableListUiState()
    data class Error(val exception: Throwable) : SchoolTimetableListUiState()
}

sealed class LatestCameraPhotoUiState {
    data class Success(val photo: PhotoModel?) : LatestCameraPhotoUiState()
    data class Error(val exception: Throwable) : LatestCameraPhotoUiState()
}