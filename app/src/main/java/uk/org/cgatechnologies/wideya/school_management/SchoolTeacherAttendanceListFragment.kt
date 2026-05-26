package uk.org.cgatechnologies.wideya.school_management

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
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
import uk.org.cgatechnologies.wideya.databinding.FragmentTeacherAttendanceListBinding
import uk.org.cgatechnologies.wideya.school_management.adapter.AttendanceDateClassArrayAdapter
import uk.org.cgatechnologies.wideya.school_management.NoSchoolDialogFragment
import java.time.LocalDate

private const val TAG: String = "SchoolTeacherAttendanceListFragment"

class SchoolTeacherAttendanceListFragment : Fragment() {

    private var _binding: FragmentTeacherAttendanceListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private var invalidDateAlertDialog: AlertDialog? = null
    private var submissionReminderAlertDialog: AlertDialog? = null

    private lateinit var commonListAdapter: CommonListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherAttendanceListBinding.inflate(inflater, container, false)
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

        commonListAdapter = CommonListAdapter()
        commonListAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.ALLOW
        val recyclerView: RecyclerView = binding.rvMain

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        binding.btnSubmit.setOnClickListener {
            submitTeacherAttendance()
        }

        binding.btnNoSchool.setOnClickListener {
            NoSchoolDialogFragment().show(parentFragmentManager, NoSchoolDialogFragment.TAG)
        }

