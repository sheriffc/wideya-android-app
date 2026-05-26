package uk.org.cgatechnologies.wideya.sync_device

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.RoomOpenHelper
import androidx.sqlite.db.SimpleSQLiteQuery
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.analysis.AnalysisDao
import uk.org.cgatechnologies.wideya.analysis.AnalysisRepository
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.*
import uk.org.cgatechnologies.wideya.common.network.RetrofitBuilder
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner
import uk.org.cgatechnologies.wideya.learner_performance.entities.LearnerPerformance
import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup
import uk.org.cgatechnologies.wideya.school_management.entities.School
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolAcademicYear
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeeding
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolFeedingStock
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerAdmission
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesBiTables
import uk.org.cgatechnologies.wideya.sync_device.entities.TableStatesUniTables
import uk.org.cgatechnologies.wideya.sync_device.inferfaces.SyncApi
import uk.org.cgatechnologies.wideya.sync_device.models.*
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherPayroll
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherTimetable


private val json = Json { ignoreUnknownKeys = true }

@Volatile
private var syncDao: SyncDao? = null

class SyncData {
    companion object {
        private const val TAG = "SyncData"
        const val NETWORK_ERROR = -1
        const val TOKEN_ERROR = -403
        const val SCHOOL_AUTH_ERROR = -405

        @SuppressLint("MissingPermission")
        suspend fun downloadData(context: Context): Int {
            if (AppDatabase.getInstance() == null) {
                return NETWORK_ERROR
            }

            syncDao = AppDatabase.getInstance()?.syncDao()

            syncDao?.let { syncDao->

                //check credentials first
                val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
                val service = retrofit.create(SyncApi::class.java)

                val tokenRequest = TokenRequest(
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "").toString(),
                    "token_check",
                    mutableMapOf()
                )

                Log.d(TAG, "Post Token Check")
                when (val responseToken = service.postTokenCheck(tokenRequest)) {
                    is NetworkResponse.Success -> {
                        Log.d(TAG, "request success")
                        if (responseToken.response.isSuccessful) {
                            val tokenResponse: TokenResponse? = responseToken.body
                            val tokenResponseStatus = tokenResponse?.status ?: false

                            if (tokenResponseStatus) {
                                if (tokenResponse != null) {
                                    if (tokenResponse.data["scope_cache_id"].equals(
                                            Utils.getEncSharedPrefs(context)
                                                .getString(Constants.PREF_SCOPE_CACHE_ID, "")
                                        ).not()
                                    ) {
                                        Utils.getEncSharedPrefs(context).edit()
                                            .putString(
                                                Constants.PREF_SCOPE_CACHE_ID,
                                                tokenResponse.data["scope_cache_id"]
                                            )
                                            .putString(Constants.PREF_SCOPE_HASH, tokenResponse.data["scope_hash"])
                                            .putString(
                                                Constants.PREF_SCOPE_SCHOOLS,
                                                tokenResponse.data["scope_schools"]
                                            )
                                            .apply()
                                    }
                                } else {
                                    return TOKEN_ERROR
                                }
                            } else {
                                return TOKEN_ERROR
                            }
                        } else {
                            return NETWORK_ERROR
                        }
                    }
                    is NetworkResponse.Error -> {
                        Log.d(TAG, "network error")
                        return NETWORK_ERROR
                    }
                }

                val schoolListString =
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCOPE_SCHOOLS, "").toString()
                if (checkSchoolPermission(context, schoolListString) < 0) return SCHOOL_AUTH_ERROR

                val downloadDataRequest = DownloadDataRequest(
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "").toString(),
                    "download",
                    DownloadDataParams(
                        scope_cache_id = Utils.getEncSharedPrefs(context)
                            .getString(Constants.PREF_SCOPE_CACHE_ID, "").toString(),
                        sync_mode = "school_level",
                        app_version = Utils.getVersionCode(),
                        app_db_version = Utils.getDbVersionCode(),
                        install_id = Utils.getEncSharedPrefs(context)
                            .getString(Constants.PREF_INSTALL_ID, "")
                            .toString(),
                        table_states_uni_tables = getTableStateForUniTablesVersion3(syncDao),
                        table_states_bi_tables = getTableStateForBiTablesBySchoolVersion3(syncDao, schoolListString)
                    )
                )

