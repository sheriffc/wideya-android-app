package uk.org.cgatechnologies.wideya.common.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.fragment.app.Fragment
import uk.org.cgatechnologies.wideya.common.utils.Utils.byteArrayToBase64
import java.io.ByteArrayOutputStream

/**
 * Created by Mohamad Abuzaid on 12/23/2022.
 */
object Extensions {
    fun Bitmap.rotateBitmap(angle: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(angle.toFloat())
        return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
    }

    fun Bitmap.toBase64(): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray().byteArrayToBase64()
    }

    fun Int.nextOrientationAngle(): Int {
        return when (this) {
            0 -> 90
            90 -> 180
            180 -> 270
            270 -> 0
            else -> 0
        }
    }

    //cipher for orientation

    fun Int.orientationAngleToSingleDigit(): Int {
        return when (this) {
            0 -> 1
            90 -> 2
            180 -> 3
            270 -> 4
            else -> 0
        }
    }

    fun Int.singleDigitToOrientationAngle(): Int {
        return when (this) {
            1 -> 0
            2 -> 90
            3 -> 180
            4 -> 270
            else -> 0
        }
    }

    fun <T : Any> MutableSet<T>.addToFlow(item: T): MutableSet<T> {
        val updatedList = this.toMutableSet()
        updatedList.add(item)
        return updatedList
    }

    fun <T : Any> MutableSet<T>.addAllToFlow(items: List<T>): MutableSet<T> {
        val updatedList = this.toMutableSet()
        updatedList.addAll(items)
        return updatedList
    }

    fun <T : Any> MutableSet<T>.removeFromFlow(item: T): MutableSet<T> {
        val updatedList = this.toMutableSet()
        updatedList.remove(item)
        return updatedList
    }

    fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }
}