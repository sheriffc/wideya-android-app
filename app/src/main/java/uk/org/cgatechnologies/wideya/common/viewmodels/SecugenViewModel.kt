package uk.org.cgatechnologies.wideya.common.viewmodels

import SecuGen.FDxSDKPro.*
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.machinezoo.sourceafis.FingerprintImage
import com.machinezoo.sourceafis.FingerprintImageOptions
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate
import kotlinx.coroutines.launch
import uk.org.cgatechnologies.wideya.MainActivity
import uk.org.cgatechnologies.wideya.common.utils.Utils.base64ToByteArray
import uk.org.cgatechnologies.wideya.common.data.CommonRepository
import uk.org.cgatechnologies.wideya.common.data.Constants
import uk.org.cgatechnologies.wideya.common.data.database.AppDatabase
import uk.org.cgatechnologies.wideya.common.data.entities.PersonFingerprint
import java.io.*
import java.nio.ByteBuffer
import java.io.File.separator as SEPARATOR

class SecugenViewModel(application: Application): AndroidViewModel(application) {
//class SecugenViewModel: ViewModel() {
    val TAG = "SecugenViewModel"
    lateinit var sgfplib: JSGFPLib
    var bSecuGenDeviceOpened: Boolean = false
    val IMAGE_CAPTURE_TIMEOUT_MS = 10000
    val IMAGE_CAPTURE_QUALITY = 50
    var mImageWidth = 300
    var mImageHeight = 400
    var mImageDPI = 500

