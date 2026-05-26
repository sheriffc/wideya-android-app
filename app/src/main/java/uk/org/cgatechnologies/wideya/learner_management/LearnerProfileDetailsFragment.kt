package uk.org.cgatechnologies.wideya.learner_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.makeTextInputEditable
import uk.org.cgatechnologies.wideya.common.utils.Utils.setViewBackgroundColorToEditable
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.*
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnerProfileDetailsBinding
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementViewModel
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

private const val TAG: String = "LearnerProfileDetailsFragment"

class LearnerProfileDetailsFragment : Fragment() {

    private var _binding: FragmentLearnerProfileDetailsBinding? = null
    private val binding get() = _binding!!
    private val learnerManagementViewModel by activityViewModels<LearnerManagementViewModel>()
    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()
    private var schoolGroupUuid: String = String()
    private var schoolGroupOptionItem: OptionList? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<LearnerAdmissionModel>("learner")
            val id = bundle.getInt("resId")
            // Do something with the result
            loadLearnerDetailsInView(result as LearnerAdmissionModel, listOf(id))
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //confirm
            when (learnerManagementViewModel.currentDetailsMode) {
                DetailsMode.VIEW -> {
                    this.isEnabled = false
                    requireActivity().onBackPressed()
                }
                DetailsMode.NEW -> confirmDiscard()
                DetailsMode.EDIT -> confirmDiscard()
                DetailsMode.FROM_SEARCH -> confirmDiscard()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnerProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if(learnerManagementViewModel.isCurrentLearnerInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        binding.apply {
            val inputValidations = setOf(
                NameInputValidation(etLearnerFirstName, tlLearnerFirstName, true),
                NameInputValidation(etLearnerMiddleName, tlLearnerMiddleName, false),
                NameInputValidation(etLearnerLastName, tlLearnerLastName, true),
                RequiredDialogValidation(etLearnerSex, tlLearnerSex),
                NinInputValidation(etLearnerNin, tlLearnerNin),
                NameInputValidation(etGuardianFirstName, tlGuardianFirstName, false),
                NameInputValidation(etGuardianMiddleName, tlGuardianMiddleName, false),
                NameInputValidation(etGuardianLastName, tlGuardianLastName, false),
                NinInputValidation(etGuardianNin, tlGuardianNin),
                PhoneInputValidation(etGuardianPhone1, tlGuardianPhone1, false),
                PhoneInputValidation(etGuardianPhone2, tlGuardianPhone2, false),
                FreetextInputValidation(etGuardianAddress, tlGuardianAddress),
                LearnerAdmissionNumberValidation(
                    etLearnerAdmissionNumber,
                    tlLearnerAdmissionNumber,
                    learnerManagementViewModel.currentLearner.school_uuid!!,
                    learnerManagementViewModel.currentLearner.learner_uuid ?: String()
                )
            )
            validationsArray.clear()
            validationsArray.addAll(inputValidations)
        }

        binding.apply {
            // hide field by default
            tlLearnerMaternalStatus.visibility = View.GONE

            etLearnerSex.doOnTextChanged { _, _, _, _ ->
                // do NOT use etLearnerSex.getHint below in the if condition
                // since creates race condition upon whether text or hint gets updated first after user interacts
                // causes incorrect behaviour in real life
                if (etLearnerSex.text.toString() == "Female") {
                    tlLearnerMaternalStatus.visibility = View.VISIBLE
                } else {
                    tlLearnerMaternalStatus.visibility = View.GONE
                }
            }

        }

        when (learnerManagementViewModel.currentDetailsMode) {
            DetailsMode.VIEW -> {
                makeFieldsViewOnly()
                bindData()

                binding.btSave.visibility = View.GONE
                binding.btEdit.visibility = View.VISIBLE
                binding.btEdit.setOnClickListener {
                    learnerManagementViewModel.currentDetailsMode = DetailsMode.EDIT
                    Navigation.findNavController(binding.root).navigate(
                        LearnerProfileFragmentDirections.actionLearnerProfileFragmentToLearnerProfileDetailsFragment(
                            null
                        )
                    )
                }

                binding.btRemove.visibility = View.VISIBLE
                binding.btRemove.setOnClickListener {
                    Navigation.findNavController(binding.root).navigate(
                        LearnerProfileFragmentDirections.actionLearnerProfileFragmentToLearnerProfileRemoveFragment()
                    )
                }
            }
            DetailsMode.NEW -> {
                val bundle = requireArguments()
                val args = LearnerProfileDetailsFragmentArgs.fromBundle(bundle)
                if (!args.schoolGroupUuid.isNullOrEmpty()) {
                    schoolGroupUuid = args.schoolGroupUuid!!
                    val classroomList =
                        schoolGroupManagementViewModel.getSchoolGroupOptionsList(learnerManagementViewModel.currentLearner.school_uuid)
                    schoolGroupOptionItem = classroomList.find { it.item_id == schoolGroupUuid }
                }
                loadNewLearnerDefaultDetailsInView()
                ValidationUtils.addLiveValidation(validationsArray)
                makeFieldsEditable()
                //all fields should be empty
                //show save button
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    //save data
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        binding.btSave.isEnabled = false
                        saveLearnerDetailsInView()
                        goBack()
                    }
                }
            }
            DetailsMode.EDIT -> {
                ValidationUtils.addLiveValidation(validationsArray)
                //change title
                loadLearnerDetailsInView(learnerManagementViewModel.currentLearner)
                makeFieldsEditable()
                //show save button
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    //save data
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        binding.btSave.isEnabled = false
                        saveLearnerDetailsInView()
                        goBack()
                    }
                }
            }
            DetailsMode.FROM_SEARCH -> {
                val bundle = requireArguments()
                val args = LearnerProfileDetailsFragmentArgs.fromBundle(bundle)
                if (!args.schoolGroupUuid.isNullOrEmpty()) {
                    schoolGroupUuid = args.schoolGroupUuid!!
                    val classroomList =
                        schoolGroupManagementViewModel.getSchoolGroupOptionsList(learnerManagementViewModel.currentLearner.school_uuid)
                    schoolGroupOptionItem = classroomList.find { it.item_id == schoolGroupUuid }
                }
                loadLearnerDetailsInView(learnerManagementViewModel.currentLearner)
                ValidationUtils.addLiveValidation(validationsArray)
                makeFieldsEditable()
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        binding.btSave.isEnabled = false
                        val learner = learnerManagementViewModel.currentLearner
                        viewLifecycleOwner.lifecycleScope.launch {
                            val isDuplicate = withContext(Dispatchers.IO) {
                                learnerManagementViewModel.isDuplicateLearnerAtSchool(
                                    learner.school_uuid.orEmpty(),
                                    learner.learner_uuid.orEmpty(),
                                    learner.learner_nin,
                                    learner.learner_id
                                )
                            }
                            if (isDuplicate) {
                                binding.btSave.isEnabled = true
                                Snackbar.make(
                                    binding.root,
                                    "A learner with the same NIN or UID is already enrolled at this school",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            } else {
                                saveLearnerDetailsInView()
                                goBack()
                            }
                        }
                    }
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        setTitle()
    }

    private fun goBack() {
        Navigation.findNavController(binding.root).navigateUp()
    }

    private fun bindData() {
        //set observer
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                learnerManagementViewModel.learnerDetail.collect { item ->
                    when (item) {
                        is LatestLearnerDetailUiState.Success -> {
                            setTitle()
                            loadLearnerDetailsInView(item.learner)
                        }
                        is LatestLearnerDetailUiState.Error -> {
                            val mySnackBar = Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun loadNewLearnerDefaultDetailsInView(idList: List<Int> = listOf()) {
        binding.apply {
            if (idList.isEmpty() || idList.contains(etLearnerId.id)) etLearnerId.setText("")

            if (schoolGroupUuid.isNotEmpty()) {
                if (idList.isEmpty() || idList.contains(etLearnerClassroom.id)) etLearnerClassroom.setText(
                    schoolGroupOptionItem?.item_name ?: ""
                )
                if (idList.isEmpty() || idList.contains(etLearnerClassroom.id)) etLearnerClassroom.setTag(
                    R.string.idTag,
                    schoolGroupOptionItem?.item_id ?: ""
                )
            }
        }
    }

    private fun loadLearnerDetailsInView(learner: LearnerAdmissionModel?, idList: List<Int> = listOf()) {
        binding.apply {

            if (idList.isEmpty() || idList.contains(etLearnerFirstName.id)) etLearnerFirstName.setText(
                learner?.learner_first_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerMiddleName.id)) etLearnerMiddleName.setText(
                learner?.learner_middle_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerLastName.id)) etLearnerLastName.setText(
                learner?.learner_last_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerAdmissionNumber.id)) etLearnerAdmissionNumber.setText(
                learner?.admission_number?.toString() ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerId.id)) etLearnerId.setText(learner?.learner_id ?: "")
            if (idList.isEmpty() || idList.contains(etLearnerNin.id)) etLearnerNin.setText(learner?.learner_nin ?: "")
            if (idList.isEmpty() || idList.contains(etLearnerSex.id)) etLearnerSex.setText(
                learner?.learner_sex_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerSex.id)) etLearnerSex.setTag(
                R.string.idTag,
                learner?.learner_sex_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDob.id)) etLearnerDob.setText(
                learner?.learner_date_of_birth ?: ""
            )
            if (idList.isEmpty() || idList.contains(tvLearnerAgeValue.id)) tvLearnerAgeValue.text =
                learner?.learner_age?.toString() ?: "unknown"
            if (idList.isEmpty() || idList.contains(etLearnerLanguage.id)) etLearnerLanguage.setTag(
                R.string.idTag,
                learner?.language_oid_strongest ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerLanguage.id)) etLearnerLanguage.setText(
                learner?.language_oid_strongest_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerMaternalStatus.id)) etLearnerMaternalStatus.setTag(
                R.string.idTag,
                learner?.learner_maternal_status_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerMaternalStatus.id)) etLearnerMaternalStatus.setText(
                learner?.learner_maternal_status_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerStartDate.id)) etLearnerStartDate.setText(
                learner?.start_date ?: ""
            )

            if (idList.isEmpty() || idList.contains(etLearnerDisabilityVision.id)) etLearnerDisabilityVision.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_vision ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityVision.id)) etLearnerDisabilityVision.setText(
                learner?.learner_disability_severity_oid_vision_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityHearing.id)) etLearnerDisabilityHearing.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_hearing ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityHearing.id)) etLearnerDisabilityHearing.setText(
                learner?.learner_disability_severity_oid_hearing_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityMobility.id)) etLearnerDisabilityMobility.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_mobility ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityMobility.id)) etLearnerDisabilityMobility.setText(
                learner?.learner_disability_severity_oid_mobility_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityCognition.id)) etLearnerDisabilityCognition.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_cognition ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityCognition.id)) etLearnerDisabilityCognition.setText(
                learner?.learner_disability_severity_oid_cognition_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilitySelfcare.id)) etLearnerDisabilitySelfcare.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_selfcare ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilitySelfcare.id)) etLearnerDisabilitySelfcare.setText(
                learner?.learner_disability_severity_oid_selfcare_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityCommunication.id)) etLearnerDisabilityCommunication.setTag(
                R.string.idTag,
                learner?.learner_disability_severity_oid_communication ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityCommunication.id)) etLearnerDisabilityCommunication.setText(
                learner?.learner_disability_severity_oid_communication_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityOtherCondition.id)) etLearnerDisabilityOtherCondition.setTag(
                R.string.idTag,
                learner?.learner_disability_other_condition_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerDisabilityOtherCondition.id)) etLearnerDisabilityOtherCondition.setText(
                learner?.learner_disability_other_condition_name ?: ""
            )

            if (idList.isEmpty() || idList.contains(etLearnerEndDate.id)) etLearnerEndDate.setText(
                learner?.end_date ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerEndReason.id)) etLearnerEndReason.setText(
                learner?.end_reason_learner_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerEndReason.id)) etLearnerEndReason.setTag(
                R.string.idTag,
                learner?.end_reason_learner_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerEndReasonOther.id)) etLearnerEndReasonOther.setText(
                learner?.end_reason_learner_other ?: ""
            )


            if (idList.isEmpty() || idList.contains(etGuardianFirstName.id)) etGuardianFirstName.setText(
                learner?.guardian_first_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianMiddleName.id)) etGuardianMiddleName.setText(
                learner?.guardian_middle_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianLastName.id)) etGuardianLastName.setText(
                learner?.guardian_last_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianNin.id)) etGuardianNin.setText(
                learner?.guardian_nin ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianSex.id)) etGuardianSex.setText(
                learner?.guardian_sex_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianSex.id)) etGuardianSex.setTag(
                R.string.idTag,
                learner?.guardian_sex_oid ?: ""
            )
