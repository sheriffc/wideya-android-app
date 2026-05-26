package uk.org.cgatechnologies.wideya.learner_performance

data class ExistingPerformanceRecord(
    val existing_uuid: String,
    val subject_oid: String,
    val assessment_1_score: Float?,
    val assessment_2_score: Float?,
    val max_score: Float
)
