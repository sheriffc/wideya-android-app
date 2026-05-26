package uk.org.cgatechnologies.wideya.school_management.models.mappers

import android.content.Context
import uk.org.cgatechnologies.wideya.common.data.entities.PersonAttendance
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.school_management.entities.School
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel
import uk.org.cgatechnologies.wideya.school_management.models.SchoolModel

/**
 * Created by Mohamad Abuzaid on 12/26/2022.
 */

fun SchoolModel.toSchool() = School(
    uuid = uuid,
    name = name,
    school_education_level_oid = education_level_oid,
    emis_id = emis_id,
    wideya_id = wideya_id,
    payroll_sid = payroll_sid,
    waec_id = waec_id,
    fabinc_recordid = fabinc_recordid,
    district_id = district_id,
    chiefdom_id = chiefdom_id,
    section_name = section_name,
    town_name = town_name,
    address = address,
    classrooms_oid = classrooms_oid,
    wash_oids = wash_oids,
    electricity_oids = electricity_oids,
    mno_oids = mno_oids,
    learning_materials_oids = learning_materials_oids,
    receives_feeding = receives_feeding,
    district_office_uuid = district_office_uuid,
    lat = lat,
    lng = lng,
    media_photo_uuid = media_photo_uuid,
    tablet_phone_number = tablet_phone_number,
    active = active,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = deleted_at,
    deleted_by = deleted_by,
    sync_flag = sync_flag
)

fun PersonAttendanceModel.toPersonAttendance(context: Context): PersonAttendance {
    val time = Utils.getISODateTimeUTC()
    return PersonAttendance(
        uuid = uuid!!,
        date = date!!,
        person_uuid = person_uuid,
        entity_type_oid = entity_type_oid ?: "",
        academic_year = academic_year,
        school_uuid = school_uuid,
        school_group_uuid = school_group_uuid,
        attendance_am_status_oid = attendance_am_status_oid,
        attendance_pm_status_oid = attendance_pm_status_oid,
        attendance_status_oid = attendance_status_oid,
        absent_reason_oid = absent_reason_oid,
        absent_reason_other = absent_reason_other,
        lat = lat,
        lng = lng,
        biometric_method_oid = biometric_method_oid,
        biometric_reference = biometric_reference,
        submitted = submitted,
        created_at  = created_at ?: time,
        created_by  = created_by ?: 0,
        updated_at  = updated_at ?: time,
        updated_by  = updated_by ?: 0,
        deleted_at = deleted_at,
        deleted_by = deleted_by,
        sync_flag = sync_flag
    )
}