package uk.org.cgatechnologies.wideya.school_management

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.databinding.DialogNoSchoolBinding
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class NoSchoolDialogFragment : DialogFragment() {

    private var _binding: DialogNoSchoolBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<SchoolManagementViewModel>()

    private var selectedReason: OptionList? = null
    private var selectedStartDate: LocalDate? = null
    private var selectedEndDate: LocalDate? = null

    // Loaded asynchronously; starts as empty (only today constraint active)
    private var blockedMillis: Set<Long> = emptySet()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogNoSchoolBinding.inflate(LayoutInflater.from(requireContext()))

        setupReasonDropdown()
        setupDatePickers()

        // Load blocked dates in background; date picker buttons use the latest value when tapped
        lifecycleScope.launch {
            blockedMillis = withContext(Dispatchers.IO) {
                viewModel.getBlockedDatesMillisForNoSchool()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Mark No School Days")
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton("Confirm", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (validate()) {
                            val reasonOid   = selectedReason!!.item_id.orEmpty()
                            val reasonOther = if (reasonOid == Constants.NO_SCHOOL_OTHER_ITEM_ID)
                                binding.etReasonOther.text?.toString()?.trim() else null
                            viewModel.markNoSchoolDays(
                                reasonOid, reasonOther,
                                selectedStartDate!!.toString(),
                                selectedEndDate!!.toString()
                            )
                            dismiss()
                        }
                    }
                }
            }
    }

    private fun setupReasonDropdown() {
        val reasons = viewModel.getOptionList(Constants.NO_SCHOOL_REASON_LIST_NAME)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            reasons.map { it.item_name.orEmpty() }
        )
        binding.actvReason.setAdapter(adapter)
        binding.actvReason.setOnItemClickListener { _, _, position, _ ->
            selectedReason = reasons[position]
            binding.lyReasonOther.isVisible =
                selectedReason?.item_id == Constants.NO_SCHOOL_OTHER_ITEM_ID
        }
    }

    private fun setupDatePickers() {
        binding.etStartDate.setOnClickListener { showDatePicker(isStart = true) }
        binding.lyStartDate.setEndIconOnClickListener { showDatePicker(isStart = true) }
        binding.etEndDate.setOnClickListener { showDatePicker(isStart = false) }
        binding.lyEndDate.setEndIconOnClickListener { showDatePicker(isStart = false) }
    }

    private fun showDatePicker(isStart: Boolean) {
        val validator = NoSchoolDateValidator(blockedMillis)

        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(validator)
            .setStart(MaterialDatePicker.todayInUtcMilliseconds())

        // For end date, also restrict to >= start date
        if (!isStart && selectedStartDate != null) {
            val startMillis = selectedStartDate!!
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            constraintsBuilder.setStart(startMillis)
        }

        val tag = if (isStart) "start_picker" else "end_picker"
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStart) "Select Start Date" else "Select End Date")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        picker.addOnPositiveButtonClickListener { selectionMillis ->
            val picked = Instant.ofEpochMilli(selectionMillis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()

            if (isStart) {
                selectedStartDate = picked
                binding.etStartDate.setText(picked.toString())
                binding.lyStartDate.error = null
                // Reset end date if it's now before the new start
                if (selectedEndDate != null && selectedEndDate!!.isBefore(picked)) {
                    selectedEndDate = null
                    binding.etEndDate.setText("")
                }
            } else {
                selectedEndDate = picked
                binding.etEndDate.setText(picked.toString())
                binding.lyEndDate.error = null
            }
        }

        picker.show(parentFragmentManager, tag)
    }

    private fun validate(): Boolean {
        var ok = true

        if (selectedReason == null) {
            binding.lyReason.error = "Please select a reason"
            ok = false
        } else {
            binding.lyReason.error = null
        }

        if (selectedReason?.item_id == Constants.NO_SCHOOL_OTHER_ITEM_ID &&
            binding.etReasonOther.text.isNullOrBlank()
        ) {
            binding.lyReasonOther.error = "Please specify the reason"
            ok = false
        } else {
            binding.lyReasonOther.error = null
        }

        if (selectedStartDate == null) {
            binding.lyStartDate.error = "Please select a start date"
            ok = false
        } else {
            binding.lyStartDate.error = null
        }

        if (selectedEndDate == null) {
            binding.lyEndDate.error = "Please select an end date"
            ok = false
        } else {
            binding.lyEndDate.error = null
        }

        return ok
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "NoSchoolDialogFragment"
    }
}