//            if(idList.isEmpty() || idList.contains(etGuardianDob.id)) etGuardianDob.setText(learner?.guardian_date_of_birth ?: "")
            if (idList.isEmpty() || idList.contains(etGuardianRelationToLearner.id)) etGuardianRelationToLearner.setText(
                learner?.guardian_relation_to_learner_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianRelationToLearner.id)) etGuardianRelationToLearner.setTag(
                R.string.idTag,
                learner?.guardian_relation_to_learner_oid ?: ""
            )
//            if(idList.isEmpty() || idList.contains(etGuardianRelationToLearnerOther.id)) etGuardianRelationToLearnerOther.setText(learner?.guardian_relation_to_learner_other ?: "")

            if (idList.isEmpty() || idList.contains(etGuardianPhone1.id)) etGuardianPhone1.setText(
                learner?.guardian_phone_1 ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianPhone2.id)) etGuardianPhone2.setText(
                learner?.guardian_phone_2 ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianAddress.id)) etGuardianAddress.setText(
                learner?.guardian_address ?: ""
            )
            if (idList.isEmpty() || idList.contains(etGuardianEmail.id)) etGuardianEmail.setText(
                learner?.guardian_email ?: ""
            )

            if (idList.isEmpty() || idList.contains(etLearnerClassroom.id)) etLearnerClassroom.setText(
                learner?.enrolment_school_group_concat_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etLearnerClassroom.id)) etLearnerClassroom.setTag(
                R.string.idTag,
                learner?.enrolment_school_group_uuid ?: ""
            )

            val prevGroup = learner?.prev_enrolment_school_group_concat_name?.trim()
            val prevYear = learner?.prev_enrolment_academic_year_name
            if (!prevGroup.isNullOrEmpty() && !prevYear.isNullOrEmpty()) {
                tlPrevYearGroup.hint = "Previous Year Group ($prevYear)"
                etPrevYearGroup.setText(prevGroup)
                tlPrevYearGroup.visibility = android.view.View.VISIBLE
            } else {
                tlPrevYearGroup.visibility = android.view.View.GONE
            }
        }
    }

    private fun saveLearnerDetailsInView() {
        binding.apply {
            learnerManagementViewModel.currentLearner.apply learner@{

                this@learner.learner_first_name = Utils.trimWhiteSpace(etLearnerFirstName).uppercase()
                this@learner.learner_middle_name = Utils.trimWhiteSpace(etLearnerMiddleName).uppercase()
                this@learner.learner_last_name = Utils.trimWhiteSpace(etLearnerLastName).uppercase()
                this@learner.admission_number = Utils.trimWhiteSpace(etLearnerAdmissionNumber).toIntOrNull()
                this@learner.learner_id = Utils.trimWhiteSpace(etLearnerId)
                this@learner.learner_nin = Utils.trimWhiteSpace(etLearnerNin).uppercase()
                this@learner.learner_sex_name = etLearnerSex.text.toString()
                this@learner.learner_sex_oid = etLearnerSex.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_date_of_birth = etLearnerDob.text.toString()
                this@learner.learner_age = tvLearnerAgeValue.text.toString().toIntOrNull()
                this@learner.language_oid_strongest_name = etLearnerLanguage.text.toString()
                this@learner.language_oid_strongest =
                    etLearnerLanguage.getTag(R.string.idTag)?.toString().orEmpty()
                if (this@learner.learner_sex_oid == "female") {
                    this@learner.learner_maternal_status_name = etLearnerMaternalStatus.text.toString()
                    this@learner.learner_maternal_status_oid =
                        etLearnerMaternalStatus.getTag(R.string.idTag)?.toString().orEmpty()
                } else {
                    this@learner.learner_maternal_status_name = ""
                    this@learner.learner_maternal_status_oid = ""
                    this@learner.learner_maternal_status_updated_at = ""
                }
                this@learner.start_date = etLearnerStartDate.text.toString()

                this@learner.learner_disability_severity_oid_vision_name = etLearnerDisabilityVision.text.toString()
                this@learner.learner_disability_severity_oid_vision =
                    etLearnerDisabilityVision.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_severity_oid_hearing_name = etLearnerDisabilityHearing.text.toString()
                this@learner.learner_disability_severity_oid_hearing =
                    etLearnerDisabilityHearing.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_severity_oid_mobility_name = etLearnerDisabilityMobility.text.toString()
                this@learner.learner_disability_severity_oid_mobility =
                    etLearnerDisabilityMobility.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_severity_oid_cognition_name = etLearnerDisabilityCognition.text.toString()
                this@learner.learner_disability_severity_oid_cognition =
                    etLearnerDisabilityCognition.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_severity_oid_selfcare_name = etLearnerDisabilitySelfcare.text.toString()
                this@learner.learner_disability_severity_oid_selfcare =
                    etLearnerDisabilitySelfcare.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_severity_oid_communication_name = etLearnerDisabilityCommunication.text.toString()
                this@learner.learner_disability_severity_oid_communication =
                    etLearnerDisabilityCommunication.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.learner_disability_other_condition_name = etLearnerDisabilityOtherCondition.text.toString()
                this@learner.learner_disability_other_condition_oid =
                    etLearnerDisabilityOtherCondition.getTag(R.string.idTag)?.toString().orEmpty()

                this@learner.end_date = etLearnerEndDate.text.toString()
                this@learner.end_reason_learner_name = etLearnerEndReason.text.toString()
                this@learner.end_reason_learner_oid = etLearnerEndReason.getTag(R.string.idTag)?.toString().orEmpty()
                this@learner.end_reason_learner_other = Utils.trimWhiteSpace(etLearnerEndReasonOther)


                this@learner.guardian_first_name = Utils.trimWhiteSpace(etGuardianFirstName).uppercase()
                this@learner.guardian_middle_name = Utils.trimWhiteSpace(etGuardianMiddleName).uppercase()
                this@learner.guardian_last_name = Utils.trimWhiteSpace(etGuardianLastName).uppercase()
                this@learner.guardian_nin = Utils.trimWhiteSpace(etGuardianNin).uppercase()
                this@learner.guardian_sex_name = etGuardianSex.text.toString()
                this@learner.guardian_sex_oid = etGuardianSex.getTag(R.string.idTag)?.toString().orEmpty()
//                this@learner.guardian_date_of_birth = etGuardianDob.text.toString()
                this@learner.guardian_relation_to_learner_name = etGuardianRelationToLearner.text.toString()
                this@learner.guardian_relation_to_learner_oid =
                    etGuardianRelationToLearner.getTag(R.string.idTag)?.toString().orEmpty()
//                this@learner.guardian_relation_to_learner_other = Utils.trimWhiteSpace(etGuardianRelationToLearnerOther)

                this@learner.guardian_phone_1 = Utils.trimWhiteSpace(etGuardianPhone1)
                this@learner.guardian_phone_2 = Utils.trimWhiteSpace(etGuardianPhone2)
                this@learner.guardian_address = Utils.trimWhiteSpace(etGuardianAddress)
                this@learner.guardian_email = Utils.trimWhiteSpace(etGuardianEmail)

                this@learner.enrolment_school_group_concat_name = etLearnerClassroom.text.toString()
                this@learner.enrolment_school_group_uuid =
                    etLearnerClassroom.getTag(R.string.idTag)?.toString().orEmpty()

                when (learnerManagementViewModel.currentDetailsMode) {
                    DetailsMode.EDIT -> learnerManagementViewModel.updateLearner(this@learner)
                    DetailsMode.NEW -> learnerManagementViewModel.insertLearner(this@learner)
                    DetailsMode.FROM_SEARCH -> learnerManagementViewModel.addExistingLearnerToSchool(this@learner)
                    else -> {}
                }
            }
        }
    }

    private fun makeFieldsViewOnly() {
        binding.apply {
            tlLearnerSex.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerLanguage.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerMaternalStatus.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerEndReason.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityCognition.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityCommunication.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityHearing.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityMobility.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilitySelfcare.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityVision.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerDisabilityOtherCondition.endIconMode = TextInputLayout.END_ICON_NONE
            tlGuardianSex.endIconMode = TextInputLayout.END_ICON_NONE
            tlGuardianRelationToLearner.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearnerClassroom.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    private fun makeFieldsEditable() {
        binding.apply {
            // old style used to have the whole form as white, but have now migrated to grey background w white fields
//            cvLearnerSection.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
//            cvLearnerClassroomSection.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
//            cvLearnerDisabilitySection.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
//            cvGuardianSection.setCardBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))

            setViewBackgroundColorToEditable(ivGuardianPortrait)

            // GENERAL TEXT FIELDS

            makeTextInputEditable(etLearnerId, tlLearnerId)
            makeTextInputEditable(etLearnerAdmissionNumber, tlLearnerAdmissionNumber)
            makeTextInputEditable(etLearnerFirstName, tlLearnerFirstName)
            makeTextInputEditable(etLearnerMiddleName, tlLearnerMiddleName)
            makeTextInputEditable(etLearnerLastName, tlLearnerLastName)
            makeTextInputEditable(etLearnerNin, tlLearnerNin)
            makeTextInputEditable(etGuardianFirstName, tlGuardianFirstName)
            makeTextInputEditable(etGuardianMiddleName, tlGuardianMiddleName)
            makeTextInputEditable(etGuardianLastName, tlGuardianLastName)
            makeTextInputEditable(etGuardianNin, tlGuardianNin)
            makeTextInputEditable(etGuardianPhone1, tlGuardianPhone1)
            makeTextInputEditable(etGuardianPhone2, tlGuardianPhone2)
            makeTextInputEditable(etGuardianAddress, tlGuardianAddress)
            makeTextInputEditable(etGuardianEmail, tlGuardianEmail)

            // SELECT ONE (DROPDOWNS)

            setViewBackgroundColorToEditable(tlLearnerSex)
            etLearnerSex.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.sex_oid",
                        it,
                        tlLearnerSex.hint.toString(),
                        learnerManagementViewModel.getOptionList("sex"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerLanguage)
            etLearnerLanguage.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.language_oid_strongest",
                        it,
                        tlLearnerLanguage.hint.toString(),
                        learnerManagementViewModel.getOptionList("language"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerMaternalStatus)
            etLearnerMaternalStatus.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_maternal_status_oid",
                        it,
                        tlLearnerMaternalStatus.hint.toString(),
                        learnerManagementViewModel.getOptionList("maternal_status"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityVision)
            etLearnerDisabilityVision.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_vision",
                        it,
                        tlLearnerDisabilityVision.hint.toString() +
                                ": Do they have difficulty seeing, even if wearing glasses?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. The learner struggles to read their exercise books, or the board unless they sit close to it.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityHearing)
            etLearnerDisabilityHearing.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_hearing",
                        it,
                        tlLearnerDisabilityHearing.hint.toString() +
                                ": Do they have difficulty hearing, even if using a hearing aid?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. The learner does not respond to you. You have to use a louder voice, or go closer to them so they can hear.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityMobility)
            etLearnerDisabilityMobility.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_mobility",
                        it,
                        tlLearnerDisabilityMobility.hint.toString() +
                                ": Do they have difficulty walking or climbing steps?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. The learner struggles to move around the school.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityCognition)
            etLearnerDisabilityCognition.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_cognition",
                        it,
                        tlLearnerDisabilityCognition.hint.toString() +
                                ": Do they have difficulty remembering or concentrating?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. The learner needs more time for simple tasks, does not remember tasks or what they have learnt, or cannot concentrate in class.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilitySelfcare)
            etLearnerDisabilitySelfcare.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_selfcare",
                        it,
                        tlLearnerDisabilitySelfcare.hint.toString() +
                                ": Do they have difficulty with self-care, such as washing all over or dressing?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. The learner struggles in daily self-care, such as using the bathroom and feeding themselves.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityCommunication)
            etLearnerDisabilityCommunication.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_severity_oid_communication",
                        it,
                        tlLearnerDisabilityCommunication.hint.toString() +
                                ": Using their usual language, do they have difficulty communicating, for example understanding or being understood?" +
                                System.getProperty("line.separator") + System.getProperty("line.separator") +
                                "e.g. Using their usual language, you often struggle to understand what the learner is telling you, and/or they struggle to understand you.",
                        learnerManagementViewModel.getOptionList("disability_severity"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerDisabilityOtherCondition)
            etLearnerDisabilityOtherCondition.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.learner_disability_other_condition_oid",
                        it,
                        tlLearnerDisabilityOtherCondition.hint.toString(),
                        learnerManagementViewModel.getOptionList("disability_other_condition"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlGuardianSex)
            etGuardianSex.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.guardian_sex_oid",
                        it,
                        tlGuardianSex.hint.toString(),
                        learnerManagementViewModel.getOptionList("sex"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlLearnerEndReason)
            etLearnerEndReason.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.end_reason_learner_oid",
                        it,
                        tlLearnerEndReason.hint.toString(),
                        learnerManagementViewModel.getOptionList("end_reason_learner"),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlGuardianRelationToLearner)
            etGuardianRelationToLearner.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.guardian_relation_to_learner_oid",
                        it,
                        tlGuardianRelationToLearner.hint.toString(),
                        learnerManagementViewModel.getOptionList("guardian_relation_to_learner"),
                        childFragmentManager
                    )
                }
            }

            // DATE PICKERS

            setViewBackgroundColorToEditable(tlLearnerDob)
            etLearnerDob.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 2012, tvLearnerAgeValue)
                }
            }

            setViewBackgroundColorToEditable(tlLearnerStartDate)
            etLearnerStartDate.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 0)
                }
            }

            setViewBackgroundColorToEditable(tlLearnerEndDate)
            etLearnerEndDate.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initDatePickerDialog(requireContext(), it, 0)
                }
            }

            setViewBackgroundColorToEditable(tlLearnerClassroom)
            etLearnerClassroom.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "learner.enrolment_school_group_uuid",
                        it,
                        tlLearnerClassroom.hint.toString(),
                        schoolGroupManagementViewModel.getSchoolGroupOptionsList(learnerManagementViewModel.currentLearner.school_uuid),
                        childFragmentManager
                    )
                }
            }
        }
    }

    fun setTitle() {
        learnerManagementViewModel.apply {
            val title = when (this.currentDetailsMode) {
                DetailsMode.VIEW -> (requireActivity() as AppCompatActivity).supportActionBar?.title
                DetailsMode.EDIT -> "${this.currentLearner.learner_full_name} (Edit Mode)"
                DetailsMode.NEW -> "Add New Learner"
                DetailsMode.FROM_SEARCH -> "${this.currentLearner.learner_full_name} (Add to School)"
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
        }
    }

    private fun confirmDiscard() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage("Discard any changes made?")
            .setTitle("Confirmation")

        builder.setPositiveButton("OK") { _, _ ->
            goBack()
        }
        builder.setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}