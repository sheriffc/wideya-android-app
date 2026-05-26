package uk.org.cgatechnologies.wideya.teacher_management.models

data class TeacherSearchResponse(
    val status: Boolean,
    val message: String,
    val data: TeacherSearchData?
)

data class TeacherSearchData(
    val teachers: List<TeacherPayrollModel>?
)
