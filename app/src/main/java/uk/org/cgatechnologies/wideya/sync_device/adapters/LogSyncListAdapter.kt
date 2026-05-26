package uk.org.cgatechnologies.wideya.sync_device.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.*
import uk.org.cgatechnologies.wideya.sync_device.LogSyncItemViewHolder
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync

class LogSyncListAdapter() :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val items = mutableListOf<Any>()

    fun setItems(items: List<Any>){
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ViewholderLogSyncItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogSyncItemViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items.get(position)

        with(holder as LogSyncItemViewHolder){
            val logSyncItem: LogSync = item as LogSync
            bind(logSyncItem)
        }
    }
    override fun getItemCount(): Int {
        return items.size
    }
}