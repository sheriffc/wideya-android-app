package uk.org.cgatechnologies.wideya.school_management

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnerAttendanceListBinding
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.adapter.AttendanceDateClassArrayAdapter
import uk.org.cgatechnologies.wideya.school_management.adapter.SchoolGroupArrayAdapter
import uk.org.cgatechnologies.wideya.school_management.models.DisplaySchoolGroupModel
import java.time.LocalDate

private const val TAG: String = "SchoolLearnerAttendanceListFragment"

class SchoolLearnerAttendanceListFragment : Fragment() {
    private var _binding: FragmentLearnerAttendanceListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private var invalidDateAlertDialog: AlertDialog? = null
    private var submissionReminderAlertDialog: AlertDialog? = null

    private lateinit var commonListAdapter: CommonListAdapter
    private lateinit var classroomPicker: AutoCompleteTextView

    // 0 = empty, 1 = present (green), 2 = absent (red)
    private var amSelectAllState = 0
    private var pmSelectAllState = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnerAttendanceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.cancelBackButton(activity, viewLifecycleOwner)

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        // temporary migration to handle existing app users (< v13) before we stored user ID in SharedPrefs
        Utils.getSharedPrefsState(requireContext()).apply {
            val spUserId = getString(Constants.PREF_USER_ID, "-1")
            if (spUserId.isNullOrEmpty() || spUserId == "-1" || spUserId == "0") {
                Utils.copyUserIdFromEncSharedPrefsToSharedPrefs(requireContext())
            }
        }

        binding.loadingProgress.isVisible = true

        schoolManagementViewModel.setLearnerAttendanceSchoolGroupOptionList(schoolManagementViewModel.currentSchool.uuid)

        classroomPicker = binding.classroomPicker

        commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvMain

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        binding.tvCurrentDate.text = Utils.getReadableCurrentDayDate()

        binding.btnSubmit.setOnClickListener {
            submitLearnerAttendance()
        }

