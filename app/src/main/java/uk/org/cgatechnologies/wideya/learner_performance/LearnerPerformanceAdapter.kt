package uk.org.cgatechnologies.wideya.learner_performance

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.ItemLearnerPerformanceBinding
import uk.org.cgatechnologies.wideya.learner_performance.models.LearnerListItemModel

class LearnerPerformanceAdapter(
    private val items: MutableList<LearnerListItemModel>,
    private val onAssessClick: (LearnerListItemModel) -> Unit
) : RecyclerView.Adapter<LearnerPerformanceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemLearnerPerformanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLearnerPerformanceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.binding

        b.tvLearnerName.text = item.learner_name
        b.tvClassName.text = item.school_group_name ?: "No class assigned"

        val hasClass = item.school_group_uuid != null
        b.btnAssess.isEnabled = hasClass
        b.btnAssess.setOnClickListener {
            onAssessClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<LearnerListItemModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
