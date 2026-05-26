package uk.org.cgatechnologies.wideya.school_management.models

import uk.org.cgatechnologies.wideya.school_management.data.AttendanceStatus

data class DisplayAttendanceDateModel(
    val date_text:String,
    val status: AttendanceStatus,
    val date:String

) {
    override fun toString(): String {
        return date_text
    }
}
