package uk.org.cgatechnologies.wideya.sync_device.models

import uk.org.cgatechnologies.wideya.sync_device.SyncDao

data class TableState(
    var rec_count: Int,
    var max_pk: String?,
    var max_synced_at: String?,
){
    companion object {
        val uniNonPayrollTables by lazy {
            listOf("non_payroll_teachers")
        }
        val uniTablesWithUuid by lazy {
            listOf("district_office","school_academic_year","teacher_payroll","learner")
        }
        val uniTablesWithId by lazy {
            listOf("option_list","option_list_link","geo")
        }
        val biConfig by lazy {
            listOf(
                Pair("person",SyncDao.SyncQueries.TABLE_STATE_PERSON),
                Pair("person_attendance",SyncDao.SyncQueries.TABLE_STATE_PERSON_ATTENDANCE),
                Pair("school",SyncDao.SyncQueries.TABLE_STATE_SCHOOL),
                Pair("school_group",SyncDao.SyncQueries.TABLE_STATE_SCHOOL_GROUP),
                Pair("school_learner_admission",SyncDao.SyncQueries.TABLE_STATE_SCHOOL_LEARNER_ADMISSION),
                Pair("school_learner_enrolment",SyncDao.SyncQueries.TABLE_STATE_SCHOOL_LEARNER_ENROLMENT),
                Pair("teacher",SyncDao.SyncQueries.TABLE_STATE_TEACHER),
                Pair("person_fingerprint",SyncDao.SyncQueries.TABLE_STATE_PERSON_FINGERPRINT),
                Pair("media_photo",SyncDao.SyncQueries.TABLE_STATE_MEDIA_PHOTO),
                Pair("teacher_timetable",SyncDao.SyncQueries.TABLE_STATE_TEACHER_TIMETABLE),
                Pair("school_feeding",SyncDao.SyncQueries.TABLE_STATE_SCHOOL_FEEDING),
                Pair("school_feeding_stock",SyncDao.SyncQueries.TABLE_STATE_SCHOOL_FEEDING_STOCK),
                Pair("learner_performance",SyncDao.SyncQueries.TABLE_STATE_LEARNER_PERFORMANCE),
            )
        }
    }
}
