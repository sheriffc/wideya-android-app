package uk.org.cgatechnologies.wideya.common.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.camera.PhotoUtilsDialogFragment
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.setTint
import uk.org.cgatechnologies.wideya.common.viewmodels.Fp
import uk.org.cgatechnologies.wideya.common.viewmodels.SecugenViewModel
import uk.org.cgatechnologies.wideya.databinding.FragmentBiometricVerificationBinding
import uk.org.cgatechnologies.wideya.school_management.LatestCameraPhotoUiState
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel
import java.io.File
import java.util.*

class BiometricVerificationFragment : Fragment(), PhotoUtilsDialogFragment.IPhotoUtilsListener {
    private var _binding: FragmentBiometricVerificationBinding? = null
    private val binding get() = _binding!!

    private val secugenViewModel by activityViewModels<SecugenViewModel>()
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private var personUuid: String = ""

    private var result: Boolean = false

    private lateinit var storageDirPath: String
    private var attendanceStatus = ""
    private var personAttendanceModel: PersonAttendanceModel = PersonAttendanceModel()

    private lateinit var title: String

    private fun initPhotoListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.cameraPhoto.collect { cameraPhoto ->
                    Log.i(TAG, "Photo Received at Dialog")
                    when (cameraPhoto) {
                        is LatestCameraPhotoUiState.Success -> {
                            cameraPhoto.photo?.let {

                                val tempPhoto = File(it.absolutePath)

                                if (it.absolutePath == PhotoModel.THUMB_REFRESH) {
                                    updatePhotoThumbnailPreview()

                                } else if (tempPhoto.length() > 0) {
                                    //delete the old photo and reference
                                    if (schoolManagementViewModel.tempPhotoPath.isNullOrEmpty()
                                            .not() &&
                                        schoolManagementViewModel.tempPhotoPath != it.absolutePath
                                    ) {
                                        schoolManagementViewModel.deleteTempFile()
                                    }
                                    schoolManagementViewModel.tempPhotoPath = it.absolutePath

                                    val job: Job = launch(Dispatchers.Default) {
                                        schoolManagementViewModel.processTempPhoto()
                                    }

                                    job.join()

                                    launch {
                                        updatePhotoThumbnailPreview()
                                    }

                                } else {
                                    schoolManagementViewModel.deletePhotoCache(storageDirPath)
                                }
                            } ?: run {
                                updatePhotoThumbnailPreview()
                            }
                        }
                        is LatestCameraPhotoUiState.Error -> {
                            schoolManagementViewModel.deletePhotoCache(storageDirPath)
                            val mySnackBar = Snackbar.make(
                                binding.root,
                                cameraPhoto.exception.message.toString(),
                                5000
                            )
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun updatePhotoThumbnailPreview() {
        activity?.runOnUiThread {
            if (schoolManagementViewModel.currentPortraitThumbnail != null) {
                binding.ivStep2Portrait.setImageBitmap(schoolManagementViewModel.currentPortraitThumbnail)
                binding.btnSave.isEnabled = true
            } else {
                binding.ivStep2Portrait.setImageResource(R.drawable.ic_person)
                binding.btnSave.isEnabled = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageDirPath = Utils.getTempPhotoPath(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG, "Dialog Created")
        Log.i(TAG, "Bundle: $savedInstanceState")
        _binding =
            FragmentBiometricVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initPhotoListener()

        binding.tvTitle2.text = getString(R.string.photo_verification)

        arguments?.let {
            personAttendanceModel =
                it.getParcelable("personAttendanceModel") ?: PersonAttendanceModel()
            title = getString(R.string.attendance_title, personAttendanceModel.full_name)

            attendanceStatus = it.getString("attendanceStatus") ?: ""

            personUuid = personAttendanceModel.person_uuid

            secugenViewModel.setCandidatesForPerson(personUuid)

            if (secugenViewModel.candidates.isNotEmpty()) {
                binding.tvTitle.text = getString(R.string.fingerprint_verification)
                secugenViewModel.candidates.forEach {
                    binding.apply {
                        when (it.uuid) {
                            "li" -> {
                                binding.ivLeftHandArrowIndex.visibility = View.VISIBLE
                                binding.ivLeftHandCircleIndex.visibility = View.VISIBLE
                            }
                            "lt" -> {
                                binding.ivLeftHandArrowThumb.visibility = View.VISIBLE
                                binding.ivLeftHandCircleThumb.visibility = View.VISIBLE
                            }
                            "ri" -> {
                                binding.ivRightHandArrowIndex.visibility = View.VISIBLE
                                binding.ivRightHandCircleIndex.visibility = View.VISIBLE
                            }
                            "rt" -> {
                                binding.ivRightHandArrowThumb.visibility = View.VISIBLE
                                binding.ivRightHandCircleThumb.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                binding.apply {
                    btnStep1.setOnClickListener {
                        lifecycleScope.launch(Dispatchers.Default) {
                            try {
                                activity?.runOnUiThread {
                                    initStep1ScanningState()
                                }
                                captureFingerA()
                            } catch (e: Exception) {
                                // Handle exception
                                Log.e(TAG, e.toString())
                                activity?.runOnUiThread {
                                    loadStep1State()
                                }
                            }
                        }
                    }
                }


            } else {
                //no fps available to match
                binding.apply {
                    tvTitle.text = getString(R.string.no_fingerprints_registered)
                    //disable fp section
                    cvStep1.visibility = View.GONE
                    ivLeftHand.visibility = View.GONE
                    ivRightHand.visibility = View.GONE
                    noFingerprintsRegisteredBannerLayout.visibility = View.VISIBLE
                }
            }

        }

        if (personAttendanceModel.portrait_base64_data.isNullOrEmpty().not()) {
            binding.tvTitle2.text = getString(R.string.photo_verification)
            binding.btnStep2.setOnClickListener {
                Utils.initCameraFunctionsWidget(
                    this,
                    PhotoModel.BIOMETRIC_PHOTO,
                    childFragmentManager,
                    title
                )
            }

        } else {
            binding.apply {
                tvTitle2.text = getString(R.string.no_profile_photo_registered)
                //disable Biometric Photo section
                cvStep2.visibility = View.GONE
                noProfilePhotoBannerLayout.visibility = View.VISIBLE
            }
        }

        binding.btnBack.setOnClickListener {
            Log.d(TAG, "dismissed")
            closeDialog()
        }

        binding.btnSave.setOnClickListener {
            saveAttendance(Constants.BIOMETRIC_METHOD_PHOTO_ID)
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.title = title
    }

    override fun onSaveClicked() {
        saveAttendance(Constants.BIOMETRIC_METHOD_PHOTO_ID)
    }

    private fun loadStep1State() {
        _binding?.let {
            binding.apply {
                when (secugenViewModel.nfiqA.toInt()) {
                    4 -> {
                        tvStep1FpQuality.text = getString(R.string.warning_low_quality)
                        ivStep1Fp.setTint(R.color.fp_low)
                        restoreStep1State()
                    }
                    5 -> {
                        tvStep1FpQuality.text = getString(R.string.warning_lowest_quality)
                        ivStep1Fp.setTint(R.color.fp_lowest)
                        restoreStep1State()
                    }
                    -1 -> {
                        tvStep1FpQuality.text = getString(R.string.error_scanning_finger)
                        ivStep1Fp.setTint(R.color.fp_error)
                        restoreStep1State()
                    }
                    -2 -> {
                        tvStep1FpQuality.text = getString(R.string.fingerprint_verified)
                        ivStep1Fp.setTint(R.color.fp_high)
                        result = true
                        saveAttendance(Constants.BIOMETRIC_METHOD_FINGERPRINT_ID)
                    }
                    else -> {
                        tvStep1FpQuality.text = getString(R.string.error_no_match)
                        ivStep1Fp.setTint(R.color.fp_no_match)
                        restoreStep1State()
                    }
                }
            }
        }
    }

    private fun initStep1ScanningState() {
        _binding?.let {
            binding.apply {
                pbStep1.visibility = View.VISIBLE
                btnStep1.textSize = 0F
                btnStep1.isEnabled = false
            }
        }
    }

    private fun restoreStep1State() {
        _binding?.let {
            binding.apply {
                pbStep1.visibility = View.INVISIBLE
                btnStep1.textSize = 14F
                btnStep1.isEnabled = true
            }
        }
    }

    private fun captureFingerA() {
        Log.d(TAG, "try capture A")
//        val activity = requireActivity()
        activity?.runOnUiThread {
            _binding?.let {
                binding.tvStep1FpQuality.text =
                    getString(R.string.guide_scanning_finger, System.getProperty("line.separator"))
            }
        }
        secugenViewModel.apply {
            fpBufferA = ByteArray(mImageWidth * mImageHeight)
            dwTimeStart = System.currentTimeMillis()
            Log.d(TAG, "$dwTimeStart")
            val result = sgfplib.GetImageEx(
                fpBufferA, IMAGE_CAPTURE_TIMEOUT_MS.toLong(), IMAGE_CAPTURE_QUALITY.toLong()
            )
            nfiqA = sgfplib.ComputeNFIQ(
                fpBufferA, mImageWidth.toLong(), mImageHeight.toLong()
            )
            dwTimeEnd = System.currentTimeMillis()
            dwTimeElapsed = dwTimeEnd - dwTimeStart
            Log.d(TAG, "getImageEx() ret: $result $nfiqA \n")
            setBitmapA(fpBufferA)

            storeImage(
                "fpA.jpg", fpBitmapA, Bitmap.CompressFormat.JPEG, 90, requireActivity()
            )

            secugenViewModel.fpCborA =
                afisTemplateExtraction("fpA.jpg", requireContext(), requireActivity())

            //check if fingerprint already exists in the database
            when (nfiqA.toInt()) {
                1, 2, 3 -> {
                    val matcher = FingerprintMatcher(FingerprintTemplate(secugenViewModel.fpCborA))
                    val templateToMatchFp = Fp(
                        personUuid, FingerprintTemplate(secugenViewModel.fpCborA)
                    )
                    var match = templateToMatchFp

                    var high = 0.0
                    for (candidate in secugenViewModel.candidates) {
                        val score = matcher.match(candidate.template)
                        if (score > high) {
                            high = score
                            match = candidate
                            //store position of fp
                            if (high >= 40) {
                                secugenViewModel.fpPositionA = candidate.uuid
                            }
                        }
                    }

                    if ((templateToMatchFp == match).not()) {
                        if (high >= 40) {
                            //fingerprint exists
                            secugenViewModel.nfiqA = -2
                        }
                    }
                }
            }

            activity?.runOnUiThread {
                Log.d(TAG, "nfiq score: $nfiqA (${dwTimeElapsed}ms)")
                loadStep1State()
            }

        }
    }

    private fun saveAttendance(biometricMethod: String) {
        personAttendanceModel.attendance_status_oid = attendanceStatus
        personAttendanceModel.absent_reason_oid = null
        personAttendanceModel.absent_reason_other = null
        personAttendanceModel.biometric_method_oid = biometricMethod
        when (biometricMethod) {
            Constants.BIOMETRIC_METHOD_FINGERPRINT_ID -> {
                personAttendanceModel.biometric_reference = secugenViewModel.fpPositionA
            }
            Constants.BIOMETRIC_METHOD_PHOTO_ID -> {
                personAttendanceModel.biometric_reference = schoolManagementViewModel.currentPortraitOrientation.toString()
                personAttendanceModel.portrait_display_orientation = schoolManagementViewModel.currentPortraitOrientation.toShort()
            }
            else -> {
                personAttendanceModel.biometric_reference = "unsupported biometric type"
            }
        }

        schoolManagementViewModel.savePersonAttendance(personAttendanceModel, storageDirPath)

        secugenViewModel.apply {
            nfiqA = 0
            nfiqB = 0
        }

        closeDialog()
    }

    private fun closeDialog() {
        findNavController().popBackStack()
    }

    override fun onDetach() {
//        schoolManagementViewModel.deletePhotoCache(storageDirPath)
        schoolManagementViewModel.deletePhotoCacheInViewModelScope(storageDirPath)
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "BiometricVerificationFragment"
    }
}