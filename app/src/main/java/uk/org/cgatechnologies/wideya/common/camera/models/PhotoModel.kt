/**
 * Created by "Mohamad Abuzaid" on 12/13/2022.
 */

package uk.org.cgatechnologies.wideya.common.camera.models

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoModel(
    val type: Int,
    val uri: Uri,
    val absolutePath: String,
    val timeStamp: Long = System.currentTimeMillis()
) : Parcelable {
    companion object {
        const val THUMB_REFRESH = "thumb_refresh"
        const val BIOMETRIC_PHOTO = 0
        const val PROFILE_PHOTO = 1
    }
}