        initObservers()
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolTeacherAttendanceList.collect { teacherAttendanceList ->
                    binding.loadingProgress.isVisible = false
                    when (teacherAttendanceList) {
                        is LatestSchoolTeacherAttendanceListUiState.Loading -> {
                            binding.loadingProgress.isVisible = true
                        }
                        is LatestSchoolTeacherAttendanceListUiState.Success -> {
                            Log.d(TAG, "Teacher Attendances Found")
                            binding.loadingProgress.isVisible = false

                            commonListAdapter.setType(CommonListAdapter.SCHOOL_TEACHER_ATTENDANCE)
                            commonListAdapter.setItems(teacherAttendanceList.teacherAttendanceList)
                            commonListAdapter.setSchoolManagementViewModel(schoolManagementViewModel)
                            commonListAdapter.setFragmentManager(childFragmentManager)

                            val attendanceProgress =
                                schoolManagementViewModel.calculateTeacherAttendanceCompleteness(teacherAttendanceList.teacherAttendanceList)
                            binding.apply {
                                progressBar.progress = attendanceProgress.percentComplete
                                tvCompletenessPercent.text = getString(
                                    R.string.percentage,
                                    attendanceProgress.percentComplete
                                )

                                if (schoolManagementViewModel.isTeacherAttendanceDateSelectInitialized() &&
                                    teacherAttendanceList.teacherAttendanceList.isEmpty()
                                ) {
                                    rvMain.visibility = View.GONE
                                    emptyAttendanceBanner.root.visibility = View.VISIBLE
                                } else {
                                    rvMain.visibility = View.VISIBLE
                                    emptyAttendanceBanner.root.visibility = View.GONE

                                    try {
                                        Log.d(
                                            TAG,
                                            "goto position : ${schoolManagementViewModel.teacherAttendanceRecyclerPosition}"
                                        )
                                        rvMain.scrollToPosition(schoolManagementViewModel.teacherAttendanceRecyclerPosition)
                                    } catch (e: Exception) {
                                        Log.e(TAG, e.message.toString())
                                        schoolManagementViewModel.teacherAttendanceRecyclerPosition = 0
                                        rvMain.scrollToPosition(
                                            schoolManagementViewModel
                                                .teacherAttendanceRecyclerPosition
                                        )
                                    }
                                }

                                tvCompletenessTally.text =
                                    getString(
                                        R.string.divide,
                                        attendanceProgress.countMarkedAttendance,
                                        attendanceProgress.countAllPersons
                                    )

                                if (attendanceProgress.percentComplete == 100) {
                                    if (schoolManagementViewModel
                                            .isTeacherAttendanceListSubmitted(teacherAttendanceList.teacherAttendanceList)
                                    ) {
                                        btnSubmit.isEnabled = false
                                        submittedAttendanceBanner.root.isVisible = true
                                        submissionReminderAlertDialog?.dismiss()

                                    } else if (schoolManagementViewModel.isSelectedTeacherAttendanceCurrentDate()) {
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
                        is LatestSchoolTeacherAttendanceListUiState.Error -> {
                            binding.loadingProgress.isVisible = false

                            val mySnackBar =
                                Snackbar.make(binding.root, teacherAttendanceList.exception.message.toString(), 5000)
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
                    schoolManagementViewModel.getAttendanceDateList(schoolUuid, Constants.TEACHER_ENTITY_ID)
                val adapter = AttendanceDateClassArrayAdapter(requireContext(), attendanceDateList)
                val autoCompleteTextView: AutoCompleteTextView = binding.attendanceDatePicker

                autoCompleteTextView.apply {
                    setAdapter(adapter)
                    setOnItemClickListener { _, _, i, _ ->
                        val selectedDate = attendanceDateList[i]
                        schoolManagementViewModel.selectedTeacherAttendanceDate = selectedDate
                        if (schoolManagementViewModel.isSelectedTeacherAttendanceCurrentDate()) {
                            Log.d(TAG, "Set Attendance From Launch")
                            schoolManagementViewModel.setSchoolTeacherAttendanceList(schoolUuid)
                        } else {
                            Log.d(TAG, "Set Attendance By Date")
                            schoolManagementViewModel.setSchoolTeacherAttendanceListByDate(
                                schoolUuid,
                                selectedDate.date
                            )
                        }

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
        schoolManagementViewModel.insertBlankPersonsAttendanceListForToday(Constants.TEACHER_ENTITY_ID)

        initTeacherAttendance()
    }

    private fun initTeacherAttendance() {
        if (!schoolManagementViewModel.isTeacherAttendanceDateSelectInitialized()) {
            Log.d(TAG, "Set Teachers Attendance From Init")
//            binding.loadingProgress.isVisible = true
            schoolManagementViewModel.setSchoolTeacherAttendanceList(schoolManagementViewModel.currentSchool.uuid)
            binding.tvCurrentDate.text = Utils.getReadableCurrentDayDate()
        } else binding.tvCurrentDate.text = schoolManagementViewModel.selectedTeacherAttendanceDate.date_text

        if (schoolManagementViewModel.isDeviceDateInvalid()) {
            showInvalidDateAlertDialog()
        } else {
            if (schoolManagementViewModel.isInvalidDeviceDateInitialized()
                && schoolManagementViewModel.isIncorrectTeacherAttendanceDateDialogInitialized()
            ) {
                invalidDateAlertDialog?.dismiss()
                schoolManagementViewModel.incorrectTeacherAttendanceDate = null
            }
        }
    }

    private fun submitTeacherAttendance() {
        schoolManagementViewModel.submitPersonAttendanceList(Constants.TEACHER_ENTITY_ID)
        binding.submittedAttendanceBanner.root.visibility = View.VISIBLE
    }

    private fun showAttendanceSubmissionReminderDialog() {
        submissionReminderAlertDialog?.dismiss()

        val builder = AlertDialog.Builder(requireContext())
        builder.apply {
            setTitle(R.string.submit_teacher_attendance_title)
            setMessage(R.string.submit_attendance_message)
            setPositiveButton(R.string.submit) { _, _ ->
                submitTeacherAttendance()
            }
            setNegativeButton(R.string.cancel, null)
        }

        submissionReminderAlertDialog = builder.create()
        submissionReminderAlertDialog?.show()
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
        schoolManagementViewModel.incorrectTeacherAttendanceDate = LocalDate.now().toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}