package uk.org.cgatechnologies.wideya.sync_device

import androidx.core.view.marginLeft
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.databinding.ViewholderLogSyncItemBinding
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync

class LogSyncItemViewHolder(
    private val binding: ViewholderLogSyncItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(logSync: LogSync){
        binding.apply {
            tvTimestamp.text = logSync.created_at
            tvMessage.text = logSync.message
        }
    }
}
