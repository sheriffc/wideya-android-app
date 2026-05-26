package uk.org.cgatechnologies.wideya.school_group_management.models

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

@Parcelize
data class SchoolGroupLearnerModel(
    var uuid: String = Utils.getUuidOrdered(),
    var academic_year: String? = null,
    var academic_year_name: String? = null,
    var learner_uuid: String? = null,
    var learner_full_name: String? = null,
    var learner_nin: String? = null,
    var learner_date_of_birth: String? = null,
    var learner_age: String? = null,
    var learner_portrait_uuid: String? = null,
    var learner_person_created_at: String? = null,
    var learner_person_updated_at: String? = null,
    var learner_sex_oid: String? = null,
    var learner_sex_name: String? = null,
    var admission_number: String? = null,
    var school_group_uuid: String? = null,
    var checked: Boolean = false,
    var created_at: String? = null,
    var updated_at: String? = null
) : Parcelable {
    @Ignore
    @IgnoredOnParcel
    var isSelected: Boolean = false
}