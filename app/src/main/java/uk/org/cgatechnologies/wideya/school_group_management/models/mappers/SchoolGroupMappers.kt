package uk.org.cgatechnologies.wideya.school_group_management.models

import uk.org.cgatechnologies.wideya.school_group_management.entities.SchoolGroup

/**
 * Created by Mohamad Abuzaid on 12/26/2022.
 */
fun SchoolGroupModel.toSchoolGroup() = SchoolGroup(
    uuid = uuid,
    school_uuid = school_uuid.toString(),
    academic_year  = academic_year ?: 0,
    teacher_uuid = teacher_uuid,
    school_group_name  = school_group_name ?: "",
    school_group_level_oid = school_group_level_oid,
    active  = active ?: 1,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = deleted_at,
    deleted_by = deleted_by,
    sync_flag = sync_flag
)