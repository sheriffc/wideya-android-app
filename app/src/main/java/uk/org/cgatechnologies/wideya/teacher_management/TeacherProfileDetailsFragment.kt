package uk.org.cgatechnologies.wideya.teacher_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.fragments.FingerprintRegisterFragment
import uk.org.cgatechnologies.wideya.common.utils.Extensions.toBase64
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.makeTextInputEditable
import uk.org.cgatechnologies.wideya.common.utils.Utils.setTint
import uk.org.cgatechnologies.wideya.common.utils.Utils.setViewBackgroundColorToEditable
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.*
import uk.org.cgatechnologies.wideya.databinding.FragmentTeacherProfileDetailsBinding
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel
import java.io.File

class TeacherProfileDetailsFragment : Fragment() {

    private var _binding: FragmentTeacherProfileDetailsBinding? = null
    private val binding get() = _binding!!

    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()

    private lateinit var storageDirPath: String

    private fun initPhotoListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                teacherManagementViewModel.cameraPhoto.collect { cameraPhoto ->
                    Log.i(TAG, "Photo Received at Dialog")
                    when (cameraPhoto) {
                        is LatestCameraProfilePhotoUiState.Success -> {
                            Log.i(TAG, "Photo Received -> ${cameraPhoto.photo}")
                            cameraPhoto.photo?.let {

                                if (it.absolutePath == PhotoModel.THUMB_REFRESH) {
                                    updatePhotoThumbnailPreview()

                                } else if (File(it.absolutePath).length() > 0) {
                                    //delete the old photo and reference
                                    if (teacherManagementViewModel.tempPhotoPath.isNullOrEmpty().not() &&
                                        teacherManagementViewModel.tempPhotoPath != it.absolutePath
                                    ) {
                                        teacherManagementViewModel.deleteTempFile()
                                    }
                                    teacherManagementViewModel.tempPhotoPath = it.absolutePath


                                        val job: Job = launch(Dispatchers.Default) {
                                            teacherManagementViewModel.processTempPhoto()
                                        }

                                        job.join()

                                        launch {
                                            updatePhotoThumbnailPreview()
                                        }

                                } else {
                                    teacherManagementViewModel.deletePhotoCache(storageDirPath)
                                }
                            } ?: run {
                                updatePhotoThumbnailPreview()
                            }
                        }
                        is LatestCameraProfilePhotoUiState.Error -> {
                            teacherManagementViewModel.deletePhotoCache(storageDirPath)
                            val mySnackBar = Snackbar.make(binding.root, cameraPhoto.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun updatePhotoThumbnailPreview() {
        activity?.runOnUiThread {
            if (teacherManagementViewModel.currentPortraitThumbnail != null) {
                binding.ivTeacherPortrait.setImageBitmap(teacherManagementViewModel.currentPortraitThumbnail)
            } else {
                loadTeacherSavedThumbnailImage(teacherManagementViewModel.currentTeacher)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Going onCreate")

        // unsatisfactory attempt to solve the occasional crashes due to non initialisation
        // if(teacherManagementViewModel.isCurrentTeacherInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        storageDirPath = Utils.getTempPhotoPath(requireContext())
        teacherManagementViewModel.portraitBinariesDelete = false

        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<TeacherModel>("teacher")
            val id = bundle.getInt("resId")
            loadTeacherDetailsInView(result as TeacherModel, listOf(id))
        }

        childFragmentManager.setFragmentResultListener("FingerprintRegisterFragment", this) { _, bundle ->
            val result = bundle.getParcelable<TeacherModel>("teacher")
            teacherManagementViewModel.currentTeacher = result as TeacherModel
            setFingerprintColors(result)
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //confirm
            when (teacherManagementViewModel.currentDetailsMode) {
                DetailsMode.VIEW -> {
                    this.isEnabled = false
                    requireActivity().onBackPressed()
                }
                DetailsMode.NEW -> confirmDiscard()
                DetailsMode.EDIT -> confirmDiscard()
                DetailsMode.FROM_SEARCH -> confirmDiscard()
            }
        }

        when (teacherManagementViewModel.currentDetailsMode) {
            DetailsMode.NEW -> {
                val bundle = requireArguments()
                val args = TeacherProfileDetailsFragmentArgs.fromBundle(bundle)

                teacherManagementViewModel.currentTeacher.apply {
                    if (person_uuid.isNullOrEmpty()) {
                        person_uuid = Utils.getUuidOrdered()
                    }
                }

                args.teacherPayroll.let { payroll ->
                    if (payroll.pin.isNullOrEmpty()) {
                        //non payroll
                        teacherManagementViewModel.currentTeacherEmploymentStatus = "nonpayroll"
                        teacherManagementViewModel.currentTeacher.apply {
                            uuid = Utils.getUuidOrdered()
                            person_uuid = null
                            full_name = payroll.full_name
                            last_name = payroll.last_name
                            middle_name = payroll.middle_name
                            first_name = payroll.first_name
                            sex_oid = payroll.sex
                            sex_name = payroll.sex
                            date_of_birth = payroll.date_of_birth
                            age = payroll.age
                            nin = payroll.nin
                            portrait_uuid = null
                            portrait_base64_data = null
                            portrait_active = 0
                            portrait_created_at = null
                            portrait_created_by = 0
                            phone_1 = null
                            phone_2 = null
                            email = null
                            address = null
                            fp_lt_uuid = null
                            fp_li_uuid = null
                            fp_rt_uuid = null
                            fp_ri_uuid = null
                            person_created_at = Utils.getISODateTimeUTC()
                            person_created_by = Utils.getUserId(requireContext())
                            person_updated_at = Utils.getISODateTimeUTC()
                            person_updated_by = Utils.getUserId(requireContext())
                            person_deleted_at = null
                            person_deleted_by = null
                            employment_status_oid = "nonpayroll"
                            employment_status_name = "Non-Payroll"
                            teacher_role_oid = null
                            teacher_role_name = null
                            teacher_role_other = null
                            pin = null
                            nassit_number = payroll.nassit_number
                            tsc_licence_id = null
                            start_date = null
                            end_date = null
                            end_reason_name = null
                            end_reason_other = null
                            end_reason_detail = null
                            created_at = Utils.getISODateTimeUTC()
                            created_by = Utils.getUserId(requireContext())
                            updated_at = Utils.getISODateTimeUTC()
                            updated_by = Utils.getUserId(requireContext())
                            deleted_at = null
                            deleted_by = null
                        }

                    } else {
                        //payroll teacher
                        teacherManagementViewModel.currentTeacherEmploymentStatus = "payroll"
                        teacherManagementViewModel.currentTeacher.apply {
                            uuid = Utils.getUuidOrdered()
                            person_uuid = null
                            full_name = payroll.full_name
                            last_name = payroll.last_name
                            middle_name = payroll.middle_name
                            first_name = payroll.first_name
                            sex_oid = payroll.sex
                            sex_name = payroll.sex
                            date_of_birth = payroll.date_of_birth
                            age = payroll.age
                            nin = payroll.nin
                            portrait_uuid = null
                            portrait_base64_data = null
                            portrait_active = 0
                            portrait_created_at = null
                            portrait_created_by = 0
                            phone_1 = null
                            phone_2 = null
                            email = null
                            address = null
                            fp_lt_uuid = null
                            fp_li_uuid = null
                            fp_rt_uuid = null
                            fp_ri_uuid = null
                            person_created_at = Utils.getISODateTimeUTC()
                            person_created_by = Utils.getUserId(requireContext())
                            person_updated_at = Utils.getISODateTimeUTC()
                            person_updated_by = Utils.getUserId(requireContext())
                            person_deleted_at = null
                            person_deleted_by = null
                            employment_status_oid = "payroll"
                            employment_status_name = "Payroll"
                            teacher_role_oid = null
                            teacher_role_name = null
                            teacher_role_other = null
                            pin = payroll.pin
                            nassit_number = payroll.nassit_number
                            tsc_licence_id = null
                            start_date = null
                            end_date = null
                            end_reason_name = null
                            end_reason_other = null
                            end_reason_detail = null
                            created_at = Utils.getISODateTimeUTC()
                            created_by = Utils.getUserId(requireContext())
                            updated_at = Utils.getISODateTimeUTC()
                            updated_by = Utils.getUserId(requireContext())
                            deleted_at = null
                            deleted_by = null
                        }
                    }
                }
            }
            DetailsMode.EDIT -> {
                teacherManagementViewModel.currentTeacherEmploymentStatus =
                    teacherManagementViewModel.currentTeacher.employment_status_oid ?: "unknown"
            }
            else -> {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "Going onCreateView")

        _binding = FragmentTeacherProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "Going onViewCreated")

        initPhotoListener()

        binding.apply {
            val nonPayrollValidations = setOf(
                NameInputValidation(etFirstName, tlFirstName, true),
                NameInputValidation(etMiddleName, tlMiddleName, false),
                NameInputValidation(etLastName, tlLastName, true),
                RequiredDialogValidation(etSex, tlSex),
                NinInputValidation(etNin, tlNin),
                NassitInputValidation(etNassit, tlNassit),
            )

            val generalValidations = setOf(
                RequiredDialogValidation(etTeacherRole, tlTeacherRole),
                PhoneInputValidation(etPhone1, tlPhone1, false),
                PhoneInputValidation(etPhone2, tlPhone2, false),
                EmailInputValidation(etEmail, tlEmail),
                FreetextInputValidation(etAddress, tlAddress),
            )

            // must clear the validations array first, otherwise when page reloads after fingerprint fragment
            // then the validations get duplicated in a nasty way
            validationsArray.clear()
            validationsArray.addAll(generalValidations)
            if (teacherManagementViewModel.currentTeacher.employment_status_oid != "payroll") {
                validationsArray.addAll(nonPayrollValidations)
            }
        }

        when (teacherManagementViewModel.currentDetailsMode) {
            DetailsMode.VIEW -> {
                makeFieldsViewOnly()
                bindData()

                binding.btSave.visibility = View.GONE
                binding.btEdit.visibility = View.VISIBLE
                binding.btEdit.setOnClickListener {
                    teacherManagementViewModel.currentDetailsMode = DetailsMode.EDIT
                    Navigation.findNavController(binding.root).navigate(
                        TeacherProfileFragmentDirections.actionTeacherProfileFragmentToTeacherProfileDetailsFragment(
                            TeacherPayrollModel(
                                uuid = "",
                                first_name = "",
                                middle_name = "",
                                last_name = "",
                                full_name = "",
                                sex = "",
                                date_of_birth = "",
                                age = null,
                                pin = "",
                                nin = "",
                                nassit_number = "",
                                created_at = "",
                                updated_at = ""
                            )
                        )
                    )
                }

                binding.btRemove.visibility = View.VISIBLE
                binding.btRemove.setOnClickListener {
                    Navigation.findNavController(binding.root)
                        .navigate(TeacherProfileFragmentDirections.actionTeacherProfileFragmentToTeacherProfileRemoveFragment())
                }
            }
            DetailsMode.NEW -> {
                loadTeacherDetailsInView(teacherManagementViewModel.currentTeacher)

                makeFieldsEditable()
                ValidationUtils.addLiveValidation(validationsArray)

                //all fields should be empty
                //show save button
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    //save data
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveTeacherDetailsInView()
                        goBack()
                        if (teacherManagementViewModel.currentTeacherEmploymentStatus == "payroll") {
                            goBack()
                        }
                    }
                }
                binding.ivTeacherPortrait.setOnClickListener {
                    showCameraDialog()
                }
            }
            DetailsMode.EDIT -> {
                loadTeacherDetailsInView(teacherManagementViewModel.currentTeacher)
                makeFieldsEditable()

                //show save button
                ValidationUtils.addLiveValidation(validationsArray)

                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveTeacherDetailsInView()
                        goBack()
                    }
                }

                binding.ivTeacherPortrait.setOnClickListener {
                    showCameraDialog()
                }
            }
            DetailsMode.FROM_SEARCH -> {}
        }
    }

    private fun setFingerprintColors(teacher: TeacherModel?) {
        binding.apply {
            if (teacher?.fp_lt_uuid.isNullOrEmpty().not()) {
                ivLeftThumb.setTint(R.color.green_bold)
            } else {
                ivLeftThumb.setTint(R.color.black)
            }

            if (teacher?.fp_li_uuid.isNullOrEmpty().not()) {
                ivLeftIndex.setTint(R.color.green_bold)
            } else {
                ivLeftIndex.setTint(R.color.black)
            }
            if (teacher?.fp_rt_uuid.isNullOrEmpty().not()) {
                ivRightThumb.setTint(R.color.green_bold)
            } else {
                ivRightThumb.setTint(R.color.black)
            }
            if (teacher?.fp_ri_uuid.isNullOrEmpty().not()) {
                ivRightIndex.setTint(R.color.green_bold)
            } else {
                ivRightIndex.setTint(R.color.black)
            }
        }
    }

    private fun promptFingerprintRegistration(fingerCode: String, fingerName: String) {
        val bundle = Bundle()
        bundle.putString("fingerPosition", fingerCode)
        bundle.putString("title", fingerName)
        bundle.putParcelable("teacherModel", teacherManagementViewModel.currentTeacher)
        val fingerprintRegisterFragment: DialogFragment = FingerprintRegisterFragment()
        fingerprintRegisterFragment.arguments = bundle
        fingerprintRegisterFragment.show(
            childFragmentManager, FingerprintRegisterFragment.TAG
        )
    }

    private fun setFingerprintListeners() {
        binding.ivLeftIndex.apply {
            setOnClickListener {
                promptFingerprintRegistration("li", "Left Hand Index Finger")
            }
        }

        binding.ivLeftThumb.apply {
            setOnClickListener {
                promptFingerprintRegistration("lt", "Left Hand Thumb")
            }
        }

        binding.ivRightIndex.apply {
            setOnClickListener {
                promptFingerprintRegistration("ri", "Right Hand Index Finger")
            }
        }

        binding.ivRightThumb.apply {
            setOnClickListener {
                promptFingerprintRegistration("rt", "Right Hand Thumb")
            }
        }
    }

    private fun showCameraDialog() {
        Utils.initCameraFunctionsWidget(null, PhotoModel.PROFILE_PHOTO, childFragmentManager)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "Going onResume")
        setTitle()
    }

