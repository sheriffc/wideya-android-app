package uk.org.cgatechnologies.wideya.school_management.data

data class AttendanceCompletenessData(
    var countAllPersons: Int = 0,
    var countMarkedAttendance: Int = 0,
    var percentComplete:Int = 0
)
