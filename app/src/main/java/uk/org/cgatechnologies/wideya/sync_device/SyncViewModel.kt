package uk.org.cgatechnologies.wideya.sync_device

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    val TAG: String = "SyncViewModel"

    private var syncDao: SyncDao? = null

    private val _logSyncList = MutableStateFlow(
        LatestLogSyncListUiState.Success(
            emptyList()
        )
    )
    val logSyncList: StateFlow<LatestLogSyncListUiState> = _logSyncList

    val logSyncListJob: Job = setLogSyncList()

    private var recordsCount: Int = 0
    private var filesCount: Int = 0

    init {
        this.syncDao =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .syncDao()
    }

    private fun setLogSyncList(): Job {
        return viewModelScope.launch() {
            syncDao?.let {syncDao ->
                syncDao.getLogSyncList().collect { logSyncList ->
                    Log.d(TAG, logSyncList.count().toString())
                    _logSyncList.value = LatestLogSyncListUiState.Success(logSyncList)
                }
            }
        }
    }

    fun setSyncFlagCount() {
        syncDao?.let { syncDao ->
            recordsCount = syncDao.getSyncFlagCount()
        }
    }

    fun getSyncFlagCount(): Int {
        return recordsCount
    }

    fun setFilesCount(context: Context) {
        val paths = listOf(Constants.PATH_FINGERPRINTS, Constants.PATH_TEACHER_PROFILE_PHOTOS, Constants.PATH_TEACHER_ATT_PHOTOS)
        var counter = 0
        paths.forEach{
            counter += SyncFiles.countFilesInFolder(context, it)
        }
        filesCount = counter
    }

    fun getFilesCount(): Int {
        return filesCount
    }

}

sealed class LatestLogSyncListUiState {
    data class Success(val logSyncList: List<LogSync>) : LatestLogSyncListUiState()
    data class Error(val exception: Throwable) : LatestLogSyncListUiState()
}
