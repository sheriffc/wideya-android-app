package uk.org.cgatechnologies.wideya.common.dialogfragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.DialogFragmentUpdateBinding

private const val TAG: String = "AppUpdateDialog"

class AppUpdateDialog : DialogFragment() {
    private var _binding: DialogFragmentUpdateBinding? = null
    private val binding get() = _binding!!

    init {
        INSTANCE = this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        _binding = DialogFragmentUpdateBinding.inflate(layoutInflater)

        var description = String()
        var thisVersionCode = 0
        var newVersionCode = 0
        var forceUpdate = 0
        var uri = String()

        if (arguments != null) {
            val mArgs = arguments
            description = mArgs?.getString(Constants.APP_DESCRIPTION) ?: ""
            thisVersionCode = mArgs?.getInt(Constants.APP_THIS_VERSION_CODE) ?: 0
            newVersionCode = mArgs?.getInt(Constants.APP_NEW_VERSION_CODE) ?: 0
            forceUpdate = mArgs?.getInt(Constants.APP_FORCE_UPDATE) ?: 0
            uri = mArgs?.getString(Constants.APP_URI) ?: ""
        }

        binding.apply {
            tvYourVersion.text = getString(R.string.your_version, thisVersionCode)
            tvNewVersion.text = getString(R.string.new_version, newVersionCode)
            tvDescription.text = description

            if (description.isEmpty()) {
                tvDescriptionHeader.visibility = View.GONE
                svContent.visibility = View.GONE
            }

            btnPositive.setOnClickListener {
                //go to URI
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

                activity?.startActivity(browserIntent)
            }

            btnNegative.apply {
                if (forceUpdate == 0) {
                    setOnClickListener {
                        Utils.getEncSharedPrefs(requireContext()).edit()
                            .putBoolean(Constants.PREF_APP_UPDATE_NAG_BOOL, false).apply()
                        dismiss()
                    }
                } else {
                    visibility = View.INVISIBLE
//                    text = getString(R.string.mandatory_update)
//                    isEnabled = false
                }
            }

            if (forceUpdate == 1) {
                tvForceUpdateExplanation.visibility = View.VISIBLE
            }
        }

        val builder = AlertDialog.Builder(requireContext())
        return builder.setView(binding.root).create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        Log.d(TAG, "Dismissing Older Dialog")
        INSTANCE = null
        super.onDismiss(dialog)
    }

    companion object {
        private const val TAG = "UpdateDialog"

        private var INSTANCE: AppUpdateDialog? = null
        fun checkAndClear(): Boolean =
            if (INSTANCE != null) {
                Log.d(TAG, "Older Dialog Exists")
                INSTANCE!!.dismiss()
                INSTANCE = null
                true
            } else {
                Log.d(TAG, "No Older Dialogs")
                false
            }
    }
}