    private fun goBack() {
        Navigation.findNavController(binding.root).navigateUp()
    }

    private fun bindData() {
        //set observer
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                teacherManagementViewModel.teacherDetail.collect { item ->
                    when (item) {
                        is LatestTeacherDetailUiState.Success -> {
                            loadTeacherDetailsInView(item.teacher)
                        }
                        is LatestTeacherDetailUiState.Error -> {
                            val mySnackBar = Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun loadTeacherDetailsInView(teacher: TeacherModel?, idList: List<Int> = listOf()) {
        binding.apply {

            if (idList.isEmpty() || idList.contains(etEmploymentStatus.id))
                etEmploymentStatus.setText(teacher?.employment_status_name ?: "")
            if (idList.isEmpty() || idList.contains(etEmploymentStatus.id))
                etEmploymentStatus.setTag(R.string.idTag, teacher?.employment_status_oid ?: "")
            if (idList.isEmpty() || idList.contains(etPin.id)) etPin.setText(teacher?.pin ?: "None")

            //personal
            if (idList.isEmpty() || idList.contains(etFirstName.id)) etFirstName.setText(teacher?.first_name ?: "")
            if (idList.isEmpty() || idList.contains(etMiddleName.id)) etMiddleName.setText(teacher?.middle_name ?: "")
            if (idList.isEmpty() || idList.contains(etLastName.id)) etLastName.setText(teacher?.last_name ?: "")
            if (idList.isEmpty() || idList.contains(etNin.id)) etNin.setText(teacher?.nin ?: "")
            if (idList.isEmpty() || idList.contains(etSex.id)) etSex.setText(teacher?.sex_name ?: "")
            if (idList.isEmpty() || idList.contains(etSex.id)) etSex.setTag(R.string.idTag, teacher?.sex_oid ?: "")
            if (idList.isEmpty() || idList.contains(etDob.id)) etDob.setText(teacher?.date_of_birth ?: "")
            if (idList.isEmpty() || idList.contains(tvTeacherAgeValue.id))
                tvTeacherAgeValue.text = teacher?.age?.toString() ?: "unknown"

            //teacher
            if (idList.isEmpty() || idList.contains(etTeacherRole.id))
                etTeacherRole.setText(teacher?.teacher_role_name ?: "")
            if (idList.isEmpty() || idList.contains(etTeacherRole.id))
                etTeacherRole.setTag(R.string.idTag, teacher?.teacher_role_oid ?: "")

            if (idList.isEmpty() || idList.contains(etNassit.id)) etNassit.setText(teacher?.nassit_number ?: "")
            if (idList.isEmpty() || idList.contains(etStartDate.id)) etStartDate.setText(teacher?.start_date ?: "")
            if (idList.isEmpty() || idList.contains(etEndDate.id)) etEndDate.setText(teacher?.end_date ?: "")
            if (idList.isEmpty() || idList.contains(etEndReason.id)) etEndReason.setText(teacher?.end_reason_name ?: "")

            if (idList.isEmpty() || idList.contains(etEndReason.id))
                etEndReason.setTag(R.string.idTag, teacher?.end_reason_oid ?: "")
            if (idList.isEmpty() || idList.contains(etEndReasonOther.id))
                etEndReasonOther.setText(teacher?.end_reason_other ?: "")

            //contact

            if (idList.isEmpty() || idList.contains(etPhone1.id)) etPhone1.setText(teacher?.phone_1 ?: "")
            if (idList.isEmpty() || idList.contains(etPhone2.id)) etPhone2.setText(teacher?.phone_2 ?: "")
            if (idList.isEmpty() || idList.contains(etAddress.id)) etAddress.setText(teacher?.address ?: "")
            if (idList.isEmpty() || idList.contains(etEmail.id)) etEmail.setText(teacher?.email ?: "")

            updatePhotoThumbnailPreview()
            setFingerprintColors(teacher)
        }
    }

    private fun loadTeacherSavedThumbnailImage(teacher: TeacherModel?) {
        val bitmap = if (!teacherManagementViewModel.portraitBinariesDelete && !teacher?.portrait_base64_data.isNullOrEmpty())
            Utils.decodeBase64StringToBitmap(teacher!!.portrait_base64_data!!) else null
        if (bitmap != null) {
            teacherManagementViewModel.apply {
                currentPortraitThumbnail = bitmap
                binding.ivTeacherPortrait.setImageBitmap(currentPortraitThumbnail)
            }
        } else {
            binding.ivTeacherPortrait.setImageResource(R.drawable.ic_person)
        }
    }

    private fun saveTeacherDetailsInView() {
        binding.apply {
            teacherManagementViewModel.currentTeacher.apply teacher@{
                this@teacher.first_name = Utils.trimWhiteSpace(etFirstName).uppercase()
                this@teacher.middle_name = Utils.trimWhiteSpace(etMiddleName).uppercase()
                this@teacher.last_name = Utils.trimWhiteSpace(etLastName).uppercase()
                this@teacher.nin = Utils.trimWhiteSpace(etNin).uppercase()
                this@teacher.sex_name = etSex.text.toString()
                this@teacher.sex_oid = etSex.getTag(R.string.idTag)?.toString().orEmpty()
                this@teacher.date_of_birth = etDob.text.toString()
                this@teacher.age = tvTeacherAgeValue.text.toString().toIntOrNull()

                this@teacher.teacher_role_name = etTeacherRole.text.toString()
                this@teacher.teacher_role_oid = etTeacherRole.getTag(R.string.idTag)?.toString().orEmpty()
                // Do not set PIN or Employment Status since non-editable; also the PIN text says "None" when empty which we don't want to save
                this@teacher.nassit_number = Utils.trimWhiteSpace(etNassit).uppercase()
                this@teacher.start_date = etStartDate.text.toString()
                this@teacher.end_date = etEndDate.text.toString()
                this@teacher.end_reason_name = etEndReason.text.toString()
                this@teacher.end_reason_oid = etEndReason.getTag(R.string.idTag)?.toString().orEmpty()
                this@teacher.end_reason_other = Utils.trimWhiteSpace(etEndReasonOther)

                this@teacher.phone_1 = Utils.trimWhiteSpace(etPhone1)
                this@teacher.phone_2 = Utils.trimWhiteSpace(etPhone2)
                this@teacher.address = Utils.trimWhiteSpace(etAddress)
                this@teacher.email = Utils.trimWhiteSpace(etEmail)

                if (teacherManagementViewModel.currentPortraitThumbnail != null) {
                    this@teacher.portrait_base64_data =
                        teacherManagementViewModel.currentPortraitThumbnail!!.toBase64()
                }

                if (teacherManagementViewModel.portraitBinariesDelete) {
                    this@teacher.portrait_uuid = null
                    this@teacher.portrait_base64_data = null
                    teacherManagementViewModel.portraitBinariesDelete = false
                }
            }
        }

        teacherManagementViewModel.saveTeacher(storageDirPath)
    }

    private fun makeFieldsViewOnly() {
        binding.apply {

            tlSex.endIconMode = TextInputLayout.END_ICON_NONE
            tlDob.endIconMode = TextInputLayout.END_ICON_NONE
            tlEmploymentStatus.endIconMode = TextInputLayout.END_ICON_NONE
            tlTeacherRole.endIconMode = TextInputLayout.END_ICON_NONE
            tlStartDate.endIconMode = TextInputLayout.END_ICON_NONE
            tlEndDate.endIconMode = TextInputLayout.END_ICON_NONE
            tlEndReason.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    private fun makeFieldsEditable() {
        binding.apply {

            // distinguish between payroll vs nonpayroll staff
            // with payroll staff, not as many fields can be edited
            // with nonpayroll staff, most fields can be edited

            // safety net to avoid accidental editability of payroll teacher fields
            // if value is anything other than "nonpayroll", then will lock fields to non-editable
            // in case a payroll teacher ends up with a dodgy or blank value here
            if (teacherManagementViewModel.currentTeacherEmploymentStatus == "nonpayroll") {

                makeTextInputEditable(etFirstName, tlFirstName)
                makeTextInputEditable(etMiddleName, tlMiddleName)
                makeTextInputEditable(etLastName, tlLastName)
                makeTextInputEditable(etNin, tlNin)
                makeTextInputEditable(etNassit, tlNassit)

                setViewBackgroundColorToEditable(tlSex)
                etSex.apply {
                    isFocusable = false
                    setOnClickListener {
                        Utils.initSelectOneWidget(
                            "teacher.sex_oid",
                            it,
                            tlSex.hint.toString(),
                            teacherManagementViewModel.getOptionList("sex"),
                            childFragmentManager
                        )
                    }
                }

                setViewBackgroundColorToEditable(tlDob)
                etDob.apply {
                    isFocusable = false
                    setOnClickListener {
                        Utils.initDatePickerDialog(requireContext(), it, 1980, tvTeacherAgeValue)
                    }
                }

            } else {
                tlSex.endIconMode = TextInputLayout.END_ICON_NONE
                tlDob.endIconMode = TextInputLayout.END_ICON_NONE
            }

            // --- the following field settings do not depend on payroll vs nonpayroll ---

            setViewBackgroundColorToEditable(ivTeacherPortrait)

            makeTextInputEditable(etEndReasonOther, tlEndReasonOther)
            makeTextInputEditable(etPhone1, tlPhone1)
            makeTextInputEditable(etPhone2, tlPhone2)
            makeTextInputEditable(etAddress, tlAddress)
            makeTextInputEditable(etEmail, tlEmail)

            // EmploymentStatus is locked (user not allowed to edit)
            tlEmploymentStatus.endIconMode = TextInputLayout.END_ICON_NONE

            setViewBackgroundColorToEditable(tlTeacherRole)
            etTeacherRole.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "teacher.teacher_role_oid",
                        it,
                        tlTeacherRole.hint.toString(),
                        teacherManagementViewModel.getOptionList("teacher_role"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlStartDate)
            etStartDate.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 2015)
                }
            }

            setViewBackgroundColorToEditable(tlEndDate)
            etEndDate.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 0)
                }
            }

            setViewBackgroundColorToEditable(tlEndReason)
            etEndReason.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "teacher.end_reason_learner_oid",
                        it,
                        tlEndReason.hint.toString(),
                        teacherManagementViewModel.getOptionList("end_reason_teacher"),
                        childFragmentManager
                    )
                }
            }

            setFingerprintListeners()

            cvFingerprintSection.setCardBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    R.color.editable_area,
                    null
                )
            )
        }
    }

    fun setTitle() {
        teacherManagementViewModel.apply {
            val title = when (this.currentDetailsMode) {
                DetailsMode.VIEW -> (requireActivity() as AppCompatActivity).supportActionBar?.title //use same
                DetailsMode.EDIT -> "${this.currentTeacher.full_name} (Edit Mode)"
                DetailsMode.NEW -> "Add New Teacher"
                DetailsMode.FROM_SEARCH -> ""
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        }
    }

    private fun confirmDiscard() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage("Discard any changes made?")
            .setTitle("Confirmation")

        builder.setPositiveButton("OK") { _, _ ->
            doCleanup()
            goBack()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // User cancelled the dialog
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun doCleanup() {
        //routine to delete any temp files created but not saved
        teacherManagementViewModel.deleteTempFile()
    }

    override fun onDetach() {
        Log.d(TAG, "Going onDetach")

//        teacherManagementViewModel.deletePhotoCache(storageDirPath)

        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "Going onDestroyView")
        teacherManagementViewModel.clearPhotoCache()
        _binding = null
    }

    companion object {
        private const val TAG = "TeacherProfileDetailsFragment"
    }
}