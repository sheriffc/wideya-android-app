package uk.org.cgatechnologies.wideya.teacher_management.dialogfragments

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.DetailsMode
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.common.validator.ValidationUtils
import uk.org.cgatechnologies.wideya.common.validator.base.BaseInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.TimetableLessonTimingValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.NameInputValidation
import uk.org.cgatechnologies.wideya.common.validator.inputValidation.RequiredDialogValidation
import uk.org.cgatechnologies.wideya.databinding.DialogFragmentTimetableEntryBinding
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementViewModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel

/**
 * Created by Mohamad Abuzaid on 1/6/2023.
 */

class TimetableEntryDialogFragment : DialogFragment() {

    private var _binding: DialogFragmentTimetableEntryBinding? = null
    private val binding get() = _binding!!

    private val teacherManagementViewModel by activityViewModels<TeacherManagementViewModel>()
    private val validationsArray = mutableSetOf<BaseInputValidation>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFragmentTimetableEntryBinding.inflate(layoutInflater)

        //dialog frag comm
        childFragmentManager.setFragmentResultListener("requestKey", this) { _, bundle ->
            val result = bundle.getParcelable<TeacherTimetableModel>("timetable")
            val id = bundle.getInt("resId")
            loadEntryDetailsInView(result!!, id)
        }

        binding.btClose.setOnClickListener {
            if (teacherManagementViewModel.timetableDetailsMode == DetailsMode.VIEW) dismiss()
            else confirmAction(ENTRY_DISCARD, getString(R.string.dialog_discard))
        }

