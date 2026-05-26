package uk.org.cgatechnologies.wideya.learner_management.models

import uk.org.cgatechnologies.wideya.common.data.entities.Person
import uk.org.cgatechnologies.wideya.learner_management.entities.Learner
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerAdmission
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolLearnerEnrolment
import uk.org.cgatechnologies.wideya.school_management.models.LearnerAdmissionModel

/**
 * Created by Mohamad Abuzaid on 12/26/2022.
 */
fun LearnerAdmissionModel.toLearner() = Learner(
    uuid = learner_uuid!!,
    person_uuid = learner_person_uuid!!,
    learner_id = learner_id,
    language_oid_strongest = language_oid_strongest,
    maternal_status_oid = learner_maternal_status_oid,
    maternal_status_updated_at = learner_maternal_status_updated_at,
    disability_severity_oid_vision = learner_disability_severity_oid_vision,
    disability_severity_oid_hearing = learner_disability_severity_oid_hearing,
    disability_severity_oid_mobility = learner_disability_severity_oid_mobility,
    disability_severity_oid_cognition = learner_disability_severity_oid_cognition,
    disability_severity_oid_selfcare = learner_disability_severity_oid_selfcare,
    disability_severity_oid_communication = learner_disability_severity_oid_communication,
    disability_other_condition_oid = learner_disability_other_condition_oid,
    guardian_person_uuid = guardian_person_uuid,
    guardian_relation_to_learner_oid = guardian_relation_to_learner_oid,
    guardian_relation_to_learner_other = guardian_relation_to_learner_other,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = null,
    deleted_by = null,
    sync_flag = sync_flag
)

fun LearnerAdmissionModel.toPerson() = Person(
    uuid = learner_person_uuid.toString(),
    last_name = learner_last_name,
    middle_name = learner_middle_name,
    first_name = learner_first_name,
    sex_oid = learner_sex_oid,
    date_of_birth = learner_date_of_birth,
    nin = learner_nin,
    portrait_uuid = learner_portrait_uuid,
    phone_1 = null,
    phone_2 = null,
    email = null,
    address = null,
    fp_lt_uuid = null,
    fp_li_uuid = null,
    fp_rt_uuid = null,
    fp_ri_uuid = null,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = null,
    deleted_by = null,
    sync_flag = sync_flag
)

fun LearnerAdmissionModel.toGuardianPerson() = Person(
    uuid = guardian_person_uuid.toString(),
    last_name = guardian_last_name,
    middle_name = guardian_middle_name,
    first_name = guardian_first_name,
    sex_oid = guardian_sex_oid,
    date_of_birth = guardian_date_of_birth,
    nin = guardian_nin,
    portrait_uuid = guardian_portrait_uuid,
    phone_1 = guardian_phone_1,
    phone_2 = guardian_phone_2,
    email = guardian_email,
    address = guardian_address,
    fp_lt_uuid = null,
    fp_li_uuid = null,
    fp_rt_uuid = null,
    fp_ri_uuid = null,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = null,
    deleted_by = null,
    sync_flag = sync_flag
)

fun LearnerAdmissionModel.toSchoolLearnerAdmission() = SchoolLearnerAdmission(
    uuid = uuid,
    school_uuid = school_uuid!!,
    learner_uuid = learner_uuid!!,
    admission_number = admission_number,
    start_date = start_date,
    end_date = end_date,
    end_reason_learner_oid = end_reason_learner_oid,
    end_reason_learner_other = end_reason_learner_other,
    end_reason_learner_detail = end_reason_learner_detail,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = deleted_at,
    deleted_by = deleted_by,
    sync_flag = sync_flag
)

fun LearnerAdmissionModel.toSchoolLearnerEnrolment() = SchoolLearnerEnrolment(
    uuid = enrolment_uuid!!,
    academic_year = enrolment_current_academic_year!!.toShort(),
    learner_uuid = learner_uuid!!,
    school_group_uuid = enrolment_school_group_uuid,
    created_at = created_at,
    created_by = created_by,
    updated_at = updated_at,
    updated_by = updated_by,
    deleted_at = null,
    deleted_by = null,
    sync_flag = sync_flag,
)