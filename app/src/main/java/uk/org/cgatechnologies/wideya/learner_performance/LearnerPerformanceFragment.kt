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
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentLearnerPerformanceBinding
import uk.org.cgatechnologies.wideya.learner_performance.models.LearnerListItemModel
import uk.org.cgatechnologies.wideya.learner_performance.models.SchoolGroupDropdownModel
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel

class LearnerPerformanceFragment : Fragment() {

    private var _binding: FragmentLearnerPerformanceBinding? = null
    private val binding get() = _binding!!

    private val schoolVm by activityViewModels<SchoolManagementViewModel>()
    private val vm by activityViewModels<LearnerPerformanceViewModel>()

    private var groupList: List<SchoolGroupDropdownModel> = emptyList()
    private val classFilterItems = mutableListOf<String>()

    private val learnerItems = mutableListOf<LearnerListItemModel>()
    private lateinit var adapter: LearnerPerformanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnerPerformanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LearnerPerformanceAdapter(learnerItems) { learner ->
            vm.currentLearner = learner
            findNavController().navigate(R.id.action_SchoolProfileFragment_to_LearnerAssessmentFragment)
        }
        binding.rvLearners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLearners.adapter = adapter

        val school = schoolVm.currentSchool
        val acYear = Utils.getAcademicYear().toShort()

        vm.loadSchoolGroups(school.uuid, acYear)
        vm.loadAllLearners(school.uuid, acYear)

        observeViewModels()
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            vm.schoolGroups.collect { groups ->
                groupList = groups
                classFilterItems.clear()
                classFilterItems.add("All Classes")
                classFilterItems.addAll(groups.map { it.school_group_name })
                binding.actvClass.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, classFilterItems)
                )
                if (binding.actvClass.text.isNullOrEmpty()) {
                    binding.actvClass.setText("All Classes", false)
                }
                binding.actvClass.setOnItemClickListener { _, _, position, _ ->
                    val school = schoolVm.currentSchool
                    val acYear = Utils.getAcademicYear().toShort()
                    if (position == 0) {
                        vm.loadAllLearners(school.uuid, acYear)
                    } else {
                        val groupUuid = groupList.getOrNull(position - 1)?.uuid ?: return@setOnItemClickListener
                        vm.loadLearnersByGroup(groupUuid, acYear)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.learnerList.collect { learners ->
                if (learners.isEmpty()) {
                    binding.rvLearners.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.updateData(learners)
                    binding.rvLearners.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