        setupDialogView()
        val builder = MaterialAlertDialogBuilder(requireContext())
        return builder.setView(binding.root).create()
    }

    private fun setupDialogView() {

        binding.apply {
            val inputValidations = setOf(
                RequiredDialogValidation(etTableSubject, tiTableSubject),
                NameInputValidation(etTableSubjectOther, tiTableSubjectOther, false),
                RequiredDialogValidation(etTableDay, tiTableDay),
                RequiredDialogValidation(etTableStartTime, tiTableStartTime),
                TimetableLessonTimingValidation(etTableStartTime, etTableEndTime, tiTableEndTime, "8"),
            )
            validationsArray.clear()
            validationsArray.addAll(inputValidations)
        }

        when (teacherManagementViewModel.timetableDetailsMode) {
            DetailsMode.VIEW -> {
                makeFieldsViewOnly()
                loadEntryDetailsInView(teacherManagementViewModel.currentTimetableEntry)

                binding.tvTableTitle.text = getString(R.string.timetable_entry_title, "")

                binding.btTableEdit.visibility = View.VISIBLE
                binding.btTableSave.visibility = View.GONE
                binding.btTableRemove.visibility = View.VISIBLE

                binding.btTableEdit.setOnClickListener {
                    teacherManagementViewModel.timetableDetailsMode = DetailsMode.EDIT
                    setupDialogView()
                }

                binding.btTableRemove.setOnClickListener {
                    confirmAction(ENTRY_DELETE, getString(R.string.dialog_delete))
                }
            }
            DetailsMode.EDIT -> {
                ValidationUtils.addLiveValidation(validationsArray)
                loadEntryDetailsInView(teacherManagementViewModel.currentTimetableEntry)
                makeFieldsEditable()

                binding.tvTableTitle.text =
                    getString(R.string.timetable_entry_title, getString(R.string.edit))

                binding.btTableEdit.visibility = View.GONE
                binding.btTableSave.visibility = View.VISIBLE
                binding.btTableRemove.visibility = View.GONE

                binding.btTableSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        checkTimetableOverlap(teacherManagementViewModel.currentTimetableEntry.uuid) { isValid ->
                            if (isValid) {
                                saveTimetableEntryInView()
                                dismiss()
                            } else {
                                showOverlapErrorDialog()
                            }
                        }
                    }
                }
            }
            DetailsMode.FROM_SEARCH -> {}
            DetailsMode.NEW -> {
                ValidationUtils.addLiveValidation(validationsArray)
                makeFieldsEditable()

                binding.tvTableTitle.text =
                    getString(R.string.timetable_entry_title, getString(R.string.add))

                binding.btTableEdit.visibility = View.GONE
                binding.btTableSave.visibility = View.VISIBLE
                binding.btTableRemove.visibility = View.GONE

                binding.btTableSave.setOnClickListener {
                    if (ValidationUtils.checkFieldsAreValid(validationsArray)) {
                        checkTimetableOverlap { isValid ->
                            if (isValid) {
                                saveTimetableEntryInView()
                                dismiss()
                            } else {
                                showOverlapErrorDialog()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadEntryDetailsInView(entry: TeacherTimetableModel, id: Int? = null) {
        binding.apply {

            if (id == null || id == etTableSubject.id)
                etTableSubject.setTag(R.string.idTag, entry.school_subject_oid)
            if (id == null || id == etTableSubject.id) {
                etTableSubject.setText(entry.school_subject_name)
                etTableSubjectOther.setText(entry.school_subject_other)
            }
            if (id == null || id == etTableDay.id)
                etTableDay.setTag(R.string.idTag, entry.day_of_the_week_oid)
            if (id == null || id == etTableDay.id)
                etTableDay.setText(entry.day_of_the_week_name)
            if (id == null || id == etTableStartTime.id)
                etTableStartTime.setText(entry.start_time)
            if (id == null || id == etTableEndTime.id)
                etTableEndTime.setText(entry.end_time)
        }
    }

    private fun checkTimetableOverlap(uuid: String = "", onComplete: (isValid: Boolean) -> Unit) {
        val day = binding.etTableDay.text.toString()
        val start = binding.etTableStartTime.text.toString()
        val end = binding.etTableEndTime.text.toString()

        val isValid = teacherManagementViewModel.checkTimetableEntryTiming(uuid, day, start, end)
        onComplete(isValid)
    }

    private fun saveTimetableEntryInView() {
        binding.apply {
            teacherManagementViewModel.currentTimetableEntry.apply entry@{

                this@entry.start_time = etTableStartTime.text.toString()
                this@entry.end_time = etTableEndTime.text.toString()

                if (teacherManagementViewModel.timetableDetailsMode == DetailsMode.EDIT)
                    teacherManagementViewModel.updateTimetableEntry(this@entry)
                else if (teacherManagementViewModel.timetableDetailsMode == DetailsMode.NEW)
                    teacherManagementViewModel.insertTimetableEntry(this@entry)
            }
        }
    }

    private fun makeFieldsViewOnly() {
        binding.apply {
            tiTableSubject.endIconMode = TextInputLayout.END_ICON_NONE
            tiTableDay.endIconMode = TextInputLayout.END_ICON_NONE
        }
    }

    private fun makeFieldsEditable() {
        binding.apply {
            tiTableSubject.endIconMode = TextInputLayout.END_ICON_CUSTOM
            Utils.setViewBackgroundColorToEditable(tiTableSubject)
            etTableSubject.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "timetable.school_subject_oid",
                        it,
                        tiTableSubject.hint.toString(),
                        teacherManagementViewModel.getSubjectsForEducationLevel("school_subject"),
                        childFragmentManager,
                        otherName = etTableSubjectOther.text.toString()
                    )
                }
            }

            tiTableDay.endIconMode = TextInputLayout.END_ICON_CUSTOM
            Utils.setViewBackgroundColorToEditable(tiTableDay)
            etTableDay.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initSelectOneWidget(
                        "timetable.day_of_the_week_oid",
                        it,
                        tiTableDay.hint.toString(),
                        teacherManagementViewModel.getOptionList("day_of_the_week"),
                        childFragmentManager
                    )
                }
            }

            var defaultHour = 8

            Utils.setViewBackgroundColorToEditable(tiTableStartTime)
            etTableStartTime.apply {
                isFocusable = false
                setOnClickListener {
                    Utils.initTimePickerDialog(requireContext(), it, defaultHour)
                }
            }

            Utils.setViewBackgroundColorToEditable(tiTableEndTime)
            etTableEndTime.apply {
                isFocusable = false
                setOnClickListener {

                    if (etTableStartTime.text.isNullOrEmpty().not()) {
                        val startTime: String = etTableStartTime.text.toString()
                        defaultHour = startTime.take(2).toInt()
                    }

                    Utils.initTimePickerDialog(requireContext(), it, defaultHour)
                }
            }
        }
    }

    private fun confirmAction(action: Int, message: String) {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage(message)
            .setTitle(R.string.dialog_title)

        builder.setPositiveButton(R.string.confirm) { _, _ ->
            if (action == ENTRY_DELETE)
                teacherManagementViewModel.deleteTimetableEntry()

            dismiss()
        }
        builder.setNegativeButton(R.string.cancel) { _, _ ->
            // User cancelled the dialog
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showOverlapErrorDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())

        builder.setMessage(R.string.overlap_entry_message)
            .setTitle(R.string.overlap_entry_title)

        builder.setPositiveButton(R.string.ok) { _, _ ->
            // To nothing, just dismiss
        }
        val dialog = builder.create()
        dialog.show()
    }

    companion object {
        const val TAG = "TimetableEntryDialogFragment"

        const val ENTRY_DISCARD = 0
        const val ENTRY_DELETE = 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}