package uk.org.cgatechnologies.wideya.learner_management

import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.ViewholderLearnerSearchItemBinding
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

class LearnerSearchItemViewHolder(
    private val binding: ViewholderLearnerSearchItemBinding,
    private val onClick: (LearnerAdmissionModel) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(learner: LearnerAdmissionModel) {
        binding.apply {
            tvLearnerName.text = learner.learner_full_name ?: "[Name N/A]"
            tvSex.text = learner.learner_sex_name ?: "[N/A]"
            tvDob.text = learner.learner_date_of_birth ?: "[N/A]"
            tvNin.text = learner.learner_nin ?: "[N/A]"
            root.setOnClickListener { onClick(learner) }
        }
    }
}
