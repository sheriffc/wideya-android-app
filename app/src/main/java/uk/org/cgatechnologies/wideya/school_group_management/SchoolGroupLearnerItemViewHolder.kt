package uk.org.cgatechnologies.wideya.school_group_management

import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.selection.ItemDetailsLookup.ItemDetails
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolGroupLearnerListItemBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementDao
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupLearnerModel


class SchoolGroupLearnerItemViewHolder(
    private val binding: ViewholderSchoolGroupLearnerListItemBinding
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(learner: SchoolGroupLearnerModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = learner.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = learner.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            clSchoolGroupLearnerListItem.setBackgroundColor(
                ResourcesCompat.getColor(
                    clSchoolGroupLearnerListItem.resources,
                    R.color.yellow_faint,
                    null
                )
            )

            tvLearnerName.text = learner.learner_full_name ?: "NAME NOT FOUND"
            if (learner.learner_age != null) {
                ("Age: " + (learner.learner_age ?: "unknown")).also { tvAge.text = it }
            } else {
                tvAge.text = ""
            }
            tvSex.text = learner.learner_sex_name ?: ""

            if (learner.learner_sex_oid.equals("male")) {
                learnerIcon.setImageResource(R.drawable.learner_male_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.blue_mid))
            } else if (learner.learner_sex_oid.equals("female")) {
                learnerIcon.setImageResource(R.drawable.learner_female_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.red_mid))
            } else {
                learnerIcon.setImageResource(R.drawable.learners_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.black))
            }

            learner.admission_number?.let {
                ((learner.admission_number ?: "none") + " (Admission Num)").also { tvLearnerIdentifier.text = it }
            } ?: kotlin.run {
                tvLearnerIdentifier.text = ""
            }

            var navController: NavController

            binding.root.setOnClickListener {
                val learnerManagementDao: LearnerManagementDao? = AppDatabase.getInstance()?.learnerManagementDao()

                if (learnerManagementDao != null) {
                    val learnerAdmission = learnerManagementDao.getLearnerAdmissionModel(learner.learner_uuid!!)

                    navController = Navigation.findNavController(binding.root)
                    val directions =
                        SchoolGroupProfileFragmentDirections.actionSchoolGroupProfileFragmentToLearnerProfileFragment(
                            learnerAdmission!!
                        )
                    navController.navigate(directions)
                }
            }
        }
    }
}


class Details : ItemDetails<Long?>() {
    var position: Long = 0
    override fun getPosition(): Int {
        return position.toInt()
    }

    override fun getSelectionKey(): Long {
        return position
    }

    override fun inSelectionHotspot(e: MotionEvent): Boolean {
        return false
    }

    override fun inDragRegion(e: MotionEvent): Boolean {
        return true
    }
}