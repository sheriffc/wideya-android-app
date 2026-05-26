package uk.org.cgatechnologies.wideya.school_management.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.DateDropDownItemBinding
import uk.org.cgatechnologies.wideya.school_management.data.AttendanceStatus
import uk.org.cgatechnologies.wideya.school_management.models.DisplayAttendanceDateModel

class AttendanceDateClassArrayAdapter(context: Context, dateList: List<DisplayAttendanceDateModel>):ArrayAdapter<DisplayAttendanceDateModel>(context,0,dateList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View{
        val item = getItem(position)
        val itemBinding = DateDropDownItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        itemBinding.apply {
            tvItem.text = item?.date_text
            when(item?.status){
//                AttendanceStatus.TODAY -> ""
                AttendanceStatus.SUBMITTED -> tvItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.green_pastel))
                AttendanceStatus.UNSUBMITTED -> tvItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.yellow_pastel))
                AttendanceStatus.NOT_DONE -> tvItem.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.red_pastel))
                else -> null
            }

        }
        return  itemBinding.root
    }

}