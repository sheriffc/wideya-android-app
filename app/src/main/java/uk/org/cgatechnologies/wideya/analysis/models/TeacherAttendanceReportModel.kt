package uk.org.cgatechnologies.wideya.analysis.models
data class TeacherAttendanceReportModel(
    var count_teachers: Int =0,
    var count_attendance: Int = 0,
    var count_present: Int= 0,
    var count_absent: Int= 0,
    var count_late: Int= 0,
    var count_submitted: Int= 0,
    var attendance_date: String? = String()
)
