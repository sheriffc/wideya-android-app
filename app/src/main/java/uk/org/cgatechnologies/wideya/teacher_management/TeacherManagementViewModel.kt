package uk.org.cgatechnologies.wideya.teacher_management

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
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
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.data.CommonRepository
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.MediaPhoto
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.common.utils.Extensions.rotateBitmap
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.school_management.PersonAttendanceRepository
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel
import uk.org.cgatechnologies.wideya.teacher_management.models.mappers.toPerson
import uk.org.cgatechnologies.wideya.teacher_management.models.mappers.toTeacher
import uk.org.cgatechnologies.wideya.teacher_management.models.mappers.toTeacherTimetable
import java.io.File
import java.io.FileOutputStream

private const val TAG: String = "TeacherMgmtViewModel"

class TeacherManagementViewModel(private val application: Application) :
    AndroidViewModel(application),
    IViewModel.ICameraViewModel {

    private val repository: TeacherManagementRepository
    private val commonRepository: CommonRepository
    private val personAttendanceRepository: PersonAttendanceRepository

    lateinit var currentTeacher: TeacherModel
    lateinit var currentTimetableEntry: TeacherTimetableModel
    lateinit var currentTeacherEmploymentStatus: String
    lateinit var currentDetailsMode: DetailsMode
    lateinit var timetableDetailsMode: DetailsMode

    override var portraitBinariesDelete: Boolean = false
    private var currentTeacherPortrait: Bitmap? = null
    override var currentPortraitThumbnail: Bitmap? = null
    override var currentPortraitOrientation: Int = 0

    override var tempPhotoPath: String? = null

    private val _teacherDetail =
        MutableStateFlow(LatestTeacherDetailUiState.Success(TeacherModel()))
    val teacherDetail: StateFlow<LatestTeacherDetailUiState> = _teacherDetail

    private val _payrollTeacherList: MutableStateFlow<LatestPayrollTeacherListUiState> =
        MutableStateFlow(LatestPayrollTeacherListUiState.Success(emptyList()))
    val payrollTeacherList: StateFlow<LatestPayrollTeacherListUiState> = _payrollTeacherList

    private val _nonPayrollTeacherList: MutableStateFlow<LatestNonPayrollTeacherListUiState> =
        MutableStateFlow(LatestNonPayrollTeacherListUiState.Success(emptyList()))
    val nonPayrollTeacherList: StateFlow<LatestNonPayrollTeacherListUiState> = _nonPayrollTeacherList

    private val _timetableListFlow: MutableStateFlow<TeacherTimetableListUiState> =
        MutableStateFlow(TeacherTimetableListUiState.Success(emptyList()))
    val timetableListFlow: StateFlow<TeacherTimetableListUiState> = _timetableListFlow

    private val _cameraPhoto = MutableStateFlow(LatestCameraProfilePhotoUiState.Success(null))
    val cameraPhoto: StateFlow<LatestCameraProfilePhotoUiState> = _cameraPhoto

    private var teacherFlowJob: Job = Job()
    private var payrollListFlowJob: Job = Job()
    private var nonPayrollListFlowJob: Job = Job()
    private var timetableListFlowJob: Job = Job()

    fun isCurrentTeacherInitialized(): Boolean {
        return this::currentTeacher.isInitialized
    }

    fun processTempPhoto() {
        if (tempPhotoPath.isNullOrEmpty()) return
        val PROFILE_PORTRAIT_MAX_PX = 1400
        val THUMB_MAX_PX = 320

        //resize
        currentTeacherPortrait =
            Utils.decodeScaledBitmap(
                Uri.fromFile(File(tempPhotoPath!!)),
                getApplication(),
                PROFILE_PORTRAIT_MAX_PX
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

    init {
        Log.d(TAG, "initialise vm")

        this.repository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .teacherManagementDao()
                .let { dao ->
                    TeacherManagementRepository.getInstance(dao)
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
    }

    fun setTeacherDetail(uuid: String) {
        teacherFlowJob.cancel()
        teacherFlowJob = viewModelScope.launch(Dispatchers.IO) {
            repository.getTeacher(uuid).cancellable().collect { teacher ->
                if (teacher == null) {
                    _teacherDetail.value = LatestTeacherDetailUiState.Success(TeacherModel())
                } else {
//                    Log.d("binding", "Flow setDetail() returns teacher: " + teacher.full_name)
                    _teacherDetail.value = LatestTeacherDetailUiState.Success(teacher)
                }
            }
        }
    }

    fun setPayrollTeacherList(uuid: String?) {
        payrollListFlowJob.cancel()
        payrollListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _payrollTeacherList.value = LatestPayrollTeacherListUiState.Loading
            repository.getPayrollTeacherList(uuid).cancellable().collect { payrollList ->
                _payrollTeacherList.value = LatestPayrollTeacherListUiState.Success(payrollList)
            }
        }
    }

    fun setPayrollTeacherListByQuery(uuid: String?, query: String?) {
        payrollListFlowJob.cancel()
        payrollListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _payrollTeacherList.value = LatestPayrollTeacherListUiState.Loading
            repository.getPayrollTeacherListByRawQuery(uuid.toString(), query.toString())
                .cancellable()
                .collect { payrollList ->
                    _payrollTeacherList.value = LatestPayrollTeacherListUiState.Success(payrollList)
                }
        }
    }

    fun setNonPayrollTeacherList() {
        nonPayrollListFlowJob.cancel()
        nonPayrollListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _nonPayrollTeacherList.value = LatestNonPayrollTeacherListUiState.Loading
            repository.getNonPayrollTeacherList().cancellable().collect { list ->
                _nonPayrollTeacherList.value = LatestNonPayrollTeacherListUiState.Success(list)
            }
        }
    }

    fun setNonPayrollTeacherListByQuery(query: String?) {
        nonPayrollListFlowJob.cancel()
        nonPayrollListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _nonPayrollTeacherList.value = LatestNonPayrollTeacherListUiState.Loading
            repository.getNonPayrollTeacherListByRawQuery(query.toString())
                .cancellable()
                .collect { list ->
                    _nonPayrollTeacherList.value = LatestNonPayrollTeacherListUiState.Success(list)
                }
        }
    }

    fun getOptionList(listName: String): List<OptionList> {
        return commonRepository.getOptionList(listName)
    }

    fun getSubjectsForEducationLevel(listName: String): List<OptionList> {

        if (currentTeacher.school_uuid.isNullOrEmpty()) {
            return commonRepository.getOptionList(listName)
        }

        val schoolEducationLevel = getEducationLevelForSchoolId(currentTeacher.school_uuid!!)

        if (schoolEducationLevel.isNullOrEmpty()) {
            return commonRepository.getOptionList(listName)
        }
        return commonRepository.getSchoolSubjectsBySchoolEducationLevel(schoolEducationLevel)
    }

    private fun getEducationLevelForSchoolId(schoolUuid: String): String? {
        return commonRepository.getEducationLevelForSchoolId(schoolUuid)
    }

    fun saveTeacher(storageDirPath: String) {
        when (currentDetailsMode) {
            DetailsMode.EDIT -> {
                viewModelScope.launch {
                    val job: Job = launch(Dispatchers.IO) {
                        handleMediaPhoto(currentTeacher)
                    }
                    job.join()

                    deletePhotoCache(storageDirPath)

                    job.join()

                    updateTeacher(currentTeacher)

                }
            }
            DetailsMode.NEW -> {
                viewModelScope.launch {
                    val job: Job = launch(Dispatchers.IO) {
                        handleMediaPhoto(currentTeacher)
                    }
                    job.join()

                    deletePhotoCache(storageDirPath)

                    job.join()

                    insertTeacher(currentTeacher)
                }


            }
            else -> {}
        }
    }

    private suspend fun insertTeacher(teacher: TeacherModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())
            teacher.apply {
                // person_uuid is now generated earlier on when the blank form loads
                if (person_uuid.isNullOrEmpty()) {
                    person_uuid = Utils.getUuidOrdered()
                }
                person_created_at = time
                person_created_by = user
                person_updated_at = time
                person_updated_by = user
                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }

            commonRepository.insertPerson(teacher.toPerson())
            repository.insertTeacher(teacher.toTeacher())
        }
    }

    private suspend fun updateTeacher(teacher: TeacherModel) {
        viewModelScope.launch(Dispatchers.IO) {
            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())
            teacher.apply {
                updated_at = time
                updated_by = user
                person_updated_at = time
                person_updated_by = user
                person_deleted_at = null
                person_deleted_by = null
                sync_flag = 1
            }

            repository.updateTeacher(teacher.toTeacher())
            commonRepository.updatePerson(teacher.toPerson())
        }
    }

    private suspend fun handleMediaPhoto(teacher: TeacherModel) {
        if (teacher.portrait_base64_data.isNullOrEmpty().not()) {
            tempPhotoPath?.let { tp ->
                val sourceFile = File(tp)
                if (sourceFile.exists()) {
                    if (sourceFile.length() > 0) {
                        currentTeacher.portrait_uuid = insertMediaPhoto(teacher)

                        //save photo
                        val storageDir = File(Utils.getProfilePhotoPath(application))
                        if (storageDir.exists().not()) storageDir.mkdirs()
                        val targetFile = File(storageDir, "${teacher.portrait_uuid}.jpg")
                        if (sourceFile.renameTo(targetFile)) {
                            Log.d(TAG, "saved")
                            tempPhotoPath = null
                        } else {
                            Log.e(TAG, "not saved")
                        }
//                            try {
//                                sourceFile.copyTo(targetFile, true)
//                                Log.d(TAG, "saved")
//                            } catch (e: Exception) {
//                                Log.e(TAG, e.message.toString())
//                            }
                    }
                }
            } ?: run { teacher.portrait_uuid = updateMediaPhoto(teacher) }
        }
    }

    fun removeTeacher(teacher: TeacherModel) {
        viewModelScope.launch(Dispatchers.IO) {
            teacher.apply {
                val time = Utils.getISODateTimeUTC()
                val user = Utils.getUserId(getApplication())
                updated_at = time
                updated_by = user
                deleted_at = time
                deleted_by = user
                sync_flag = 1
            }

            repository.updateTeacher(teacher.toTeacher())
            personAttendanceRepository.softDeletePersonAttendanceForToday(
                teacher.person_uuid!!,
                teacher.updated_by
            )
            commonRepository.softDeletePersonFingerprintForPerson(
                teacher.person_uuid!!,
                teacher.updated_by
            )
            commonRepository.softDeletePersonFingerprintForPerson(
                teacher.person_uuid!!,
                teacher.updated_by
            )
            commonRepository.softDeleteMediaPhotoForPerson(
                teacher.person_uuid!!,
                teacher.updated_by
            )
        }
    }

    private suspend fun insertMediaPhoto(teacher: TeacherModel): String {
        val photoUuid = Utils.getUuidOrdered()

        val user = Utils.getUserId(getApplication())
        val time = Utils.getISODateTimeUTC()
        teacher.apply {
            val mediaPhoto = MediaPhoto(
                uuid = photoUuid,
                ref_uuid = person_uuid,
                base64_data = portrait_base64_data, //already set base64
                display_orientation = currentPortraitOrientation.toShort(),
                active = 1,
                created_by = user,
                created_at = time,
                updated_by = user,
                updated_at = time,
                sync_flag = 1
            )
            commonRepository.insertMediaPhoto(mediaPhoto)
        }

        return photoUuid
    }

    private suspend fun updateMediaPhoto(teacher: TeacherModel): String {
        val photoUuid = Utils.getUuidOrdered()

//        viewModelScope.launch(Dispatchers.IO) {
        val user = Utils.getUserId(getApplication())
        val time = Utils.getISODateTimeUTC()
        teacher.apply {
            val mediaPhoto = MediaPhoto(
                uuid = portrait_uuid ?: photoUuid,
                ref_uuid = person_uuid,
                base64_data = portrait_base64_data,
                display_orientation = currentPortraitOrientation.toShort(),
                active = 1,
                created_by = portrait_created_by,
                created_at = portrait_created_at ?: time,
                updated_by = user,
                updated_at = time,
                sync_flag = 1
            )
            commonRepository.updateMediaPhoto(mediaPhoto)
//            }
        }

        return teacher.portrait_uuid ?: photoUuid
    }

    fun getTeacherTimetableList(uuid: String) {
        timetableListFlowJob.cancel()
        timetableListFlowJob = viewModelScope.launch(Dispatchers.IO) {
            _timetableListFlow.value = TeacherTimetableListUiState.Loading
            repository.getTeacherTimetableList(uuid).cancellable().collect { timetableList ->
                _timetableListFlow.value = TeacherTimetableListUiState.Success(timetableList)
            }
        }
    }

    fun checkTimetableEntryTiming(uuid: String, day: String, start: String, end: String): Boolean {
        var isValid = true
        viewModelScope.launch {
            timetableListFlow.collect { item ->
                if (item is TeacherTimetableListUiState.Success) {
                    val dayList = item.timetableList.filter {
                        it.day_of_the_week_name.equals(day, true) &&
                                it.uuid != uuid
                    }
                    if (dayList.isNotEmpty()) {
                        val overlap = dayList.find {
                            (start >= it.start_time!! && start < it.end_time!!) ||
                                    (end > it.start_time!! && end <= it.end_time!!)
                        }
                        if (overlap != null) isValid = false
                    }
                }
            }
        }

        return isValid
    }

    fun insertTimetableEntry(timetableModel: TeacherTimetableModel) {
        viewModelScope.launch(Dispatchers.IO) {

            val time = Utils.getISODateTimeUTC()
            val user = Utils.getUserId(getApplication())

            timetableModel.apply {
                // in case UUIDs needs to be generated earlier in some use cases
                if (uuid.isEmpty()) {
                    uuid = Utils.getUuidOrdered()
                }
                if (teacher_uuid.isEmpty()) {
                    teacher_uuid = currentTeacher.uuid
                }
                if (school_uuid.isNullOrEmpty()) {
                    school_uuid = currentTeacher.school_uuid
                }

                created_at = time
                created_by = user
                updated_at = time
                updated_by = user
                sync_flag = 1
            }
            repository.insertTimetableEntry(timetableModel.toTeacherTimetable())
        }
    }

    fun updateTimetableEntry(timetableModel: TeacherTimetableModel) {
        viewModelScope.launch(Dispatchers.IO) {

            timetableModel.apply {
                updated_at = Utils.getISODateTimeUTC()
                updated_by = Utils.getUserId(getApplication())
                sync_flag = 1
            }

            repository.updateTimetableEntry(timetableModel.toTeacherTimetable())
        }
    }

    fun deleteTimetableEntry() {
        viewModelScope.launch(Dispatchers.IO) {
            currentTimetableEntry.apply {
                deleted_at = Utils.getISODateTimeUTC()
                deleted_by = Utils.getUserId(getApplication())
                sync_flag = 1
            }

            repository.deleteTimetableEntry(currentTimetableEntry.toTeacherTimetable())
        }
    }

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

    override fun getPortraitBinaries(): String? = currentTeacher.portrait_base64_data

    override fun deletePhotoCache(storageDirPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            //PL -- could this use the function deleteTempFile?
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
    }
     fun clearPhotoCache() {
        tempPhotoPath = null
        currentTeacherPortrait = null
        currentPortraitThumbnail = null
        setLatestCameraPhoto(null)
    }


    override fun setLatestCameraPhoto(photo: PhotoModel?) {
        viewModelScope.launch(Dispatchers.Default) {
            _cameraPhoto.update { cp -> cp.copy(photo = photo) }
        }
    }
}

