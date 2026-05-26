package uk.org.cgatechnologies.wideya.school_group_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

@Parcelize
data class SchoolGroupModel(
    var uuid: String = Utils.getUuidOrdered(),
    var school_uuid: String? = null,
    var school_name: String? = null,
    var school_education_level_oid: String? = null,
    var academic_year: Short? = null,
    var academic_year_name: String? = null,
    var school_group_name: String? = null,
    var school_group_level_oid: String? = null,
    var school_group_level_name: String? = null,
    var teacher_uuid: String? = null,
    var teacher_pin: String? = null,
    var teacher_full_name: String? = null,
    var teacher_first_name: String? = null,
    var teacher_middle_name: String? = null,
    var teacher_last_name: String? = null,
    var learner_count: Int? = null,
    var display_order: Short? = null,
    var active: Byte? = null,
    var created_at: String = Utils.getISODateTimeUTC(),
    var created_by: Int = 0,
    var updated_at: String = Utils.getISODateTimeUTC(),
    var updated_by: Int = 0,
    var deleted_at: String? = null,
    var deleted_by: Int? = null,
    var sync_flag: Byte = 0
) : Parcelable