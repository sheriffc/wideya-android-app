package uk.org.cgatechnologies.wideya.common.camera

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import cn.liyuyu.akpermission.PermissionRationale
import cn.liyuyu.akpermission.callWithPermissions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.common.utils.Extensions.nextOrientationAngle
import uk.org.cgatechnologies.wideya.common.utils.Extensions.rotateBitmap
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.viewmodels.IViewModelFactory
import uk.org.cgatechnologies.wideya.databinding.DialogFragmentPhotoUtilsBinding

/**
 * Created by Mohamad Abuzaid on 02/13/2023.
 */

class PhotoUtilsDialogFragment(private val listener: IPhotoUtilsListener?) : DialogFragment() {
    private var _binding: DialogFragmentPhotoUtilsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IViewModel.ICameraViewModel
    private var title = ""
    private var imageType = PhotoModel.PROFILE_PHOTO

    private var isCreated = true

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFragmentPhotoUtilsBinding.inflate(layoutInflater)

        arguments?.let {
            title = it.getString(TITLE, "")
            imageType = it.getInt(IMAGE_TYPE)
            viewModel = IViewModelFactory.getCameraViewModel(this, imageType)
        } ?: dismiss()

        initClickListeners()

        val builder = MaterialAlertDialogBuilder(requireContext())
        return builder.setView(binding.root).create()
    }

    override fun onResume() {
        super.onResume()

        if (isCreated && imageType == PhotoModel.BIOMETRIC_PHOTO) {
            isCreated = false
            startCameraView()

        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                delay(700)
                updatePhotoThumbnailPreview()
                updatePictureButtons()
            }
        }
    }

    private fun updatePhotoThumbnailPreview() {
        _binding?.let {
            if (viewModel.currentPortraitThumbnail != null) {
                binding.ivImgPortrait.setImageBitmap(viewModel.currentPortraitThumbnail)
            } else {
                loadSavedThumbnailImage()
            }
        }
    }

    private fun loadSavedThumbnailImage() {
        if (viewModel.getPortraitBinaries() != null) {
            viewModel.apply {
                currentPortraitThumbnail =
                    Utils.decodeBase64StringToBitmap(viewModel.getPortraitBinaries()!!)
                binding.ivImgPortrait.setImageBitmap(currentPortraitThumbnail)
            }
        } else {
            binding.ivImgPortrait.setImageResource(R.drawable.ic_person)
        }
    }

    private fun initClickListeners() {
        with(binding) {
            btnImgRotate.setOnClickListener {
                viewModel.apply {
                    if (currentPortraitThumbnail != null) {
                        currentPortraitThumbnail = currentPortraitThumbnail?.rotateBitmap(90)
                        ivImgPortrait.setImageBitmap(currentPortraitThumbnail)
                        currentPortraitOrientation =
                            currentPortraitOrientation.nextOrientationAngle()
                    }
                }
            }

            btnImgRemove.setOnClickListener {
                ivImgPortrait.setImageBitmap(null)
                ivImgPortrait.setImageResource(R.drawable.ic_person)
                viewModel.apply {
                    portraitBinariesDelete = true
                    viewModel.setLatestCameraPhoto(PhotoModel(imageType, Uri.EMPTY, ""))
                }
            }

            btnImgSnap.setOnClickListener {
                startCameraView()
            }

            btnImgConfirm.setOnClickListener {
                viewModel.setLatestCameraPhoto(
                    PhotoModel(
                        imageType,
                        Uri.EMPTY,
                        PhotoModel.THUMB_REFRESH
                    )
                )
                listener?.onSaveClicked()
                dismiss()
            }

            btnImgCancel.setOnClickListener {
                viewModel.portraitBinariesDelete = false
                viewModel.setLatestCameraPhoto(PhotoModel(imageType, Uri.EMPTY, ""))
                dismiss()
            }
        }
    }

    private fun updatePictureButtons() {
        _binding?.let {
            with(binding) {
                btnImgRemove.isVisible = imageType != PhotoModel.BIOMETRIC_PHOTO &&
                        viewModel.currentPortraitThumbnail != null
                btnImgRotate.isVisible = viewModel.currentPortraitThumbnail != null
                btnImgConfirm.isVisible = viewModel.currentPortraitThumbnail != null
            }
        }
    }

    private fun startCameraView() {
        callWithPermissions(Manifest.permission.CAMERA) {
            onGranted {
                val bundle = Bundle().apply {
                    putString(CameraFragment.TITLE, title)
                    putInt(CameraFragment.TYPE, imageType)
                    putBoolean(CameraFragment.OVERLAY, true)
                }
                findNavController().navigate(R.id.CameraFragment, bundle)
            }
            onShowRationale {
                showRationaleDialog(R.string.request_camera_permission, it)
            }
            onDenied {
                showCameraDeniedDialog(R.string.camera_denied_title, R.string.camera_denied_message)
            }
        }
    }

    private fun showCameraDeniedDialog(@StringRes titleResId: Int, @StringRes messageResId: Int) {
        AlertDialog.Builder(requireContext()).setTitle(titleResId).setMessage(messageResId)
            .setPositiveButton(R.string.dialog_settings) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .setCancelable(false).show()
    }

    private fun showRationaleDialog(@StringRes messageResId: Int, rationale: PermissionRationale) {
        AlertDialog.Builder(requireContext())
            .setPositiveButton(R.string.permission_allow) { _, _ -> rationale.retry() }
            .setNegativeButton(R.string.permission_deny, null).setCancelable(false)
            .setMessage(messageResId).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface IPhotoUtilsListener {
        fun onSaveClicked()
    }

    companion object {
        private const val TAG = "PhotoUtilsDialogFragment"
        const val TITLE = "title"
        const val IMAGE_TYPE = "image_type"
    }
}