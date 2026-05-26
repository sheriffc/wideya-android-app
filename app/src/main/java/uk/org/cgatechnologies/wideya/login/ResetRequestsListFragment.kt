package uk.org.cgatechnologies.wideya.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentResetRequestsListBinding
import uk.org.cgatechnologies.wideya.login.adapters.ResetRequestsAdapter
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationErrorStatus
import uk.org.cgatechnologies.wideya.login.models.resetpassword.ResetPasswordResponseData

/**
 * Created by Mohamad Abuzaid on 02/21/2023.
 */

class ResetRequestsListFragment : Fragment(), ResetRequestsAdapter.IRequestsListener {

    private var _binding: FragmentResetRequestsListBinding? = null
    private val binding get() = _binding!!

    private val authenticationViewModel by activityViewModels<AuthenticationViewModel>()

    private lateinit var requestsAdapter: ResetRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetRequestsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)

        initStateFlowListeners()

        requestsAdapter = ResetRequestsAdapter(this)
        binding.rvRequests.adapter = requestsAdapter

        binding.btAddRequest.setOnClickListener {
            findNavController().navigate(R.id.action_ResetRequestsListFragment_to_ResetPasswordRequestFragment)
        }

        fetchRequestsStatuses()
    }

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authenticationViewModel.resetRequestsStatusState.collect { requestStatus ->
                    requestStatus?.let { status ->
                        binding.progressBar.isVisible = false
                        when (status) {
                            is LatestResetPasswordUiState.Success -> {
                                status.requests?.let {
                                    requestsAdapter.updateRequestsList(it)
                                    updateCountLabel(it.size)
                                }
                            }
                            is LatestResetPasswordUiState.Fail -> {
                                updateCountLabel(0)
                                when (status.authenticationStatus) {
                                    AuthenticationErrorStatus.ACCESS_DENIED -> {
                                        infoDialog("Access Denied", status.message)
                                    }
                                    AuthenticationErrorStatus.NETWORK_ERROR -> {
                                        infoDialog("Network Error", status.message)
                                    }
                                    AuthenticationErrorStatus.UNKNOWN_ERROR -> {
                                        infoDialog("Unknown Error", status.message)
                                    }
                                }
                            }
                            is LatestResetPasswordUiState.Error -> {
                                updateCountLabel(0)
                                infoDialog("Network Error", "Please, try again")
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authenticationViewModel.cancelResetPasswordState.collect { cancelStatus ->
                    cancelStatus?.let { status ->
                        binding.progressBar.isVisible = false
                        when (status) {
                            is LatestResetPasswordUiState.Success -> {
                                status.requests?.let {
                                    requestsAdapter.updateRequestsList(it)
                                    binding.tvCount.text = it.size.toString()
                                }
                            }
                            is LatestResetPasswordUiState.Fail -> {
                                when (status.authenticationStatus) {
                                    AuthenticationErrorStatus.ACCESS_DENIED -> {
                                        infoDialog("Access Denied", status.message)
                                    }
                                    AuthenticationErrorStatus.NETWORK_ERROR -> {
                                        infoDialog("Network Error", status.message)
                                    }
                                    AuthenticationErrorStatus.UNKNOWN_ERROR -> {
                                        infoDialog("Unknown Error", status.message)
                                    }
                                }
                            }
                            is LatestResetPasswordUiState.Error -> {
                                infoDialog("Network Error", "Please, try again")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCountLabel(size: Int) {
        if (size > 0) {
            binding.countLabel.isVisible = true
//            binding.tvCount.isVisible = true
//            binding.tvCount.text = size.toString()
        } else {
            binding.countLabel.isVisible = false
//            binding.tvCount.isVisible = false
        }
    }

    private fun fetchRequestsStatuses() {
        binding.progressBar.isVisible = true
        authenticationViewModel.postResetRequestsStatus()
    }

    override fun onRequestCancelled(request: ResetPasswordResponseData) {
        binding.progressBar.isVisible = true
        confirmAction(R.string.request_cancel_confirm_message) {
            authenticationViewModel.postCancelResetRequest(request.username, request.id)
        }
    }

    private fun infoDialog(title: String = "", message: String = "") {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dismiss, null)

        val dialog = builder.create()
        dialog.show()
    }

    private fun confirmAction(@StringRes message: Int, confirmedAction: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())

        builder.setMessage(message)
            .setTitle(R.string.confirm)

        builder.setPositiveButton(R.string.confirm) { _, _ ->
            confirmedAction()
        }
        builder.setNegativeButton(R.string.cancel, null)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}