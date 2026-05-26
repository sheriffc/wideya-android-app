package uk.org.cgatechnologies.wideya.common.viewmodels

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel
import uk.org.cgatechnologies.wideya.common.interfaces.IViewModel
import uk.org.cgatechnologies.wideya.school_management.SchoolManagementViewModel
import uk.org.cgatechnologies.wideya.teacher_management.TeacherManagementViewModel

/**
 * Created by Mohamad Abuzaid on 1/26/2023.
 */
object IViewModelFactory {
    fun getCameraViewModel(fragment: Fragment, type: Int): IViewModel.ICameraViewModel {
        return when (type) {
            PhotoModel.BIOMETRIC_PHOTO -> {
                val schoolManagementViewModel by fragment.activityViewModels<SchoolManagementViewModel>()
                schoolManagementViewModel
            }
            else -> {
                val teacherManagementViewModel by fragment.activityViewModels<TeacherManagementViewModel>()
                teacherManagementViewModel
            }
        }
    }
}