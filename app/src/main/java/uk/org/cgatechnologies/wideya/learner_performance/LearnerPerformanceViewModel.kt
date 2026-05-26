package uk.org.cgatechnologies.wideya.learner_performance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.utils.Utils
import uk.org.cgatechnologies.wideya.learner_performance.entities.LearnerPerformance
import uk.org.cgatechnologies.wideya.learner_performance.models.LearnerListItemModel
import uk.org.cgatechnologies.wideya.learner_performance.models.SchoolGroupDropdownModel
import uk.org.cgatechnologies.wideya.learner_performance.models.SubjectAssessmentItem
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper

class LearnerPerformanceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao: LearnerPerformanceDao =
        AppDatabase.getInstance()!!.learnerPerformanceDao()

    private val _schoolGroups = MutableStateFlow<List<SchoolGroupDropdownModel>>(emptyList())
    val schoolGroups: StateFlow<List<SchoolGroupDropdownModel>> = _schoolGroups

    private val _learnerList = MutableStateFlow<List<LearnerListItemModel>>(emptyList())
    val learnerList: StateFlow<List<LearnerListItemModel>> = _learnerList

    private val _subjectAssessments = MutableStateFlow<List<SubjectAssessmentItem>>(emptyList())
    val subjectAssessments: StateFlow<List<SubjectAssessmentItem>> = _subjectAssessments

    var currentLearner: LearnerListItemModel? = null
        set(value) {
            field = value
            _subjectAssessments.value = emptyList()
        }

    fun loadSchoolGroups(schoolUuid: String, academicYear: Short) {
        viewModelScope.launch(Dispatchers.IO) {
            _schoolGroups.value = dao.getSchoolGroupsForSchool(schoolUuid, academicYear)
        }
    }

    fun loadAllLearners(schoolUuid: String, academicYear: Short) {
        viewModelScope.launch(Dispatchers.IO) {
            _learnerList.value = dao.getAllLearnersForSchool(schoolUuid, academicYear)
        }
    }

    fun loadLearnersByGroup(schoolGroupUuid: String, academicYear: Short) {
        viewModelScope.launch(Dispatchers.IO) {
            _learnerList.value = dao.getLearnersByGroup(schoolGroupUuid, academicYear)
        }
    }

    fun loadSubjectAssessments(learner: LearnerListItemModel, termOid: String, academicYear: Short) {
        viewModelScope.launch(Dispatchers.IO) {
            val levelOid = learner.school_group_level_oid ?: return@launch
            val groupUuid = learner.school_group_uuid ?: return@launch

            val educationLevelOid = dao.getEducationLevelForGroupLevel(levelOid) ?: return@launch
            val subjects = AppDatabase.getInstance()!!.commonDao()
                .getSchoolSubjectsBySchoolEducationLevel(educationLevelOid)

            val existing = dao.getExistingPerformanceForLearner(
                learnerUuid = learner.learner_uuid,
                schoolGroupUuid = groupUuid,
                termOid = termOid,
                academicYear = academicYear
            ).associateBy { it.subject_oid }

            _subjectAssessments.value = subjects.map { subject ->
                val rec = existing[subject.item_id.orEmpty()]
                SubjectAssessmentItem(
                    subject_oid        = subject.item_id.orEmpty(),
                    subject_name       = subject.item_name.orEmpty(),
                    existing_uuid      = rec?.existing_uuid,
                    assessment_1_score = rec?.assessment_1_score,
                    assessment_2_score = rec?.assessment_2_score,
                    max_score          = rec?.max_score ?: 100f
                )
            }
        }
    }

    fun saveAssessments(
        schoolUuid: String,
        termOid: String,
        academicYear: Short,
        items: List<SubjectAssessmentItem>
    ) {
        val learner = currentLearner ?: return
        val groupUuid = learner.school_group_uuid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val now = Utils.getISODateTimeUTC()
            val userId = Utils.getUserId(getApplication())

            items.forEach { item ->
                if (item.assessment_1_score == null && item.assessment_2_score == null) return@forEach

                val record = LearnerPerformance(
                    uuid               = item.existing_uuid ?: Utils.getUuidOrdered(),
                    school_uuid        = schoolUuid,
                    school_group_uuid  = groupUuid,
                    learner_uuid       = learner.learner_uuid,
                    subject_oid        = item.subject_oid,
                    academic_year      = academicYear,
                    term_oid           = termOid,
                    assessment_1_score = item.assessment_1_score,
                    assessment_2_score = item.assessment_2_score,
                    max_score          = item.max_score,
                    created_at         = now,
                    created_by         = userId,
                    updated_at         = now,
                    updated_by         = userId,
                    sync_flag          = 1
                )
                dao.insertOrUpdatePerformance(record)
            }

            WorkerHelper.initUniqueUploadWorkRequest(getApplication())
        }
    }
}
