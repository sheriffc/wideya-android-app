package uk.org.cgatechnologies.wideya.learner_performance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnerAssessmentBinding
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel

class LearnerAssessmentFragment : Fragment() {

    private var _binding: FragmentLearnerAssessmentBinding? = null
    private val binding get() = _binding!!

    private val schoolVm by activityViewModels<SchoolManagementViewModel>()
    private val vm by activityViewModels<LearnerPerformanceViewModel>()

    private val termItems = listOf("First Term", "Second Term", "Third Term")
    private val termOids  = listOf("first_term", "second_term", "third_term")

    private var selectedAssessmentNumber = 0

    private lateinit var adapter: SubjectAssessmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnerAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SubjectAssessmentAdapter(mutableListOf())
        binding.rvSubjects.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSubjects.adapter = adapter

        val learner = vm.currentLearner
        if (learner != null) {
            binding.tvLearnerName.text = learner.learner_name
            binding.tvClassName.text = learner.school_group_name ?: ""
        }

        setupTermDropdown()
        observeSubjectAssessments()
        setupSaveButton()
    }

    private fun setupTermDropdown() {
        binding.actvTerm.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, termItems)
        )
        binding.actvTerm.setOnItemClickListener { _, _, _, _ ->
            val termOid = selectedTermOid() ?: return@setOnItemClickListener

            // Reset assessment type selection and hide subject list
            selectedAssessmentNumber = 0
            binding.actvAssessmentType.setText("", false)
            binding.tlAssessmentType.visibility = View.VISIBLE
            binding.rvSubjects.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Select an assessment"

            // Populate assessment type options based on term
            val isThirdTerm = termOid == "third_term"
            val typeItems = if (isThirdTerm)
                listOf("First Assessment", "Final Exam")
            else
                listOf("First Assessment", "Second Assessment")

            binding.actvAssessmentType.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, typeItems)
            )
            binding.actvAssessmentType.setOnItemClickListener { _, _, position, _ ->
                selectedAssessmentNumber = if (position == 0) 1 else 2
                val label = typeItems[position]
                adapter.setAssessment(selectedAssessmentNumber, label)
                loadSubjectAssessments()
            }
        }
    }

    private fun loadSubjectAssessments() {
        val learner = vm.currentLearner ?: return
        val termOid = selectedTermOid() ?: return
        val acYear  = Utils.getAcademicYear().toShort()
        vm.loadSubjectAssessments(learner, termOid, acYear)
    }

    private fun observeSubjectAssessments() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.subjectAssessments.collect { subjects ->
                // Don't react until both term and assessment type are selected
                if (selectedAssessmentNumber == 0) return@collect

                if (subjects.isEmpty()) {
                    binding.rvSubjects.visibility = View.GONE
                    binding.btnSave.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "No subjects found for this class level"
                } else {
                    val termOid = selectedTermOid()
                    val label = when {
                        selectedAssessmentNumber == 1 -> "First Assessment"
                        termOid == "third_term" -> "Final Exam"
                        else -> "Second Assessment"
                    }
                    adapter.updateData(subjects, selectedAssessmentNumber, label)
                    binding.rvSubjects.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val termOid = selectedTermOid() ?: return@setOnClickListener
            val acYear  = Utils.getAcademicYear().toShort()

            val blankSubjects = adapter.getCurrentItems()
                .filter { item ->
                    if (selectedAssessmentNumber == 1) item.assessment_1_score == null
                    else item.assessment_2_score == null
                }
                .map { it.subject_name }

            if (blankSubjects.isEmpty()) {
                performSave(termOid, acYear)
            } else {
                val subjectList = blankSubjects.joinToString("\n") { "• $it" }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Missing Grades")
                    .setMessage("The following subjects have no grade entered:\n\n$subjectList\n\nSave anyway without grades for these subjects?")
                    .setPositiveButton("Save Anyway") { _, _ -> performSave(termOid, acYear) }
                    .setNegativeButton("Go Back", null)
                    .show()
            }
        }
    }

    private fun performSave(termOid: String, acYear: Short) {
        vm.saveAssessments(
            schoolUuid   = schoolVm.currentSchool.uuid,
            termOid      = termOid,
            academicYear = acYear,
            items        = adapter.getCurrentItems()
        )
        findNavController().popBackStack()
    }

    private fun selectedTermOid(): String? {
        val name = binding.actvTerm.text.toString().trim()
        val idx  = termItems.indexOf(name)
        return if (idx >= 0) termOids[idx] else null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
