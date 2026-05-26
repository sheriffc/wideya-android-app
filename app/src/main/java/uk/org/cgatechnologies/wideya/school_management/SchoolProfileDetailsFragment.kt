package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.makeTextInputEditable
import uk.org.cgatechnologies.wideya.common.utils.Utils.setViewBackgroundColorToEditable
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.PhoneInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.RequiredDialogValidation
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolProfileDetailsBinding
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel

private const val TAG: String = "SchoolProfileDetailsFragment"

private val CLASSROOMS_ITEMS = arrayOf("Permanent", "Temporary")
private val CLASSROOMS_OIDS  = arrayOf("permanent", "temporary")

private val WASH_ITEMS = arrayOf("None", "Water", "Toilet", "Bin")
private val WASH_OIDS  = arrayOf("none", "water", "toilet", "bin")

private val ELECTRICITY_ITEMS = arrayOf("None", "EDSA", "Generator", "Solar", "Power Bank")
private val ELECTRICITY_OIDS  = arrayOf("none", "edsa", "generator", "solar", "power_bank")

private val MNO_ITEMS = arrayOf("None", "Africell", "Orange", "Qcell", "Sierra Tel")
private val MNO_OIDS  = arrayOf("none", "africell", "orange", "qcell", "sierra_tel")

private val LEARNING_ITEMS = arrayOf("Textbooks", "Teaching Aid", "Science Equipment")
private val LEARNING_OIDS  = arrayOf("textbooks", "teaching_aid", "science_equipment")

class SchoolProfileDetailsFragment : Fragment() {

