package uk.org.cgatechnologies.wideya.teacher_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentTeacherTimetableBinding
import uk.org.cgatechnologies.wideya.teacher_management.adapters.TeacherTimetableAdapter
import uk.org.cgatechnologies.wideya.teacher_management.dialogfragments.TimetableEntryDialogFragment
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel

/**
 * Created by Mohamad Abuzaid on 1/5/2023.
 */

class TeacherTimetableFragment : Fragment(), TeacherTimetableAdapter.ITimetableListener {
    private var _binding: FragmentTeacherTimetableBinding? = null
    private val binding get() = _binding!!

    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()

    private lateinit var sundayAdapter: TeacherTimetableAdapter
    private lateinit var mondayAdapter: TeacherTimetableAdapter
    private lateinit var tuesdayAdapter: TeacherTimetableAdapter
    private lateinit var wednesdayAdapter: TeacherTimetableAdapter
    private lateinit var thursdayAdapter: TeacherTimetableAdapter
    private lateinit var fridayAdapter: TeacherTimetableAdapter
    private lateinit var saturdayAdapter: TeacherTimetableAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            val args = TeacherTimetableFragmentArgs.fromBundle(bundle)
            args.teacher?.let { teacherManagementViewModel.currentTeacher = it }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = getString(
            R.string.teacher_timetable_title,
            teacherManagementViewModel.currentTeacher.last_name,
            teacherManagementViewModel.currentTeacher.first_name
        )
        binding.btAddEntry.setOnClickListener {
            promptTimetableEntryDialog(null)
        }

        initObservers()
        initRecyclers()
        teacherManagementViewModel.getTeacherTimetableList(teacherManagementViewModel.currentTeacher.uuid)
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                teacherManagementViewModel.timetableListFlow.collect { item ->
                    when (item) {
                        is TeacherTimetableListUiState.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                        is TeacherTimetableListUiState.Success -> {
                            binding.progressBar.isVisible = false

                            loadTeacherTimetableInView(item.timetableList)
                        }
                        is TeacherTimetableListUiState.Error -> {
                            binding.progressBar.isVisible = false

                            val mySnackBar = Snackbar.make(binding.root, item.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun initRecyclers() {
        sundayAdapter = TeacherTimetableAdapter(this)
        mondayAdapter = TeacherTimetableAdapter(this)
        tuesdayAdapter = TeacherTimetableAdapter(this)
        wednesdayAdapter = TeacherTimetableAdapter(this)
        thursdayAdapter = TeacherTimetableAdapter(this)
        fridayAdapter = TeacherTimetableAdapter(this)
        saturdayAdapter = TeacherTimetableAdapter(this)

        binding.rvSunday.adapter = sundayAdapter
        binding.rvMonday.adapter = mondayAdapter
        binding.rvTuesday.adapter = tuesdayAdapter
        binding.rvWednesday.adapter = wednesdayAdapter
        binding.rvThursday.adapter = thursdayAdapter
        binding.rvFriday.adapter = fridayAdapter
        binding.rvSaturday.adapter = saturdayAdapter
    }

    private fun loadTeacherTimetableInView(timetableList: List<TeacherTimetableModel>) {
        binding.tvEmpty.isVisible = timetableList.isEmpty()

        val sundays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.sunday), true) }
        if (sundays.isNotEmpty()) {
            binding.llSunday.visibility = View.VISIBLE
            sundayAdapter.updateTimetableList(sundays.sortedBy { it.start_time })
        } else {
            binding.llSunday.visibility = View.GONE
        }

        val mondays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.monday), true) }
        if (mondays.isNotEmpty()) {
            binding.llMonday.visibility = View.VISIBLE
            mondayAdapter.updateTimetableList(mondays.sortedBy { it.start_time })
        } else {
            binding.llMonday.visibility = View.GONE
        }

        val tuesdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.tuesday), true) }
        if (tuesdays.isNotEmpty()) {
            binding.llTuesday.visibility = View.VISIBLE
            tuesdayAdapter.updateTimetableList(tuesdays.sortedBy { it.start_time })
        } else {
            binding.llTuesday.visibility = View.GONE
        }

        val wednesdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.wednesday), true) }
        if (wednesdays.isNotEmpty()) {
            binding.llWednesday.visibility = View.VISIBLE
            wednesdayAdapter.updateTimetableList(wednesdays.sortedBy { it.start_time })
        } else {
            binding.llWednesday.visibility = View.GONE
        }

        val thursdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.thursday), true) }
        if (thursdays.isNotEmpty()) {
            binding.llThursday.visibility = View.VISIBLE
            thursdayAdapter.updateTimetableList(thursdays.sortedBy { it.start_time })
        } else {
            binding.llThursday.visibility = View.GONE
        }

        val fridays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.friday), true) }
        if (fridays.isNotEmpty()) {
            binding.llFriday.visibility = View.VISIBLE
            fridayAdapter.updateTimetableList(fridays.sortedBy { it.start_time })
        } else {
            binding.llFriday.visibility = View.GONE
        }

        val saturdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.saturday), true) }
        if (saturdays.isNotEmpty()) {
            binding.llSaturday.visibility = View.VISIBLE
            saturdayAdapter.updateTimetableList(saturdays.sortedBy { it.start_time })
        } else {
            binding.llSaturday.visibility = View.GONE
        }
    }

    override fun onEntryClicked(entry: TeacherTimetableModel) {
        promptTimetableEntryDialog(entry)
    }

    private fun promptTimetableEntryDialog(timetableModel: TeacherTimetableModel?) {
        if (timetableModel != null) {
            teacherManagementViewModel.timetableDetailsMode = DetailsMode.VIEW
            teacherManagementViewModel.currentTimetableEntry = timetableModel
        } else {
            teacherManagementViewModel.timetableDetailsMode = DetailsMode.NEW
            teacherManagementViewModel.currentTimetableEntry = TeacherTimetableModel()
        }

        val timetableEntryDialogFragment: DialogFragment = TimetableEntryDialogFragment()
        timetableEntryDialogFragment.show(
            parentFragmentManager, TimetableEntryDialogFragment.TAG
        )
        timetableEntryDialogFragment.isCancelable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "TeacherProfileDetailsFragment"
    }
}