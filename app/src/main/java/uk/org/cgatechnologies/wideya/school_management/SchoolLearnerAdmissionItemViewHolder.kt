package uk.org.cgatechnologies.wideya.school_management

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.ViewholderSchoolLearnerListItemBinding
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

class SchoolLearnerAdmissionItemViewHolder(
    private val binding: ViewholderSchoolLearnerListItemBinding
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(learnerAdmission: LearnerAdmissionModel, multiSelect: Boolean) {
        binding.apply {
            cbSelect.tag = learnerAdmission.uuid
            if (multiSelect) {
                cbSelect.visibility = View.VISIBLE
                cbSelect.isChecked = learnerAdmission.isSelected
            } else {
                cbSelect.visibility = View.GONE
            }

            clSchoolLearnerListItem.setBackgroundColor(
                ResourcesCompat.getColor(
                    clSchoolLearnerListItem.resources,
                    R.color.yellow_faint,
                    null
                )
            )

            tvLearnerName.text = learnerAdmission.learner_full_name ?: "NAME NOT FOUND"
            if (!learnerAdmission.enrolment_school_group_uuid.isNullOrEmpty()) {
                "${learnerAdmission.enrolment_school_group_level_name?.plus(", ") ?: ""}${learnerAdmission.enrolment_school_group_name ?: ""} (${learnerAdmission.enrolment_current_academic_year_name ?: ""})".also {
                    tvEnrolmentStatus.text = it
                }
                tvEnrolmentStatus.setTextColor(ContextCompat.getColor(tvEnrolmentStatus.context, R.color.black))
                tvPrevEnrolment.visibility = View.GONE
            } else {
                tvEnrolmentStatus.text = tvEnrolmentStatus.context.getString(R.string.assign_learner_to_class)
                tvEnrolmentStatus.setTextColor(
                    ContextCompat.getColor(
                        tvEnrolmentStatus.context,
                        R.color.md_theme_light_error
                    )
                )
                val prevGroup = learnerAdmission.prev_enrolment_school_group_concat_name?.trim()
                val prevYear = learnerAdmission.prev_enrolment_academic_year_name
                if (!prevGroup.isNullOrEmpty() && !prevYear.isNullOrEmpty()) {
                    tvPrevEnrolment.text = "Last year: $prevGroup ($prevYear)"
                    tvPrevEnrolment.visibility = View.VISIBLE
                } else {
                    tvPrevEnrolment.visibility = View.GONE
                }
            }

            if (!learnerAdmission.learner_id.isNullOrEmpty()) {
                tvLearnerIdentifier.text = "UID: ${learnerAdmission.learner_id}"
            } else {
                tvLearnerIdentifier.text = ""
            }

            learnerAdmission.learner_age?.let {
                ("Age: " + (learnerAdmission.learner_age ?: "unknown")).also { tvAge.text = it }
            } ?: kotlin.run {
                tvAge.text = ""
            }

            tvSex.text = learnerAdmission.learner_sex_name ?: ""

            if (learnerAdmission.learner_sex_oid.equals("male")) {
                learnerIcon.setImageResource(R.drawable.learner_male_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.blue_mid))
            } else if (learnerAdmission.learner_sex_oid.equals("female")) {
                learnerIcon.setImageResource(R.drawable.learner_female_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.red_mid))
            } else {
                learnerIcon.setImageResource(R.drawable.learners_128)
                learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.black))
            }

            root.setOnClickListener {
                Log.d("learneradmisviewholder", "I was clicked" + learnerAdmission.learner_full_name)
                val bundle = Bundle().apply {
                    putParcelable("learner", learnerAdmission)
                }
                Navigation.findNavController(root).navigate(R.id.LearnerProfileFragment, bundle)
            }
        }
    }
}
