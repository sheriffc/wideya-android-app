package uk.org.cgatechnologies.wideya.sync_device

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.utils.Extensions.runOnUiThread
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentSyncBinding
import uk.org.cgatechnologies.wideya.sync_device.adapters.LogSyncListAdapter
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper

private const val TAG: String = "syncfragment"
class SyncFragment : Fragment() {

    private var _binding: FragmentSyncBinding? = null

    private val binding get() = _binding!!

        private val syncViewModel by activityViewModels<SyncViewModel>()

    private lateinit var logSyncListAdapter: LogSyncListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSyncBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)

        refreshCounters()

        initObservers()

        setBgSyncDataObserver()

        logSyncListAdapter = LogSyncListAdapter()
        val recyclerView: RecyclerView = binding.rvSyncLog

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = logSyncListAdapter
        }

        binding.apply {
            switchBgSync.apply {
                isChecked = Utils.getEncSharedPrefs(requireContext()).getBoolean(
                    Constants.PREF_BACKGROUND_SYNC_BOOL, true
                )
                toggleOnDemandButtons(!isChecked)

                setOnCheckedChangeListener { _, isChecked ->
                    Utils.getEncSharedPrefs(requireContext()).edit()
                        .putBoolean(Constants.PREF_BACKGROUND_SYNC_BOOL, isChecked)
                        .apply()
                    WorkerHelper.initBackgroundSync(requireContext())
                    toggleOnDemandButtons(!isChecked)
                }
            }

            fabScrollToTop.setOnClickListener {
                recyclerView.scrollToPosition(0)
            }

            btnSync.setOnClickListener {

                //check if s3 credentials exist
                if (Utils.getEncSharedPrefs(requireContext())
                        .getString(Constants.PREF_S3_KEY, "").isNullOrEmpty()
                ) {
                    WorkerHelper.initCheckinThenUniqueSyncWorkRequest(requireContext())
                } else {
                    WorkerHelper.initUniqueSyncWorkRequest(requireContext())
                }
                setSyncDataObserver()
            }
        }
    }

    fun refreshCounters(){
        Log.d("syncfrag", "refreshing counters")
        lifecycleScope.launch(Dispatchers.IO) {
            syncViewModel.setSyncFlagCount()
            runOnUiThread {
                binding.tvSyncStateRecordsCount.text = syncViewModel.getSyncFlagCount().toString()
            }

            syncViewModel.setFilesCount(requireContext())
            runOnUiThread {
                binding.tvSyncStateFilesCount.text = syncViewModel.getFilesCount().toString()
            }
        }

    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                syncViewModel.logSyncList.collect { logSyncList ->
                    when (logSyncList) {
                        is LatestLogSyncListUiState.Success -> {
                            logSyncListAdapter.setItems(logSyncList.logSyncList)
                        }
                        is LatestLogSyncListUiState.Error -> {
                            val mySnackBar = Snackbar.make(binding.root, logSyncList.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun setSyncDataObserver() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_SYNC)
            .observe(viewLifecycleOwner, Observer {
                workInfoList: List<WorkInfo> ->
                val isBgSyncEnabled: Boolean =
                    Utils.getEncSharedPrefs(requireContext()).getBoolean(
                        Constants.PREF_BACKGROUND_SYNC_BOOL, true
                    )
                workInfoList.forEach {
                    binding.apply {
                        when (it.state) {
                            WorkInfo.State.RUNNING -> {
                                Log.d(TAG, "running state" )
                                pbSync.visibility = View.VISIBLE
                                btnSync.textSize = 0f
                                btnSync.isEnabled = false
                            }
                            WorkInfo.State.BLOCKED,
                            WorkInfo.State.ENQUEUED,
                            WorkInfo.State.FAILED,
                            WorkInfo.State.CANCELLED,
                            WorkInfo.State.SUCCEEDED -> {
                                Log.d(TAG, "succeeded state ${it.tags}" )
                                rvSyncLog.scrollToPosition(0)
                            }
                        }
                        //check for the last worker in the chain
                        if( it.state.isFinished.and(it.tags.contains(Constants.WORKER_DOWNLOAD_SYNC_TAG) )){
                                pbSync.visibility = View.INVISIBLE
                                btnSync.textSize = 14f
                                if (!isBgSyncEnabled) btnSync.isEnabled = true
                            refreshCounters()
                        }
                    }
                }
            })
    }

    private fun setBgSyncDataObserver() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosByTagLiveData(Constants.WORKER_SYNC_GROUP_TAG)
            .observe(viewLifecycleOwner, Observer {
                workInfoList: List<WorkInfo> ->
                    workInfoList.forEach {
                        binding.apply {
                            //check for the last worker in the chain
                            if( it.state.isFinished ){
                                refreshCounters()
                            }
                        }
                    }
            })
    }

    private fun toggleOnDemandButtons(isEnabled: Boolean) {
        binding.apply {
            if (isEnabled) {
                if (pbSync.visibility == View.INVISIBLE) btnSync.isEnabled = true
            } else {
                if (pbSync.visibility == View.INVISIBLE) btnSync.isEnabled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}