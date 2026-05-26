package uk.org.cgatechnologies.wideya.learner_performance.models

data class LearnerPerformanceEntryModel(
    val existing_uuid: String?,
    val learner_uuid: String,
    val learner_name: String,
    var assessment_1_score: Float? = null,
    var assessment_2_score: Float? = null,
    val max_score: Float = 100f
) {
    val average: Float?
        get() {
            val a1 = assessment_1_score ?: return null
            val a2 = assessment_2_score ?: return null
            return (a1 + a2) / 2f
        }

    val grade: String
        get() = when {
            average == null -> "—"
            average!! >= 75f -> "A"
            average!! >= 65f -> "B"
            average!! >= 50f -> "C"
            average!! >= 40f -> "D"
            else -> "F"
        }
}
