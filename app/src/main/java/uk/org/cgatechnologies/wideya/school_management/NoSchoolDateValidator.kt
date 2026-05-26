package uk.org.cgatechnologies.wideya.school_management

import android.os.Parcel
import android.os.Parcelable
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker

/**
 * Allows only dates >= today that are not in [blockedMillis] (dates with submitted attendance).
 */
class NoSchoolDateValidator(private val blockedMillis: Set<Long>) :
    CalendarConstraints.DateValidator {

    constructor(parcel: Parcel) : this(
        parcel.createLongArray()?.toHashSet() ?: hashSetOf()
    )

    override fun isValid(date: Long): Boolean {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        return date >= today && date !in blockedMillis
    }

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLongArray(blockedMillis.toLongArray())
    }

    companion object CREATOR : Parcelable.Creator<NoSchoolDateValidator> {
        override fun createFromParcel(parcel: Parcel) = NoSchoolDateValidator(parcel)
        override fun newArray(size: Int) = arrayOfNulls<NoSchoolDateValidator>(size)
    }
}
