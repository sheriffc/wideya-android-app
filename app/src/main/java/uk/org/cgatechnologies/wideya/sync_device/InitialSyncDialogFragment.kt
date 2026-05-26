package uk.org.cgatechnologies.wideya.sync_device

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.databinding.DialogFragmentInitialSyncBinding
import uk.org.cgatechnologies.wideya.sync_device.adapters.LogSyncListAdapter

class InitialSyncDialogFragment : DialogFragment() {
    private var _binding: DialogFragmentInitialSyncBinding? = null
    val binding get() = _binding!!
    private val syncViewModel by activityViewModels<SyncViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFragmentInitialSyncBinding.inflate(layoutInflater)

        if (arguments != null) {
            val textLoading = arguments?.getString("text")
            if(textLoading.isNullOrEmpty().not()){
                binding.tvLoading.text = textLoading
            }
        }

        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.apply {
            isCancelable = false
        }

        return builder.setView(binding.root).create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val logSyncListAdapter = LogSyncListAdapter()
        val recyclerView: RecyclerView = binding.rvSyncLog

        with(recyclerView) {
            this.setHasFixedSize(true)
            this.layoutManager = LinearLayoutManager(this.context)
            this.adapter = logSyncListAdapter
        }

        this.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                syncViewModel.logSyncList.collect { logSyncList->
                    when (logSyncList){
                        is LatestLogSyncListUiState.Success -> {
                            logSyncListAdapter.setItems(logSyncList.logSyncList)
                        }
                        is LatestLogSyncListUiState.Error -> {
                            val mySnackBar = Snackbar.make(binding.root, logSyncList.exception.message.toString(), 5000)
                            mySnackBar.show()
                        }
                    }
                }
            }
        }


        return super.onCreateView(inflater, container, savedInstanceState)
    }

    companion object {
        const val TAG = "InitialSyncDialogFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}