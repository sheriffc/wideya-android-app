package uk.org.cgatechnologies.wideya.learner_performance

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.ItemSubjectAssessmentBinding
import uk.org.cgatechnologies.wideya.learner_performance.models.SubjectAssessmentItem

class SubjectAssessmentAdapter(
    private val items: MutableList<SubjectAssessmentItem>,
    private var assessmentNumber: Int = 1,
    private var scoreLabel: String = "First Assessment"
) : RecyclerView.Adapter<SubjectAssessmentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSubjectAssessmentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectAssessmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        val isAssessment1 = assessmentNumber == 1
        val currentScore = if (isAssessment1) item.assessment_1_score else item.assessment_2_score
        val isLocked = item.existing_uuid != null && currentScore != null

        b.tvSubjectName.text = item.subject_name
        b.tlScore.hint = scoreLabel
        b.tlScore.isEnabled = !isLocked

        b.etScore.removeTextChangedListener(b.etScore.tag as? TextWatcher)
        b.etScore.setText(currentScore?.let { formatScore(it) } ?: "")

        refreshSummary(b, item)

        if (!isLocked) {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val pos = holder.bindingAdapterPosition
                    if (pos == RecyclerView.NO_ID.toInt()) return
                    val score = s.toString().toFloatOrNull()
                    if (assessmentNumber == 1) items[pos].assessment_1_score = score
                    else items[pos].assessment_2_score = score
                    refreshSummary(b, items[pos])
                }
            }
            b.etScore.addTextChangedListener(watcher)
            b.etScore.tag = watcher
        } else {
            b.etScore.tag = null
        }
    }

    private fun refreshSummary(b: ItemSubjectAssessmentBinding, item: SubjectAssessmentItem) {
        val avg = item.average
        b.tvAverage.text = if (avg != null) "Avg: ${"%.1f".format(avg)}" else ""
        b.tvGrade.text = if (avg != null) item.grade else ""
    }

    private fun formatScore(score: Float): String =
        if (score == score.toLong().toFloat()) score.toLong().toString() else score.toString()

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<SubjectAssessmentItem>, number: Int, label: String) {
        assessmentNumber = number
        scoreLabel = label
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setAssessment(number: Int, label: String) {
        assessmentNumber = number
        scoreLabel = label
        notifyDataSetChanged()
    }

    fun getCurrentItems(): List<SubjectAssessmentItem> = items.toList()
}
