package uk.org.cgatechnologies.wideya.learner_management.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

// ** this model is unused and potentially out of date **
@Parcelize
data class LearnerModel(
    var uuid: String = Utils.getUuidOrdered(),
    var person_uuid: String? = null,
    var full_name: String? = null,
    var last_name: String? = null,
    var middle_name: String? = null,
    var first_name: String? = null,
    var sex_oid: String? = null,
    var sex_name: String? = null,
    var date_of_birth: String? = null,
    var nin: String? = null,
    var portrait_uuid: String? = null,
    var language_oid_strongest: String? = null,
    var language_oid_strongest_name: String? = null,
    var learner_maternal_status_oid: String? = null,
    var learner_maternal_status_name: String? = null,
    var learner_maternal_status_updated_at: String? = null,
    var disability_severity_oid_vision: String? = null,
    var disability_severity_oid_vision_name: String? = null,
    var disability_severity_oid_hearing: String? = null,
    var disability_severity_oid_hearing_name: String? = null,
    var disability_severity_oid_mobility: String? = null,
    var disability_severity_oid_mobility_name: String? = null,
    var disability_severity_oid_cognition: String? = null,
    var disability_severity_oid_cognition_name: String? = null,
    var disability_severity_oid_selfcare: String? = null,
    var disability_severity_oid_selfcare_name: String? = null,
    var disability_severity_oid_communication: String? = null,
    var disability_severity_oid_communication_name: String? = null,
    var disability_other_condition_oid: String? = null,
    var disability_other_condition_name: String? = null,
    var school_uuid: String? = null,
    var school_name: String? = null,
    var employment_status_oid: String? = null,
    var employment_status_name: String? = null,
    var employment_role_oid: String? = null,
    var employment_role_name: String? = null,
    var pin: String? = null,
    var nassit_number: String? = null,
    var start_date: String? = null,
    var end_date: String? = null,
    var end_reason_oid: String? = null,
    var end_reason_name: String? = null,
    var end_reason_other: String? = null,
    var active: Boolean? = true,
    var created_at: String = "",
    var created_by: Int = 0,
    var updated_at: String = "",
    var updated_by: Int = 0,
    var deleted_at: String? = null,
    var deleted_by: Int? = null,
    var sync_flag: Byte = 0
) : Parcelable