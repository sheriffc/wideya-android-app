package uk.org.cgatechnologies.wideya.school_group_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.utils.Utils.makeTextInputEditable
import uk.org.cgatechnologies.wideya.common.utils.Utils.setViewBackgroundColorToEditable
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.RequiredDialogValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.SchoolGroupNameValidation
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolGroupProfileDetailsBinding
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel

private const val TAG: String = "SchoolGroupProfileDetailsFragment"

class SchoolGroupProfileDetailsFragment : Fragment() {

    private var _binding: FragmentSchoolGroupProfileDetailsBinding? = null
    private val binding get() = _binding!!
    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if(schoolGroupManagementViewModel.isCurrentSchoolGroupInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { key, bundle ->
            val result = bundle.getParcelable<SchoolGroupModel>("schoolGroup")
            val id = bundle.getInt("resId")
            // Do something with the result
            loadSchoolGroupDetailsInView(result as SchoolGroupModel, listOf(id))
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            //confirm
            when (schoolGroupManagementViewModel.currentDetailsMode) {
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
        _binding = FragmentSchoolGroupProfileDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            val inputValidations = setOf(
                RequiredDialogValidation(etSchoolGroupLevel, tlSchoolGroupLevel),
                SchoolGroupNameValidation(
                    etSchoolGroupName,
                    tlSchoolGroupName,
                    schoolGroupManagementViewModel.currentSchoolGroup.school_uuid!!,
                    schoolGroupManagementViewModel.currentSchoolGroup.uuid,
                    etSchoolGroupLevel,
                )
            )
            validationsArray.clear()
            validationsArray.addAll(inputValidations)
        }


        when (schoolGroupManagementViewModel.currentDetailsMode) {
            DetailsMode.VIEW -> {
                makeFieldsViewOnly()
                bindData()

                binding.btSave.visibility = View.GONE
                binding.btEdit.visibility = View.VISIBLE
                binding.btEdit.setOnClickListener {
                    schoolGroupManagementViewModel.currentDetailsMode = DetailsMode.EDIT
                    Navigation.findNavController(binding.root)
                        .navigate(SchoolGroupProfileFragmentDirections.actionSchoolGroupProfileFragmentToSchoolGroupProfileDetailsFragment())
                }

                binding.btRemove.visibility = View.VISIBLE
                binding.btRemove.setOnClickListener {
                    val builder = AlertDialog.Builder(requireContext())

                    builder.setMessage("Are you sure you want to delete this classroom? Associated learners will NOT be deleted, but they will need to be assigned to a new classroom afterwards")
                        .setTitle("Delete Classroom?")

                    builder.setPositiveButton("Yes") { _, _ ->
                        //delete
//                        schoolGroupManagementViewModel.deleteSchoolGroup(schoolGroupManagementViewModel.currentSchoolGroup)
                        schoolGroupManagementViewModel.softDeleteSchoolGroup(
                            schoolGroupManagementViewModel.currentSchoolGroup
                        )
                        Navigation.findNavController(binding.root).navigateUp()
                    }
                    builder.setNegativeButton("Cancel") { _, _ ->
                        // User cancelled the dialog
                    }
                    val dialog = builder.create()
                    dialog.show()
                }
            }
            DetailsMode.NEW -> {
                loadNewSchoolGroupDetailsInView(schoolGroupManagementViewModel.currentSchoolGroup)
                makeFieldsEditable()
                ValidationUtils.addLiveValidation(validationsArray)
                //all fields should be empty
                //show save button
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    //save data
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveSchoolGroupDetailsInView()
                        goBack()
                    }
                }
            }
            DetailsMode.EDIT -> {

                //change title
                loadSchoolGroupDetailsInView(schoolGroupManagementViewModel.currentSchoolGroup)
                makeFieldsEditable()
                ValidationUtils.addLiveValidation(validationsArray)
                //show save button
                binding.btEdit.visibility = View.GONE
                binding.btSave.visibility = View.VISIBLE
                binding.btSave.setOnClickListener {
                    //save data
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        saveSchoolGroupDetailsInView()
                        goBack()
                    }
                }
            }
            DetailsMode.FROM_SEARCH -> {}

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
                schoolGroupManagementViewModel.schoolGroupDetail.collect { item ->
                    when (item) {
                        is LatestSchoolGroupDetailUiState.Success -> {
                            loadSchoolGroupDetailsInView(item.schoolGroup)
                        }
                        is LatestSchoolGroupDetailUiState.Error -> {
                            val mySnackBar =
                                Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun loadNewSchoolGroupDetailsInView(
        schoolGroup: SchoolGroupModel?,
        idList: List<Int> = listOf()
    ) {
        binding.apply {
            if (idList.isEmpty() || idList.contains(etAcademicYearName.id)) etAcademicYearName.setText(
                schoolGroup?.academic_year_name ?: Utils.getAcademicYearName()
            )
        }
    }

    private fun loadSchoolGroupDetailsInView(
        schoolGroup: SchoolGroupModel?,
        idList: List<Int> = listOf()
    ) {
        binding.apply {
            if (idList.isEmpty() || idList.contains(etSchoolGroupName.id)) etSchoolGroupName.setText(
                schoolGroup?.school_group_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etAcademicYearName.id)) etAcademicYearName.setText(
                schoolGroup?.academic_year_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etSchoolGroupLevel.id)) etSchoolGroupLevel.setText(
                schoolGroup?.school_group_level_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etSchoolGroupLevel.id)) etSchoolGroupLevel.setTag(
                R.string.idTag,
                schoolGroup?.school_group_level_oid ?: ""
            )
            if (idList.isEmpty() || idList.contains(etTeacherName.id)) etTeacherName.setText(
                schoolGroup?.teacher_full_name ?: ""
            )
            if (idList.isEmpty() || idList.contains(etTeacherName.id)) etTeacherName.setTag(
                R.string.idTag,
                schoolGroup?.teacher_uuid ?: ""
            )
        }
    }

