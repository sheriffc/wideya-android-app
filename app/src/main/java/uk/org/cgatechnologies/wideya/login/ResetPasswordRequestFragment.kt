package uk.org.cgatechnologies.wideya.login

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
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
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentResetPasswordRequestBinding
import uk.org.cgatechnologies.wideya.login.models.common.AuthenticationErrorStatus

/**
 * Created by Mohamad Abuzaid on 02/16/2023.
 */

private const val TAG = "ResetPasswordRequestFragment"

class ResetPasswordRequestFragment : Fragment() {

    private var _binding: FragmentResetPasswordRequestBinding? = null
    private val binding get() = _binding!!

    private val authenticationViewModel by activityViewModels<AuthenticationViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordRequestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        initStateFlowListeners()

        if (!Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.PREF_LAST_USERNAME, "")
                .isNullOrEmpty()
        ) {
            binding.etUsername.setText(
                Utils.getEncSharedPrefs(requireContext())
                    .getString(Constants.PREF_LAST_USERNAME, "")
            )
        }

        initClickListeners()
    }

    private fun initStateFlowListeners() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authenticationViewModel.resetPasswordState.collect { resetStatus ->
                    resetStatus?.let { status ->
                        binding.progressBar.isVisible = false
                        when (status) {
                            is LatestResetPasswordUiState.Success -> {
                                successDialog()
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

    private fun initClickListeners() {
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

        binding.ivViewConfPassword.setOnClickListener {
            binding.etConfPassword.apply {
                val etInputType = inputType
                val subType = etInputType and InputType.TYPE_MASK_VARIATION
                inputType = if (subType == InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                setSelection(binding.etConfPassword.length())
            }
        }

        binding.btnRequestReset.setOnClickListener {

            if (preValidateUsername().not() or preValidatePassword().not()) {
                Log.d(TAG, "Bad username/password")
                return@setOnClickListener
            }

            if (Utils.isInternetConnected(requireContext()).not()) {
                infoDialog("Error", "Not connected to network")
                return@setOnClickListener
            }

            binding.progressBar.isVisible = true

            val installId = Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.PREF_INSTALL_ID, null)

            installId?.let {
                authenticationViewModel.postResetPassword(
                    binding.etUsername.text.toString(),
                    binding.etPassword.text.toString()
                )
            } ?: run {
                Utils.generateInstallId(requireContext())

                Toast.makeText(
                    requireContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
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

    private fun successDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setTitle(R.string.request_success_dialog_title)
            .setMessage(R.string.request_success_dialog_message)
            .setPositiveButton(R.string.ok) { _, _ ->
                findNavController().popBackStack()
            }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
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
        val password: String = binding.etPassword.text.toString()
        val confPassword: String = binding.etConfPassword.text.toString()
        binding.tlPassword.apply {
            return if (password.isEmpty()) {
                error = "Field can't be empty"
                false
            } else if (password.length < 8) {
                error = "Password must be at least 8 characters long"
                false
            } else if (password.length > 40) {
                error = "Password too long"
                false
            } else if (Regex("(?=.*\\d)(?=.*[a-z]|[A-Z])^[^ ]+$").matches(password).not()) {
                error = "Password must contain at least one letter, one number and no white spaces"
                false
            } else if (password != confPassword) {
                error = "Password and its confirmation don't match"
                false
            } else {
                error = null
                true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}