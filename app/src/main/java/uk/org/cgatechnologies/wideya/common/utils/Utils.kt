package uk.org.cgatechnologies.wideya.common.utils

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fasterxml.uuid.Generators
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import uk.org.cgatechnologies.wideya.BuildConfig
import uk.org.cgatechnologies.wideya.R
import uk.org.cgatechnologies.wideya.common.camera.PhotoUtilsDialogFragment
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.OptionList
import uk.org.cgatechnologies.wideya.common.dialogfragments.AppUpdateDialog
import uk.org.cgatechnologies.wideya.common.dialogfragments.SelectOneDialogFragment
import uk.org.cgatechnologies.wideya.school_management.entities.SchoolAcademicYear
import uk.org.cgatechnologies.wideya.school_management.models.PersonAttendanceModel
import uk.org.cgatechnologies.wideya.sync_device.entities.LogSync
import uk.org.cgatechnologies.wideya.sync_device.workers.WorkerHelper
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


private const val TAG = "Utils"

object Utils {

    fun getUuidOrdered(): String {
//        return Generators.timeBasedGenerator().generate().toString() //version 1
        return Generators.timeBasedEpochGenerator().generate().toString() //version 7
    }
    fun getUuidRandom(): String {
        return Generators.randomBasedGenerator().generate().toString()
    }

