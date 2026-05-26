package uk.org.cgatechnologies.wideya.analysis.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LearnerDisabilityModel (
    var learner_uuid: String? = null,
    var full_name: String? = null,
    var learner_sex_oid: String? = null,
    var admission_number: String? = null,
    var disability_vision_id: String? = null,
    var disability_vision: String? = null,
    var disability_cognition_id: String? = null,
    var disability_cognition: String? = null,
    var disability_communication_id: String? = null,
    var disability_communication: String? = null,
    var disability_hearing_id: String? = null,
    var disability_hearing: String? = null,
    var disability_mobility_id: String? = null,
    var disability_mobility: String? = null,
    var disability_selfcare_id: String? = null,
    var disability_selfcare: String? = null,
    var disability_other_condition_id: String? = null,
    var disability_other_condition: String? = null
): Parcelable