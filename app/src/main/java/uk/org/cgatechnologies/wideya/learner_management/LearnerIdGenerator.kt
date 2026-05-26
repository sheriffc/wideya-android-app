package uk.org.cgatechnologies.wideya.learner_management

object LearnerIdGenerator {
    fun format(emisId: String, academicYear: Int, sequence: Int): String {
        val yearSuffix = academicYear.toString().takeLast(2)
        val paddedSequence = sequence.toString().padStart(4, '0')
        return "$emisId-$yearSuffix-$paddedSequence"
    }
}