        setupSelectAllCheckboxes()
        initObservers()
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolLearnerAttendanceList.collect { learnerAttendanceList ->
                    binding.loadingProgress.isVisible = false
                    when (learnerAttendanceList) {
                        is LatestSchoolLearnerAttendanceListUiState.Loading -> {
                            binding.loadingProgress.isVisible = true
                        }
                        is LatestSchoolLearnerAttendanceListUiState.Success -> {
                            Log.d(TAG, "Learner Attendances Found")
                            binding.loadingProgress.isVisible = false

                            val isCurrentDate = schoolManagementViewModel.isSelectedLearnerAttendanceCurrentDate()
                            binding.cbAmSelectAll.isEnabled = isCurrentDate
                            binding.cbPmSelectAll.isEnabled = isCurrentDate

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_LEARNER_ATTENDANCE)
                            commonListAdapter.setItems(learnerAttendanceList.learnerAttendanceList)
                            commonListAdapter.setSchoolManagementViewModel(schoolManagementViewModel)

                            val attendanceList = learnerAttendanceList.learnerAttendanceList
                            val attendanceProgress =
                                schoolManagementViewModel.calculateLearnerAttendanceCompleteness(attendanceList)
                            binding.apply {
                                progressBar.progress = attendanceProgress.percentComplete
                                tvCompletenessPercent.text = getString(
                                    R.string.percentage,
                                    attendanceProgress.percentComplete
                                )

                                if (schoolManagementViewModel.isLearnerAttendanceDateSelectInitialized() && attendanceList.isEmpty()) {
                                    rvMain.visibility = View.GONE
                                    emptyAttendanceBanner.root.visibility = View.VISIBLE
                                } else {
                                    rvMain.visibility = View.VISIBLE
                                    emptyAttendanceBanner.root.visibility = View.GONE
                                }

                                tvCompletenessTally.text = getString(
                                    R.string.divide,
                                    attendanceProgress.countMarkedAttendance,
                                    attendanceProgress.countAllPersons
                                )

                                if (attendanceProgress.percentComplete == 100) {
                                    if (schoolManagementViewModel.isLearnerAttendanceListSubmitted(attendanceList)) {
                                        btnSubmit.isEnabled = false
                                        submittedAttendanceBanner.root.isVisible = true
                                        submissionReminderAlertDialog?.dismiss()

                                    } else if (schoolManagementViewModel.isSelectedLearnerAttendanceCurrentDate()) {
                                        btnSubmit.isEnabled = true
                                        submittedAttendanceBanner.root.isVisible = false
                                        showAttendanceSubmissionReminderDialog()
                                    }

                                } else {
                                    btnSubmit.isEnabled = false
                                    submittedAttendanceBanner.root.isVisible = false
                                    submissionReminderAlertDialog?.dismiss()
                                }
                            }
                        }
                        is LatestSchoolLearnerAttendanceListUiState.Error -> {
                            binding.loadingProgress.isVisible = false

                            val mySnackBar = Snackbar.make(
                                binding.root,
                                learnerAttendanceList.exception.message.toString(),
                                5
                            )
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.attendanceSchoolGroupList.collect { schoolGroupList ->
                    when (schoolGroupList) {
                        is LatestSchoolAttendanceGroupListUiState.Loading -> {
                            binding.loadingProgress.isVisible = true
                        }
                        is LatestSchoolAttendanceGroupListUiState.Success -> {
                            binding.loadingProgress.isVisible = false

                            val schoolGroupDisplayModelList =
                                setSchoolGroupDisplay(schoolGroupList.schoolAttendanceGroupList)
                            val schoolGroupAdapter =
                                SchoolGroupArrayAdapter(requireContext(), schoolGroupDisplayModelList)

                            classroomPicker.apply {
                                setAdapter(schoolGroupAdapter)
                                setOnItemClickListener { _, _, i, _ ->
                                    resetSelectAllCheckboxes()
                                    val schoolUuid = schoolManagementViewModel.currentSchool.uuid
                                    val groupId = schoolGroupDisplayModelList[i].school_group_uuid
                                    if (schoolManagementViewModel.isLearnerAttendanceDateSelectInitialized()
                                        && !schoolManagementViewModel.isSelectedLearnerAttendanceCurrentDate()
                                        && groupId.isNotEmpty()
                                    ) {
                                        schoolManagementViewModel.setSchoolLearnerAttendanceListByDateAndSchoolGroup(
                                            schoolUuid,
                                            schoolManagementViewModel.selectedLearnerAttendanceDate.date, groupId
                                        )
                                    } else if (schoolManagementViewModel.isLearnerAttendanceDateSelectInitialized()
                                        && !schoolManagementViewModel.isSelectedLearnerAttendanceCurrentDate()
                                        && groupId.isEmpty()
                                    ) {
                                        schoolManagementViewModel.setSchoolLearnerAttendanceListByDate(
                                            schoolUuid,
                                            schoolManagementViewModel.selectedLearnerAttendanceDate.date
                                        )
                                    } else fetchLearnerAttendanceList(groupId)
                                    schoolManagementViewModel.selectedGroupId = groupId
                                }
                            }

                        }
                        is LatestSchoolAttendanceGroupListUiState.Error -> {
                            binding.loadingProgress.isVisible = false

                            val mySnackBar = Snackbar.make(
                                binding.root,
                                schoolGroupList.exception.message.toString(),
                                5
                            )
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                val schoolUuid = schoolManagementViewModel.currentSchool.uuid
                val attendanceDateList =
                    schoolManagementViewModel.getAttendanceDateList(schoolUuid, Constants.LEARNER_ENTITY_ID)
                val adapter = AttendanceDateClassArrayAdapter(requireContext(), attendanceDateList)
                val autoCompleteTextView: AutoCompleteTextView = binding.attendanceDatePicker

                autoCompleteTextView.apply {
                    setAdapter(adapter)
                    setOnItemClickListener { _, _, i, _ ->
                        resetSelectAllCheckboxes()
                        val selectedDate = attendanceDateList[i]
                        var schoolGroupUuid = String()
                        if (schoolManagementViewModel.isSchoolGroupSelectedInitialized()) {
                            schoolGroupUuid = schoolManagementViewModel.selectedGroupId
                        }
                        schoolManagementViewModel.selectedLearnerAttendanceDate = selectedDate
                        if (schoolManagementViewModel.isSelectedLearnerAttendanceCurrentDate()) {
                            fetchLearnerAttendanceList(schoolGroupUuid)
                        } else if (schoolManagementViewModel.isSchoolGroupSelectedInitialized() && schoolGroupUuid.isNotEmpty()) {
                            schoolManagementViewModel.setSchoolLearnerAttendanceListByDateAndSchoolGroup(
                                schoolUuid,
                                selectedDate.date,
                                schoolGroupUuid
                            )
                        } else schoolManagementViewModel.setSchoolLearnerAttendanceListByDate(
                            schoolUuid,
                            selectedDate.date
                        )

                        binding.tvCurrentDate.text = selectedDate.date_text
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            subtitle = schoolManagementViewModel.currentSchool.name
        }
        schoolManagementViewModel.insertBlankPersonsAttendanceListForToday(Constants.LEARNER_ENTITY_ID)

        initLearnerAttendance()
    }

    private fun setSchoolGroupDisplay(schoolGroup: List<SchoolGroupModel>): ArrayList<DisplaySchoolGroupModel> {
        val schoolGroupArrayList = ArrayList<DisplaySchoolGroupModel>()

        for (group in schoolGroup) {
            val name = "${group.school_group_level_name ?: ""} ${group.school_group_name ?: ""}"
            schoolGroupArrayList.add(DisplaySchoolGroupModel(name, group.uuid))
        }
        schoolGroupArrayList.sortBy { displaySchoolGroupModel -> displaySchoolGroupModel.school_group_name }
        schoolGroupArrayList.add(0, DisplaySchoolGroupModel("All", String()))
        return schoolGroupArrayList
    }

    private fun fetchLearnerAttendanceList(uuid: String) {
        if (uuid.isEmpty()) {
            schoolManagementViewModel.setSchoolLearnerAttendanceList(
                schoolManagementViewModel.currentSchool.uuid
            )
        } else {
            schoolManagementViewModel.setSchoolLearnerAttendanceListBySchoolGroup(
                uuid,
                schoolManagementViewModel.currentSchool.uuid
            )
        }
    }

    private fun initLearnerAttendance() {
        resetSelectAllCheckboxes()
        if (!schoolManagementViewModel.isSchoolGroupSelectedInitialized() && !schoolManagementViewModel.isLearnerAttendanceDateSelectInitialized()) {
            Log.d(TAG, "Set Learners Attendance From Init")
//            binding.loadingProgress.isVisible = true
            schoolManagementViewModel.setSchoolLearnerAttendanceList(schoolManagementViewModel.currentSchool.uuid)
            binding.tvCurrentDate.text = Utils.getReadableCurrentDayDate()
        } else if (schoolManagementViewModel.isSchoolGroupSelectedInitialized() && !schoolManagementViewModel.isLearnerAttendanceDateSelectInitialized()) {
            fetchLearnerAttendanceList(schoolManagementViewModel.selectedGroupId)
        } else binding.tvCurrentDate.text = schoolManagementViewModel.selectedLearnerAttendanceDate.date_text

        if (schoolManagementViewModel.isDeviceDateInvalid()) {
            showInvalidDateAlertDialog()
        } else {
            if (schoolManagementViewModel.isInvalidDeviceDateInitialized()
                && schoolManagementViewModel.isIncorrectLearnerAttendanceDateDialogInitialized()
            ) {
                invalidDateAlertDialog?.dismiss()
                schoolManagementViewModel.incorrectLearnerAttendanceDate = null
            }
        }
    }

    private fun submitLearnerAttendance() {
        schoolManagementViewModel.submitPersonAttendanceList(Constants.LEARNER_ENTITY_ID)
        binding.submittedAttendanceBanner.root.visibility = View.VISIBLE
    }

    private fun showAttendanceSubmissionReminderDialog() {
        submissionReminderAlertDialog?.dismiss()

        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle(R.string.submit_learner_attendance_title)
            .setMessage(R.string.submit_attendance_message)

        builder.setPositiveButton(R.string.submit) { _, _ ->
            submitLearnerAttendance()
        }
        builder.setNegativeButton(R.string.cancel, null)

        submissionReminderAlertDialog = builder.create()
        submissionReminderAlertDialog?.show()
    }

    private fun setupSelectAllCheckboxes() {
        binding.cbAmSelectAll.setOnClickListener {
            amSelectAllState = (amSelectAllState + 1) % 3
            applySelectAllVisual(binding.cbAmSelectAll, amSelectAllState)
            schoolManagementViewModel.bulkSetLearnerAttendanceSession(
                isAm   = true,
                status = selectAllStateToStatus(amSelectAllState)
            )
        }
        binding.cbPmSelectAll.setOnClickListener {
            pmSelectAllState = (pmSelectAllState + 1) % 3
            applySelectAllVisual(binding.cbPmSelectAll, pmSelectAllState)
            schoolManagementViewModel.bulkSetLearnerAttendanceSession(
                isAm   = false,
                status = selectAllStateToStatus(pmSelectAllState)
            )
        }
    }

    private fun applySelectAllVisual(cb: CheckBox, state: Int) {
        when (state) {
            0 -> {
                cb.isChecked       = false
                cb.buttonTintList  = null
                cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }
            1 -> {
                cb.isChecked      = true
                cb.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.green_dark)
                )
                cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_dark))
            }
            2 -> {
                cb.isChecked      = true
                cb.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.md_theme_light_error)
                )
                cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_error))
            }
        }
    }

    private fun selectAllStateToStatus(state: Int): String? = when (state) {
        1    -> Constants.ATTENDANCE_PRESENT_ID
        2    -> Constants.ATTENDANCE_ABSENT_ID
        else -> null
    }

    private fun resetSelectAllCheckboxes() {
        amSelectAllState = 0
        pmSelectAllState = 0
        applySelectAllVisual(binding.cbAmSelectAll, 0)
        applySelectAllVisual(binding.cbPmSelectAll, 0)
    }

    private fun showInvalidDateAlertDialog() {
        invalidDateAlertDialog?.dismiss()

        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle(getString(R.string.incorrect_device_date))
            setMessage(getString(R.string.incorrect_device_date_subtitle_max_attendance_date))
            setCancelable(false)
            setIcon(R.drawable.ic_baseline_warning_24)
        }
        invalidDateAlertDialog = builder.create()
        invalidDateAlertDialog?.show()
        schoolManagementViewModel.incorrectLearnerAttendanceDate = LocalDate.now().toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}