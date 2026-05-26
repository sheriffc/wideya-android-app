package uk.org.cgatechnologies.wideya.home_screen

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentHomeBinding
import uk.org.cgatechnologies.wideya.sync_device.InitialSyncDialogFragment
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper

private const val TAG = "HomeFragment"

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var initialSyncDialogFragment: DialogFragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val username = Utils.getEncSharedPrefs(requireContext()).getString(Constants.PREF_USERNAME, "")

        if (username.isNullOrEmpty()) {
            Log.d(TAG, "From Home To Login: No UserName")
            findNavController().navigate(R.id.LoginFragment)
        } else {
            binding.tvUserDetails.text = getString(R.string.logged_in_as, username)
        }

        binding.apply {
            tvAppVersionCode.text = getString(R.string.app_version_v, BuildConfig.VERSION_CODE)
            tvAppVersionName.text = BuildConfig.VERSION_NAME

            if (BuildConfig.BUILD_TYPE == "demo") {
                binding.tvAppVariantTitle.text = "DEMO"
                binding.tvAppVariantTitle.visibility = View.VISIBLE
            }
        }

        if (Utils.checkIfLoggedIn(requireContext())) {

            //prevents work until a full initial sync is done
            if (Utils.getEncSharedPrefs(requireContext())
                    .getBoolean(Constants.PREF_INITIAL_LOAD_REQUIRED_BOOL, false)
            ) {
                val bundle = Bundle()
                bundle.putString("text", "Please wait for initial data sync to complete")

                initialSyncDialogFragment = InitialSyncDialogFragment()
                initialSyncDialogFragment.arguments = bundle
                initialSyncDialogFragment.show(childFragmentManager, InitialSyncDialogFragment.TAG)

                WorkerHelper.initUniqueOneTimeCheckIn(requireContext())
                setCheckOneTimeObserver()

                WorkerHelper.initInitialDownloadWorker(requireContext())
                setInitialDownloadObserver()

            } else {
                WorkerHelper.initBackgroundSyncOnStart(requireContext())

                if (Utils.getEncSharedPrefs(requireContext())
                        .getBoolean(Constants.PREF_APP_UPDATE_NAG_BOOL, true)
                ) {
                    WorkerHelper.initUniqueOneTimeCheckIn(requireContext())
                    setCheckOneTimeObserver()
                }
            }

            WorkerHelper.enqueuePeriodicCheckIn(requireContext())
        }

        binding.btnSchools.setOnClickListener {
            enterSchool()
        }
    }

    private fun setCheckOneTimeObserver() {
        checkToPromptUpdate()

        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_CHECK_IN_ONETIME)
            .observe(viewLifecycleOwner) { workInfos ->
                if (workInfos != null) {
                    for (workInfo in workInfos) {
                        if (workInfo != null) {
                            Log.d(TAG, "With Unique Name")
                            binding.apply {
                                when (workInfo.state) {
                                    WorkInfo.State.RUNNING -> pbUpdate.visibility = View.VISIBLE
                                    WorkInfo.State.FAILED,
                                    WorkInfo.State.BLOCKED,
                                    WorkInfo.State.CANCELLED,
                                    WorkInfo.State.ENQUEUED,
                                    WorkInfo.State.SUCCEEDED -> {
                                        pbUpdate.visibility = View.GONE
                                        checkToPromptUpdate()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun checkToPromptUpdate() {
        if (Utils.isNewVersionPresent(requireContext())) {
            Utils.showDialogUpdateAvailable(requireContext(), childFragmentManager)
        } else {
            Utils.updateDialogCheckAndClear()
        }
    }

    private fun setInitialDownloadObserver() {
        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_INITIAL_DOWNLOAD_SYNC)
            .observe(viewLifecycleOwner) {
                it?.forEach { work ->
                    if (work != null) {
                        when (work.state) {
                            WorkInfo.State.RUNNING -> Log.d(TAG, "running")
                            WorkInfo.State.ENQUEUED -> Log.d(TAG, "enqueued")
                            WorkInfo.State.SUCCEEDED -> {
                                Log.d(TAG, "success and dismiss")
                                initialSyncDialogFragment.dismiss()
                                WorkerHelper.initBackgroundSyncOnStart(requireContext())
                                enterSchool()
                            }
                            else -> Log.d(TAG, "failed")
                        }
                    }
                }
            }
    }

    private fun enterSchool() {
        Log.d(TAG, "Check School Permission")
        val schoolUuid = Utils.getEncSharedPrefs(requireContext()).getString(Constants.PREF_SCHOOL_ID, null)
        val schoolListString =
            Utils.getEncSharedPrefs(requireContext()).getString(Constants.PREF_SCOPE_SCHOOLS, "").toString()
        val schoolList = schoolListString.split(",").map { it.trim() }

        if (schoolUuid.isNullOrEmpty() || schoolList.contains(schoolUuid).not()) {
            findNavController().navigate(R.id.action_HomeFragment_to_SchoolListFragment)
        } else {
            findNavController().navigate(R.id.action_HomeFragment_to_SchoolProfileFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}