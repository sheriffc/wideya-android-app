package uk.org.cgatechnologies.wideya.login

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentLoginBinding
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationErrorStatus
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper


private const val TAG = "LoginFragment"

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authenticationViewModel by activityViewModels<AuthenticationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)

        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        val username = Utils.getEncSharedPrefs(requireContext())
            .getString(Constants.PREF_USERNAME, "")

        if (username.isNullOrEmpty().not()) {
            findNavController().navigate(R.id.HomeFragment)
        }

        binding.apply {
            tvAppVersionCode.text = getString(R.string.app_version_v, BuildConfig.VERSION_CODE)
            tvAppVersionName.text = BuildConfig.VERSION_NAME

            if (BuildConfig.BUILD_TYPE == "demo") {
                binding.tvAppVariantTitle.text = "DEMO"
                binding.tvAppVariantTitle.visibility = View.VISIBLE
            }

            tvRegisterLink.isVisible = BuildConfig.BUILD_TYPE != "demo"
            tvRegisterLink.setOnClickListener {
                try {
                    val myIntent =
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SERVER_URL + "/register"))
                    startActivity(Intent.createChooser(myIntent, "Choose Browser"));
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        if (!Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.PREF_LAST_USERNAME, "")
                .isNullOrEmpty()
        ) {
            binding.etUsername.setText(
                Utils.getEncSharedPrefs(requireContext())
                    .getString(Constants.PREF_LAST_USERNAME, "")
            )
        }

        initStateFlowListeners()
        initClickListeners()

        //checkin and prompt for update if necessary
        Log.d(TAG, "onViewCreated")
        WorkerHelper.initUniqueOneTimeCheckAppUpdate(requireContext())
        setCheckUpdateObserver()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = ""
    }

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authenticationViewModel.registrationState.collect { registrationStatus ->
                    registrationStatus?.let { status ->
                        binding.progressBar.isVisible = false
                        when (status) {
                            is LatestRegistrationUiState.Success -> {
                                Log.d(TAG, "From Login To Home")
                                findNavController().navigate(R.id.HomeFragment)
                            }
                            is LatestRegistrationUiState.Fail -> {
                                when (status.authenticationStatus) {
                                    AuthenticationErrorStatus.ACCESS_DENIED -> {
                                        Utils.infoDialog(
                                            requireContext(),
                                            "Access Denied",
                                            status.message
                                        )
                                    }
                                    AuthenticationErrorStatus.NETWORK_ERROR -> {
                                        Utils.infoDialog(
                                            requireContext(),
                                            "Network Error",
                                            status.message
                                        )
                                    }
                                    AuthenticationErrorStatus.UNKNOWN_ERROR -> {
                                        Utils.infoDialog(
                                            requireContext(),
                                            "Unknown Error",
                                            status.message
                                        )
                                    }
                                }
                            }
                            is LatestRegistrationUiState.Error -> {
                                Utils.infoDialog(
                                    requireContext(),
                                    "Network Error",
                                    "Please, try again"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.tvResetPassword.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_ResetRequestsListFragment)
        }

        binding.ivViewPassword.setOnClickListener {
            binding.etPassword.apply {
                val etInputType = inputType
                val subType = etInputType and InputType.TYPE_MASK_VARIATION
                inputType = if (subType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                setSelection(binding.etPassword.length())
            }
        }

        binding.btnSignIn.setOnClickListener {

            if (preValidateUsername().not() or preValidatePassword().not()) {
                Log.d(TAG, "exit")
                return@setOnClickListener
            }

            if (Utils.checkForInternet(requireContext()).not()) return@setOnClickListener

            binding.progressBar.isVisible = true
            authenticationViewModel.postRegistration(
                binding.etUsername.text.toString(),
                binding.etPassword.text.toString()
            )
        }
    }

    private fun preValidateUsername(): Boolean {
        val input: String = binding.etUsername.text.toString()
        binding.tlUsername.apply {
            return if (input.isEmpty()) {
                error = "Field can't be empty"
                false
            } else if (input.length > 55) {
                error = "Email/Username too long"
                false
            } else {
                error = null
                true
            }
        }
    }

    private fun preValidatePassword(): Boolean {
        val input: String = binding.etPassword.text.toString()
        binding.tlPassword.apply {
            return if (input.isEmpty()) {
                error = "Field can't be empty"
                false
            } else if (input.length > 40) {
                error = "Password too long"
                false
            } else {
                error = null
                true
            }
        }
    }

    private fun setCheckUpdateObserver() {
        Log.d(TAG, "Set Check Update Observer")
        checkToPromptUpdate()

        WorkManager.getInstance(requireContext())
            .getWorkInfosForUniqueWorkLiveData(Constants.WORKER_CHECK_APP_UPDATE_ONETIME)
            .observe(
                viewLifecycleOwner
            ) { workInfos ->
                if (workInfos != null) {
                    for (workInfo in workInfos) {
                        if (workInfo != null) {
                            binding.apply {
                                when (workInfo.state) {
                                    WorkInfo.State.RUNNING -> {
                                        pbUpdate.visibility = View.VISIBLE
                                        tvUpdate.visibility = View.VISIBLE
                                    }
                                    WorkInfo.State.FAILED,
                                    WorkInfo.State.BLOCKED,
                                    WorkInfo.State.CANCELLED,
                                    WorkInfo.State.ENQUEUED,
                                    WorkInfo.State.SUCCEEDED -> {
                                        //check for update
                                        Log.d(TAG, "Worker Finished")
                                        pbUpdate.visibility = View.INVISIBLE
                                        tvUpdate.visibility = View.INVISIBLE
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
        Log.d(TAG, "Checking For New Version")
        if (Utils.isNewVersionPresent(requireContext())) {
            Utils.showDialogUpdateAvailable(requireContext(), childFragmentManager)
        } else {
            Utils.updateDialogCheckAndClear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}