    private var _binding: FragmentSchoolProfileDetailsBinding? = null
    private val binding get() = _binding!!
    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<SchoolModel>("school")
            val id = bundle.getInt("resId")
            // Do something with the result
            loadSchoolDetailsInView(result as SchoolModel, listOf(id))
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //confirm
            when (schoolManagementViewModel.currentDetailsMode) {
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
        _binding = FragmentSchoolProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        binding.apply {
            val inputValidations = setOf(
                RequiredDialogValidation(etEducationLevel, tlEducationLevel),
                PhoneInputValidation(etTabletPhoneNumber, tlTabletPhoneNumber, true),
            )
            validationsArray.clear()
            validationsArray.addAll(inputValidations)

        }
        when (schoolManagementViewModel.currentDetailsMode) {
            DetailsMode.VIEW -> {
                makeFieldsViewOnly()
                bindData()

                binding.btSave.visibility = View.GONE
                binding.btEdit.visibility = View.VISIBLE
                binding.btEdit.setOnClickListener {
                    schoolManagementViewModel.currentDetailsMode = DetailsMode.EDIT
                    Navigation.findNavController(binding.root)
                        .navigate(SchoolProfileFragmentDirections.actionSchoolProfileFragmentToSchoolProfileDetailsFragment())
                }

                binding.btTimetable.apply {
                    visibility = View.VISIBLE
                    setOnClickListener {
                        findNavController().navigate(R.id.action_SchoolProfileFragment_to_SchoolTimetableFragment)
                    }
                }

            }
            DetailsMode.NEW -> {
                // TODO: make new mode another colour as indicator
                //https://developer.android.com/reference/android/content/res/ColorStateList
                //https://m3.material.io/components/floating-action-button/implementation/android
                makeFieldsEditable()
                ValidationUtils.addLiveValidation(validationsArray)
                //all fields should be empty
                //show save button
                schoolManagementViewModel.currentSchool = SchoolModel()

                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveSchoolDetailsInView()
                        goBack()
                    }
                }

                binding.btTimetable.apply {
                    visibility = View.GONE
                }
            }
            DetailsMode.EDIT -> {
                // TODO: make edit mode another colour as indicator
                //change title
                loadSchoolDetailsInView(schoolManagementViewModel.currentSchool)
                makeFieldsEditable()
                ValidationUtils.addLiveValidation(validationsArray)

                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveSchoolDetailsInView()
                        goBack()
                    }
                }

                binding.btTimetable.apply {
                    visibility = View.GONE
                }
            }
            DetailsMode.FROM_SEARCH -> {}

        }
    }

    private fun bindData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolDetail.collect { item ->
                    when (item) {
                        is LatestSchoolDetailUiState.Success -> {
                            item.school?.let {
                                setTitle()
                                loadSchoolDetailsInView(item.school)
                            }
                        }
                        is LatestSchoolDetailUiState.Error -> {
                            Snackbar.make(binding.root, item.exception.message.toString(), 5000).show()
                        }
                    }
                }
            }
        }
    }

    private fun loadSchoolDetailsInView(school: SchoolModel?, idList: List<Int> = listOf()) {
        binding.apply {
            if (idList.isEmpty() || idList.contains(etSchoolName.id)) etSchoolName.setText(school?.name ?: "")
            if (idList.isEmpty() || idList.contains(etEducationLevel.id)) etEducationLevel.setTag(
                R.string.idTag,
                school?.education_level_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEducationLevel.id)) etEducationLevel.setText(
                school?.education_level_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etEmisId.id)) etEmisId.setText(school?.emis_id ?: "")
            if (idList.isEmpty() || idList.contains(etPayrollSid.id)) etPayrollSid.setText(school?.payroll_sid ?: "None")
            if (idList.isEmpty() || idList.contains(etWaecId.id)) etWaecId.setText(school?.waec_id ?: "None")
            if (idList.isEmpty() || idList.contains(etDistrictOffice.id)) etDistrictOffice.setTag(
                R.string.idTag,
                school?.district_office_uuid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etDistrictOffice.id)) etDistrictOffice.setText(
                school?.district_office_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etChiefdom.id)) etChiefdom.setText(school?.chiefdom_name ?: "")
            if (idList.isEmpty() || idList.contains(etSection.id)) etSection.setText(school?.section_name ?: "")
            if (idList.isEmpty() || idList.contains(etTown.id)) etTown.setText(school?.town_name ?: "")
            if (idList.isEmpty() || idList.contains(etAddress.id)) etAddress.setText(school?.address ?: "")
            if (idList.isEmpty() || idList.contains(etClassrooms.id)) {
                etClassrooms.setTag(R.string.idTag, school?.classrooms_oid)
                etClassrooms.setText(oidsToNames(school?.classrooms_oid, CLASSROOMS_OIDS, CLASSROOMS_ITEMS))
            }
            if (idList.isEmpty() || idList.contains(etWash.id)) {
                etWash.setTag(R.string.idTag, school?.wash_oids)
                etWash.setText(oidsToNames(school?.wash_oids, WASH_OIDS, WASH_ITEMS))
            }
            if (idList.isEmpty() || idList.contains(etElectricity.id)) {
                etElectricity.setTag(R.string.idTag, school?.electricity_oids)
                etElectricity.setText(oidsToNames(school?.electricity_oids, ELECTRICITY_OIDS, ELECTRICITY_ITEMS))
            }
            if (idList.isEmpty() || idList.contains(etMno.id)) {
                etMno.setTag(R.string.idTag, school?.mno_oids)
                etMno.setText(oidsToNames(school?.mno_oids, MNO_OIDS, MNO_ITEMS))
            }
            if (idList.isEmpty() || idList.contains(etLearningMaterials.id)) {
                etLearningMaterials.setTag(R.string.idTag, school?.learning_materials_oids)
                etLearningMaterials.setText(oidsToNames(school?.learning_materials_oids, LEARNING_OIDS, LEARNING_ITEMS))
            }
            if (idList.isEmpty() || idList.contains(etTabletPhoneNumber.id)) etTabletPhoneNumber.setText(school?.tablet_phone_number ?: "")
            if (idList.isEmpty()) {
                when (school?.receives_feeding) {
                    1    -> binding.toggleReceivesFeeding.check(binding.btnFeedingYes.id)
                    0    -> binding.toggleReceivesFeeding.check(binding.btnFeedingNo.id)
                    else -> binding.toggleReceivesFeeding.clearChecked()
                }
            }
        }
    }

    private fun saveSchoolDetailsInView() {
        binding.apply {
            schoolManagementViewModel.currentSchool.apply school@{
                // most of the school fields are non-editable, so don't save them just in case
                this@school.education_level_oid = etEducationLevel.getTag(R.string.idTag).toString()
                this@school.education_level_name = etEducationLevel.text.toString()
                this@school.address = Utils.trimWhiteSpace(etAddress)
                this@school.classrooms_oid = etClassrooms.getTag(R.string.idTag)?.toString()
                this@school.wash_oids = etWash.getTag(R.string.idTag)?.toString()
                this@school.electricity_oids = etElectricity.getTag(R.string.idTag)?.toString()
                this@school.mno_oids = etMno.getTag(R.string.idTag)?.toString()
                this@school.learning_materials_oids = etLearningMaterials.getTag(R.string.idTag)?.toString()
                this@school.tablet_phone_number = Utils.trimWhiteSpace(etTabletPhoneNumber)
                this@school.receives_feeding = when (binding.toggleReceivesFeeding.checkedButtonId) {
                    binding.btnFeedingYes.id -> 1
                    binding.btnFeedingNo.id  -> 0
                    else                     -> null
                }
//                this@school.name = Utils.trimWhiteSpace(etSchoolName)
//                this@school.emis_id = Utils.trimWhiteSpace(etEmisId)
//                this@school.district_office_name = etDistrictOffice.text.toString()
//                this@school.district_office_uuid = etDistrictOffice.getTag(R.string.idTag).toString()
//                this@school.payroll_sid = Utils.trimWhiteSpace(etPayrollSid)

                when (schoolManagementViewModel.currentDetailsMode) {
                    DetailsMode.EDIT -> schoolManagementViewModel.updateSchool(this@school)
                    DetailsMode.NEW -> schoolManagementViewModel.insertSchool(this@school)
                    else -> {}
                }
            }
        }
    }

    private fun makeFieldsViewOnly() {
        binding.apply {
            tlEducationLevel.endIconMode = TextInputLayout.END_ICON_NONE
            tlDistrictOffice.endIconMode = TextInputLayout.END_ICON_NONE
            tlChiefdom.endIconMode = TextInputLayout.END_ICON_NONE
            tlClassrooms.endIconMode = TextInputLayout.END_ICON_NONE
            tlWash.endIconMode = TextInputLayout.END_ICON_NONE
            tlElectricity.endIconMode = TextInputLayout.END_ICON_NONE
            tlMno.endIconMode = TextInputLayout.END_ICON_NONE
            tlLearningMaterials.endIconMode = TextInputLayout.END_ICON_NONE
            btnFeedingYes.isEnabled = false
            btnFeedingNo.isEnabled = false
        }
    }

    private fun makeFieldsEditable() {
        binding.apply {

            // some fields are locked and can't be edited even when in edit mode

            makeTextInputEditable(etAddress, tlAddress)
            makeTextInputEditable(etTabletPhoneNumber, tlTabletPhoneNumber)

            setViewBackgroundColorToEditable(tlEducationLevel)
            etEducationLevel.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "school.school_education_level_oid",
                        it,
                        tlEducationLevel.hint.toString(),
                        schoolManagementViewModel.getOptionList("school_education_level"),
                        childFragmentManager,
                        "no",
                    )
                }
            }

            tlDistrictOffice.endIconMode = TextInputLayout.END_ICON_NONE

            tlChiefdom.endIconMode = TextInputLayout.END_ICON_NONE

            setViewBackgroundColorToEditable(tlClassrooms)
            etClassrooms.isFocusable = false
            etClassrooms.setOnClickListener {
                showSingleSelectDialog("Classrooms", CLASSROOMS_ITEMS, CLASSROOMS_OIDS, etClassrooms)
            }
            tlClassrooms.setEndIconOnClickListener {
                showSingleSelectDialog("Classrooms", CLASSROOMS_ITEMS, CLASSROOMS_OIDS, etClassrooms)
            }

            setViewBackgroundColorToEditable(tlWash)
            etWash.isFocusable = false
            etWash.setOnClickListener {
                showMultiSelectDialog("WASH Facilities", WASH_ITEMS, WASH_OIDS, etWash, hasNone = true)
            }
            tlWash.setEndIconOnClickListener {
                showMultiSelectDialog("WASH Facilities", WASH_ITEMS, WASH_OIDS, etWash, hasNone = true)
            }

            setViewBackgroundColorToEditable(tlElectricity)
            etElectricity.isFocusable = false
            etElectricity.setOnClickListener {
                showMultiSelectDialog("Electricity", ELECTRICITY_ITEMS, ELECTRICITY_OIDS, etElectricity, hasNone = true)
            }
            tlElectricity.setEndIconOnClickListener {
                showMultiSelectDialog("Electricity", ELECTRICITY_ITEMS, ELECTRICITY_OIDS, etElectricity, hasNone = true)
            }

            setViewBackgroundColorToEditable(tlMno)
            etMno.isFocusable = false
            etMno.setOnClickListener {
                showMultiSelectDialog("MNO (Mobile Network)", MNO_ITEMS, MNO_OIDS, etMno, hasNone = true)
            }
            tlMno.setEndIconOnClickListener {
                showMultiSelectDialog("MNO (Mobile Network)", MNO_ITEMS, MNO_OIDS, etMno, hasNone = true)
            }

            setViewBackgroundColorToEditable(tlLearningMaterials)
            etLearningMaterials.isFocusable = false
            etLearningMaterials.setOnClickListener {
                showMultiSelectDialog("Learning Materials", LEARNING_ITEMS, LEARNING_OIDS, etLearningMaterials, hasNone = false)
            }
            tlLearningMaterials.setEndIconOnClickListener {
                showMultiSelectDialog("Learning Materials", LEARNING_ITEMS, LEARNING_OIDS, etLearningMaterials, hasNone = false)
            }

            btnFeedingYes.isEnabled = true
            btnFeedingNo.isEnabled = true
        }
    }

    private fun showSingleSelectDialog(
        title: String,
        items: Array<String>,
        oids: Array<String>,
        target: TextInputEditText
    ) {
        val currentOid = target.getTag(R.string.idTag)?.toString() ?: ""
        val checkedItem = oids.indexOf(currentOid).takeIf { it >= 0 } ?: -1
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(items, checkedItem) { dialog, which ->
                target.setTag(R.string.idTag, oids[which])
                target.setText(items[which])
                dialog.dismiss()
            }
            .setNegativeButton("Clear") { _, _ ->
                target.setTag(R.string.idTag, null)
                target.setText("")
            }
            .show()
    }

    private fun showMultiSelectDialog(
        title: String,
        items: Array<String>,
        oids: Array<String>,
        target: TextInputEditText,
        hasNone: Boolean
    ) {
        val noneIdx = if (hasNone) oids.indexOf("none") else -1
        val currentOids = target.getTag(R.string.idTag)?.toString()
            ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()
        val checked = BooleanArray(items.size) { oids[it] in currentOids }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                if (hasNone && isChecked && which == noneIdx) {
                    // selecting None clears all others
                    for (i in checked.indices) checked[i] = false
                    checked[noneIdx] = true
                } else if (hasNone && isChecked && which != noneIdx) {
                    // selecting any real item clears None
                    checked[which] = true
                    if (noneIdx >= 0) checked[noneIdx] = false
                } else {
                    checked[which] = isChecked
                }
            }
            .setPositiveButton("OK") { _, _ ->
                val selectedOids = oids.filterIndexed { i, _ -> checked[i] }.joinToString(",")
                val selectedNames = items.filterIndexed { i, _ -> checked[i] }.joinToString(", ")
                target.setTag(R.string.idTag, selectedOids.ifEmpty { null })
                target.setText(selectedNames)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun oidsToNames(oidStr: String?, oids: Array<String>, items: Array<String>): String {
        if (oidStr.isNullOrBlank()) return ""
        return oidStr.split(",")
            .mapNotNull { oid -> oids.indexOf(oid.trim()).takeIf { it >= 0 }?.let { items[it] } }
            .joinToString(", ")
    }

    fun setTitle() {
        schoolManagementViewModel.apply {
            val newTitle = when (this.currentDetailsMode) {
                DetailsMode.VIEW -> schoolManagementViewModel.currentSchool.name
                DetailsMode.EDIT -> "${this.currentSchool.name} (Edit Mode)"
                DetailsMode.NEW -> getString(R.string.adding_new_school)
                DetailsMode.FROM_SEARCH -> ""
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.apply {
                title = newTitle
                subtitle = null
            }
        }
    }

    private fun confirmDiscard() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setTitle(R.string.confirm)
            .setMessage(R.string.dialog_discard)

        builder.setPositiveButton(R.string.ok) { _, _ ->
            goBack()
        }
        builder.setNegativeButton(R.string.cancel, null)
        val dialog = builder.create()
        dialog.show()
    }

    private fun goBack() {
        Navigation.findNavController(binding.root).navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}