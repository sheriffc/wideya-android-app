package uk.org.cgatechnologies.wideya.analysis

class AnalysisRepository(private val analysisDao: AnalysisDao) {

    suspend fun getSchoolTeacherAttendanceReportByDate(schoolUuid: String, date: String) =
        analysisDao.getSchoolTeacherAttendanceReportByDate(schoolUuid, date)

    suspend fun getTodayTeacherAttendanceReport(schoolUuid: String) =
        analysisDao.getTodayTeacherAttendanceReport(schoolUuid)

    suspend fun getSchoolTeacherAttendanceReportByInterval(schoolUuid: String, startDate: String, endDate: String) =
        analysisDao.getSchoolTeacherAttendanceReportByInterval(schoolUuid, startDate, endDate)

    suspend fun getSchoolLearnerAttendanceReportByDate(schoolUuid: String, date: String) =
        analysisDao.getSchoolLearnerAttendanceReportByDate(schoolUuid, date)

    suspend fun getTodayLearnerAttendanceReport(schoolUuid: String) =
        analysisDao.getTodayLearnerAttendanceReport(schoolUuid)

    suspend fun getSchoolLearnerAttendanceReportByInterval(schoolUuid: String, startDate: String, endDate: String) =
        analysisDao.getSchoolLearnerAttendanceReportByInterval(schoolUuid, startDate, endDate)

    fun getSchoolLearnerDisabilityReport(schoolUuid: String) = analysisDao.getSchoolLearnerDisabilityReport(schoolUuid)
    fun getSchoolLearnerDisabilityByQuery(schoolUuid: String, query: String?) =
        analysisDao.getSchoolLearnerDisabilityByQuery(schoolUuid, query)

    fun getSchoolLearnerDisabilityByRawQuery(schoolUuid: String, query: String) =
        analysisDao.buildSchoolLearnerDisabilityByRawQuery(schoolUuid, query)

    companion object {
        @Volatile
        private var instance: AnalysisRepository? = null

        fun getInstance(analysisDao: AnalysisDao) =
            this.instance ?: synchronized(this) {
                instance ?: AnalysisRepository(analysisDao).also {
                    instance = it
                }
            }
    }
}