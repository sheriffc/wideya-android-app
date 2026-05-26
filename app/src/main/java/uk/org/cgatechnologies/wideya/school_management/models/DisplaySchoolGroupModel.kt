package uk.org.cgatechnologies.wideya.school_management.models

import uk.org.cgatechnologies.wideya.school_management.data.AttendanceStatus

data class DisplaySchoolGroupModel(
    val school_group_name: String,
    val school_group_uuid: String

) {
    override fun toString(): String {
        return school_group_name
    }
}
