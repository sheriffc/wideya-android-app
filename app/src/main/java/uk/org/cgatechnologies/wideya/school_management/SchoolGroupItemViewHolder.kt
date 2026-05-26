package uk.org.cgatechnologies.wideya.school_management

import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolGroupListItemBinding
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel

class SchoolGroupItemViewHolder(
    private val binding: ViewholderSchoolGroupListItemBinding
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(schoolGroup: SchoolGroupModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = schoolGroup.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                //cbSelect.isChecked = schoolGroup.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            clSchoolGroupListItem.setBackgroundColor(
                ResourcesCompat.getColor(
                    clSchoolGroupListItem.resources,
                    R.color.purple_faint,
                    null
                )
            )

            tvSchoolGroupName.text = schoolGroup.school_group_name ?: ""
            tvSchoolGroupLevel.text = schoolGroup.school_group_level_name ?: "[Level N/A]"
            tvAcademicYearName.text = schoolGroup.academic_year_name ?: "[Academic Year N/A]"
            tvLearnersTally.text = schoolGroup.learner_count?.toString() ?: ""
            tvTeacherName.text = schoolGroup.teacher_full_name ?: "[No Teacher Assigned]"

            var navController: NavController

            binding.root.setOnClickListener {
                navController = Navigation.findNavController(binding.root)
                val directions =
                    SchoolProfileFragmentDirections.actionSchoolProfileFragmentToSchoolGroupProfileFragment(
                        schoolGroup
                    )
                navController.navigate(directions)
            }
        }
    }
}
