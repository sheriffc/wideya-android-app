package uk.org.cgatechnologies.wideya.learner_management

object LearnerIdGenerator {
    private val PATTERN = Regex("^(\\d{2})-(\\d{4,})$")

    fun format(emisId: String, academicYear: Int, sequence: Int): String {
        val yearSuffix = academicYear.toString().takeLast(2)
        val paddedSequence = sequence.toString().padStart(4, '0')
        return "$emisId-$yearSuffix-$paddedSequence"
    }

    /**
     * Parse the "$yearSuffix-$paddedSequence" remainder of a learner_id once the
     * known emis_id prefix has already been stripped off by the caller (via a
     * range scan on "$emisId-" .. "$emisId."). Returns null if it doesn't match
     * the expected {yy}-{seq} shape.
     */
    fun parseSuffix(suffix: String): Pair<String, Int>? {
        val match = PATTERN.matchEntire(suffix) ?: return null
        val (yearSuffix, sequence) = match.destructured
        return yearSuffix to sequence.toInt()
    }
}
