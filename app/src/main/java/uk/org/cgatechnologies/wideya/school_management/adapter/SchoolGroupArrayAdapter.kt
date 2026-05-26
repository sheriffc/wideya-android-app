package uk.org.cgatechnologies.wideya.school_management.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.databinding.DateDropDownItemBinding
import uk.org.cgatechnologies.wideya.school_group_management.models.SchoolGroupModel
import uk.org.cgatechnologies.wideya.school_management.data.AttendanceStatus
import uk.org.cgatechnologies.wideya.school_management.models.DisplayAttendanceDateModel
import uk.org.cgatechnologies.wideya.school_management.models.DisplaySchoolGroupModel

class SchoolGroupArrayAdapter(context: Context, schoolGroupModel: List<DisplaySchoolGroupModel>):ArrayAdapter<DisplaySchoolGroupModel>(context,0,schoolGroupModel) {

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
            tvItem.text = item?.school_group_name

        }
        return  itemBinding.root
    }

}