package uk.org.cgatechnologies.wideya.analysis

import android.view.View
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.analysis.models.LearnerDisabilityModel
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.databinding.ViewholderLearnerDisabilityItemBinding
import uk.org.cgatechnologies.wideya.learner_management.LearnerManagementDao

class LearnerDisabilityItemViewHolder(
    private val binding: ViewholderLearnerDisabilityItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LearnerDisabilityModel) {
        binding.apply {
            tvLearnerName.text = item.full_name
            item.apply {

                if (item.learner_sex_oid.equals("male")) {
                    learnerIcon.setImageResource(R.drawable.learner_male_128)
                    learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.blue_mid))
                } else if (item.learner_sex_oid.equals("female")) {
                    learnerIcon.setImageResource(R.drawable.learner_female_128)
                    learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.red_mid))
                } else {
                    learnerIcon.setImageResource(R.drawable.learners_128)
                    learnerIcon.setColorFilter(ContextCompat.getColor(learnerIcon.context, R.color.black))
                }

                tvLearnerIdentifier.apply {
                    if (!admission_number.isNullOrEmpty()) {
                        text = "Admission Num: " + admission_number
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.INVISIBLE
                    }
                }

                tvCognition.apply {
                    if (!disability_cognition_id.isNullOrEmpty() && disability_cognition_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_cognition
                        tvCognitionLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvCognitionLabel.visibility = View.GONE
                    }
                }

                tvCommunication.apply {
                    if (!disability_communication_id.isNullOrEmpty() && disability_communication_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_communication
                        tvCommunicationLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvCommunicationLabel.visibility = View.GONE
                    }
                }

                tvVision.apply {
                    if (!disability_vision_id.isNullOrEmpty() && disability_vision_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_vision
                        tvVisionLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvVisionLabel.visibility = View.GONE
                    }
                }

                tvHearing.apply {
                    if (!disability_hearing_id.isNullOrEmpty() && disability_hearing_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_hearing
                        tvHearingLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvHearingLabel.visibility = View.GONE
                    }
                }

                tvMobility.apply {
                    if (!disability_mobility_id.isNullOrEmpty() && disability_mobility_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_mobility
                        tvMobilityLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvMobilityLabel.visibility = View.GONE
                    }
                }

                tvSelfCare.apply {
                    if (!disability_selfcare_id.isNullOrEmpty() && disability_selfcare_id != "0_no_difficulty") {
                        visibility = View.VISIBLE
                        text = disability_selfcare
                        tvSelfCareLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvSelfCareLabel.visibility = View.GONE
                    }
                }

                tvOtherCondition.apply {
                    if (!disability_other_condition_id.isNullOrEmpty() && disability_other_condition_id != "none") {
                        visibility = View.VISIBLE
                        text = disability_other_condition
                        tvOtherConditionLabel.visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                        tvOtherConditionLabel.visibility = View.GONE
                    }
                }

            }

            var navController: NavController

            root.setOnClickListener {
                val learnerManagementDao: LearnerManagementDao? = AppDatabase.getInstance()?.learnerManagementDao()

                if (learnerManagementDao != null) {
                    val learnerAdmission =
                        learnerManagementDao.getLearnerAdmissionModel(item.learner_uuid!!)

                    navController = Navigation.findNavController(root)
                    val directions = SchoolAnalysisFragmentDirections
                        .actionSchoolAnalysisFragmentToLearnerProfileFragment(learnerAdmission!!)
                    navController.navigate(directions)
                }
            }
        }
    }

}