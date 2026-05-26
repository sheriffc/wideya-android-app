package uk.org.cgatechnologies.wideya.teacher_management

import androidx.annotation.WorkerThread
import androidx.sqlite.db.SimpleSQLiteQuery
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherTimetable

class TeacherManagementRepository(private val teacherManagementDao: TeacherManagementDao) {

    fun getTeacher(uuid: String) = teacherManagementDao.getTeacherModel(uuid)

    @WorkerThread
    suspend fun insertTeacher(teacher: Teacher) = teacherManagementDao.insertTeacher(teacher)

    @WorkerThread
    suspend fun updateTeacher(teacher: Teacher) = teacherManagementDao.updateTeacher(teacher)

    @WorkerThread
    suspend fun deleteTeacher(teacher: Teacher) = teacherManagementDao.deleteTeacher(teacher)

    //Payroll Teacher
    @WorkerThread
    fun getPayrollTeacherList(uuid: String?) = teacherManagementDao.getPayrollTeacherList(uuid)

    @WorkerThread
    fun getPayrollTeacherListByQuery(uuid: String?, query: String?) =
        teacherManagementDao.getPayrollTeacherListByQuery(uuid, query)

    @WorkerThread
    fun getPayrollTeacherListByRawQuery(uuid: String, query: String) =
        teacherManagementDao.buildPayrollTeacherListByRawQuery(uuid, query)

    //Non-Payroll Teacher
    @WorkerThread
    fun getNonPayrollTeacherList(schoolUuid: String?) = teacherManagementDao.getNonPayrollTeacherList(schoolUuid)

    @WorkerThread
    fun getNonPayrollTeacherListByRawQuery(schoolUuid: String, query: String) =
        teacherManagementDao.buildNonPayrollTeacherListByRawQuery(schoolUuid, query)

    @WorkerThread
    fun getTeacherTimetableList(uuid: String?) = teacherManagementDao.getTeacherTimetableList(uuid)

    @WorkerThread
    suspend fun insertTimetableEntry(timetable: TeacherTimetable) = teacherManagementDao.insertTimetableEntry(timetable)
    @WorkerThread
    suspend fun updateTimetableEntry(timetable: TeacherTimetable) = teacherManagementDao.updateTimetableEntry(timetable)
    @WorkerThread
    suspend fun deleteTimetableEntry(timetable: TeacherTimetable) = teacherManagementDao.deleteTimetableEntry(timetable)

    companion object {
        @Volatile
        private var instance: TeacherManagementRepository? = null

        fun getInstance(teacherManagementDao: TeacherManagementDao) =
            this.instance ?: synchronized(this) {
                instance ?: TeacherManagementRepository(teacherManagementDao).also {
                    instance = it
                }
            }
    }
}