    fun generateInstallId(context: Context) {
        val sp = getEncSharedPrefs(context)
        if (sp.contains(Constants.PREF_INSTALL_ID).not()) {
            sp.edit()
                .putString(Constants.PREF_INSTALL_ID, getUuidRandom())
                .apply()
        }
        Log.d(
            TAG, "installId: ${
                sp.getString(Constants.PREF_INSTALL_ID, "No Install ID Saved")
            }"
        )
    }

    fun getInstallId(context: Context): String {
        return getEncSharedPrefs(context).getString(Constants.PREF_INSTALL_ID, "").toString()
    }

    fun getISODateUTC(): String {
        return getISODateUTC(Date())
    }

    private fun getISODateUTC(d: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return dateFormat.format(d)
    }

    fun getISODateTimeUTC(): String {
        return getISODateTimeUTC(Date())
    }

    fun getTempPhotoPath(context: Context): String =
        "${context.filesDir.absolutePath}${File.separator}${Constants.PATH_TMP}"

    fun getAttendancePhotoPath(context: Context): String =
        "${context.filesDir.absolutePath}${File.separator}${Constants.PATH_TEACHER_ATT_PHOTOS}"

    fun getProfilePhotoPath(context: Context): String =
        "${context.filesDir.absolutePath}${File.separator}${Constants.PATH_TEACHER_PROFILE_PHOTOS}"

    fun getISODateTimeUTC(d: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(d)
    }

    fun getISODateTimeUTCFileFriendly(): String {
        return getISODateTimeUTCFileFriendly(Date())
    }

    private fun getISODateTimeUTCFileFriendly(d: Date): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ENGLISH)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(d)
    }

    fun getISODateUTCFromMs(input: Long): String {
        val date = Date(input)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        return formatter.format(date)
    }

    fun getYearFromMs(input: Long): String {
        val date = Date(input)
        val formatter = SimpleDateFormat("yyyy", Locale.ENGLISH)
        return formatter.format(date)
    }

    /**
     * Sets a text input field as editable, specifically setting clickable, longClickable,
     * focusable, and focusableInTouchMode. Also sets background color if textInputLayout is passed.
     *
     * @param editText the EditText view to be made editable.
     * @param textInputLayout (optional) if textInputLayout is passed then its background color will be set
     * to the editable color.
     */
    fun makeTextInputEditable(editText: EditText, textInputLayout: TextInputLayout? = null) {
        editText.apply {
            isClickable = true
            isLongClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        textInputLayout?.let {
            setViewBackgroundColorToEditable(it)
        }
    }

    fun setViewBackgroundColorToEditable(view: View) {
        view.apply {
            setBackgroundColor(ResourcesCompat.getColor(resources, R.color.editable_area, null))
        }
    }

    fun getReadableCurrentDayDate(): String {
        val readableDatePattern = "E, dd MMM yyyy"
        val dateFormat: DateFormat = SimpleDateFormat(readableDatePattern, Locale.ENGLISH)
        val currentDay: Calendar = Calendar.getInstance()
        currentDay.add(Calendar.DAY_OF_MONTH, -0)
        return dateFormat.format(currentDay.time)
    }

    fun getReadableLocalDate(date: LocalDate): String {
        val readableDatePattern = "E, dd MMM yyyy"
        val dateFormat = DateTimeFormatter.ofPattern(readableDatePattern)
        return dateFormat.format(date)
    }

    fun getReadableSimpleLocalDate(date: LocalDate): String {
        val readableDatePattern = "E, dd MMM yy"
        val dateFormat = DateTimeFormatter.ofPattern(readableDatePattern)
        return dateFormat.format(date)
    }

    fun initSelectOneWidget(
        propertyName: String,
        view: View,
        title: String,
        list: List<OptionList>,
        childFragmentManager: FragmentManager,
        includeClearButton: String = "yes",
        otherName: String = "",
    ) {
        val et: EditText = view as EditText
        val widget = SelectOneDialogFragment()
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("propertyName", propertyName)
        bundle.putInt("viewId", view.id)
        bundle.putString("otherName", otherName)
        bundle.putString("currentName", et.text.toString())
        bundle.putString("currentId", et.getTag(R.string.idTag)?.toString().orEmpty())
        bundle.putString("includeClearButton", includeClearButton)
        val arrayList = arrayListOf<OptionList>()
        arrayList.addAll(list)
        bundle.putParcelableArrayList("list", arrayList)
        widget.arguments = bundle
        widget.show(childFragmentManager, widget.tag)
    }

    fun initSelectOneWidgetChip(
        propertyName: String,
        view: View,
        title: String,
        list: List<OptionList>,
        personAttendanceModel: PersonAttendanceModel,
        childFragmentManager: FragmentManager
    ) {
        val et: Chip = view as Chip
        val widget = SelectOneDialogFragment()
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("propertyName", propertyName)
        bundle.putInt("viewId", view.id)
        bundle.putString("otherName", personAttendanceModel.absent_reason_other)
        bundle.putString("currentName", et.text.toString())
        bundle.putString("currentId", et.getTag(R.string.idTag)?.toString().orEmpty())
        bundle.putString("includeClearButton", "no")
        val arrayList = arrayListOf<OptionList>()
        arrayList.addAll(list)
        bundle.putParcelableArrayList("list", arrayList)
        bundle.putParcelable("item", personAttendanceModel)
        widget.arguments = bundle
        widget.show(childFragmentManager, widget.tag)
    }

    fun initSelectOneWidgetNoView(
        propertyName: String,
        title: String,
        list: List<OptionList>,
        childFragmentManager: FragmentManager,
        includeClearButton: String = "no"
    ) {
        val widget = SelectOneDialogFragment()
        val bundle = Bundle()
        bundle.putString("title", title)
        bundle.putString("propertyName", propertyName)
        bundle.putString("includeClearButton", includeClearButton)
        val arrayList = arrayListOf<OptionList>()
        arrayList.addAll(list)
        bundle.putParcelableArrayList("list", arrayList)
        widget.arguments = bundle
        widget.show(childFragmentManager, widget.tag)
    }

    fun initCameraFunctionsWidget(
        listener: PhotoUtilsDialogFragment.IPhotoUtilsListener?,
        photoType: Int,
        childFragmentManager: FragmentManager,
        title: String = ""
    ) {
        val widget = PhotoUtilsDialogFragment(listener)
        val bundle = Bundle()
        bundle.putInt(PhotoUtilsDialogFragment.IMAGE_TYPE, photoType)
        bundle.putString(PhotoUtilsDialogFragment.TITLE, title)
        widget.arguments = bundle
        widget.show(childFragmentManager, widget.tag)
        widget.isCancelable = false
    }

    fun initDatePickerDialog(
        context: Context,
        view: View,
        defaultYear: Int,
        ageValueView: TextView? = null
    ) {
        val et: EditText = view as EditText
        val til: TextInputLayout = et.parent.parent as TextInputLayout
        val fieldTitle: String = til.hint.toString()
        val cal = Calendar.getInstance()

        val yearToday = cal.get(Calendar.YEAR)
        val monthToday = cal.get(Calendar.MONTH)
        val dayToday = cal.get(Calendar.DAY_OF_MONTH)

        val yearInput: Int
        val monthInput: Int
        val dayInput: Int

        // TODO: check input date from editText is valid
        if (et.text.toString().isNotEmpty()) {
            val existingDateString = et.text.toString()
            yearInput = existingDateString.substring(0, 4).toIntOrNull() ?: defaultYear
            // Calendar package counts month from 0, so need to offset 1
            monthInput = (existingDateString.substring(5, 7).toIntOrNull() ?: 1) - 1
            dayInput = existingDateString.substring(8, 10).toIntOrNull() ?: 1
        } else if (defaultYear in 1900..2100) {
            yearInput = defaultYear
            monthInput = 0
            dayInput = 1
        } else {
            yearInput = yearToday
            monthInput = monthToday
            dayInput = dayToday
        }

        // optionally also sets a corresponding "Age" TextView alongside the date field
        val datePickerDialog = DatePickerDialog(
            context,
            R.style.PickerDialogCustom,
            { _, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                // left-pad with 0 so that value is "07" instead of "7"
                // Calendar package counts month from 0, so need to offset 1
                val outputYear = String.format("%04d", (year))
                val outputMonth = String.format("%02d", (month + 1))
                val outputDay = String.format("%02d", (day))

                et.setText("$outputYear-$outputMonth-$outputDay")

                // if 'Age' TextView passed in then calculate and set text
                ageValueView?.let {
                    val yearDiff = yearToday - year
                    val monthDiff = monthToday - month
                    val dayDiff = dayToday - day

                    var carryover: Int = if (dayDiff < 0) {
                        -1
                    } else {
                        0
                    }

                    carryover = if ((monthDiff + carryover < 0)) {
                        -1
                    } else {
                        0
                    }

                    val ageRoundedDown = yearDiff + carryover

                    it.text = ageRoundedDown.toString()
                }
            },
            yearInput,
            monthInput,
            dayInput
        )
        datePickerDialog.datePicker.calendarViewShown = false
        datePickerDialog.setTitle(fieldTitle)
        datePickerDialog.setIcon(R.drawable.ic_calendar)
        datePickerDialog.show()

    }

    fun initTimePickerDialog(context: Context, view: View, defaultHour: Int) {
        val et: EditText = view as EditText
        val til: TextInputLayout = et.parent.parent as TextInputLayout
        val fieldTitle: String = til.hint.toString()
        val cal = Calendar.getInstance()

        val hoursNow = cal.get(Calendar.HOUR_OF_DAY)
        val minutesNow = cal.get(Calendar.MINUTE)

        val hoursInput: Int
        val minutesInput: Int

        // TODO: check input date from editText is valid
        if (et.text.toString().isNotEmpty()) {
            val existingTimeString = et.text.toString()
            hoursInput = existingTimeString.substring(0, 2).toIntOrNull() ?: defaultHour
            minutesInput = existingTimeString.substring(3, 5).toIntOrNull() ?: 0
        } else if (defaultHour in 0..23) {
            hoursInput = defaultHour
            minutesInput = 0
        } else {
            hoursInput = hoursNow
            minutesInput = minutesNow
        }

        val timePickerDialog = TimePickerDialog(
            context,
            R.style.PickerDialogCustom,
            { _, hours, minutes ->
                cal.set(Calendar.HOUR_OF_DAY, hours)
                cal.set(Calendar.MINUTE, minutes)

                // left-pad with 0 so that value is "07" instead of "7"
                val outputHours = String.format("%02d", hours)
                val outputMinutes = String.format("%02d", minutes)

                et.setText("$outputHours:$outputMinutes")
            },
            hoursInput,
            minutesInput,
            true
        )
        timePickerDialog.setTitle(fieldTitle)
        timePickerDialog.setIcon(R.drawable.ic_calendar)
        timePickerDialog.show()
    }

    fun ImageView.setTint(@ColorRes colorRes: Int) {
        ImageViewCompat.setImageTintList(
            this,
            ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
        )
    }

    fun trimWhiteSpace(input: TextInputEditText): String {
        return trimWhiteSpaceString(input.text.toString())
    }

    fun trimWhiteSpaceString(input: String): String {
        return input.trim()
    }

    @Synchronized
    fun getEncSharedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            Constants.SP_FILENAME_SETTINGS,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // SharedPrefs is significantly faster to fetch (5ms) than EncSharedPrefs (100ms)
    // so use it for non-sensitive data, especially when performance matters
    @Synchronized
    fun getSharedPrefsState(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.SP_FILENAME_STATE, 0)
    }

    fun copyUserIdFromEncSharedPrefsToSharedPrefs(context: Context) {
        val userId = getUserId(context).toString()
        getSharedPrefsState(context).edit()
            .putString(Constants.PREF_USER_ID, userId)
            .apply()
    }

    fun getUserId(context: Context): Int {
        return getEncSharedPrefs(context).getString(Constants.PREF_USER_ID, "-1")
            ?.toIntOrNull() ?: -1
    }

    fun getLastKnownLat(context: Context): Float? {
        return getEncSharedPrefs(context).getString(Constants.PREF_LOCATION_LAT, "")
            ?.toFloatOrNull()
    }

    fun getLastKnownLng(context: Context): Float? {
        return getEncSharedPrefs(context).getString(Constants.PREF_LOCATION_LNG, "")
            ?.toFloatOrNull()
    }

    fun getAcademicYear(): Int {
        val academicYear: SchoolAcademicYear? =
            AppDatabase.getInstance()?.commonDao()?.getAcademicYear()
        return academicYear?.academic_year ?: 0
    }

    fun getAcademicYearName(): String {
        val academicYear: SchoolAcademicYear? =
            AppDatabase.getInstance()?.commonDao()?.getAcademicYear()
        return academicYear?.academic_year_name ?: ""
    }

    fun clearSavedSchoolPreferences(context: Context) {
        Log.d(TAG, "Prune Workers Data")
        WorkerHelper.pruneWorkersData(context)

        Log.d(TAG, "Clearing Saved School UUID")
        getEncSharedPrefs(context).edit()
            .remove(Constants.PREF_SCHOOL_ID)
            .apply()
    }

    fun logout(context: Context) {
        Log.d(TAG, "Logging User Out")

        Log.d(TAG, "Cancel All SYNC Workers")
        WorkerHelper.cancelBackgroundSync(context)

        Log.d(TAG, "Prune Workers Data")
        WorkerHelper.pruneWorkersData(context)

        getEncSharedPrefs(context).apply {

            val preferencesToPreserve = mutableListOf<Pair<String, String>>()

            preferencesToPreserve.apply {
                add(Pair(Constants.PREF_LAST_USERNAME, getString(Constants.PREF_LAST_USERNAME, "").toString()))
                add(Pair(Constants.PREF_INSTALL_ID, getString(Constants.PREF_INSTALL_ID, "").toString()))
                add(Pair(Constants.PREF_LOCATION_LAT, getString(Constants.PREF_LOCATION_LAT, "").toString()))
                add(Pair(Constants.PREF_LOCATION_LNG, getString(Constants.PREF_LOCATION_LNG, "").toString()))
                add(Pair(Constants.PREF_LOCATION_DATETIME_ACQUIRED, getString(Constants.PREF_LOCATION_DATETIME_ACQUIRED, "").toString()))
            }

            edit().clear().apply()

            preferencesToPreserve.forEach {
                edit().putString(it.first, it.second).apply()
            }

            // wipe user id from SP state??
        }
    }

    fun checkIfLoggedIn(context: Context): Boolean {
        return getEncSharedPrefs(context).getString(Constants.PREF_USERNAME, "")
            .isNullOrEmpty().not()
    }

    fun xorEncrypt(message: String, key: String): String {
        return try {
            val keys: CharArray = key.toCharArray()
            val mesg: CharArray = message.toCharArray()
            val ml = mesg.size
            val kl = keys.size
            val newmsg = CharArray(ml)
            for (i in 0 until ml) {
                newmsg[i] = (mesg[i].code xor keys[i % kl].code).toChar()
            }
            Base64.encodeToString(String(newmsg).toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun xorDecrypt(message: String, key: String): String {
        return try {
            val keys: CharArray = key.toCharArray()
            val mesg: CharArray = String(Base64.decode(message, Base64.DEFAULT)).toCharArray()
            val ml = mesg.size
            val kl = keys.size
            val newmsg = CharArray(ml)
            for (i in 0 until ml) {
                newmsg[i] = (mesg[i].code xor keys[i % kl].code).toChar()
            }
            String(newmsg)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun String.xorDecryptExt(key: String): String = xorDecrypt(this, key)

    fun String.xorEncryptExt(key: String): String = xorEncrypt(this, key)

    //https://stackoverflow.com/questions/60827877/how-can-i-check-internet-connection-in-android-q
    //https://developer.android.com/training/basics/network-ops/reading-network-state
    fun isInternetConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null && cm.getNetworkCapabilities(cm.activeNetwork) != null
    }

    fun insertLog(context: Context, message: String) {
        AppDatabase.getInstance()?.syncDao()
            ?.insertLogEntry(LogSync(0, message, getISODateTimeUTC(), getUserId(context)))
    }

    fun ByteArray.byteArrayToBase64(): String = Base64.encodeToString(this, Base64.DEFAULT)

    fun String.base64ToByteArray(): ByteArray = Base64.decode(this, Base64.DEFAULT)

    fun getVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun getVersionCode(): Int {
        return BuildConfig.VERSION_CODE
    }

    fun getDbVersionCode(): Int {
        return AppDatabase.getInstance()?.openHelper?.readableDatabase?.version ?: 0
    }

    fun getAppId(): String {
        return BuildConfig.APPLICATION_ID
    }

    fun getSdk(): Int {
        return Build.VERSION.SDK_INT
    }

    fun getRelease(): String {
        return Build.VERSION.RELEASE
    }

    fun getSystemUsername(): String {
        return System.getProperty("user.name") ?: ""
    }

    fun getSystemOsName(): String {
        return System.getProperty("os.name") ?: ""
    }

    fun getSystemOsArch(): String {
        return System.getProperty("os.arch") ?: ""
    }

    fun getSystemOsVersion(): String {
        return System.getProperty("os.version") ?: ""
    }

    fun isNewVersionPresent(context: Context): Boolean {
        val newVersionCode =
            getEncSharedPrefs(context).getString(Constants.APP_NEW_VERSION_CODE, "0")
                ?.toIntOrNull() ?: 0
        return (newVersionCode > BuildConfig.VERSION_CODE)
    }

    fun updateDialogCheckAndClear() {
        Log.d(TAG, "Clearing Last Dialog")
        AppUpdateDialog.checkAndClear()
    }

    fun showDialogUpdateAvailable(context: Context, childFragmentManager: FragmentManager) {

        val newVersionCode =
            getEncSharedPrefs(context).getString(Constants.APP_NEW_VERSION_CODE, "0")
                ?.toIntOrNull() ?: 0

        if (newVersionCode == 0) return

        val forceUpdate =
            getEncSharedPrefs(context).getString(Constants.APP_FORCE_UPDATE, "0")
                ?.toIntOrNull() ?: 0

        updateDialogCheckAndClear()
        val widget = AppUpdateDialog()
        val bundle = Bundle()

        bundle.putInt(Constants.APP_THIS_VERSION_CODE, BuildConfig.VERSION_CODE)
        bundle.putInt(Constants.APP_NEW_VERSION_CODE, newVersionCode)
        bundle.putString(
            Constants.APP_DESCRIPTION,
            getEncSharedPrefs(context).getString(Constants.APP_DESCRIPTION, "")
        )
        bundle.putInt(Constants.APP_FORCE_UPDATE, forceUpdate)
        bundle.putString(
            Constants.APP_URI,
            getEncSharedPrefs(context).getString(Constants.APP_URI, "")
        )

        widget.arguments = bundle
        widget.isCancelable = forceUpdate == 0
        widget.show(childFragmentManager, widget.tag)
    }

    fun decryptFileToStream(context: Context, file: File): InputStream {

        val mainKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput()
    }

    fun decodeBase64StringToBitmap(encodedString: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(encodedString, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun getOrientation(photoPath: String): Int {
        try {
            val ei = ExifInterface(photoPath)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                ExifInterface.ORIENTATION_NORMAL -> 0
                else -> 0
            }
        } catch (e: Exception) {
            return -1
        }
    }

    private fun getImageBitmap(
        image: Uri,
        context: Context,
        options: BitmapFactory.Options
    ): Bitmap? {
        val input = try {
            context.contentResolver.openInputStream(image)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }

        return BitmapFactory.decodeStream(input, null, options)
    }

    private fun getImageSize(image: Uri, context: Context): Size {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        getImageBitmap(image, context, options)
        return Size(options.outWidth, options.outHeight)
    }

    fun decodeScaledBitmap(image: Uri, context: Context, REQUIRED_IMAGE_MAX_PX: Int): Bitmap? {
        val rawImageSize = getImageSize(image, context)
//        Log.d(TAG, "raw photo width: ${rawImageSize.width} height: ${rawImageSize.height}")

        val rawImageMaxPx = Integer.max(rawImageSize.width, rawImageSize.height)

        // don't scale the image if it's already below the max px, since then you'd be upscaling the image
        val requiredPx = Integer.min(REQUIRED_IMAGE_MAX_PX, rawImageMaxPx)
        val sampleSize = calculateSampleSize(rawImageMaxPx, requiredPx)

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inDensity = rawImageMaxPx
            inTargetDensity = requiredPx * sampleSize
        }
        val input = context.contentResolver.openInputStream(image)!!
        val bitmap = BitmapFactory.decodeStream(input, null, options)
        // reset density to display bitmap correctly
        bitmap?.density = context.resources.displayMetrics.densityDpi
        return bitmap
    }

    private fun calculateSampleSize(currentWidth: Int, requiredWidth: Int): Int {
        var inSampleSize = 1
        if (currentWidth > requiredWidth) {
            val halfWidth = currentWidth / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps
            // width larger than the requested width
            while (halfWidth / inSampleSize >= requiredWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun dpToPixel(context: Context, dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    ).toInt()

    fun cancelBackButton(fragmentActivity: FragmentActivity?, viewLifecycleOwner: LifecycleOwner) {
        fragmentActivity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {}
            })
    }

    fun getPassphrase(context: Context): ByteArray {
        val passphrase =
            getEncSharedPrefs(context).getString(Constants.PREF_PASSPHRASE, "")
                .toString()
        return passphrase.toByteArray()
    }

    fun infoDialog(context: Context, title: String = "", message: String = "") {
        val builder = MaterialAlertDialogBuilder(context)

        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.dismiss, null)

        val dialog = builder.create()
        dialog.show()
    }

    fun checkForInternet(context: Context): Boolean {
        if (isInternetConnected(context).not()) {
            infoDialog(context, "Error", "Not connected to network")
            return false
        } else {
            return true
        }
    }
}