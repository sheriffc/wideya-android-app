package uk.org.cgatechnologies.wideya.school_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

@Parcelize
data class MediaPhotoModel(
    var uuid: String = Utils.getUuidOrdered(),
    var ref_uuid: String?,
    var base64_data: String?,
    var display_orientation: Short = 0,
    var active: Byte = 1,
    var created_at: String = "",
    var updated_at: String = "",
    var created_by: Int = 0,
    var updated_by: Int = 0,
) : Parcelable
