package uk.org.cgatechnologies.wideya.teacher_management.models

data class TeacherSearchRequest(
    val access_token: String,
    val request: String = "teacher_search",
    val params: TeacherSearchParams
)

data class TeacherSearchParams(
    val type: String,
    val query: String,
    val school_uuid: String?
)
