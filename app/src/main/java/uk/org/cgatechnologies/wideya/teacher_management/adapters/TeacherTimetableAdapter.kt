package uk.org.cgatechnologies.wideya.teacher_management.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel

/**
 * Created by Mohamad Abuzaid on 1/5/2023.
 */

class TeacherTimetableAdapter(private val listener: ITimetableListener) :
    RecyclerView.Adapter<TeacherTimetableAdapter.ViewHolder>() {

    private val timetableList = mutableListOf<TeacherTimetableModel>()

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_teacher_timetable, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val subject = if (timetableList[position].school_subject_other.isNullOrEmpty()) {
            timetableList[position].school_subject_name
        } else {
            timetableList[position].school_subject_other
        }
        viewHolder.subject.text = subject
        viewHolder.timeStart.text = timetableList[position].start_time?.take(5) ?: ""
        viewHolder.timeEnd.text = timetableList[position].end_time?.take(5) ?: ""
        viewHolder.itemView.setOnClickListener {
            listener.onEntryClicked(timetableList[position])
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = timetableList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateTimetableList(timetable: List<TeacherTimetableModel>) {
        timetableList.clear()
        timetableList.addAll(timetable)
        notifyDataSetChanged()
    }

    interface ITimetableListener {
        fun onEntryClicked(entry: TeacherTimetableModel)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeStart: TextView
        val timeEnd: TextView
        val subject: TextView

        init {
            // Define click listener for the ViewHolder's View
            timeStart = view.findViewById(R.id.tv_time_start)
            timeEnd = view.findViewById(R.id.tv_time_end)
            subject = view.findViewById(R.id.tv_subject)
        }
    }
}