    private fun saveSchoolGroupDetailsInView() {
        binding.apply {
            schoolGroupManagementViewModel.currentSchoolGroup.apply schoolGroup@{
                this@schoolGroup.school_group_name = Utils.trimWhiteSpace(etSchoolGroupName)
                this@schoolGroup.academic_year_name = etAcademicYearName.text.toString()
                this@schoolGroup.school_group_level_oid =
                    etSchoolGroupLevel.getTag(R.string.idTag)?.toString().orEmpty()
                this@schoolGroup.school_group_level_name = etSchoolGroupLevel.text.toString()
                this@schoolGroup.teacher_uuid =
                    etTeacherName.getTag(R.string.idTag)?.toString().orEmpty()
                this@schoolGroup.teacher_full_name = etTeacherName.text.toString()

                when (schoolGroupManagementViewModel.currentDetailsMode) {
                    DetailsMode.EDIT -> schoolGroupManagementViewModel.updateSchoolGroup(this@schoolGroup)
                    DetailsMode.NEW -> schoolGroupManagementViewModel.insertSchoolGroup(this@schoolGroup)
                    else -> {}
                }
            }
        }
    }

    private fun makeFieldsViewOnly() {
        binding.apply {
            tlSchoolGroupLevel.endIconMode = END_ICON_NONE
            tlTeacherName.endIconMode = END_ICON_NONE
        }
    }

    private fun makeFieldsEditable() {
        binding.apply {

            // do not allow editing of academic year; this is auto-managed

            makeTextInputEditable(etSchoolGroupName, tlSchoolGroupName)

            // SELECT ONE (DROPDOWNS)

            setViewBackgroundColorToEditable(tlSchoolGroupLevel)
            etSchoolGroupLevel.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "school_group.school_group_level_oid",
                        it,
                        tlSchoolGroupLevel.hint.toString(),
                        schoolGroupManagementViewModel.getSchoolGroupLevelsBySchoolEducationLevel(
                            schoolGroupManagementViewModel.currentSchoolGroup.school_education_level_oid.orEmpty()
                        ),
                        childFragmentManager
                    )
                }
            }

            setViewBackgroundColorToEditable(tlTeacherName)
            etTeacherName.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "school_group.teacher_uuid",
                        it,
                        tlTeacherName.hint.toString(),
                        schoolGroupManagementViewModel.getTeacherOptionsList(
                            schoolGroupManagementViewModel.currentSchoolGroup.school_uuid.toString()
                        ),
                        childFragmentManager
                    )
                }
            }

        }
    }

    fun setTitle() {
        with(schoolGroupManagementViewModel) {
            val title = when (this.currentDetailsMode) {
                DetailsMode.VIEW -> ((this.currentSchoolGroup.school_group_level_name ?: "")
                        + " " + (this.currentSchoolGroup.school_group_name ?: ""))
                DetailsMode.EDIT -> "${this.currentSchoolGroup.school_group_level_name ?: ""} " +
                        "${this.currentSchoolGroup.school_group_name ?: ""} (Edit Mode)"
                DetailsMode.NEW -> "Add New Classroom"
                DetailsMode.FROM_SEARCH -> ""
            }
            val subtitle = when (this.currentDetailsMode) {
                DetailsMode.VIEW -> this.currentSchoolGroup.school_name
                DetailsMode.EDIT -> ""
                DetailsMode.NEW -> ""
                DetailsMode.FROM_SEARCH -> ""
            }
            (requireActivity() as AppCompatActivity).supportActionBar?.title = title
            (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = subtitle
        }
    }

    private fun confirmDiscard() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage("Discard any changes made?")
            .setTitle("Confirmation")

        builder.setPositiveButton("OK") { _, _ ->
            goBack()
        }
        builder.setNegativeButton("Cancel") { _, _ ->
            // User cancelled the dialog
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}