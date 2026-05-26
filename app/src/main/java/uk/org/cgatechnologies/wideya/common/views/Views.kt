package uk.org.cgatechnologies.wideya.common.views

import android.content.Context
import android.content.res.ColorStateList
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.setPadding
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.utils.Utils

/**
 * Created by Mohamad Abuzaid on 12/21/2022.
 */
object Views {
    fun createActionButton(context: Context, txt: String) = AppCompatButton(context).apply {
        val lp = MarginLayoutParams(
            250,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        lp.marginStart = Utils.dpToPixel(context, 15f)

        text = txt
        layoutParams = lp
        setPadding(Utils.dpToPixel(context, 5f))
        isAllCaps = false
        textSize = 20f
        setTextColor(ColorStateList.valueOf(context.getColor(R.color.white)))

        setBackgroundResource(R.drawable.bg_action_button)
        backgroundTintList = when {
            txt.contains("add", true) -> ColorStateList.valueOf(context.getColor(R.color.green_neutral))
            txt.contains("save", true) -> ColorStateList.valueOf(context.getColor(R.color.green_neutral))
            txt.contains("remove", true) -> ColorStateList.valueOf(context.getColor(R.color.red_light))
            txt.contains("unassign", true) -> ColorStateList.valueOf(context.getColor(R.color.red_light))
            else -> ColorStateList.valueOf(context.getColor(R.color.blue_mid))
        }
    }
}