package uk.org.cgatechnologies.wideya.learner_performance.models

data class LearnerListItemModel(
    val learner_uuid: String,
    val learner_name: String,
    val school_group_uuid: String?,
    val school_group_name: String?,
    val school_group_level_oid: String?
)
