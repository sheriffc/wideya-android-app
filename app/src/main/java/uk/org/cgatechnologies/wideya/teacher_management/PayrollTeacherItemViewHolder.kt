package uk.org.cgatechnologies.wideya.teacher_management

import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.ViewholderTeacherPayrollListItemBinding
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherPayrollModel

class PayrollTeacherItemViewHolder(
    private val binding: ViewholderTeacherPayrollListItemBinding,
    private val onItemClick: ((TeacherPayrollModel) -> Unit)? = null
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(teacher: TeacherPayrollModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = teacher.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
            } else {
                cbSelect.visibility = View.GONE
            }

            tvTeacherName.text = teacher.full_name
            tvDob.text = teacher.date_of_birth ?: "[DoB N/A]"
            tvSex.text = teacher.sex ?: "[Gender N/A]"
            tvPin.text = teacher.pin

            binding.root.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick.invoke(teacher)
                } else {
                    Navigation.findNavController(binding.root).navigate(
                        TeacherPayrollListFragmentDirections
                            .actionTeacherPayrollListFragmentToTeacherProfileDetailsFragment(teacher)
                    )
                }
            }
        }
    }
}
