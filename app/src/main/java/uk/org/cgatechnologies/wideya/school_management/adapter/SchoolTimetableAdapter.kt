package uk.org.cgatechnologies.wideya.school_management.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.ItemSchoolSubjectBinding
import uk.org.cgatechnologies.wideya.school_management.models.SchoolTimetableModel
import java.util.*

/**
 * Created by Mohamad Abuzaid on 1/18/2023.
 */

class SchoolTimetableAdapter : RecyclerView.Adapter<SchoolTimetableAdapter.ViewHolder>() {

    private val timetableSortedMap = sortedMapOf<String, List<SchoolTimetableModel>>()
    private val teachersNames = mutableListOf<String?>()

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_school_timetable, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        repeat(teachersNames.size) {
            val subjectBinding = ItemSchoolSubjectBinding.inflate(LayoutInflater.from(viewHolder.subjects.context))
            viewHolder.subjects.addView(subjectBinding.root)
        }

        val key = timetableSortedMap.keys.elementAt(position)
        viewHolder.timeStart.text = timetableSortedMap[key]?.get(0)?.start_time?.take(5) ?: ""
        viewHolder.timeEnd.text = timetableSortedMap[key]?.get(0)?.end_time?.take(5) ?: ""
        timetableSortedMap[key]?.forEach { entry ->
            val index = teachersNames.indexOfFirst { it == entry.teacher_full_name }
            (viewHolder.subjects.getChildAt(index) as TextView).text = entry.school_subject_name
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = timetableSortedMap.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTimetableList(
        timetable: SortedMap<String, List<SchoolTimetableModel>>,
        teachers: List<String?>
    ) {
        timetableSortedMap.clear()
        teachersNames.clear()
        timetableSortedMap.putAll(timetable)
        teachersNames.addAll(teachers)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeStart: TextView
        val timeEnd: TextView
        val subjects: LinearLayout

        init {
            // Define click listener for the ViewHolder's View
            timeStart = view.findViewById(R.id.tv_time_start)
            timeEnd = view.findViewById(R.id.tv_time_end)
            subjects = view.findViewById(R.id.ll_subjects)
        }
    }
}
