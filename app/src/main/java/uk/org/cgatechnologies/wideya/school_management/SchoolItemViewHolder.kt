package uk.org.cgatechnologies.wideya.school_management

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolListItemBinding
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel

class SchoolItemViewHolder(
    private val binding: ViewholderSchoolListItemBinding,
    private val viewModel: SchoolManagementViewModel,
    private val context: Context
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(school: SchoolModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = school.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                //cbSelect.isChecked = school.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            tvSchoolName.text = school.name
            tvEducationLevel.text = school.education_level_name ?: "[Education Level N/A]"
            ((school.district_office_name ?: "[Education Level N/A]") +
                    " (" +
                    (school.chiefdom_name ?: "[Chiefdom N/A]") + ", " +
                    (school.section_name ?: "[Section N/A]") + ", " +
                    (school.town_name ?: "[Town N/A]") +
                    ")").also { tvDistrictOffice.text = it }
            tvEmisId.text = school.emis_id ?: "[EMIS Code N/A]"
            tvSID.text = school.payroll_sid ?: "[Payroll SID N/A]"

            var navController: NavController

            binding.root.setOnClickListener {
                Log.d("schoolviewholder", "I was clicked" + school.uuid)
                viewModel.saveSchoolIdUserPreferences(context, school.uuid)
                navController = Navigation.findNavController(binding.root)

                navController.popBackStack(R.id.SchoolListFragment, true)
                val bundle = Bundle()
                bundle.putParcelable("school", school)
                navController.navigate(R.id.SchoolProfileFragment, bundle)
            }
        }
    }
}
