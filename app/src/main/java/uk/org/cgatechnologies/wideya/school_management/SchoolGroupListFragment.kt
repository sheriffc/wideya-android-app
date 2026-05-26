package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.school_group_management.SchoolGroupManagementViewModel
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel

private const val TAG: String = "SchoolListFragment"

class SchoolGroupListFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()
    private val schoolGroupManagementViewModel by activityViewModels<SchoolGroupManagementViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommonListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        if(schoolManagementViewModel.isCurrentSchoolInitialized().not()) findNavController().navigate(R.id.HomeFragment)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolDetail.collect { item ->
                    when (item) {
                        is LatestSchoolDetailUiState.Success -> {
                            item.school?.let {
                                bindData(view)
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

    private fun bindData(view: View) {
        setTitle()

        schoolManagementViewModel.setSchoolGroupList(schoolManagementViewModel.currentSchool.uuid)

        val commonListAdapter = CommonListAdapter()
        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolGroupList.collect { schoolGroupList ->
                    when (schoolGroupList) {
                        is LatestSchoolGroupListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestSchoolGroupListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.GROUP)
                            commonListAdapter.setItems(schoolGroupList.schoolGroupList)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = schoolGroupList.schoolGroupList.size.toString()
                            }
                        }
                        is LatestSchoolGroupListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar =
                                Snackbar.make(binding.root, schoolGroupList.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }

        binding.btAdd.setOnClickListener {
            //initialise a new schoolGroup at school
            schoolGroupManagementViewModel.currentDetailsMode = DetailsMode.NEW
            schoolGroupManagementViewModel.currentSchoolGroup = SchoolGroupModel()
            schoolGroupManagementViewModel.currentSchoolGroup.apply {
                school_uuid = schoolManagementViewModel.currentSchool.uuid
                school_name = schoolManagementViewModel.currentSchool.name
                school_education_level_oid = schoolManagementViewModel.currentSchool.education_level_oid

            }
            Navigation.findNavController(binding.root)
                .navigate(SchoolProfileFragmentDirections.actionSchoolProfileFragmentToSchoolGroupProfileDetailsFragment())
        }

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        schoolManagementViewModel.setSchoolGroupListByQuery(
                            schoolManagementViewModel.currentSchool.uuid,
                            newText
                        )
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    schoolManagementViewModel.setSchoolGroupListByQuery(
                        schoolManagementViewModel.currentSchool.uuid,
                        query
                    )
                    return true
                }
            })
        }
    }

    fun setTitle() {
        schoolManagementViewModel.apply {
            (requireActivity() as AppCompatActivity).supportActionBar?.apply {
                title = schoolManagementViewModel.currentSchool.name
                subtitle = null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}