                Log.d(TAG, "Post Download Data")
                when (val response = service.postDownloadData(downloadDataRequest)) {
                    is NetworkResponse.Success -> {
                        if (response.response.isSuccessful) {
                            Log.d(TAG, "response success")
                            val downloadResponse: DownloadDataResponse? = response.body
                            val responseStatus = downloadResponse?.status ?: false

                            if (responseStatus) {
                                if (downloadResponse != null) {

                                    var insertCounter = 0

                                    val builder =
                                        NotificationCompat.Builder(context, Constants.CHANNEL_ID).apply {
                                            setContentTitle("Downloading Data")
                                            setContentText("Downloading in progress")
                                            setSmallIcon(R.drawable.ic_wideya_logo_square_colour)
                                            priority = NotificationCompat.PRIORITY_LOW
                                            setOnlyAlertOnce(true)
                                            setTimeoutAfter(10000)
                                        }

                                    val PROGRESS_MAX = downloadResponse.data.size
                                    val PROGRESS_CURRENT = 0

                                    var tableCounter = 0

                                    NotificationManagerCompat.from(context).apply {
                                        Log.d(TAG, "Notification Manager Compat")

                                        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, true)
                                        notify(Constants.NOTIFY_SYNC_ID, builder.build())

                                        downloadResponse.data.forEach { (tableName, tableData) ->
                                            Log.d(TAG, "Data Response: $tableName")

                                            builder.setContentText("checking table $tableName")
                                            builder.setProgress(PROGRESS_MAX, tableCounter.plus(1), false)
                                            notify(Constants.NOTIFY_SYNC_ID, builder.build())
                                            tableCounter += 1

                                            try { when (tableName) {
                                                "district_office" -> {
                                                    val list = json.decodeFromString<List<DistrictOffice>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertDistrictOffice(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_academic_year" -> {
                                                    val list = json.decodeFromString<List<SchoolAcademicYear>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolAcademicYear(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "non_payroll_teachers" -> {
                                                    val list = json.decodeFromString<List<TeacherPayroll>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertTeacherPayroll(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into teacher_payroll (non-payroll)")
                                                }
                                                "teacher_payroll" -> {
                                                    val list = json.decodeFromString<List<TeacherPayroll>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertTeacherPayroll(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "option_list" -> {
                                                    val list = json.decodeFromString<List<OptionList>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertOptionList(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "option_list_link" -> {
                                                    val list = json.decodeFromString<List<OptionListLink>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertOptionListLink(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "geo" -> {
                                                    val list = json.decodeFromString<List<Geo>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertGeo(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "learner" -> {
                                                    val list = json.decodeFromString<List<Learner>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertLearner(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "person" -> {
                                                    val list = json.decodeFromString<List<Person>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertPerson(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "person_attendance" -> {
                                                    val list = json.decodeFromString<List<PersonAttendance>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertPersonAttendance(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "person_fingerprint" -> {
                                                    val list = json.decodeFromString<List<PersonFingerprint>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertPersonFingerprint(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "person_contact" -> {
                                                    val list = json.decodeFromString<List<PersonContact>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertPersonContact(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "media_photo" -> {
                                                    val list = json.decodeFromString<List<MediaPhoto>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertMediaPhoto(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school" -> {
                                                    val list = json.decodeFromString<List<School>>(tableData.toString())
                                                    Log.d(TAG, "Server Schools $list")
                                                    insertCounter += list.size
                                                    syncDao.upsertSchool(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_group" -> {
                                                    val list = json.decodeFromString<List<SchoolGroup>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolGroup(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_learner_admission" -> {
                                                    val list = json.decodeFromString<List<SchoolLearnerAdmission>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolLearnerAdmission(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_learner_enrolment" -> {
                                                    val list = json.decodeFromString<List<SchoolLearnerEnrolment>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolLearnerEnrolment(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "teacher" -> {
                                                    val list = json.decodeFromString<List<Teacher>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertTeacher(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "teacher_timetable" -> {
                                                    val list = json.decodeFromString<List<TeacherTimetable>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertTeacherTimetable(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_feeding" -> {
                                                    val list = json.decodeFromString<List<SchoolFeeding>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolFeeding(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "school_feeding_stock" -> {
                                                    val list = json.decodeFromString<List<SchoolFeedingStock>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertSchoolFeedingStock(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "learner_performance" -> {
                                                    val list = json.decodeFromString<List<LearnerPerformance>>(tableData.toString())
                                                    insertCounter += list.size
                                                    syncDao.upsertLearnerPerformance(list)
                                                    Utils.insertLog(context, "⬇️  Inserted ${list.size} records into $tableName")
                                                }
                                                "table_states_uni_tables" -> {
                                                    val list = json.decodeFromString<List<TableStatesUniTables>>(tableData.toString())
                                                    syncDao.upsertTableStatesUniTables(list)
                                                }
                                                "table_states_bi_tables" -> {
                                                    val list = json.decodeFromString<List<TableStatesBiTables>>(tableData.toString())
                                                    syncDao.upsertTableStatesBiTables(list)
                                                }
                                            } } catch (e: Exception) {
                                                Log.e(TAG, "Error processing table $tableName", e)
                                                Utils.insertLog(context, "⚠️  Error processing $tableName: ${e.message}")
                                            }
                                        }

                                        //final notification update
                                        builder.setContentText("Download complete, inserted $insertCounter records")
                                            .setProgress(0, 0, false)
                                        notify(Constants.NOTIFY_SYNC_ID, builder.build())
                                    }

                                    return insertCounter
                                }
                            }

                        } else {
                            Log.d(TAG, "response failed")
                            return NETWORK_ERROR
                        }
                    }
                    is NetworkResponse.Error -> {
                        Log.d(TAG, "network error")
                        return NETWORK_ERROR
                    }
                }
            }
            return NETWORK_ERROR
        }

        @SuppressLint("MissingPermission")
        suspend fun uploadData(context: Context): Int {

            syncDao = AppDatabase.getInstance()?.syncDao()

            syncDao?.let { syncDao ->

                val recordsToUpload = getRecordCountsForUpload()

                val uploadCount: Int = recordsToUpload.values.sum()

                if (uploadCount == 0) {
                    return 0
                }

                //check credentials first
                val retrofit = RetrofitBuilder.getRetrofit(BuildConfig.SERVER_URL)
                val service = retrofit.create(SyncApi::class.java)

                val tokenRequest = TokenRequest(
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_ACCESS_TOKEN, "").toString(),
                    "token_check",
                    mutableMapOf()
                )

                when (val responseToken = service.postTokenCheck(tokenRequest)) {
                    is NetworkResponse.Success -> {
                        if (responseToken.response.isSuccessful) {
                            val tokenResponse: TokenResponse? = responseToken.body
                            val tokenResponseStatus = tokenResponse?.status ?: false
                            if (tokenResponseStatus) {
                                if (tokenResponse != null) {
                                    if (tokenResponse.data["scope_cache_id"].equals(
                                            Utils.getEncSharedPrefs(context)
                                                .getString(Constants.PREF_SCOPE_CACHE_ID, "")
                                        ).not()
                                    ) {
                                        Utils.getEncSharedPrefs(context).edit()
                                            .putString(
                                                Constants.PREF_SCOPE_CACHE_ID,
                                                tokenResponse.data["scope_cache_id"]
                                            )
                                            .putString(Constants.PREF_SCOPE_HASH, tokenResponse.data["scope_hash"])
                                            .putString(
                                                Constants.PREF_SCOPE_SCHOOLS,
                                                tokenResponse.data["scope_schools"]
                                            )
                                            .apply()
                                    }
                                } else {
                                    return TOKEN_ERROR
                                }
                            } else {
                                return TOKEN_ERROR
                            }
                        } else {
                            return NETWORK_ERROR
                        }
                    }
                    is NetworkResponse.Error -> {
                        Log.d(TAG, "network error")
                        return NETWORK_ERROR
                    }
                }

                val schoolListString =
                    Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCOPE_SCHOOLS, "").toString()
                if (checkSchoolPermission(context, schoolListString) < 0) return SCHOOL_AUTH_ERROR

                //send records
                val uploadMap = mutableMapOf<String, List<JSONObject>>()

                val recordsUploadLimit = 1000
                var counter = 0

                recordsToUpload.forEach { (tableName, syncCount) ->
                    if (syncCount > 0) {
                        //get data for query
                        val query = SimpleSQLiteQuery(
                            "SELECT * FROM ? WHERE sync_flag LIMIT $recordsUploadLimit".replace(
                                "?",
                                tableName
                            )
                        )

                        val cursor = syncDao.getRecordsToSync(query)

                        if (cursor.count > 0) {
                            uploadMap[tableName] = cursorToJsonList(cursor)
                            counter += cursor.count
                        }

                        if (counter >= recordsUploadLimit) return@forEach
                    }
                }

                //send to server
                val uploadDataRequest = UploadDataRequest(
                    access_token = Utils.getEncSharedPrefs(context).getString("access_token", "")
                        .toString(),
                    app_version = Utils.getVersionCode(),
                    app_db_version = Utils.getDbVersionCode(),
                    install_id = Utils.getEncSharedPrefs(context).getString(Constants.PREF_INSTALL_ID, "")
                        .toString(),
                    client_time = Utils.getISODateTimeUTC(),
                    request = "upload",
                    params = UploadDataParams(
                        "upload_records",
                        uploadMap
                    )
                )

                val builder =
                    NotificationCompat.Builder(context, Constants.CHANNEL_ID).apply {
                        setContentTitle("Uploading Data")
                        setContentText("Uploading in progress")
                        setSmallIcon(R.drawable.ic_wideya_logo_square_colour)
                        priority = NotificationCompat.PRIORITY_LOW
                        setOnlyAlertOnce(true)
                        setTimeoutAfter(10000)
                    }

                when (val response = service.postUploadData(uploadDataRequest)) {
                    is NetworkResponse.Success -> {
                        if (response.response.isSuccessful) {
                            Log.d(TAG, "response success")
                            val uploadResponse: UploadDataResponse? = response.body
                            val responseStatus = uploadResponse?.status ?: false
                            if (responseStatus) {
                                if (uploadResponse != null) {

                                    val PROGRESS_MAX = uploadResponse.data.size
                                    val PROGRESS_CURRENT = 0

                                    var insertCounter = 0

                                    var tableCounter = 0


                                    NotificationManagerCompat.from(context).apply {
                                        // Issue the initial notification with zero progress
                                        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false)
                                        notify(Constants.NOTIFY_SYNC_ID, builder.build())

                                        uploadResponse.data.forEach { (tableName, confirmedList) ->

                                            builder.setContentText("checking table $tableName")
                                            builder.setProgress(PROGRESS_MAX, tableCounter.plus(1), false)
                                            notify(Constants.NOTIFY_SYNC_ID, builder.build())
                                            tableCounter += 1

//                                    confirmedList.forEach {
//
//                                        val pkCn: String = it.asJsonObject.get("pkCn").asString
//                                        val id = it.asJsonObject.get("id").asString
//                                        Log.d(TAG, pkCn)
//                                        Log.d(TAG, id)
//                                        //updated the sync flag
//                                        val queryString =
//                                            "UPDATE :tableName SET sync_flag=0 WHERE :pkCn = ':id'"
//                                                .replace(":tableName", tableName)
//                                                .replace(":pkCn", pkCn)
//                                                .replace(":id", id)
//                                        Log.d(TAG, queryString)
//
//                                        val query = SimpleSQLiteQuery(queryString)
//                                        val c = syncDao.updateSyncFlag(query)
//                                        c.moveToFirst()
//                                        c.close()
//                                        insertCounter += 1
//                                    }
                                            insertCounter += syncDao.updateSyncFlagInTransaction(
                                                tableName,
                                                confirmedList
                                            )
                                        }

                                        //final notification update
                                        builder.setContentText("Upload complete, inserted $insertCounter records")
                                            .setProgress(0, 0, false)
                                        notify(Constants.NOTIFY_SYNC_ID, builder.build())

                                    }

                                    return insertCounter
                                }
                            }

                        } else {
                            Log.d(TAG, "response failed")
                            return NETWORK_ERROR
                        }
                    }
                    is NetworkResponse.Error -> {
                        Log.d(TAG, "network error")
                        return NETWORK_ERROR
                    }
                }
            }

            return NETWORK_ERROR
        }

        private fun checkSchoolPermission(context: Context, schoolListString: String): Int {
            Log.d(TAG, "Check School Permission")
            val schoolId = Utils.getEncSharedPrefs(context).getString(Constants.PREF_SCHOOL_ID, null)
            val schoolList = schoolListString.split(",").map { it.trim() }

            Log.d(TAG, "School Id: $schoolId :: School List $schoolList")
            if (!(schoolId.isNullOrEmpty() || schoolList.contains(schoolId))) {
                Log.d(TAG, "No Auth To This School")
                return NETWORK_ERROR
            }
            return 1
        }

        private fun cursorToJsonList(cursor: Cursor): List<JSONObject> {
            cursor.moveToFirst()
            val recList = mutableListOf<JSONObject>()
            while (!cursor.isAfterLast) {
                val totalColumn = cursor.columnCount
                val rowObject = JSONObject()
                for (i in 0 until totalColumn) {
                    if (cursor.getColumnName(i) != null) {
                        try {
                            rowObject.put(
                                cursor.getColumnName(i),
                                cursor.getString(i)
                            )
                        } catch (e: Exception) {
                            Log.d(TAG, e.message!!)
                        }
                    }
                }
                recList.add(rowObject)
                cursor.moveToNext()
            }
            return recList
        }

        private fun getTableStateForUniTables(syncDao: SyncDao): Map<String, TableState> {
            val tableStates = mutableMapOf<String, TableState>()
            TableState.uniTablesWithUuid.forEach {
                val queryString = SyncDao.SyncQueries.TABLE_STATE_UNI_UUID.replace(":tableName", it)
                val listOfTableInfo = syncDao.getTableState(SimpleSQLiteQuery(queryString))
                tableStates[it] = listOfTableInfo.first()
            }
            TableState.uniTablesWithId.forEach {
                val queryString = SyncDao.SyncQueries.TABLE_STATE_UNI_ID.replace(":tableName", it)
                val listOfTableInfo = syncDao.getTableState(SimpleSQLiteQuery(queryString))
                tableStates[it] = listOfTableInfo.first()
            }
            return tableStates
        }

        private fun getTableStateForUniTablesVersion3(syncDao: SyncDao): Map<String, TableState> {
            val tableStates = mutableMapOf<String, TableState>()
            // non_payroll_teachers is listed first so it syncs before the large teacher_payroll set
            TableState.uniNonPayrollTables.forEach {
                val listOfTableInfo = syncDao.getTableState(SimpleSQLiteQuery(SyncDao.SyncQueries.TABLE_STATE_NON_PAYROLL_TEACHERS))
                tableStates[it] = listOfTableInfo.first()
            }
            TableState.uniTablesWithUuid.forEach {
                val queryString = SyncDao.SyncQueries.TABLE_STATE_UNI.replace(":tableName", it)
                val listOfTableInfo = syncDao.getTableState(SimpleSQLiteQuery(queryString))
                tableStates[it] = listOfTableInfo.first()
            }
            TableState.uniTablesWithId.forEach {
                val queryString = SyncDao.SyncQueries.TABLE_STATE_UNI.replace(":tableName", it)
                val listOfTableInfo = syncDao.getTableState(SimpleSQLiteQuery(queryString))
                tableStates[it] = listOfTableInfo.first()
            }
            return tableStates
        }

        private fun getTableStateForBiTablesBySchool(
            syncDao: SyncDao,
            schoolListString: String
        ): Map<String, Map<String, TableState>> {
            val tableStatesBySchool = mutableMapOf<String, Map<String, TableState>>()
            schoolListString.split(",").forEach { schoolId ->
                val tableStates = mutableMapOf<String, TableState>()
                TableState.biConfig.forEach {
                    tableStates[it.first] = getTableState(syncDao, it.first, it.second, "'${schoolId}'")
                }
                tableStatesBySchool[schoolId] = tableStates
            }
            return tableStatesBySchool
        }

        private fun getTableStateForBiTablesBySchoolVersion3(
            syncDao: SyncDao,
            schoolListString: String
        ): Map<String, Map<String, TableState>> {
            val tableStatesBySchool = mutableMapOf<String, Map<String, TableState>>()
            schoolListString.split(",").forEach { schoolId ->
                val tableStates = mutableMapOf<String, TableState>()
                TableState.biConfig.forEach {
                    tableStates[it.first] = getTableStateVersion3(syncDao, it.first, it.second, schoolId)
                }
                tableStatesBySchool[schoolId] = tableStates
            }
            return tableStatesBySchool
        }

        private fun getTableState(
            syncDao: SyncDao,
            tableName: String,
            queryConstant: String,
            whereIn: String
        ): TableState {
            val queryString = queryConstant.replace(":whereIn", whereIn)
            val res = syncDao.getTableState(SimpleSQLiteQuery(queryString))
            return if (res.isEmpty()) {
                TableState(0, null, null)
            } else {
                res.first()
            }
        }

        private fun getTableStateVersion3(syncDao: SyncDao, tableName: String, queryConstant: String, schoolId: String): TableState {
            val queryString = queryConstant.replace(":schoolId", schoolId).replace(":tableName", tableName)
            val res = syncDao.getTableState(SimpleSQLiteQuery(queryString))
            return if (res.isEmpty()) {
                TableState(0, null, null)
            } else {
                res.first()
            }
        }

        fun getRecordCountsForUpload(): Map<String, Int> {
            syncDao = AppDatabase.getInstance()?.syncDao()
            syncDao?.let { syncDao ->
                val recordCountToSync = mutableMapOf<String, Int>()
                val tables = syncDao.getTables(SimpleSQLiteQuery(SyncDao.SyncQueries.LIST_TABLES))
                tables.forEach {
                    if (it.sql.contains("sync_flag")) {
                        //check recs
                        val queryString = SyncDao.SyncQueries.RECORD_COUNT_TO_SYNC.replace(":tableName", it.name)
                        val recs = syncDao.getRecordCountToSync(SimpleSQLiteQuery(queryString))
                        recordCountToSync[it.name] = recs
                    }
                }

                return recordCountToSync
            }
            return emptyMap()
        }

        fun getRecordCountsForPurge(): Map<String, Int> {
            syncDao = AppDatabase.getInstance()?.syncDao()
            syncDao?.let { syncDao ->
                val recordCountToSync = mutableMapOf<String, Int>()
                val tables = syncDao.getTables(SimpleSQLiteQuery(SyncDao.SyncQueries.LIST_TABLES))
                tables.forEach {
                    if (it.sql.contains("sync_flag") && it.sql.contains("deleted_at")) {
                        //check recs
                        val queryString = SyncDao.SyncQueries.RECORD_COUNT_FOR_PURGE.replace(":tableName", it.name)
                        val recs = syncDao.getRecordCountToSync(SimpleSQLiteQuery(queryString))
                        recordCountToSync[it.name] = recs
                    }
                }

                return recordCountToSync
            }
            return emptyMap()
        }

        fun purgeDeletedRecords(): Map<String, Int> {
            syncDao = AppDatabase.getInstance()?.syncDao()
            syncDao?.let { syncDao ->
                val purgedByTable = mutableMapOf<String, Int>()
                val tables = syncDao.getTables(SimpleSQLiteQuery(SyncDao.SyncQueries.LIST_TABLES))
                tables.forEach {
                    if (it.sql.contains("sync_flag") && it.sql.contains("deleted_at")) {
                        //check recs
                        val queryString = SyncDao.SyncQueries.PURGE_DELETED.replace(":tableName", it.name)
                        val recs = syncDao.purgeDeletedFromTable(SimpleSQLiteQuery(queryString))
                        purgedByTable[it.name] = recs
                    }
                }
                return purgedByTable
            }

            return emptyMap()
        }
    }
}