    var dwTimeStart: Long = 0
    var dwTimeEnd: Long = 0
    var dwTimeElapsed: Long = 0
    var fpCborA: ByteArray = byteArrayOf()
    var fpBufferA: ByteArray = byteArrayOf()
    var fpBitmapA: Bitmap = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    var nfiqA: Long = 0
    var fpCborB: ByteArray = byteArrayOf()
    var fpBufferB: ByteArray = byteArrayOf()
    var fpBitmapB: Bitmap = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888)
    var nfiqB: Long = 0
    var configChanged: Boolean = false
    var fpPositionA: String = ""

    var candidates = mutableListOf<Fp>()

    private val commonRepository: CommonRepository

    init{
        Log.d(TAG, "view model created")
        this.commonRepository =
            AppDatabase
                .getDatabase(application, viewModelScope)
                .commonDao()
                .let { dao ->
                    CommonRepository.getInstance(dao)
                }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "view model cleared")
    }

    fun setCandidates(){
        candidates.clear()
        val allFps = commonRepository.getAllPersonFingerprints()
        allFps.forEach {

            try {
                val fTemplate = FingerprintTemplate(it.fp_a_cbor.base64ToByteArray())
                candidates.add(
                    Fp(
                        it.uuid,
                        fTemplate
                    )
                )
            }catch(e: Exception){
                Log.d(TAG,e.toString())
            }

        }
    }

    fun setCandidatesForPerson(personUuid: String){
        candidates.clear()
        val fps = commonRepository.getPersonFingerprints(personUuid)
        fps.forEach {

            try {
                val fTemplate = FingerprintTemplate(it.fp_a_cbor.base64ToByteArray())

                candidates.add(
                    Fp(
                        it.finger_position_oid,
                        fTemplate
                    )
                )
            }catch(e: Exception){
                Log.d(TAG,e.toString())
            }

        }
    }

    fun addToCandidates(){
        try {
            candidates.add(
                Fp(
                    fpPositionA,
                    FingerprintTemplate(fpCborA)
                )
            )
        }catch(e: Exception){
            Log.d(TAG,e.toString())
        }
    }

    fun initialiseDevice(): Boolean{
        Log.d(TAG,"opening device")
        val secError : Long = sgfplib.OpenDevice(0)
        if (secError == SGFDxErrorCode.SGFDX_ERROR_NONE){
            val deviceInfo = SGDeviceInfoParam()
            val error = sgfplib.GetDeviceInfo(deviceInfo)
            Log.d(TAG, "GetDeviceInfo() ret: ${error} \n")
            mImageWidth = deviceInfo.imageWidth
            mImageHeight = deviceInfo.imageHeight
            mImageDPI = deviceInfo.imageDPI
            bSecuGenDeviceOpened = true
            Log.d(TAG,"retrieving data")
            return true
        }else {
            Log.d(TAG,"error opening device")
            return false
        }
    }

    fun checkScannerConnected(): Boolean{
        //requires sgfplib to be initalised
        val error = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO)
        if (error != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            Log.d(TAG, "error connecting ")
            return false
        } else {
            Log.d(TAG, "device connected")
            return true
        }
    }

    fun checkPermission(device: UsbDevice, activity: MainActivity): Boolean {
        val usbManager = sgfplib.GetUsbManager()
        usbManager?.apply {
            if (sgfplib.GetUsbManager().hasPermission(device)) {
                Log.d(TAG,"device has permission")
                return true
            } else {
                Log.d(TAG,"device doesn't not have permission, requesting")
                requestPermission(device, activity.mPermissionIntent)
                return false
            }
        }
        return false
    }

    private fun saveRaw(filename:String, buffer: ByteArray, activity: Activity){
        activity.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(buffer)
        }
    }


    fun saveRawInDir(dir:String,filename:String, buffer: ByteArray, activity: Activity){

    }

    fun storeImage(filename:String, image: Bitmap, compressFormat: Enum<Bitmap.CompressFormat>, compressQuality: Int, activity: Activity) {
        try {
            val fos = activity.openFileOutput(filename, AppCompatActivity.MODE_PRIVATE)
            image.compress(compressFormat as Bitmap.CompressFormat?, compressQuality, fos)
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun storeImageInDir(dir:String, filename:String, image: Bitmap, compressFormat: Enum<Bitmap.CompressFormat>, compressQuality: Int, activity: Activity) {
        try {
            val context: Context = activity.applicationContext
            val folder = context.filesDir.absolutePath + SEPARATOR + dir
            val subFolder = File(folder)
            if (!subFolder.exists()) {
                subFolder.mkdirs()
            }
            val outputStream = FileOutputStream(File(subFolder, filename))
            image.compress(compressFormat as Bitmap.CompressFormat?, compressQuality, outputStream)
            outputStream.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun toGrayscale(mImageBuffer: ByteArray): Bitmap {
        val Bits = ByteArray(mImageBuffer.size * 4)
        for (i in mImageBuffer.indices) {
            Bits[i * 4 + 2] = mImageBuffer[i]
            Bits[i * 4 + 1] = Bits[i * 4 + 2]
            Bits[i * 4] = Bits[i * 4 + 1] // Invert the source bits
            Bits[i * 4 + 3] = -1 // 0xff, that's the alpha.
        }
        val bmpGrayscale = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888)
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits))
        return bmpGrayscale
    }

    fun afisTemplateExtraction(filename:String, ctx:Context,act:Activity): ByteArray{
        startTimer()
        val file = File(ctx.filesDir, filename)
        val probe = FingerprintTemplate(
            FingerprintImage(
                file.readBytes(),
                FingerprintImageOptions()
                    .dpi(500.0)
            )
        )
        endTimer("afis extraction")
        startTimer()
        saveRaw("${file.nameWithoutExtension}.cbor",probe.toByteArray(),act)
        endTimer("save template afis")
        return probe.toByteArray()
    }

    fun saveFingerprintsEncrypted(fileNameToRead: String, fileNameToWrite: String, context:Context){

        val mainKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val dir = Constants.PATH_FINGERPRINTS

        val folder = context.filesDir.absolutePath + SEPARATOR + dir

        val subFolder = File(folder)

        if (!subFolder.exists()) {
            subFolder.mkdirs()
        }

        val fileTarget = File(subFolder,fileNameToWrite)

        if(fileTarget.exists()) fileTarget.delete()

        val encryptedFile = EncryptedFile.Builder(
            context,
            fileTarget,
            mainKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val fileContent = File(context.filesDir,fileNameToRead).readBytes()

        encryptedFile.openFileOutput().apply {
            write(fileContent)
            flush()
            close()
        }
    }

    fun deleteFile(filename: String, ctx:Context){
        val folderPath = ctx.filesDir.absolutePath
        val folder = File(folderPath)
        val file = File(folder, filename)

        file.delete()
    }

    fun insertFingerPrint(personFingerprint: PersonFingerprint){
        viewModelScope.launch {
            commonRepository.insertPersonFingerprint(personFingerprint)
        }
    }

    fun sourceAfisMatch(ctx: Context,filenameA:String, filenameB:String): Double{

        val fileA = File(ctx.filesDir, filenameA)
        val serializedA: ByteArray = fileA.readBytes()
        val templateA = FingerprintTemplate(serializedA)

        val fileB = File(ctx.filesDir, filenameB)
        val serializedB: ByteArray = fileB.readBytes()
        val templateB = FingerprintTemplate(serializedB)

        return FingerprintMatcher(templateA)
            .match(templateB)
    }

    fun setBitmapA(mImageBuffer: ByteArray){
        fpBitmapA = toGrayscale(mImageBuffer)
    }

    fun setBitmapB(mImageBuffer: ByteArray){
        fpBitmapB = toGrayscale(mImageBuffer)
    }

    fun startTimer(){
        dwTimeStart = System.currentTimeMillis()
    }

    fun endTimer(title: String = "") {
        dwTimeEnd = System.currentTimeMillis()
        var newtitle = ""
        if(title.length>0){
            newtitle = "(${title}) "
        }
        Log.d(TAG,"${newtitle}time elapsed: ${dwTimeEnd - dwTimeStart} \n")
    }
}

data class Fp(val uuid: String, val template: FingerprintTemplate)