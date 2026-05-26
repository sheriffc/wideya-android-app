package uk.org.cgatechnologies.wideya.common.data.entities

import androidx.room.PrimaryKey
import uk.org.cgatechnologies.wideya.common.utils.Utils
import java.util.*

data class PersonFingerprintA(
    @PrimaryKey(autoGenerate = false) val uuid: String = Utils.getUuidOrdered(),
    val person_uuid: String,
    val finger_position_oid: String,
    val fp_a_cbor: String,
    val fp_a_nfiq: Int,
    val updated_at: String? = null,
) {
//    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (javaClass != other?.javaClass) return false
//
//        other as PersonFingerprintA
//
//        if (!fp_a_cbor.contentEquals(other.fp_a_cbor)) return false
//
//        return true
//    }
//
//    override fun hashCode(): Int {
//        return fp_a_cbor.contentHashCode()
//    }
}
