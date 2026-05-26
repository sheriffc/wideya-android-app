package uk.org.cgatechnologies.wideya.teacher_management.models.mappers

import uk.org.cgatechnologies.wideya.common.data.entities.Person
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.teacher_management.entities.Teacher
import uk.org.cgatechnologies.wideya.teacher_management.entities.TeacherTimetable
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherModel
import uk.org.cgatechnologies.wideya.teacher_management.models.TeacherTimetableModel

/**
 * Created by Mohamad Abuzaid on 12/26/2022.
 */
fun TeacherModel.toTeacher() = Teacher(
    uuid = uuid,
    person_uuid = person_uuid!!,
    school_uuid = school_uuid,
    employment_status_oid = employment_status_oid,
    teacher_role_oid = teacher_role_oid,
    pin = pin,
    nassit_number = nassit_number,
    tsc_licence_id = tsc_licence_id,
    start_date = start_date,
    end_date = end_date,
    end_reason_teacher_oid = end_reason_oid,
    end_reason_teacher_other = end_reason_other,
    end_reason_teacher_detail = end_reason_detail,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = deleted_at,
    deleted_by = deleted_by,
    sync_flag = sync_flag
)

fun TeacherModel.toPerson() = Person(
    uuid = person_uuid.toString(),
    last_name = last_name,
    middle_name = middle_name,
    first_name = first_name,
    sex_oid = sex_oid,
    date_of_birth = date_of_birth,
    nin = nin,
    portrait_uuid = portrait_uuid,
    phone_1 = phone_1,
    phone_2 = phone_2,
    email = email,
    address = address,
    fp_lt_uuid = fp_lt_uuid,
    fp_li_uuid = fp_li_uuid,
    fp_rt_uuid = fp_rt_uuid,
    fp_ri_uuid = fp_ri_uuid,
    created_at = person_created_at ?: Utils.getISODateTimeUTC(),
    created_by = person_created_by,
    updated_at = person_updated_at ?: Utils.getISODateTimeUTC(),
    updated_by = person_updated_by,
    deleted_at = person_deleted_at,
    deleted_by = person_deleted_by,
    sync_flag = sync_flag
)

fun TeacherTimetableModel.toTeacherTimetable() = TeacherTimetable(
    uuid = uuid,
    teacher_uuid = teacher_uuid,
    school_uuid = school_uuid!!,
    school_group_uuid = school_group_uuid,
    school_subject_oid = school_subject_oid,
    school_subject_other = school_subject_other,
    day_of_the_week_oid = day_of_the_week_oid!!,
    start_time = start_time,
    end_time = end_time,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = deleted_at,
    deleted_by = deleted_by,
    sync_flag = sync_flag
)