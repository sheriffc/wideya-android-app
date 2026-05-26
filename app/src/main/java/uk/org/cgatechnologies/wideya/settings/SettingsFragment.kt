package uk.org.cgatechnologies.wideya.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentSettingsBinding
import uk.org.cgatechnologies.wideya.sync_device.SyncData
import uk.org.cgatechnologies.wideya.sync_device.SyncFiles
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper

private const val TAG = "settings"

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var isUpdateHandled = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)

        setupWorkerObservers()
        initHiddenFields()

        binding.apply {
            btnClearLogs.setOnClickListener {
                clearSyncLogs()
            }

            btnExportDatabase.setOnClickListener {
                if (Utils.checkForInternet(requireContext()).not()) return@setOnClickListener
                confirmSendDatabaseBackup()
            }

            btnDbPurge.setOnClickListener {
                confirmPurgeDatabase()
            }

            btnDbReset.setOnClickListener {
                confirmResetDatabase()
            }
        }
        refreshAppUpdateState()
    }

    private fun initHiddenFields() {
        if (BuildConfig.BUILD_TYPE.equals("dev", true) ||
            BuildConfig.BUILD_TYPE.equals("load", true) ||
            BuildConfig.BUILD_TYPE.equals("local", true) ||
            BuildConfig.BUILD_TYPE.equals("debug", true) ||
            BuildConfig.BUILD_TYPE.equals("staging", true)
        ) {
            binding.apply {
                cvLocation.visibility = View.VISIBLE
                btnLocation.setOnClickListener {
                    showLocationDialog()
                }
            }
        }
    }

    private fun showLocationDialog() {
        val lat = Utils.getEncSharedPrefs(requireContext())
            .getString(Constants.PREF_LOCATION_LAT, "Unknown")
        val lng = Utils.getEncSharedPrefs(requireContext())
            .getString(Constants.PREF_LOCATION_LNG, "Unknown")
        val time = Utils.getEncSharedPrefs(requireContext())
            .getString(Constants.PREF_LOCATION_DATETIME_ACQUIRED, "Unknown")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.location)
        builder.setMessage(getString(R.string.saved_location, lat, lng, time))
        builder.setPositiveButton(R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun setupWorkerObservers() {
        setCheckUpdateObserver()
    }

    private fun setDatabaseExportObserver() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_UPLOAD_DB_ONETIME)
            .observe(
                viewLifecycleOwner
            ) { workInfos ->
                if (workInfos != null) {
                    for (workInfo in workInfos) {
                        if (workInfo != null) {
                            binding.apply {
                                when (workInfo.state) {
                                    WorkInfo.State.RUNNING -> {
                                        pbExportDatabase.visibility = View.VISIBLE
                                        btnExportDatabase.textSize = 0f
                                        btnExportDatabase.isEnabled = false
                                    }
                                    WorkInfo.State.FAILED -> {
                                        pbExportDatabase.visibility = View.INVISIBLE
                                        btnExportDatabase.textSize = 14f
                                        btnExportDatabase.isEnabled = true
                                        Utils.infoDialog(
                                            requireContext(), getString(
                                                R.string
                                                    .dialog_send_backup_title
                                            ), getString(
                                                R.string
                                                    .dialog_send_backup_message_error
                                            )
                                        )
                                    }
                                    WorkInfo.State.SUCCEEDED -> {
                                        pbExportDatabase.visibility = View.INVISIBLE
                                        btnExportDatabase.textSize = 14f
                                        btnExportDatabase.isEnabled = true
                                        Utils.infoDialog(
                                            requireContext(), getString(
                                                R.string
                                                    .dialog_send_backup_title
                                            ), getString(
                                                R.string
                                                    .dialog_send_backup_message_success
                                            )
                                        )
                                    }
                                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED, WorkInfo.State.CANCELLED -> {}
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun setCheckUpdateObserver() {
        refreshAppUpdateState()

        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_CHECK_IN_ONETIME)
            .observe(viewLifecycleOwner) { workInfos ->
                if (!isUpdateHandled && workInfos != null) {
                    for (workInfo in workInfos) {
                        if (workInfo != null) {
                            binding.apply {
                                when (workInfo.state) {
                                    WorkInfo.State.ENQUEUED->{}
                                    WorkInfo.State.RUNNING -> {
                                        pbUpdate.visibility = View.VISIBLE
                                        btnUpdate.textSize = 0f
                                        btnUpdate.isEnabled = false
                                    }
                                    WorkInfo.State.FAILED -> {
                                        isUpdateHandled = true
                                        pbUpdate.visibility = View.INVISIBLE
                                        btnUpdate.textSize = 14f
                                        btnUpdate.isEnabled = true

                                        Utils.infoDialog(
                                            requireContext(), getString(
                                                R.string.update_error_title
                                            ),
                                            getString(R.string.update_error_message)
                                        )
                                    }
                                    WorkInfo.State.SUCCEEDED -> {
                                        isUpdateHandled = true
                                        pbUpdate.visibility = View.INVISIBLE
                                        btnUpdate.textSize = 14f
                                        refreshAppUpdateState(workInfo.outputData)
                                    }
                                   WorkInfo.State.BLOCKED, WorkInfo.State.CANCELLED -> {}
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun refreshAppUpdateState(outputData: Data? = null) {
        val newVersionCode =
            Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.APP_NEW_VERSION_CODE, "0")
                ?.toIntOrNull() ?: 0
        val lastChecked = if (
            Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.APP_UPDATE_LAST_CHECKED, "")
                .equals("")
        ) {
            "Never checked"
        } else {
            Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.APP_UPDATE_LAST_CHECKED, "")
        }

        binding.apply {
            if (newVersionCode > BuildConfig.VERSION_CODE) {
                //enable updates
                tvUpdateDesc.text =
                    getString(R.string.new_update_description, newVersionCode).trimIndent()
                btnUpdate.apply {
                    isEnabled = true
                    text = getString(R.string.update)
                    setOnClickListener {
                        isUpdateHandled = false
                        WorkerHelper.initUniqueOneTimeCheckIn(requireContext())
                        Utils.showDialogUpdateAvailable(requireContext(), childFragmentManager)
                    }
                }

            } else {
                Utils.updateDialogCheckAndClear()

                if (outputData?.getString("app_version") != null) {
                    Utils.infoDialog(
                        requireContext(),
                        getString(R.string.no_updates_title),
                        getString(R.string.no_updates_message)
                    )
                }

                tvUpdateDesc.text =
                    getString(R.string.check_update_description, lastChecked).trimIndent()
                btnUpdate.apply {
                    isEnabled = true
                    text = getString(R.string.check)
                    setOnClickListener {
                        isUpdateHandled = false
                        WorkerHelper.initUniqueOneTimeCheckIn(requireContext())
                    }
                }
            }
        }
    }

    private fun clearSyncLogs() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle(R.string.clear_logs)
            .setMessage(R.string.sync_logs_warning)

        builder.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            lifecycleScope.launch(Dispatchers.Default) {
                try {
                    AppDatabase.getInstance()?.syncDao()?.truncateLogTable()
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }

        builder.setNegativeButton(
            R.string.cancel, null
        )

        val dialog = builder.create()
        dialog.show()
    }

    private fun confirmSendDatabaseBackup() {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle(R.string.dialog_send_backup_title)
            .setMessage(R.string.dialog_send_backup_message_info)


        builder.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            //check if s3 credentials exist
            if (Utils.getEncSharedPrefs(requireContext())
                    .getString(Constants.PREF_S3_KEY, "")
                    .isNullOrEmpty()
            ) {
                //do check in first
                WorkerHelper.initUniqueOneTimeCheckInAndUploadDb(requireContext())
                setDatabaseExportObserver()
            } else {
                WorkerHelper.initUniqueOneTimeUploadDb(requireContext())
                setDatabaseExportObserver()
            }
        }
        builder.setNegativeButton(R.string.cancel, null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun confirmPurgeDatabase() {
        val builder = AlertDialog.Builder(requireContext())

        //check number of records
//        val syncData = SyncData()
        val recordsToPurge = SyncData.getRecordCountsForPurge()
        var purgeCounter = 0
        recordsToPurge.forEach { (_, purgeCount) ->
            purgeCounter += purgeCount
        }

        builder.setTitle(R.string.db_purge_dialog_title)
            .setMessage(getString(R.string.db_purge_dialog_info, purgeCounter))

        builder.setPositiveButton(
            R.string.ok
        ) { _, _ ->
            SyncData.purgeDeletedRecords()
        }

        builder.setNegativeButton(
            R.string.cancel, null
        )

        val dialog = builder.create()
        dialog.show()
    }

    private fun confirmResetDatabase() {
        val builder = AlertDialog.Builder(requireContext())

        //check number of records
        var uploadRecordsCounter = 0
        var uploadFilesCounter = 0

//        val syncData = SyncData()
        val recordsToUpload = SyncData.getRecordCountsForUpload()

        Log.d(TAG, recordsToUpload.toString())

        recordsToUpload.forEach { (_, uploadCount) ->
            uploadRecordsCounter += uploadCount
        }

        val paths = listOf(
            Constants.PATH_FINGERPRINTS,
            Constants.PATH_TEACHER_PROFILE_PHOTOS,
            Constants.PATH_TEACHER_ATT_PHOTOS
        )

        paths.forEach {
            uploadFilesCounter += SyncFiles.countFilesInFolder(requireContext(), it)
        }

        if (uploadRecordsCounter > 0 || uploadFilesCounter > 0) {

            Utils.infoDialog(
                requireContext(), getString(R.string.db_reset_title), getString(
                    R.string
                        .db_reset_dialog_info_error, uploadRecordsCounter, uploadFilesCounter
                )
            )

        } else {
            builder.setTitle(R.string.db_reset_title)
                .setMessage(R.string.db_reset_dialog_info_confirm)

            builder.setPositiveButton(
                R.string.ok
            ) { _, _ ->
                AppDatabase.getInstance()?.clearAllTables()
                Utils.logout(requireContext())
                findNavController().navigate(R.id.LoginFragment)
            }

            builder.setNegativeButton(
                R.string.cancel, null
            )

            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}