sealed class LatestTeacherDetailUiState {
    data class Success(val teacher: TeacherModel?) : LatestTeacherDetailUiState()
    data class Error(val exception: Throwable) : LatestTeacherDetailUiState()
}

sealed class LatestPayrollTeacherListUiState {
    object Loading : LatestPayrollTeacherListUiState()

    data class Success(val payrollTeacherList: List<TeacherPayrollModel>) :
        LatestPayrollTeacherListUiState()

    data class Error(val exception: Throwable) : LatestPayrollTeacherListUiState()
}

sealed class LatestNonPayrollTeacherListUiState {
    object Loading : LatestNonPayrollTeacherListUiState()

    data class Success(val nonPayrollTeacherList: List<TeacherPayrollModel>) :
        LatestNonPayrollTeacherListUiState()

    data class Error(val exception: Throwable) : LatestNonPayrollTeacherListUiState()
}

sealed class TeacherTimetableListUiState {
    object Loading : TeacherTimetableListUiState()

    data class Success(val timetableList: List<TeacherTimetableModel>) :
        TeacherTimetableListUiState()

    data class Error(val exception: Throwable) : TeacherTimetableListUiState()
}


sealed class LatestCameraProfilePhotoUiState {
    data class Success(val photo: PhotoModel?) : LatestCameraProfilePhotoUiState()
    data class Error(val exception: Throwable) : LatestCameraProfilePhotoUiState()
}