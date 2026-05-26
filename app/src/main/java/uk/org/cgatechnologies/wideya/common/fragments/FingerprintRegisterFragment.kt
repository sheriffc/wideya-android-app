package uk.org.cgatechnologies.wideya.common.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.entities.PersonFingerprint
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.byteArrayToBase64
import uk.org.cgatechnologies.wideya.common.utils.Utils.setTint
import uk.org.cgatechnologies.wideya.common.viewmodels.Fp
import uk.org.cgatechnologies.wideya.common.viewmodels.SecugenViewModel
import uk.org.cgatechnologies.wideya.databinding.FragmentFingerprintRegisterBinding
import java.io.File
import java.util.*

private const val TAG: String = "FingerprintRegisterFragment"

class FingerprintRegisterFragment : DialogFragment() {
    private var _binding: FragmentFingerprintRegisterBinding? = null
    private val binding get() = _binding!!
    private val secugenViewModel by activityViewModels<SecugenViewModel>()
    private var personUuid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this) {
            checkIfAnyDataLost()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFingerprintRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch(Dispatchers.Default) {
            secugenViewModel.setCandidates()
        }

        val bundle = requireArguments()
        val args = FingerprintRegisterFragmentArgs.fromBundle(bundle)
        personUuid = args.teacherModel.person_uuid.toString()
        binding.tvTitle.text = args.title
        binding.tvPersonName.text = args.teacherModel.full_name
        when (args.fingerPosition) {
            "li" -> {
                //show left index
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

        //load state
        loadStep1State()
        loadStep2State()

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
            btnStep2.setOnClickListener {
                lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        activity?.runOnUiThread {
                            initStep2ScanningState()
                        }
                        captureFingerB()
                    } catch (e: Exception) {
                        // Handle exception
                        Log.e(TAG, e.toString())
                        activity?.runOnUiThread {
                            loadStep2State()
                        }
                    }
                }
            }

            btnBack.setOnClickListener {
                //confirm
                checkIfAnyDataLost()
            }

            btnSave.setOnClickListener {

                val uuid = Utils.getUuidOrdered()

                //save info to database and filesystem
                secugenViewModel.saveFingerprintsEncrypted(
                    "fpA.jpg",
                    "${uuid}_a.enc",
                    requireContext()
                )
                secugenViewModel.saveFingerprintsEncrypted(
                    "fpB.jpg",
                    "${uuid}_b.enc",
                    requireContext()
                )

                //pass back teacher model

                val pFp = PersonFingerprint(
                    uuid = uuid,
                    person_uuid = personUuid,
                    finger_position_oid = args.fingerPosition,
                    fp_a_cbor = File(requireContext().filesDir, "fpA.cbor").readBytes().byteArrayToBase64(),
                    fp_a_nfiq = secugenViewModel.nfiqA.toInt(),
                    fp_b_cbor = File(requireContext().filesDir, "fpB.cbor").readBytes().byteArrayToBase64(),
                    fp_b_nfiq = secugenViewModel.nfiqB.toInt(),
                    created_at = Utils.getISODateTimeUTC(),
                    created_by = Utils.getUserId(requireContext()),
                    updated_at = Utils.getISODateTimeUTC(),
                    updated_by = Utils.getUserId(requireContext()),
                    deleted_at = null,
                    deleted_by = null,
                    sync_flag = 1
                )

                secugenViewModel.insertFingerPrint(pFp)

                val bundleEx = Bundle()
                val teacherUpdate = args.teacherModel
                when (args.fingerPosition) {
                    "li" -> teacherUpdate.fp_li_uuid = uuid
                    "lt" -> teacherUpdate.fp_lt_uuid = uuid
                    "ri" -> teacherUpdate.fp_ri_uuid = uuid
                    "rt" -> teacherUpdate.fp_rt_uuid = uuid
                }

                bundleEx.putParcelable("teacher", teacherUpdate)
                bundleEx.putInt("resId", id)
                setFragmentResult("FingerprintRegisterFragment", bundleEx)

                clearSecugenState()

                //add to candidates list
                secugenViewModel.addToCandidates()

                goBack()

            }

        }

    }

    private fun clearSecugenState() {
//        deleteFiles()

        secugenViewModel.apply {
            nfiqA = 0
            nfiqB = 0
        }
    }

    private fun checkIfAnyDataLost() {
        secugenViewModel.apply {
            if (nfiqA > 0 || nfiqB > 0) {
                confirmDiscard()
            } else {
                clearSecugenState()
                goBack()
            }
        }
    }

    private fun initStep1ScanningState() {
        binding.apply {
            pbStep1.visibility = View.VISIBLE
            btnStep1.textSize = 0F
            btnStep1.isEnabled = false
        }
    }

    private fun restoreStep1State() {
        binding.apply {
            pbStep1.visibility = View.INVISIBLE
            btnStep1.textSize = 14F
            btnStep1.isEnabled = true
        }
    }

    private fun progressFromStep1ToStep2State() {
        binding.apply {
            pbStep1.visibility = View.INVISIBLE
            btnStep1.textSize = 14F
            btnStep1.isEnabled = false
            btnStep2.isEnabled = true

            // toggle opacity to direct user focus to the right place
            clStep1.alpha = 0.4.toFloat()
            clStep2.alpha = 1.toFloat()
        }
    }

    private fun loadStep1State() {
        binding.apply {
            when (secugenViewModel.nfiqA.toInt()) {
                1 -> {
                    tvStep1FpQuality.text = getString(R.string.highest_quality)
                    ivStep1Fp.setTint(R.color.fp_highest)
                    progressFromStep1ToStep2State()
                }
                2 -> {
                    tvStep1FpQuality.text = getString(R.string.high_quality)
                    ivStep1Fp.setTint(R.color.fp_high)
                    progressFromStep1ToStep2State()
                }
                3 -> {
//                    tvStep1FpQuality.text = "Medium quality: Please try again"
//                    ivStep1Fp.setTint(R.color.fp_medium)
//                    restoreStep1State()
                    //allow medium quality fps
                    tvStep1FpQuality.text = getString(R.string.medium_quality)
                    ivStep1Fp.setTint(R.color.fp_medium)
                    progressFromStep1ToStep2State()
                }
                4 -> {
                    tvStep1FpQuality.text = getString(R.string.low_quality)
                    ivStep1Fp.setTint(R.color.fp_low)
                    restoreStep1State()
                }
                5 -> {
                    tvStep1FpQuality.text = getString(R.string.lowest_quality)
                    ivStep1Fp.setTint(R.color.fp_lowest)
                    restoreStep1State()
                }
                -1 -> {
                    tvStep1FpQuality.text = getString(R.string.error_scan)
                    ivStep1Fp.setTint(R.color.fp_error)
                    restoreStep1State()
                }
                -2 -> {
                    "This fingerprint already exists in your school${System.getProperty("line.separator")}Duplicates are not allowed".also {
                        tvStep1FpQuality.text = it
                    }
                    ivStep1Fp.setTint(R.color.fp_error)
                    restoreStep1State()
                }
                else -> {
                    tvStep1FpQuality.text = getString(R.string.scan_fingerprint)
                    ivStep1Fp.setTint(R.color.fp_none)
                    restoreStep1State()
                }
            }
        }
    }

    private fun initStep2ScanningState() {
        binding.apply {
            pbStep2.visibility = View.VISIBLE
            btnStep2.textSize = 0F
            btnStep2.isEnabled = false
        }
    }

    private fun restoreStep2State() {
        binding.apply {
            pbStep2.visibility = View.INVISIBLE
            btnStep2.textSize = 14F
            when (secugenViewModel.nfiqA.toInt()) {
                1, 2, 3 -> btnStep2.isEnabled = true //UPDATE THESE CASES IF loadStep2State CASES CHANGE
                else -> btnStep2.isEnabled = false
            }
        }
    }

    private fun progressFromStep2ToStep2State() {
        binding.apply {
            pbStep2.visibility = View.INVISIBLE
            btnStep2.textSize = 14F
            btnStep2.isEnabled = false
            btnSave.isEnabled = true
        }
    }

    private fun loadStep2State() {
        binding.apply {
            when (secugenViewModel.nfiqB.toInt()) {
                1 -> {
                    tvStep2FpQuality.text = getString(R.string.highest_quality)
                    ivStep2Fp.setTint(R.color.fp_highest)
                    progressFromStep2ToStep2State()
                }
                2 -> {
                    tvStep2FpQuality.text = getString(R.string.high_quality)
                    ivStep2Fp.setTint(R.color.fp_high)
                    progressFromStep2ToStep2State()
                }
                3 -> {
                    tvStep2FpQuality.text = getString(R.string.medium_quality)
                    ivStep2Fp.setTint(R.color.fp_medium)
                    progressFromStep2ToStep2State()
                }
                4 -> {
                    tvStep2FpQuality.text = getString(R.string.low_quality)
                    ivStep2Fp.setTint(R.color.fp_low)
                    restoreStep2State()
                }
                5 -> {
                    tvStep2FpQuality.text = getString(R.string.lowest_quality)
                    ivStep2Fp.setTint(R.color.fp_lowest)
                    restoreStep2State()
                }
                6 -> {
                    tvStep2FpQuality.text = getString(R.string.fingerprint_not_match)
                    System.getProperty("line.separator")?.plus("Please try again")
                    ivStep2Fp.setTint(R.color.fp_no_match)
                    restoreStep2State()
                }
                -1 -> {
                    tvStep2FpQuality.text = getString(R.string.error_scan)
                    ivStep2Fp.setTint(R.color.fp_error)
                    restoreStep2State()
                }
                else -> {
                    tvStep2FpQuality.text = getString(R.string.scan_fingerprint)
                    ivStep2Fp.setTint(R.color.fp_none)
                    restoreStep2State()
                }
            }
        }
    }

    private fun captureFingerA() {
        Log.d(TAG, "try capture A")
        activity?.runOnUiThread {
            binding.tvStep1FpQuality.text =
                getString(R.string.guide_scanning_finger, System.getProperty("line.separator"))
        }
        secugenViewModel.apply {
            fpBufferA =
                ByteArray(mImageWidth * mImageHeight)
            dwTimeStart = System.currentTimeMillis()
            Log.d(TAG, "$dwTimeStart")
            val result = sgfplib.GetImageEx(
                fpBufferA,
                IMAGE_CAPTURE_TIMEOUT_MS.toLong(),
                IMAGE_CAPTURE_QUALITY.toLong()
            )
            nfiqA = sgfplib.ComputeNFIQ(
                fpBufferA,
                mImageWidth.toLong(),
                mImageHeight.toLong()
            )
            dwTimeEnd = System.currentTimeMillis()
            dwTimeElapsed = dwTimeEnd - dwTimeStart
            Log.d(TAG, "getImageEx() ret: $result $nfiqA \n")
            setBitmapA(fpBufferA)

            storeImage(
                "fpA.jpg", fpBitmapA,
                Bitmap.CompressFormat.JPEG, 90, requireActivity()
            )

            secugenViewModel.fpCborA = afisTemplateExtraction("fpA.jpg", requireContext(), requireActivity())

            //check if fingerprint already exists in the database
            when (nfiqA.toInt()) {
                1, 2 -> {
//                    val sourceAfisScore = sourceAfisMatch(requireContext(),"tmp${SEPARATOR}fpA.cbor","tmp${SEPARATOR}fpB.cbor")
//                    if(sourceAfisScore < 40) nfiqB = 6

                    val matcher = FingerprintMatcher(FingerprintTemplate(secugenViewModel.fpCborA))
                    val templateToMatchFp = Fp(personUuid, FingerprintTemplate(secugenViewModel.fpCborA))
                    var match = templateToMatchFp

                    var high = 0.0
                    for (candidate in secugenViewModel.candidates) {
                        val score = matcher.match(candidate.template)
                        if (score > high) {
                            high = score
                            match = candidate
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

    private fun captureFingerB() {
        Log.d(TAG, "try capture B")

        activity?.runOnUiThread {
            binding.tvStep2FpQuality.text =
                getString(R.string.guide_scanning_finger, System.getProperty("line.separator"))
        }
        secugenViewModel.apply {
            fpBufferB =
                ByteArray(mImageWidth * mImageHeight)
            dwTimeStart = System.currentTimeMillis()
            Log.d(TAG, "$dwTimeStart")
            val result = sgfplib.GetImageEx(
                fpBufferB,
                IMAGE_CAPTURE_TIMEOUT_MS.toLong(),
                IMAGE_CAPTURE_QUALITY.toLong()
            )
            nfiqB = sgfplib.ComputeNFIQ(
                fpBufferB,
                mImageWidth.toLong(),
                mImageHeight.toLong()
            )
            dwTimeEnd = System.currentTimeMillis()
            dwTimeElapsed = dwTimeEnd - dwTimeStart
            Log.d(TAG, "getImageEx() ret: $result $nfiqB \n")
            setBitmapB(fpBufferB)

            storeImage(
                "fpB.jpg", fpBitmapB,
                Bitmap.CompressFormat.JPEG, 90, requireActivity()
            )

            secugenViewModel.fpCborB = afisTemplateExtraction("fpB.jpg", requireContext(), requireActivity())

            when (nfiqB.toInt()) {
                1, 2, 3 -> {
                    val sourceAfisScore = sourceAfisMatch(requireContext(), "fpA.cbor", "fpB.cbor")
                    if (sourceAfisScore < 40) nfiqB = 6
                }
            }

            activity?.runOnUiThread {
                Log.d(TAG, "nfiq score: $nfiqB (${dwTimeElapsed}ms)")
                loadStep2State()
            }
        }
    }

    private fun goBack() {

        dismiss()
    }

    private fun confirmDiscard() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage("Discard any changes made?")
            .setTitle("Confirmation")

        builder.setPositiveButton("OK") { _, _ ->
            clearSecugenState()
            goBack()
            dismiss()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // User cancelled the dialog
        }
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        const val TAG = "FingerprintRegisterFragment"
    }

    override fun getTheme(): Int {
        return R.style.FullscreenDialogTheme
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}