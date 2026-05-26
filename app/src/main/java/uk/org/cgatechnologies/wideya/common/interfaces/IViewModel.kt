package uk.org.cgatechnologies.wideya.common.interfaces

import kotlinx.coroutines.flow.StateFlow
import android.graphics.Bitmap
import uk.org.cgatechnologies.wideya.common.camera.models.PhotoModel

/**
 * Created by Mohamad Abuzaid on 1/26/2023.
 */

sealed interface IViewModel {
    interface ICameraViewModel : IViewModel {
        var portraitBinariesDelete: Boolean
        var tempPhotoPath: String?
        var currentPortraitThumbnail: Bitmap?
        var currentPortraitOrientation: Int

        fun getPortraitBinaries(): String?
        fun setLatestCameraPhoto(photo: PhotoModel?)
        fun deletePhotoCache(storageDirPath: String)
    }

    interface IMultiSelectorVM: IViewModel {
        val multiSelectStateFlow: StateFlow<MutableSet<Any>>

        fun updateMultiSelectStateFlow(item: Any, selected: Boolean)
        fun updateMultiSelectStateFlowByMany(items: List<Any>, selected: Boolean)
        fun clearMultiSelectStateFlow()
    }
}
