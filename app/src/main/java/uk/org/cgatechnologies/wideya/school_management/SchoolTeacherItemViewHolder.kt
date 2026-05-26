package uk.org.cgatechnologies.wideya.school_management

import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolTeacherListItemBinding
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel

class SchoolTeacherItemViewHolder(
    private val binding: ViewholderSchoolTeacherListItemBinding
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(teacher: TeacherModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = teacher.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                //cbSelect.isChecked = teacher.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            clTeacherListItem.setBackgroundColor(
                ResourcesCompat.getColor(
                    clTeacherListItem.resources,
                    R.color.blue_faint,
                    null
                )
            )

            tvTeacherName.text = teacher.full_name ?: "[Name N/A]"
            tvEmploymentRole.text = teacher.teacher_role_name ?: ""
            tvEmploymentStatus.text = teacher.employment_status_name ?: "[Payroll Status N/A]"
            tvPin.text = teacher.pin ?: ""

            val portraitData = teacher.portrait_base64_data
            val bitmap = if (!portraitData.isNullOrEmpty())
                Utils.decodeBase64StringToBitmap(portraitData) else null
            if (bitmap != null) {
                teacherIcon.setImageBitmap(bitmap)
                teacherIcon.colorFilter = null
            } else {
                if (teacher.sex_oid == "male") {
                    teacherIcon.setImageResource(R.drawable.person_male_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.blue_mid))
                } else if (teacher.sex_oid == "female") {
                    teacherIcon.setImageResource(R.drawable.person_female_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.red_mid))
                } else {
                    teacherIcon.setImageResource(R.drawable.persons_2_128)
                    teacherIcon.setColorFilter(ContextCompat.getColor(teacherIcon.context, R.color.black))
                }
            }

            var navController: NavController

            binding.root.setOnClickListener {
                Log.d("teacherviewholder", "I was clicked" + teacher.full_name)
                navController = Navigation.findNavController(binding.root)
//            val directions = SchoolListFragmentDirections.actionSchoolListFragmentToSchoolProfileFragment(teacher)
                val directions =
                    SchoolProfileFragmentDirections.actionSchoolProfileFragmentToTeacherProfileFragment(
                        teacher
                    )
                navController.navigate(directions)
            }
        }
    }
}
