package uk.org.cgatechnologies.wideya.school_management.models

data class NoSchoolPersonModel(
    val person_uuid: String,
    val school_uuid: String,
    val school_group_uuid: String?
)
