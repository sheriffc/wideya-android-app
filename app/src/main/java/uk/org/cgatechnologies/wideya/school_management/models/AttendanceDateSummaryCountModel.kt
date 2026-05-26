package uk.org.cgatechnologies.wideya.school_management.models

data class AttendanceDateSummaryCountModel(
    val date: String,
    val count_recorded_attendance: Long,
    val count_submitted: Long
)
