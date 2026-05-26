package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.adapters.CommonListAdapter
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.FragmentCommonListBinding
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel

private const val TAG: String = "SchoolListFragment"

class SchoolListFragment : Fragment() {

    private var _binding: FragmentCommonListBinding? = null
    private val binding get() = _binding!!

    private val schoolManagementViewModel by activityViewModels<SchoolManagementViewModel>()

    private val commonListAdapter: CommonListAdapter by lazy { CommonListAdapter() }

    private lateinit var schoolsList: List<String>

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
        Utils.cancelBackButton(activity, viewLifecycleOwner)
        parseAllowedSchoolIds()

        initObservers()

        Log.d(TAG, "Fetching Schools List")
        schoolManagementViewModel.setSchoolList(schoolsList)

        val recyclerView: RecyclerView = binding.rvCommonList

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(view.context)
            this.adapter = commonListAdapter
        }

        // adding new schools is no longer permitted in the app
        binding.btAdd.isVisible = false

        with(binding.svCommonSearchBar) {
            this.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String): Boolean {
                    //debounce
                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(300L)
                        schoolManagementViewModel.setSchoolListByQuery(newText, schoolsList)
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    schoolManagementViewModel.setSchoolListByQuery(query, schoolsList)
                    return true
                }
            })
            this.setOnCloseListener {
                Log.d(TAG, "OnCloseListener: reset")
                schoolManagementViewModel.setSchoolList(schoolsList)
                true
            }
        }
    }

    private fun initObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                schoolManagementViewModel.schoolList.collect { schoolList ->
                    when (schoolList) {
                        is LatestSchoolListUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is LatestSchoolListUiState.Success -> {
                            binding.progressBar.visibility = View.GONE

                            commonListAdapter.setType(CommonListAdapter.SCHOOL)
                            commonListAdapter.setItems(filterSchoolsPermissions(schoolList.schoolList))
                            commonListAdapter.setContext(requireContext())
                            commonListAdapter.setSchoolManagementViewModel(schoolManagementViewModel)

                            binding.apply {
                                countLabel.visibility = View.VISIBLE
                                countTV.visibility = View.VISIBLE
                                binding.countTV.text = schoolList.schoolList.size.toString()
                            }

                        }
                        is LatestSchoolListUiState.Error -> {
                            binding.progressBar.visibility = View.GONE

                            val mySnackBar = Snackbar.make(
                                binding.root,
                                schoolList.exception.message.toString(),
                                5000
                            )
                            mySnackBar.show()
                        }
                    }
                }
            }
        }
    }

    private fun parseAllowedSchoolIds() {
        Log.d(TAG, "parseAllowedSchoolIds")
        val schoolListString =
            Utils.getEncSharedPrefs(requireContext())
                .getString(Constants.PREF_SCOPE_SCHOOLS, "").toString()
        schoolsList = schoolListString.split(",").map { it.trim() }
        Log.d(TAG, "Size: ${schoolsList.size} - Schools List: $schoolsList")
    }

    private fun filterSchoolsPermissions(schoolList: List<SchoolModel>): List<SchoolModel> {
        return schoolList.filter { schoolsList.contains(it.uuid) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}