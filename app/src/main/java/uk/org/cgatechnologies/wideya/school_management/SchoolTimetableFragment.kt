package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.FragmentSchoolTimetableBinding
import uk.org.cgatechnologies.wideya.databinding.ItemTeacherNameBinding
import uk.org.cgatechnologies.wideya.school_management.adapter.SchoolTimetableAdapter
import uk.org.cgatechnologies.wideya.school_management.models.SchoolTimetableModel

/**
 * Created by Mohamad Abuzaid on 1/18/2023.
 */

class SchoolTimetableFragment : Fragment() {
    private var _binding: FragmentSchoolTimetableBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private lateinit var sundayAdapter: SchoolTimetableAdapter
    private lateinit var mondayAdapter: SchoolTimetableAdapter
    private lateinit var tuesdayAdapter: SchoolTimetableAdapter
    private lateinit var wednesdayAdapter: SchoolTimetableAdapter
    private lateinit var thursdayAdapter: SchoolTimetableAdapter
    private lateinit var fridayAdapter: SchoolTimetableAdapter
    private lateinit var saturdayAdapter: SchoolTimetableAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchoolTimetableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        initObservers()
        initRecyclers()
        schoolManagementViewModel.getSchoolTimetableList(schoolManagementViewModel.currentSchool.uuid)
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.timetableListFlow.collect { item ->
                    when (item) {
                        is SchoolTimetableListUiState.Loading -> {
                            binding.progressBar.isVisible = true
                        }
                        is SchoolTimetableListUiState.Success -> {
                            binding.progressBar.isVisible = false

                            loadSchoolTimetableInView(item.timetableList)
                        }
                        is SchoolTimetableListUiState.Error -> {
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
        sundayAdapter = SchoolTimetableAdapter()
        mondayAdapter = SchoolTimetableAdapter()
        tuesdayAdapter = SchoolTimetableAdapter()
        wednesdayAdapter = SchoolTimetableAdapter()
        thursdayAdapter = SchoolTimetableAdapter()
        fridayAdapter = SchoolTimetableAdapter()
        saturdayAdapter = SchoolTimetableAdapter()

        binding.rvSunday.adapter = sundayAdapter
        binding.rvMonday.adapter = mondayAdapter
        binding.rvTuesday.adapter = tuesdayAdapter
        binding.rvWednesday.adapter = wednesdayAdapter
        binding.rvThursday.adapter = thursdayAdapter
        binding.rvFriday.adapter = fridayAdapter
        binding.rvSaturday.adapter = saturdayAdapter
    }

    private fun loadSchoolTimetableInView(timetableList: List<SchoolTimetableModel>) {
        binding.tvEmpty.isVisible = timetableList.isEmpty()

        val sundays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.sunday), true) }
        val sundaysMap = sundays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val sunTeachers = sundays.map { it.teacher_full_name }.distinct()
        if (sundays.isNotEmpty()) {
            binding.llSunday.visibility = View.VISIBLE

            sunTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llSundayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            sundayAdapter.updateTimetableList(sundaysMap.toSortedMap(compareBy { it }), sunTeachers)
        } else {
            binding.llSunday.visibility = View.GONE
        }

        val mondays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.monday), true) }
        val mondaysMap = mondays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val monTeachers = mondays.map { it.teacher_full_name }.distinct()
        if (mondays.isNotEmpty()) {
            binding.llMonday.visibility = View.VISIBLE
            monTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llMondayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            mondayAdapter.updateTimetableList(mondaysMap.toSortedMap(compareBy { it }), monTeachers)
        } else {
            binding.llMonday.visibility = View.GONE
        }

        val tuesdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.tuesday), true) }
        val tuesdaysMap = tuesdays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val tuesTeachers = tuesdays.map { it.teacher_full_name }.distinct()
        if (tuesdays.isNotEmpty()) {
            binding.llTuesday.visibility = View.VISIBLE
            tuesTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llTuesdayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            tuesdayAdapter.updateTimetableList(tuesdaysMap.toSortedMap(compareBy { it }), tuesTeachers)
        } else {
            binding.llTuesday.visibility = View.GONE
        }

        val wednesdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.wednesday), true) }
        val wednesdaysMap = wednesdays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val wednesTeachers = wednesdays.map { it.teacher_full_name }.distinct()
        if (wednesdays.isNotEmpty()) {
            binding.llWednesday.visibility = View.VISIBLE
            wednesTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llWednesdayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            wednesdayAdapter.updateTimetableList(wednesdaysMap.toSortedMap(compareBy { it }), wednesTeachers)
        } else {
            binding.llWednesday.visibility = View.GONE
        }

        val thursdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.thursday), true) }
        val thursdaysMap = thursdays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val thursTeachers = thursdays.map { it.teacher_full_name }.distinct()
        if (thursdays.isNotEmpty()) {
            binding.llThursday.visibility = View.VISIBLE
            thursTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llThursdayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            thursdayAdapter.updateTimetableList(thursdaysMap.toSortedMap(compareBy { it }), thursTeachers)
        } else {
            binding.llThursday.visibility = View.GONE
        }

        val fridays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.friday), true) }
        val fridaysMap = fridays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val friTeachers = fridays.map { it.teacher_full_name }.distinct()
        if (fridays.isNotEmpty()) {
            binding.llFriday.visibility = View.VISIBLE
            friTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llFridayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            fridayAdapter.updateTimetableList(fridaysMap.toSortedMap(compareBy { it }), friTeachers)
        } else {
            binding.llFriday.visibility = View.GONE
        }

        val saturdays = timetableList.filter { it.day_of_the_week_name.equals(getString(R.string.saturday), true) }
        val saturdaysMap = saturdays.groupBy { "${it.start_time!!}-${it.end_time!!}" }
        val saturTeachers = saturdays.map { it.teacher_full_name }.distinct()
        if (saturdays.isNotEmpty()) {
            binding.llSaturday.visibility = View.VISIBLE
            saturTeachers.forEach { name ->
                val teacherNameBinding = ItemTeacherNameBinding.inflate(LayoutInflater.from(requireContext()))
                teacherNameBinding.tvSubjectTeacher.text = name
                binding.llSaturdayNames.addView(teacherNameBinding.root)
                teacherNameBinding.root.setOnClickListener { onTeacherNameSelected(name, timetableList) }
            }
            saturdayAdapter.updateTimetableList(saturdaysMap.toSortedMap(compareBy { it }), saturTeachers)
        } else {
            binding.llSaturday.visibility = View.GONE
        }
    }

    private fun onTeacherNameSelected(name: String?, timetableList: List<SchoolTimetableModel>) {
        val teacherId = timetableList.find { it.teacher_full_name == name }?.teacher_uuid
        teacherId?.let {
            lifecycleScope.launch(Dispatchers.Main) {
                val teacherModel = schoolManagementViewModel.getSelectedTeacherByIdAsync(it).await()
                val bundle = Bundle().apply {
                    putParcelable("teacher", teacherModel)
                }
                findNavController().navigate(R.id.action_SchoolTimetableFragment_to_TeacherTimetableFragment, bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SchoolTimetableFragment"
    }
}