package uk.org.cgatechnologies.wideya.school_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

@Parcelize
data class SchoolModel(
    var uuid: String = Utils.getUuidOrdered(),
    var name: String = "",
    var education_level_oid: String? = null,
    var education_level_name: String? = null,
    var education_level_display_order: String? = null,
    var emis_id: String? = null,
    var wideya_id: String? = null,
    var payroll_sid: String? = null,
    var waec_id: String? = null,
    var fabinc_recordid: Int? = null,
    var district_id: String? = null,
    var district_name: String? = null,
    var chiefdom_id: String? = null,
    var chiefdom_name: String? = null,
    var section_name: String? = null,
    var town_name: String? = null,
    var address: String? = null,
    var classrooms_oid: String? = null,
    var wash_oids: String? = null,
    var electricity_oids: String? = null,
    var mno_oids: String? = null,
    var learning_materials_oids: String? = null,
    var receives_feeding: Int? = null,
    var district_office_uuid: String? = null,
    var district_office_name: String? = null,
    var lat: Float? = null,
    var lng: Float? = null,
    var media_photo_uuid: String? = null,
    var tablet_phone_number: String? = null,
    var active: Byte? = null,
    var created_at: String = Utils.getISODateTimeUTC(),
    var created_by: Int = 0,
    var updated_at: String = Utils.getISODateTimeUTC(),
    var updated_by: Int = 0,
    var deleted_at: String? = null,
    var deleted_by: Int? = null,
    var sync_flag: Byte = 0
) : Parcelable