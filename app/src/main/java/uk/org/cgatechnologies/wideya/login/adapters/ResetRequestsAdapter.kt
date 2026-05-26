package uk.org.cgatechnologies.wideya.login.adapters

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.login.models.common.ResetRequestStatus
import uk.org.cgatechnologies.wideya.login.models.resetpassword.ResetPasswordResponseData
import java.util.*

/**
 * Created by Mohamad Abuzaid on 02/21/2023.
 */

class ResetRequestsAdapter(private val listener: IRequestsListener) :
    RecyclerView.Adapter<ResetRequestsAdapter.ViewHolder>() {

    private val resetRequests = mutableListOf<ResetPasswordResponseData>()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.viewholder_reset_password_request_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val request = resetRequests[position]

        viewHolder.tvRequestTitle.text = viewHolder.tvRequestTitle.context.getString(
            R.string.request_title, request.username, request.created_at
        )
        viewHolder.tvRequestStatus.text = viewHolder.tvRequestStatus.context.getString(
            R.string.request_status,
            request.status
        )
        viewHolder.ivStatusIcon.backgroundTintList = when (request.status) {
            ResetRequestStatus.PENDING -> {
                viewHolder.btCancelRequest.isVisible = true
                viewHolder.tvRequestRemarks.isVisible = false
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.blue_mid))
            }
            ResetRequestStatus.APPROVED -> {
                viewHolder.btCancelRequest.isVisible = false
                viewHolder.tvRequestRemarks.isVisible = true
                viewHolder.tvRequestRemarks.text = viewHolder.tvRequestStatus.context.getString(
                    R.string.request_remarks, request.remarks ?: R.string.na
                )
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.green_mid))
            }
            ResetRequestStatus.CANCELLED -> {
                viewHolder.btCancelRequest.isVisible = false
                viewHolder.tvRequestRemarks.isVisible = false
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.yellow_mid))
            }
            ResetRequestStatus.REJECTED -> {
                viewHolder.btCancelRequest.isVisible = false
                viewHolder.tvRequestRemarks.isVisible = true
                viewHolder.tvRequestRemarks.text = viewHolder.tvRequestStatus.context.getString(
                    R.string.request_remarks, request.remarks ?: R.string.na
                )
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.red_mid))
            }
            ResetRequestStatus.EXPIRED -> {
                viewHolder.btCancelRequest.isVisible = false
                viewHolder.tvRequestRemarks.isVisible = false
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.grey_300))
            }
            else -> {
                viewHolder.btCancelRequest.isVisible = false
                viewHolder.tvRequestRemarks.isVisible = false
                ColorStateList.valueOf(viewHolder.ivStatusIcon.context.getColor(R.color.grey_500))
            }
        }
        viewHolder.btCancelRequest.setOnClickListener {
            this.listener.onRequestCancelled(request)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = resetRequests.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateRequestsList(requests: List<ResetPasswordResponseData>) {
        resetRequests.clear()
        resetRequests.addAll(requests)
        notifyDataSetChanged()
    }

    interface IRequestsListener {
        fun onRequestCancelled(request: ResetPasswordResponseData)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivStatusIcon: View
        val tvRequestTitle: TextView
        val tvRequestStatus: TextView
        val tvRequestRemarks: TextView
        val btCancelRequest: AppCompatButton

        init {
            ivStatusIcon = view.findViewById(R.id.ivStatusIcon)
            tvRequestTitle = view.findViewById(R.id.tvRequestTitle)
            tvRequestStatus = view.findViewById(R.id.tvRequestStatus)
            tvRequestRemarks = view.findViewById(R.id.tvRequestRemarks)
            btCancelRequest = view.findViewById(R.id.btCancelRequest)
        }
    }
}
